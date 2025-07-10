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

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.ReadStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A ReadStream that sorts its input using external merge sort. Uses bounded memory by spilling sorted chunks to temporary files
 * when memory limit is reached.
 *
 * @param <T> The object type being sorted.
 */
public final class SortingStream1<T> implements ReadStream<T> {

  private static final Logger logger = LoggerFactory.getLogger(SortingStream1.class);
  private static final int BUFFER_SIZE = 10; // Buffer size per source - increased for better performance

  // Configuration
  private final Context context;
  private final FileSystem fileSystem;
  private final Comparator<T> comparator;
  private final SerializeWriteStream.Serializer<T> serializer;
  private final SerializeReadStream.Deserializer<T> deserializer;
  private final String tempDir;
  private final String baseFileName;
  private final long memoryLimit;
  private final MemoryEvaluator<T> memoryEvaluator;

  // Input stream
  private final ReadStream<T> input;

  // Stream handlers
  private Handler<T> dataHandler;
  private Handler<Void> endHandler;
  private Handler<Throwable> exceptionHandler;

  // State management
  private final AtomicInteger state = new AtomicInteger(State.COLLECTING.ordinal());
  private final AtomicLong demand = new AtomicLong(0);
  private final AtomicBoolean processing = new AtomicBoolean(false);

  // Collection phase
  private final List<T> currentChunk = new ArrayList<>();
  private long currentChunkSize = 0;
  private final List<String> tempFiles = new ArrayList<>();

  // Merge phase
  private MergeState mergeState;

  public interface MemoryEvaluator<T> {
    long sizeof(T item);
  }

  private enum State {
    COLLECTING,   // Still collecting input
    MERGING,      // Merging sorted chunks
    COMPLETED,    // All data emitted
    FAILED        // Error occurred
  }

  public SortingStream1(Context context,
           FileSystem fileSystem,
           Comparator<T> comparator,
           SerializeWriteStream.Serializer<T> serializer,
           SerializeReadStream.Deserializer<T> deserializer,
           String tempDir,
           String baseFileName,
           long memoryLimit,
           MemoryEvaluator<T> memoryEvaluator,
           ReadStream<T> input
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

    // Set up input stream handlers
    input.handler(this::handleInputItem);
    input.endHandler(this::handleInputEnd);
    input.exceptionHandler(this::handleException);

    // Start reading input
    input.resume();
  }

  private void handleInputItem(T item) {
    if (state.get() != State.COLLECTING.ordinal()) {
      return;
    }

    currentChunk.add(item);
    currentChunkSize += memoryEvaluator.sizeof(item);

    if (currentChunkSize >= memoryLimit) {
      flushCurrentChunk()
              .onFailure(this::handleException);
    }
  }
  
  private void handleInputEnd(Void v) {
    if (state.get() != State.COLLECTING.ordinal()) {
      return;
    }

    logger.debug("Input ended, transitioning to merge phase");
    
    // Flush any remaining chunk
    Future<Void> flushFuture = currentChunk.isEmpty() ? 
        Future.succeededFuture() : 
        flushCurrentChunk();
    
    flushFuture
        .compose(vv -> startMergePhase())
        .onFailure(this::handleException);
  }

  private Future<Void> flushChunkAsync(List<T> chunkToFlush) {
    if (chunkToFlush.isEmpty()) {
      return Future.succeededFuture();
    }
    
    // Sort the chunk
    Collections.sort(chunkToFlush, comparator);
    
    // Write to temporary file
    String tempFileName = tempDir + "/" + baseFileName + "_" + tempFiles.size() + ".tmp";
    tempFiles.add(tempFileName);
    
    return fileSystem.open(tempFileName, new OpenOptions().setCreate(true).setWrite(true))
        .compose(file -> {
          SerializeWriteStream<T> writeStream = new SerializeWriteStream<>(file, serializer);
          return writeAllItems(writeStream, chunkToFlush);
        });
  }

  private Future<Void> flushCurrentChunk() {
    List<T> chunkToFlush = new ArrayList<>(currentChunk);
    currentChunk.clear();
    currentChunkSize = 0;
    
    return flushChunkAsync(chunkToFlush);
  }

  private Future<Void> writeAllItems(SerializeWriteStream<T> stream, List<T> items) {
    Promise<Void> promise = Promise.promise();
    
    writeNextItem(stream, items, 0, promise);
    
    return promise.future();
  }

  private void writeNextItem(SerializeWriteStream<T> stream, List<T> items, int index, Promise<Void> promise) {
    if (index >= items.size()) {
      stream.end(promise);
      return;
    }
    
    stream.write(items.get(index))
        .onSuccess(v -> writeNextItem(stream, items, index + 1, promise))
        .onFailure(promise::fail);
  }

  private Future<Void> startMergePhase() {
    if (!state.compareAndSet(State.COLLECTING.ordinal(), State.MERGING.ordinal())) {
      return Future.failedFuture(new IllegalStateException("Invalid state transition"));
    }

    logger.debug("Starting merge phase with {} temp files and {} items in memory", tempFiles.size(), currentChunk.size());

    // Choose merge strategy
    if (tempFiles.isEmpty()) {
      // All data fits in memory
      mergeState = new InMemoryMergeState();
    } else {
      // Need to merge files
      mergeState = new BufferedFileMergeState();
    }

    return mergeState.initialize()
        .onSuccess(v -> {
          logger.debug("Merge state initialized, scheduling output processing");
          scheduleProcessOutput();
        });
  }

  private void scheduleProcessOutput() {
    if (processing.compareAndSet(false, true)) {
      context.runOnContext(v -> {
        try {
          processOutput();
        } finally {
          processing.set(false);
        }
      });
    }
  }

  private void processOutput() {
    if (state.get() != State.MERGING.ordinal() || mergeState == null || dataHandler == null) {
      return;
    }

    int itemsProcessed = 0;
    final int MAX_ITEMS_PER_CYCLE = 1000; // Prevent infinite loops
    
    // Process items while we have demand
    while (demand.get() > 0 && itemsProcessed < MAX_ITEMS_PER_CYCLE) {
      if (mergeState.hasNext()) {
        T nextItem = mergeState.next();
        if (nextItem != null) {
          itemsProcessed++;
          if (demand.get() != Long.MAX_VALUE) {
            demand.decrementAndGet();
          }

          try {
            dataHandler.handle(nextItem);
          } catch (Exception e) {
            handleException(e);
            return;
          }
        }
      } else {
        // No items immediately available
        if (mergeState.hasMoreDataPotential()) {
          // Try to fill buffers and continue
          mergeState.ensureBuffersFilled()
                  .onComplete(ar -> {
                    if (ar.succeeded()) {
                      // Continue processing
                      scheduleProcessOutput();
                    } else {
                      handleException(ar.cause());
                    }
                  });
          return; // Exit and wait for async fill
        } else {
          // No more data available anywhere
          complete();
          return;
        }
      }
    }
    
    // If we processed the maximum items, schedule continuation
    if (itemsProcessed >= MAX_ITEMS_PER_CYCLE && demand.get() > 0) {
      scheduleProcessOutput();
    }
  }

  private void complete() {
    if (state.compareAndSet(State.MERGING.ordinal(), State.COMPLETED.ordinal())) {
      logger.debug("Sorting stream completed");
      if (mergeState != null) {
        mergeState.cleanup();
      }
      cleanup();
      if (endHandler != null) {
        endHandler.handle(null);
      }
    }
  }

  private void handleException(Throwable ex) {
    if (state.getAndSet(State.FAILED.ordinal()) != State.FAILED.ordinal()) {
      logger.error("Error in sorting stream", ex);
      if (mergeState != null) {
        mergeState.cleanup();
      }
      cleanup();
      if (exceptionHandler != null) {
        exceptionHandler.handle(ex);
      }
    }
  }

  private void cleanup() {
    for (String tempFile : tempFiles) {
      try {
        fileSystem.deleteBlocking(tempFile);
      } catch (Exception e) {
        logger.warn("Failed to delete temp file {}: {}", tempFile, e.getMessage());
      }
    }
    tempFiles.clear();
  }

  // ReadStream implementation
  @Override
  public ReadStream<T> handler(Handler<T> handler) {
    this.dataHandler = handler;
    return this;
  }

  @Override
  public ReadStream<T> pause() {
    demand.set(0);
    return this;
  }

  @Override
  public ReadStream<T> resume() {
    demand.set(Long.MAX_VALUE);
    scheduleProcessOutput();
    return this;
  }

  @Override
  public ReadStream<T> fetch(long amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("Fetch amount cannot be negative");
    }
    
    if (amount == 0) {
      return this;
    }
    
    long currentDemand = demand.get();
    if (currentDemand == Long.MAX_VALUE) {
      return this;
    }
    
    long newDemand = currentDemand + amount;
    if (newDemand < 0) {
      newDemand = Long.MAX_VALUE;
    }
    
    demand.set(newDemand);
    scheduleProcessOutput();
    return this;
  }

  @Override
  public ReadStream<T> exceptionHandler(Handler<Throwable> handler) {
    this.exceptionHandler = handler;
    return this;
  }

  @Override
  public ReadStream<T> endHandler(Handler<Void> endHandler) {
    this.endHandler = endHandler;
    return this;
  }

  // Merge state implementations
  private abstract class MergeState {
    abstract Future<Void> initialize();
    abstract boolean hasNext();
    abstract boolean hasMoreDataPotential();
    abstract T next();
    abstract Future<Void> ensureBuffersFilled();
    abstract void cleanup();
  }

  private class InMemoryMergeState extends MergeState {
    private Iterator<T> iterator;

    @Override
    Future<Void> initialize() {
      Collections.sort(currentChunk, comparator);
      iterator = currentChunk.iterator();
      return Future.succeededFuture();
    }

    @Override
    boolean hasNext() {
      return iterator.hasNext();
    }

    @Override
    boolean hasMoreDataPotential() {
      return iterator.hasNext();
    }

    @Override
    T next() {
      return iterator.hasNext() ? iterator.next() : null;
    }

    @Override
    Future<Void> ensureBuffersFilled() {
      return Future.succeededFuture();
    }

    @Override
    void cleanup() {
      // Nothing to clean up
    }
  }

  private class BufferedFileMergeState extends MergeState {
    private List<BufferedMergeSource> sources;
    private PriorityQueue<BufferedMergeSource> mergeQueue;

    @Override
    Future<Void> initialize() {
      sources = new ArrayList<>();
      
      // Add in-memory chunk if present
      if (!currentChunk.isEmpty()) {
        sources.add(new InMemoryBufferedSource(currentChunk));
      }
      
      // Add file sources - use async file opening
      List<Future<Void>> fileOpenFutures = new ArrayList<>();
      for (String tempFile : tempFiles) {
        Promise<Void> sourcePromise = Promise.promise();
        fileOpenFutures.add(sourcePromise.future());
        
        fileSystem.open(tempFile, new OpenOptions().setRead(true))
            .onSuccess(file -> {
              SerializeReadStream<T> readStream = new SerializeReadStream<>(file, deserializer);
              sources.add(new FileBufferedSource(readStream));
              sourcePromise.complete();
            })
            .onFailure(sourcePromise::fail);
      }

      // Create merge queue with comparator
      mergeQueue = new PriorityQueue<>((a, b) -> comparator.compare(a.peekNext(), b.peekNext()));

      // Wait for all file sources to be opened, then initialize them
      return Future.all(fileOpenFutures)
          .compose(v -> {
            // Initialize all sources
            List<Future<Void>> initFutures = new ArrayList<>();
            for (BufferedMergeSource source : sources) {
              initFutures.add(source.initialize());
            }
            return Future.all(initFutures);
          })
          .compose(v -> {
            // Fill initial buffers
            List<Future<Void>> fillFutures = new ArrayList<>();
            for (BufferedMergeSource source : sources) {
              fillFutures.add(source.fillBuffer(BUFFER_SIZE));
            }
            return Future.all(fillFutures);
          })
          .onSuccess(v -> {
            // Add sources with data to merge queue
            for (BufferedMergeSource source : sources) {
              if (source.hasNext()) {
                mergeQueue.offer(source);
              }
            }
            logger.debug("Initialized merge queue with {} sources", mergeQueue.size());
          })
          .mapEmpty();
    }

    @Override
    boolean hasNext() {
      return !mergeQueue.isEmpty();
    }

    @Override
    boolean hasMoreDataPotential() {
      // Check if any source has buffered data OR is not ended
      for (BufferedMergeSource source : sources) {
        if (source.hasNext() || !source.isEnded()) {
          return true;
        }
      }
      return false;
    }

    @Override
    T next() {
      if (mergeQueue.isEmpty()) {
        return null;
      }

      BufferedMergeSource source = mergeQueue.poll();
      T result = source.takeNext();

      logger.debug("Taking {} from source {}, queue size before re-add: {}", result, source, mergeQueue.size());

      // Only re-add if source still has buffered items
      if (source.hasNext()) {
        mergeQueue.offer(source);
        logger.debug("Re-added source, queue size now: {}", mergeQueue.size());
      } else {
        logger.debug("Source has no more buffered items, not re-adding to queue");
      }

      return result;
    }

    @Override
    Future<Void> ensureBuffersFilled() {
      List<Future<Void>> fillFutures = new ArrayList<>();

      // Fill buffers for sources that need more data
      for (BufferedMergeSource source : sources) {
        if (!source.isEnded() && source.bufferCount() < BUFFER_SIZE / 2) {
          fillFutures.add(source.fillBuffer(BUFFER_SIZE));
        }
      }

      if (fillFutures.isEmpty()) {
        return Future.succeededFuture();
      }

      return Future.all(fillFutures)
              .onSuccess(v -> {
                // Re-add sources with data back to merge queue
                for (BufferedMergeSource source : sources) {
                  if (source.hasNext() && !mergeQueue.contains(source)) {
                    mergeQueue.offer(source);
                  }
                }
                logger.debug("After buffer fill, merge queue has {} sources", mergeQueue.size());
              })
              .mapEmpty();
    }

    @Override
    void cleanup() {
      if (sources != null) {
        for (BufferedMergeSource source : sources) {
          source.cleanup();
        }
      }
    }
  }

  // Buffered merge source abstractions
  private abstract class BufferedMergeSource {
    protected final Queue<T> buffer = new ArrayDeque<>();
    protected boolean ended = false;

    abstract Future<Void> initialize();
    abstract Future<Void> fillBuffer(int targetSize);
    abstract void cleanup();

    boolean hasNext() {
      return !buffer.isEmpty();
    }

    T peekNext() {
      return buffer.peek();
    }

    T takeNext() {
      return buffer.poll();
    }

    int bufferCount() {
      return buffer.size();
    }

    boolean isEnded() {
      return ended;
    }
  }

  private class InMemoryBufferedSource extends BufferedMergeSource {
    private final Iterator<T> iterator;

    InMemoryBufferedSource(List<T> items) {
      List<T> sortedItems = new ArrayList<>(items);
      Collections.sort(sortedItems, comparator);
      this.iterator = sortedItems.iterator();
    }

    @Override
    Future<Void> initialize() {
      return Future.succeededFuture();
    }

    @Override
    Future<Void> fillBuffer(int targetSize) {
      int added = 0;
      while (iterator.hasNext() && added < targetSize) {
        buffer.offer(iterator.next());
        added++;
      }
      
      if (!iterator.hasNext()) {
        ended = true;
      }
      
      return Future.succeededFuture();
    }

    @Override
    void cleanup() {
      // Nothing to clean up
    }
  }

  private class FileBufferedSource extends BufferedMergeSource {
    private final SerializeReadStream<T> readStream;
    private Promise<Void> fillPromise;
    private int targetFillSize;
    private long totalItemsRead = 0;
    private boolean streamStarted = false;
    private boolean streamEnded = false;

    FileBufferedSource(SerializeReadStream<T> readStream) {
      this.readStream = readStream;
    }

    @Override
    Future<Void> initialize() {
      if (!streamStarted) {
        streamStarted = true;
        
        readStream.handler(item -> {
          buffer.offer(item);
          totalItemsRead++;
          logger.debug("FileBufferedSource received item {}, buffer size now: {}", item, buffer.size());
          checkFillComplete();
        });

        readStream.endHandler(v -> {
          ended = true;
          streamEnded = true;
          logger.debug("FileBufferedSource ended after reading {} items, buffer size: {}", totalItemsRead, buffer.size());
          completeFillIfWaiting();
        });

        readStream.exceptionHandler(ex -> {
          logger.error("Error in FileBufferedSource", ex);
          if (fillPromise != null) {
            fillPromise.fail(ex);
            fillPromise = null;
          }
        });
      }
      return Future.succeededFuture();
    }

    @Override
    Future<Void> fillBuffer(int targetSize) {
      if (streamEnded || buffer.size() >= targetSize) {
        return Future.succeededFuture();
      }

      if (fillPromise != null) {
        // Already filling, return existing promise
        return fillPromise.future();
      }

      fillPromise = Promise.promise();
      targetFillSize = targetSize;

      logger.debug("FileBufferedSource starting fill, target: {}, current buffer: {}", targetSize, buffer.size());
      
      // Start reading from the stream
      readStream.fetch(targetSize);
      
      return fillPromise.future();
    }

    private void checkFillComplete() {
      if (fillPromise != null && (buffer.size() >= targetFillSize || streamEnded)) {
        logger.debug("FileBufferedSource fill complete, buffer size: {}, target was: {}", buffer.size(), targetFillSize);
        completeFillIfWaiting();
      }
    }

    private void completeFillIfWaiting() {
      if (fillPromise != null) {
        fillPromise.complete();
        fillPromise = null;
      }
    }

    @Override
    void cleanup() {
      // The SerializeReadStream should handle cleanup internally
      // when the underlying AsyncFile is closed
    }
  }
}