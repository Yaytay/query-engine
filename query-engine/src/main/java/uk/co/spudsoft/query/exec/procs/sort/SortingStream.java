/*
 * Copyright (C) 2025 jtalbut
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import java.util.ListIterator;
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
public final class SortingStream<T> implements ReadStream<T> {

  private static final Logger logger = LoggerFactory.getLogger(SortingStream.class);
  private static final int BUFFER_SIZE = 1000; // Buffer size per source

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
  private final AtomicInteger state = new AtomicInteger(State.PENDING.ordinal());
  private final AtomicLong demand = new AtomicLong(0);
  private final AtomicBoolean processing = new AtomicBoolean(false);

  // Collection phase
  private final List<T> currentChunk = new ArrayList<>();
  private long currentChunkSize = 0;
  private final List<String> tempFiles = new ArrayList<>();

  // List of outstanding chunk writes
  private final List<Future<Void>> pendingChunkFlushes = new  ArrayList<>();

  // Merge phase
  private MergeState mergeState;

  /**
   * Interface for calculating the size of an item.
   * 
   * The generated result is compared with the memoryLimit passed in to the SortingStream constructor, it does not necessarily need
   * to attempt to calculate bytes.
   * 
   * @param <T> The class of item that this MemoryEvaluator can assess.
   */
  public interface MemoryEvaluator<T> {
    /**
     * Calculate the size of an item.
     * 
     * This is intended to return a best-efforts calculation of the number of bytes used by the item in memory,
     * however Java Strings provide no way to know how much space they consume, so the results are approximate at best.
     * 
     * The return value is only used by SortingStream to determine whether the current set of items in memory should be
     * written to a (sorted) temporary file.
     * 
     * @param item The item to consider.
     * @return The size of the item.
     */
    long sizeof(T item);
  }

  private enum State {
    PENDING,      // Not yet started collecting input
    COLLECTING,   // Still collecting input
    MERGING,      // Merging sorted chunks
    COMPLETED,    // All data emitted
    FAILED        // Error occurred
  }

  /**
   * Constructor.
   * 
   * @param context The vertx {@link Context} to use for asynchronous operations.
   * @param fileSystem The vertx {@link FileSystem} to use for temporary file operations.
   * @param comparator The comparator to use to sort objects of type T.
   * @param serializer The serializer to use to convert objects of type T into byte[].
   * @param deserializer The deserializer to use to convert byte[] into objects of type T.
   * @param tempDir The temporary directory to use to store temporary files, this should be unique to this instance of the SortingStream.
   * @param baseFileName A base filename to use for the temporary files - this must consist of alphanumeric characters or underscore or dot.
   * @param memoryLimit The amount of memory to use for storing items before they spill to temporary files.
   * @param memoryEvaluator The {@link MemoryEvaluator} to use to determin the number of bytes used by items.
   * @param input The input stream of items, which should be cold (paused, with no handlers set).
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The filesystem is clearly mutable")
  public SortingStream(Context context,
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

    fileSystem.mkdirsBlocking(tempDir);

    // Set up input stream handlers
    input.handler(this::handleInputItem);
    input.endHandler(this::handleInputEnd);
    input.exceptionHandler(this::handleException);
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
    if (!state.compareAndSet(State.COLLECTING.ordinal(), State.MERGING.ordinal())) {
      handleException(new IllegalStateException("Invalid state transition"));
      return;
    }

    logger.trace("Input ended, transitioning to merge phase");

    Future.all(pendingChunkFlushes)
            .compose(v1 -> {
              logger.trace("Starting merge phase with {} temp files and {} items in memory", tempFiles.size(), currentChunk.size());

              // Choose merge strategy
              if (tempFiles.isEmpty()) {
                // All data fits in memory - sort it
                Collections.sort(currentChunk, comparator);
                mergeState = new InMemoryMergeState();
              } else {
                // Need to merge files
                mergeState = new BufferedFileMergeState();
              }

              return mergeState.initialize()
                  .onSuccess(v2 -> {
                    logger.trace("Merge state initialized, scheduling output processing");
                    // Ensure we start processing if handlers are already set
                    if (dataHandler != null && demand.get() > 0) {
                      scheduleProcessOutput();
                    }
                  });
            })
            .onFailure(this::handleException);
  }

  private Future<Void> flushChunkAsync(List<T> chunkToFlush) {
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

    Future<Void> future = flushChunkAsync(chunkToFlush);
    pendingChunkFlushes.add(future);
    future.onComplete(ar -> {
      if (ar.failed()) {
        logger.warn("Failed to write chunk to disc: ", ar.cause());
        handleException(ar.cause());
      }
      pendingChunkFlushes.remove(future);
    });
    return future;
  }

  Future<Void> writeAllItems(SerializeWriteStream<T> stream, List<T> items) {
    Promise<Void> promise = Promise.promise();

    writeItemsIteratively(stream, items.listIterator(), promise);

    return promise.future();
  }

  private void writeItemsIteratively(SerializeWriteStream<T> stream, ListIterator<T> iterator, Promise<Void> promise) {
    // Process items synchronously when possible
    while (iterator.hasNext()) {
      T item = iterator.next();
      Future<Void> writeFuture = stream.write(item);

      if (writeFuture.isComplete()) {
        // Future is already complete, check result and continue synchronously
        if (writeFuture.failed()) {
          promise.fail(writeFuture.cause());
          return;
        }
        // Success - continue the loop
      } else {
        // Future is not complete, set up async continuation
        writeFuture.onComplete(ar -> {
          if (ar.succeeded()) {
            // Continue writing remaining items
            writeItemsIteratively(stream, iterator, promise);
          } else {
            promise.fail(ar.cause());
          }
        });
        return; // Exit the loop, will continue async
      }
    }

    // All items written successfully, end the stream
    Future<Void> endFuture = stream.end();
    if (endFuture.isComplete()) {
      if (endFuture.succeeded()) {
        promise.complete();
      } else {
        promise.fail(endFuture.cause());
      }
    } else {
      endFuture.onComplete(promise);
    }
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
    if (state.get() != State.MERGING.ordinal() || mergeState == null) {
      return;
    }

    // Don't process if no handler is set
    if (dataHandler == null) {
      return;
    }

    try {
      // Process items while we have demand
      while (demand.get() > 0) {
        if (mergeState.hasNext()) {
          T nextItem = mergeState.next();
          if (nextItem != null) {
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
        } else if (mergeState.hasMoreDataPotential()) {
          // We don't have all sources ready, but some might have more data
          // Fill buffers and then continue processing
          mergeState.ensureBuffersFilled()
                  .onComplete(ar -> {
                    if (ar.succeeded()) {
                      // Continue processing after buffer fill
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

      // Check if we should complete (all sources ended and no more data)
      if (!mergeState.hasNext() && !mergeState.hasMoreDataPotential()) {
        complete();
      }
    } catch (Throwable ex) {
      handleException(ex);
    }
  }

  private void complete() {
    if (state.compareAndSet(State.MERGING.ordinal(), State.COMPLETED.ordinal())) {
      logger.trace("Sorting stream completed");
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
      Handler<Throwable> exHandler = exceptionHandler;
      if (exHandler != null) {
        exHandler.handle(ex);
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

    // If we're already in merge phase and have data, start processing
    if (state.get() == State.MERGING.ordinal() && handler != null && demand.get() > 0) {
      scheduleProcessOutput();
    }

    return this;
  }

  @Override
  public ReadStream<T> resume() {
    if (state.compareAndSet(State.PENDING.ordinal(), State.COLLECTING.ordinal())) {
      input.resume();
    }
    demand.set(Long.MAX_VALUE);

    scheduleProcessingIfInMergeStateWithHandler();

    return this;
  }
  @Override
  public ReadStream<T> pause() {
    demand.set(0);
    return this;
  }

  @Override
  public ReadStream<T> fetch(long amount) {
    if (state.compareAndSet(State.PENDING.ordinal(), State.COLLECTING.ordinal())) {
      input.resume();
    }
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

    scheduleProcessingIfInMergeStateWithHandler();

    return this;
  }

  private void scheduleProcessingIfInMergeStateWithHandler() {
    // If we're already in merge phase and have a handler, start processing
    if (state.get() == State.MERGING.ordinal() && dataHandler != null) {
      scheduleProcessOutput();
    }
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
      return iterator.next();
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
    private final List<BufferedMergeSource> sources = new ArrayList<>();

    @Override
    Future<Void> initialize() {
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

      // Wait for all file sources to be opened, then initialize them
      return Future.all(fileOpenFutures)
          .compose(v -> {
            // Initialize all sources
            for (BufferedMergeSource source : sources) {
              source.initialize();
            }
            // Fill initial buffers
            List<Future<Void>> fillFutures = new ArrayList<>();
            for (BufferedMergeSource source : sources) {
              fillFutures.add(source.fillBuffer(BUFFER_SIZE));
            }
            return Future.all(fillFutures);
          })
          .onSuccess(v -> {
            logger.trace("Initialized merge state with {} sources", sources.size());
          })
          .mapEmpty();
    }

    private boolean hasPendingSources() {
      for (BufferedMergeSource source : sources) {
        if (!source.isEnded() && !source.hasNext()) {
          return true; // This source is Pending
        }
      }
      return false;
    }

    private BufferedMergeSource findMinSource() {
      BufferedMergeSource minSource = null;
      T minValue = null;

      for (BufferedMergeSource source : sources) {
        if (source.hasNext()) { // Only consider Ready sources
          T value = source.peekNext();
          if (minValue == null || comparator.compare(value, minValue) < 0) {
            minValue = value;
            minSource = source;
          }
        }
      }

      return minSource;
    }

    @Override
    boolean hasNext() {
      // Only return true if we can definitely provide a next item
      // If there are pending sources, we can't be sure what the next item should be
      if (hasPendingSources()) {
        return false;
      }
      return findMinSource() != null;
    }

    @Override
    boolean hasMoreDataPotential() {
      // Return true if there are pending sources OR if we have a ready source
      if (hasPendingSources()) {
        return true;
      }
      return findMinSource() != null;
    }

    @Override
    T next() {
      BufferedMergeSource source = findMinSource();
      if (source == null) {
        return null;
      }

      T result = source.takeNext();
      logger.trace("Taking {} from source {}", result, source);

      return result;
    }

    @Override
    Future<Void> ensureBuffersFilled() {
      List<Future<Void>> fillFutures = new ArrayList<>();

      // Fill buffers for all Pending sources
      for (BufferedMergeSource source : sources) {
        if (!source.isEnded() && !source.hasNext()) {
          // This source is Pending - make it Ready or Ended
          fillFutures.add(source.fillBuffer(BUFFER_SIZE));
        }
      }

      return Future.all(fillFutures)
              .onSuccess(v -> {
                logger.trace("After buffer fill, no more pending sources");
              })
              .mapEmpty();
    }

    @Override
    void cleanup() {
      for (BufferedMergeSource source : sources) {
        source.cleanup();
      }
    }
  }

  // Buffered merge source abstractions
  private abstract class BufferedMergeSource {
    protected final Queue<T> buffer = new ArrayDeque<>();
    protected boolean ended = false;

    abstract void initialize();
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

    boolean isEnded() {
      return ended;
    }
  }

  private class InMemoryBufferedSource extends BufferedMergeSource {

    // Just put everything straght into the buffer
    InMemoryBufferedSource(List<T> items) {
      Collections.sort(items, comparator);
      this.buffer.addAll(items);
      this.ended = true;
    }

    @Override
    void initialize() {
    }

    @Override
    Future<Void> fillBuffer(int targetSize) {
      return Future.succeededFuture();
    }

    @Override
    void cleanup() {
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
    void initialize() {
      if (!streamStarted) {
        streamStarted = true;

        readStream.handler(item -> {
          buffer.offer(item);
          totalItemsRead++;
          logger.trace("{} received item {}, buffer size now: {}", this, item, buffer.size());
          checkFillComplete();
        });

        readStream.endHandler(v -> {
          ended = true;
          streamEnded = true;
          logger.trace("{} ended after reading {} items, buffer size: {}", this, totalItemsRead, buffer.size());
          if (fillPromise != null) {
            completeFill();
          }
        });

        readStream.exceptionHandler(ex -> {
          logger.error("Error in {}", this, ex);
          if (fillPromise != null) {
            fillPromise.fail(ex);
            fillPromise = null;
          }
        });
      }
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

      logger.trace("{} starting fill, target: {}, current buffer: {}", this, targetSize, buffer.size());

      // Start reading from the stream
      readStream.fetch(targetSize);

      return fillPromise.future();
    }

    private void checkFillComplete() {
      if (fillPromise != null && (buffer.size() >= targetFillSize || streamEnded)) {
        logger.trace("{} fill complete, buffer size: {}, target was: {}", this, buffer.size(), targetFillSize);
        completeFill();
      }
    }

    private void completeFill() {
      fillPromise.complete();
      fillPromise = null;
      logger.trace("{} complete, promise signalled", this);
    }

    @Override
    void cleanup() {
      // The SerializeReadStream should handle cleanup internally
      // when the underlying AsyncFile is closed
    }
  }
}
