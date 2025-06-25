/*
 * Copyright (C) 2024 jtalbut
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.co.spudsoft.query.exec.procs.sort;

import uk.co.spudsoft.query.exec.procs.ListReadStream;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A ReadStream wrapper that sorts the input.
 *
 * This must capture the entire stream before outputting anything, so it doesn't 'stream' as such, but the interface is that of a ReadStream.
 *
 * @param <T> the type of data being streamed
 * @author jtalbut
 */
public class SortingStream<T> implements ReadStream<T> {

  private static final Logger logger = LoggerFactory.getLogger(SortingStream.class);

  private final Context context;
  private final FileSystem fileSystem;
  private final Comparator<T> comparator;
  private final SerializeWriteStream.Serializer<T> serializer;
  private final SerializeReadStream.Deserializer<T> deserializer;

  private final String tempDir;
  private final String baseFileName;
  private final long memoryLimit;

  /**
   * Functional interface for evaluating the approximate amount of memory used by an item.
   * @param <T> The type of the item being evaluated.
   */
  public interface MemoryEvaluator<T> {
    /**
     * Return the approximate amount of memory that the argument uses.
     * @param data the item under consideration.
     * @return the approximate amount of memory that the argument uses.
     */
    int sizeof(T data);
  }

  private final MemoryEvaluator<T> memoryEvaluator;

  private final Object lock = new Object();

  private final ReadStream<T> input;
  private int inputCount = 0;

  private Handler<Throwable> exceptionHandler;
  private Handler<T> handler;
  private Handler<Void> endHandler;

  private long demand;
  private boolean emitting;
  private boolean endHandlerCalled;

  private int writeQueueMaxSize;

  private List<T> items = new ArrayList<>();
  private long sizeOfItemsInMemory;

  private final List<String> files = new ArrayList<>();

  final class SourceStream {
    // private Object source;
    private int index;
    private int count;
    private ReadStream<T> input;
    private volatile boolean ended = false;
    private T head;

    private void endHandler(Void nothing) {
      // logger.trace("SourceStream endHandler {}", this);
      this.ended = true;
      synchronized (lock) {
        if (!pending.remove(this)) {
          throw new IllegalStateException("Removal from pending failed for: " + this);
        }
      }
      context.runOnContext(v2 -> {
        processOutputs();
      });
    }

    private void itemHandler(T item) {
      // logger.trace("SourceStream handler: {} {}", this, item);
      ++count;
      this.input.pause();
      synchronized (lock) {
        this.head = item;
        if (!pending.remove(this)) {
          if (ended) {
            return ;
          } else {
            throw new IllegalStateException("Removal from pending failed for: " + this);
          }
        }
        if (!ended) {
          // logger.debug("Before adding {}: {}", item, outputs.stream().map(ss -> ss.head.toString()).collect(Collectors.joining(", ")));
          outputs.add(this);
          // logger.debug("After adding: {}", outputs.stream().map(ss -> ss.head.toString()).collect(Collectors.joining(", ")));
        }
      }
      if (!emitting) {
        context.runOnContext(v -> {
          processOutputs();
        });
      }
    }

    SourceStream(Object source, int index, ReadStream<T> input) {
      this.index = index;   // The ID of the stream, for use in the pending Set
      this.input = input;
      this.input.exceptionHandler(exceptionHandler);
      this.input.endHandler(this::endHandler);
      this.input.handler(this::itemHandler);
    }

    public boolean next() {
      this.head = null;
      if (this.ended) {
        pending.remove(this);
        return false;
      } else {
        this.input.fetch(1);
        return true;
      }
    }

    @Override
    public int hashCode() {
      return this.index;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      @SuppressWarnings("unchecked")
      final SourceStream other = (SourceStream) obj;
      return this.index == other.index;
    }

  }

  // This is just for debug purposes
  private List<SourceStream> outputHolder;
  // SourceStreams should be in one of these two at any time
  // When the SourceStream is empty it is removed from both and not put back
  private Set<SourceStream> pending;
  private PriorityQueue<SourceStream> outputs;

  /**
   * Constructor.
   *
   * @param context The Vert.x context used when asynchronous actions must be scheduled.
   * @param fileSystem The Vert.x filesystem used for managing data when it is too large to fit in memory.
   * @param comparator The comparator used to sort items
   * @param serializer The functional class for converting items into byte arrays that can be written to disc.
   * @param deserializer The functional class for converting byte arrays from disc into items for the output stream.
   * @param tempDir The temporary directory to use for writing sorted files to be merged later - if the data is too big to fit in memory.
   * @param baseFileName The base name to use for temporary files.
   * @param memoryLimit The approximate maximum amount of memory to use for storing items in memory before writing them to disc.
   * @param memoryEvaluator The functional class used to calculate the approximate amount of memory used by a single item.
   * @param input The input stream.
   */
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"}, justification = "CT_CONSTRUCTOR_THROW is false positive")
  public SortingStream(
          Context context
          , FileSystem fileSystem
          , Comparator<T> comparator
          , SerializeWriteStream.Serializer<T> serializer
          , SerializeReadStream.Deserializer<T> deserializer
          , String tempDir
          , String baseFileName
          , long memoryLimit
          , MemoryEvaluator<T> memoryEvaluator
          , ReadStream<T> input
  ) {
    this.context = context;
    this.fileSystem = fileSystem;
    this.comparator = comparator;
    this.serializer = serializer;
    this.deserializer = deserializer;
    this.tempDir = tempDir;
    this.baseFileName = baseFileName;
    this.memoryLimit = memoryLimit;
    this.memoryEvaluator = memoryEvaluator;

    this.input = input;
    input.endHandler(this::inputEndHandler);
    input.exceptionHandler(this::inputExceptionHandler);
    input.handler(this::inputHandler);
  }

  private void postException(Throwable ex) {
    logger.warn("Exception in SortingStream: ", ex);
    Handler<Throwable> exceptionHandlerCaptured;
    synchronized (lock) {
      exceptionHandlerCaptured = exceptionHandler;
    }
    if (exceptionHandlerCaptured != null) {
      exceptionHandlerCaptured.handle(ex);
    }
  }

  private void processOutputs() {
    boolean done = false;
    while (!done) {
      T next = null;
      Handler<T> handlerCaptured = null;
      Handler<Void> endHandlerCaptured = null;

      synchronized (lock) {
//        logger.debug("processOutputs: demand: {}, outputs: {} with {} pending, files: {}"
//                , demand
//                , outputs.size()
//                , pending.size()
//                , files.size()
//        );
        if (!pending.isEmpty()) {
          // Waiting for some inputs to catch up
          // logger.debug("Waiting for some inputs to catch up: {}", pending);
          emitting = false;
          return ;
        } else if (outputs.isEmpty()) {
          // Nothing is pending and nothing is to be done, we've reached the end
          if (endHandlerCalled) {
            return ;
          } else {
            endHandlerCalled = true;
            endHandlerCaptured = endHandler;
          }
          emitting = false;
          // logger.debug("End handler: {}", endHandler);
          done = true;
        } else if (demand <= 0) {
          emitting = false;
          // logger.debug("Demand: {}", demand);
          return ;
        } else {
          if (demand < Long.MAX_VALUE) {
            --demand;
          }
          handlerCaptured = handler;
          SourceStream topStream = outputs.poll();
          if (!pending.add(topStream)) {
            throw new IllegalStateException("Failed to add stream to pending: " + topStream);
          }
          next = topStream.head;
          done = topStream.next();
        }
//        heads = outputs.stream().map(ss -> ss.head.toString()).collect(Collectors.joining(", "));
      }
      if (handlerCaptured != null) {
        try {
          handlerCaptured.handle(next);
        } catch (Throwable ex) {
          postException(ex);
        }
      }
      if (endHandlerCaptured != null) {
        endHandlerCaptured.handle(null);
        return ;
      }
    }
  }

  private void inputEndHandler(Void nothing) {
    synchronized (lock) {
      // logger.debug("inputEndHandler with {} remaining", items.size());
      sortItemsList(items);
      outputs = new PriorityQueue<>(files.size() + 1, (a, b) -> {
        return comparator.compare(a.head, b.head);
      });
      outputHolder = new ArrayList<>(files.size() + 1);
      pending = new HashSet<>(files.size() + 1);

      if (!items.isEmpty()) {
        ReadStream<T> memStream = new ListReadStream<>(context, items);
        SourceStream ss = new SourceStream("list", 0, memStream);
        outputHolder.add(ss);
        pending.add(ss);
        ss.next();
      }

      emitting = true;

      List<Future<?>> futures = new ArrayList<>(files.size() + 1);
      for (String file : files) {
        Future<?> future = fileSystem.open(file, new OpenOptions().setRead(true).setDeleteOnClose(false))
                .compose(asyncFile -> {
                  SerializeReadStream<T> itemStream = new SerializeReadStream<>(asyncFile, deserializer);

                  SourceStream ss = new SourceStream(file, outputHolder.size(), itemStream);
                  synchronized (lock) {
                    outputHolder.add(ss);
                    pending.add(ss);
                    ss.next();
                  }
                  return Future.succeededFuture();
                })
                .onFailure(ex -> {
                  postException(ex);
                });
        futures.add(future);
      }
      Future.all(futures)
              .andThen(ar -> {
                synchronized (lock) {
                  emitting = false;
                }
                context.runOnContext(v -> {
                  processOutputs();
                });
              });
    }
  }

  private void inputExceptionHandler(Throwable exception) {
    postException(exception);
  }

  private void inputHandler(T item) {
    // logger.debug("inputHandler {}", item);
    int sizeofData = memoryEvaluator.sizeof(item);

    List<T> itemsToWrite = null;

    synchronized (lock) {
      ++inputCount;
      if (sizeofData + sizeOfItemsInMemory >= memoryLimit) {
        itemsToWrite = items;
        items = new ArrayList<>(itemsToWrite.size());
        sizeOfItemsInMemory = 0;
      }
      items.add(item);
      sizeOfItemsInMemory += sizeofData;
    }
    if (itemsToWrite != null) {
      input.pause();
      sortItemsList(itemsToWrite);
      writeItems(itemsToWrite)
              .onComplete(ar -> {
                if (ar.succeeded()) {
                  input.resume();
                } else {
                  postException(ar.cause());
                }
              });
    }
  }

  @Override
  public ReadStream<T> handler(Handler<T> handler) {
    this.handler = handler;
    return this;
  }


  @Override
  public SortingStream<T> pause() {
    input.pause();
    synchronized (lock) {
      demand = 0;
    }
    return this;
  }

  @Override
  public SortingStream<T> resume() {
    input.resume();
    return fetch(Long.MAX_VALUE);
  }

  @Override
  public SortingStream<T> fetch(long amount) {
    input.resume();
    if (amount < 0L) {
      throw new IllegalArgumentException();
    }
    boolean startProcess;
    synchronized (lock) {
      demand += amount;
      if (demand < 0L) {
        demand = Long.MAX_VALUE;
      }
      if (emitting) {
        return this;
      }
      emitting = true;
      startProcess = outputs != null && pending != null;
    }
    if (startProcess) {
      context.runOnContext(v -> {
        processOutputs();
      });
    }
    return this;
  }

  @Override
  public SortingStream<T> exceptionHandler(Handler<Throwable> handler) {
    this.exceptionHandler = handler;
    return this;
  }

  @Override
  public ReadStream<T> endHandler(Handler<Void> endHandler) {
    this.endHandler = endHandler;
    return this;
  }

  private void sortItemsList(List<T> items) {
    items.sort(comparator);
  }

  private Future<Void> writeAll(WriteStream<T> stream, List<T> items) {
    for (T item : items) {
      stream.write(item);
    }
    return stream.end();
  }

  private Future<Void> writeItems(List<T> items) {
    return fileSystem.createTempFile(tempDir, baseFileName, ".sort", (String) null)
            .compose(filename -> {
              synchronized (lock) {
                files.add(filename);
              }
              logger.trace("Dumping {} sorted items to {}", items.size(), filename);
              return fileSystem.open(filename, new OpenOptions().setCreate(true).setTruncateExisting(true));
            })
            .compose(asyncFile -> {
              SerializeWriteStream<T> sws = new SerializeWriteStream<>(asyncFile, serializer)
                      .setWriteQueueMaxSize(writeQueueMaxSize <= 0 ? 65536 : writeQueueMaxSize);
              return writeAll(sws, items);
            });
  }

}
