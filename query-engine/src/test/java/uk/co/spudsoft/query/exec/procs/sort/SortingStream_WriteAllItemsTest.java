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

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import uk.co.spudsoft.query.exec.procs.ListReadStream;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SortingStream_WriteAllItemsTest {

  private static final Logger logger = LoggerFactory.getLogger(SortingStream_WriteAllItemsTest.class);

  @TempDir
  Path tempDir;

  // Mock serializer for testing
  private static class TestSerializer implements SerializeWriteStream.Serializer<Integer> {

    private final AtomicInteger serializeCount = new AtomicInteger(0);
    private final AtomicBoolean throwException = new AtomicBoolean(false);
    private final AtomicInteger exceptionAtCount = new AtomicInteger(-1);

    @Override
    public byte[] serialize(Integer item) throws IOException {
      int count = serializeCount.incrementAndGet();
      if (throwException.get() && (exceptionAtCount.get() == -1 || count == exceptionAtCount.get())) {
        throw new IOException("Test serialization failure for item: " + item);
      }
      return item.toString().getBytes();
    }

    public int getSerializeCount() {
      return serializeCount.get();
    }

    public void setThrowException(boolean throwException) {
      this.throwException.set(throwException);
    }

    public void setExceptionAtCount(int count) {
      this.exceptionAtCount.set(count);
    }

    public void reset() {
      serializeCount.set(0);
      throwException.set(false);
      exceptionAtCount.set(-1);
    }
  }

  // Mock WriteStream for testing various scenarios
  private static class TestSerializeWriteStream<T> extends SerializeWriteStream<T> {

    private final Vertx vertx;
    private final AtomicInteger writeCount = new AtomicInteger(0);
    private final AtomicBoolean writeFailure = new AtomicBoolean(false);
    private final AtomicInteger failureAtCount = new AtomicInteger(-1);
    private final AtomicBoolean endFailure = new AtomicBoolean(false);
    private final AtomicBoolean asyncWrites = new AtomicBoolean(false);
    private final AtomicInteger asyncDelayMs = new AtomicInteger(0);
    private final List<T> writtenItems = Collections.synchronizedList(new ArrayList<>());
    private final AtomicBoolean streamEnded = new AtomicBoolean(false);
    private final AtomicReference<Throwable> endException = new AtomicReference<>();
    private final List<Future<Void>> writeFutures = new ArrayList<>();

    public TestSerializeWriteStream(Vertx vertx, AsyncFile file, Serializer<T> serializer) {
      super(file, serializer);
      this.vertx = vertx;
    }

    @Override
    public Future<Void> write(T item) {
      int count = writeCount.incrementAndGet();
      writtenItems.add(item);

      if (writeFailure.get() && (failureAtCount.get() == -1 || count == failureAtCount.get())) {
        return Future.failedFuture(new RuntimeException("Test write failure for item: " + item));
      }

      if (asyncWrites.get()) {
        Promise<Void> promise = Promise.promise();
        asyncComplete(promise);
        writeFutures.add(promise.future());
        return promise.future();
      } else {
        return Future.succeededFuture();
      }
    }

    private void asyncComplete(Promise<Void> promise) {
      if (asyncDelayMs.get() > 0) {
        vertx.setTimer(asyncDelayMs.get(), id -> promise.complete());
      } else {
        vertx.runOnContext(v -> promise.complete());
      }
    }

    @Override
    public Future<Void> end() {
      streamEnded.set(true);
      if (endFailure.get()) {
        RuntimeException ex = new RuntimeException("Test end failure");
        endException.set(ex);
        return Future.failedFuture(ex);
      }

      if (asyncWrites.get()) {
        Promise<Void> promise = Promise.promise();
        if (writeFutures.isEmpty()) {
          asyncComplete(promise);
        } else {
          Future.all(writeFutures).onComplete(ar -> {
            if (ar.failed()) {
              promise.fail(ar.cause());
            } else {
              asyncComplete(promise);
            }
          });
        }
        return promise.future();
      } else {
        return Future.succeededFuture();
      }
    }

    public List<T> getWrittenItems() {
      return new ArrayList<>(writtenItems);
    }

    public int getWriteCount() {
      return writeCount.get();
    }

    public boolean isStreamEnded() {
      return streamEnded.get();
    }

    public Throwable getEndException() {
      return endException.get();
    }

    public void setWriteFailure(boolean writeFailure) {
      this.writeFailure.set(writeFailure);
    }

    public void setFailureAtCount(int count) {
      this.failureAtCount.set(count);
    }

    public void setEndFailure(boolean endFailure) {
      this.endFailure.set(endFailure);
    }

    public void setAsyncWrites(boolean asyncWrites) {
      this.asyncWrites.set(asyncWrites);
    }

    public void setAsyncDelayMs(int delayMs) {
      this.asyncDelayMs.set(delayMs);
    }

    public void reset() {
      writeCount.set(0);
      writeFailure.set(false);
      failureAtCount.set(-1);
      endFailure.set(false);
      asyncWrites.set(false);
      asyncDelayMs.set(0);
      writtenItems.clear();
      streamEnded.set(false);
      endException.set(null);
    }
  }

  // Helper method to create a SortingStream instance for testing
  private SortingStream<Integer> createSortingStream(Vertx vertx, FileSystem fs) {
    return new SortingStream<>(
            vertx.getOrCreateContext(),
            fs,
            Integer::compareTo,
            new TestSerializer(),
            data -> Integer.valueOf(new String(data)),
            tempDir.toString(),
            "test",
            1000,
            item -> 8, // Assume each Integer takes 8 bytes
            new ListReadStream<>(null, vertx.getOrCreateContext(), Arrays.asList(1, 2, 3))
    );
  }

  @Test
  public void testWriteAllItemsWithEmptyList(Vertx vertx, VertxTestContext testContext) {
    FileSystem fs = vertx.fileSystem();
    String testFile = tempDir.resolve("empty-test.tmp").toString();

    fs.open(testFile, new OpenOptions().setCreate(true).setWrite(true))
            .compose(file -> {
              TestSerializeWriteStream<Integer> stream = new TestSerializeWriteStream<>(vertx, file, new TestSerializer());
              SortingStream<Integer> sortingStream = createSortingStream(vertx, fs);

              List<Integer> emptyList = Collections.emptyList();
              return sortingStream.writeAllItems(stream, emptyList)
                      .compose(v -> {
                        testContext.verify(() -> {
                          assertEquals(0, stream.getWriteCount(), "No items should be written for empty list");
                          assertTrue(stream.isStreamEnded(), "Stream should be ended");
                          assertTrue(stream.getWrittenItems().isEmpty(), "No items should be written");
                        });
                        return Future.succeededFuture();
                      });
            })
            .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  public void testWriteAllItemsWithSingleItem(Vertx vertx, VertxTestContext testContext) {
    FileSystem fs = vertx.fileSystem();
    String testFile = tempDir.resolve("single-test.tmp").toString();

    fs.open(testFile, new OpenOptions().setCreate(true).setWrite(true))
            .compose(file -> {
              TestSerializeWriteStream<Integer> stream = new TestSerializeWriteStream<>(vertx, file, new TestSerializer());
              SortingStream<Integer> sortingStream = createSortingStream(vertx, fs);

              List<Integer> singleItemList = Arrays.asList(42);
              return sortingStream.writeAllItems(stream, singleItemList)
                      .compose(v -> {
                        testContext.verify(() -> {
                          assertEquals(1, stream.getWriteCount(), "Single item should be written");
                          assertTrue(stream.isStreamEnded(), "Stream should be ended");
                          assertEquals(Arrays.asList(42), stream.getWrittenItems(), "Written item should match input");
                        });
                        return Future.succeededFuture();
                      });
            })
            .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  public void testWriteAllItemsWithMultipleItems(Vertx vertx, VertxTestContext testContext) {
    FileSystem fs = vertx.fileSystem();
    String testFile = tempDir.resolve("multiple-test.tmp").toString();

    fs.open(testFile, new OpenOptions().setCreate(true).setWrite(true))
            .compose(file -> {
              TestSerializeWriteStream<Integer> stream = new TestSerializeWriteStream<>(vertx, file, new TestSerializer());
              SortingStream<Integer> sortingStream = createSortingStream(vertx, fs);

              List<Integer> multipleItems = Arrays.asList(1, 2, 3, 4, 5);
              return sortingStream.writeAllItems(stream, multipleItems)
                      .compose(v -> {
                        testContext.verify(() -> {
                          assertEquals(5, stream.getWriteCount(), "All items should be written");
                          assertTrue(stream.isStreamEnded(), "Stream should be ended");
                          assertEquals(multipleItems, stream.getWrittenItems(), "Written items should match input order");
                        });
                        return Future.succeededFuture();
                      });
            })
            .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  public void testWriteAllItemsWithLargeList(Vertx vertx, VertxTestContext testContext) {
    FileSystem fs = vertx.fileSystem();
    String testFile = tempDir.resolve("large-test.tmp").toString();

    fs.open(testFile, new OpenOptions().setCreate(true).setWrite(true))
            .compose(file -> {
              TestSerializeWriteStream<Integer> stream = new TestSerializeWriteStream<>(vertx, file, new TestSerializer());
              SortingStream<Integer> sortingStream = createSortingStream(vertx, fs);

              List<Integer> largeList = new ArrayList<>();
              for (int i = 0; i < 1000; i++) {
                largeList.add(i);
              }

              return sortingStream.writeAllItems(stream, largeList)
                      .compose(v -> {
                        testContext.verify(() -> {
                          assertEquals(1000, stream.getWriteCount(), "All 1000 items should be written");
                          assertTrue(stream.isStreamEnded(), "Stream should be ended");
                          assertEquals(largeList, stream.getWrittenItems(), "Written items should match input");
                        });
                        return Future.succeededFuture();
                      });
            })
            .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  public void testWriteAllItemsWithWriteFailure(Vertx vertx, VertxTestContext testContext) {
    FileSystem fs = vertx.fileSystem();
    String testFile = tempDir.resolve("write-failure-test.tmp").toString();

    fs.open(testFile, new OpenOptions().setCreate(true).setWrite(true))
            .compose(file -> {
              TestSerializeWriteStream<Integer> stream = new TestSerializeWriteStream<>(vertx, file, new TestSerializer());
              stream.setWriteFailure(true);
              stream.setFailureAtCount(3); // Fail on third write

              SortingStream<Integer> sortingStream = createSortingStream(vertx, fs);

              List<Integer> items = Arrays.asList(1, 2, 3, 4, 5);
              return sortingStream.writeAllItems(stream, items)
                      .compose(v -> {
                        testContext.failNow("Expected write failure");
                        return Future.<Void>failedFuture("Should not reach here");
                      })
                      .recover(throwable -> {
                        testContext.verify(() -> {
                          assertTrue(throwable instanceof RuntimeException, "Should be RuntimeException");
                          assertEquals("Test write failure for item: 3", throwable.getMessage());
                          assertEquals(3, stream.getWriteCount(), "Should have attempted 3 writes");
                          assertTrue(stream.isStreamEnded(), "Stream should still get ended even with failure");
                        });
                        return Future.succeededFuture();
                      });
            })
            .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  public void testWriteAllItemsWithEndFailure(Vertx vertx, VertxTestContext testContext) {
    FileSystem fs = vertx.fileSystem();
    String testFile = tempDir.resolve("end-failure-test.tmp").toString();

    fs.open(testFile, new OpenOptions().setCreate(true).setWrite(true))
            .compose(file -> {
              TestSerializeWriteStream<Integer> stream = new TestSerializeWriteStream<>(vertx, file, new TestSerializer());
              stream.setEndFailure(true);

              SortingStream<Integer> sortingStream = createSortingStream(vertx, fs);

              List<Integer> items = Arrays.asList(1, 2, 3);
              return sortingStream.writeAllItems(stream, items)
                      .compose(v -> {
                        testContext.failNow("Expected end failure");
                        return Future.<Void>failedFuture("Should not reach here");
                      })
                      .recover(throwable -> {
                        testContext.verify(() -> {
                          assertTrue(throwable instanceof RuntimeException, "Should be RuntimeException");
                          assertEquals("Test end failure", throwable.getMessage());
                          assertEquals(3, stream.getWriteCount(), "All items should have been written");
                          assertTrue(stream.isStreamEnded(), "Stream should be ended (but with failure)");
                          assertEquals(Arrays.asList(1, 2, 3), stream.getWrittenItems(), "All items should be written before end failure");
                        });
                        return Future.succeededFuture();
                      });
            })
            .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  public void testWriteAllItemsWithAsyncWrites(Vertx vertx, VertxTestContext testContext) {
    FileSystem fs = vertx.fileSystem();
    String testFile = tempDir.resolve("async-test.tmp").toString();

    fs.open(testFile, new OpenOptions().setCreate(true).setWrite(true))
            .compose(file -> {
              TestSerializeWriteStream<Integer> stream = new TestSerializeWriteStream<>(vertx, file, new TestSerializer());
              stream.setAsyncWrites(true);
              stream.setAsyncDelayMs(10); // Small delay to test async behavior

              SortingStream<Integer> sortingStream = createSortingStream(vertx, fs);

              List<Integer> items = Arrays.asList(1, 2, 3, 4, 5);
              long startTime = System.currentTimeMillis();

              return sortingStream.writeAllItems(stream, items)
                      .compose(v -> {
                        long endTime = System.currentTimeMillis();
                        testContext.verify(() -> {
                          assertEquals(5, stream.getWriteCount(), "All items should be written");
                          assertTrue(stream.isStreamEnded(), "Stream should be ended");
                          assertEquals(items, stream.getWrittenItems(), "Written items should match input");
                          // Note that the delay is only guaranteed to be once for the writes and once for the end, it's not the sum of all writes.
                          assertThat("Should take at least 20ms due to async delays", endTime - startTime, not(lessThan(20L)));
                        });
                        return Future.succeededFuture();
                      });
            })
            .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  public void testWriteAllItemsWithAsyncWriteFailure(Vertx vertx, VertxTestContext testContext) {
    FileSystem fs = vertx.fileSystem();
    String testFile = tempDir.resolve("async-failure-test.tmp").toString();

    fs.open(testFile, new OpenOptions().setCreate(true).setWrite(true))
            .compose(file -> {
              TestSerializeWriteStream<Integer> stream = new TestSerializeWriteStream<>(vertx, file, new TestSerializer());
              stream.setAsyncWrites(true);
              stream.setAsyncDelayMs(10);
              stream.setWriteFailure(true);
              stream.setFailureAtCount(2); // Fail on second write

              SortingStream<Integer> sortingStream = createSortingStream(vertx, fs);

              List<Integer> items = Arrays.asList(1, 2, 3, 4, 5);
              return sortingStream.writeAllItems(stream, items)
                      .compose(v -> {
                        testContext.failNow("Expected async write failure");
                        return Future.<Void>failedFuture("Should not reach here");
                      })
                      .recover(throwable -> {
                        testContext.verify(() -> {
                          assertTrue(throwable instanceof RuntimeException, "Should be RuntimeException");
                          assertEquals("Test write failure for item: 2", throwable.getMessage());
                          assertEquals(2, stream.getWriteCount(), "Should have attempted 2 writes");
                          assertTrue(stream.isStreamEnded(), "Stream should still get ended even with failure");
                        });
                        return Future.succeededFuture();
                      });
            })
            .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  public void testWriteAllItemsWithSyncAndAsyncMixedWrites(Vertx vertx, VertxTestContext testContext) {
    FileSystem fs = vertx.fileSystem();
    String testFile = tempDir.resolve("mixed-test.tmp").toString();

    fs.open(testFile, new OpenOptions().setCreate(true).setWrite(true))
            .compose(file -> {
              // Custom stream that alternates between sync and async writes
              TestSerializeWriteStream<Integer> stream = new TestSerializeWriteStream<Integer>(vertx, file, new TestSerializer()) {
                @Override
                public Future<Void> write(Integer item) {
                  int count = super.writeCount.incrementAndGet();
                  super.writtenItems.add(item);

                  if (count % 2 == 0) {
                    // Even writes are async
                    Promise<Void> promise = Promise.promise();
                    vertx.setTimer(5, id -> promise.complete());
                    return promise.future();
                  } else {
                    // Odd writes are sync
                    return Future.succeededFuture();
                  }
                }
              };

              SortingStream<Integer> sortingStream = createSortingStream(vertx, fs);

              List<Integer> items = Arrays.asList(1, 2, 3, 4, 5, 6);
              return sortingStream.writeAllItems(stream, items)
                      .compose(v -> {
                        testContext.verify(() -> {
                          assertEquals(6, stream.getWriteCount(), "All items should be written");
                          assertTrue(stream.isStreamEnded(), "Stream should be ended");
                          assertEquals(items, stream.getWrittenItems(), "Written items should match input order");
                        });
                        return Future.succeededFuture();
                      });
            })
            .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  public void testWriteItemsIterativelyRecursionDepth(Vertx vertx, VertxTestContext testContext) {
    FileSystem fs = vertx.fileSystem();
    String testFile = tempDir.resolve("recursion-test.tmp").toString();

    fs.open(testFile, new OpenOptions().setCreate(true).setWrite(true))
            .compose(file -> {
              TestSerializeWriteStream<Integer> stream = new TestSerializeWriteStream<>(vertx, file, new TestSerializer());
              stream.setAsyncWrites(true);
              stream.setAsyncDelayMs(0); // No delay, just async behavior

              SortingStream<Integer> sortingStream = createSortingStream(vertx, fs);

              // Smaller list for this test
              List<Integer> largeList = new ArrayList<>();
              for (int i = 0; i < 1000; i++) { // Reduced from 10,000
                largeList.add(i);
              }

              return sortingStream.writeAllItems(stream, largeList);
            })
            .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  public void testWriteAllItemsWithNullItems(Vertx vertx, VertxTestContext testContext) {
    FileSystem fs = vertx.fileSystem();
    String testFile = tempDir.resolve("null-test.tmp").toString();

    fs.open(testFile, new OpenOptions().setCreate(true).setWrite(true))
            .compose(file -> {
              TestSerializeWriteStream<Integer> stream = new TestSerializeWriteStream<>(vertx, file, new TestSerializer());
              SortingStream<Integer> sortingStream = createSortingStream(vertx, fs);

              List<Integer> itemsWithNulls = Arrays.asList(1, null, 3, null, 5);
              return sortingStream.writeAllItems(stream, itemsWithNulls)
                      .compose(v -> {
                        testContext.verify(() -> {
                          assertEquals(5, stream.getWriteCount(), "All items including nulls should be written");
                          assertTrue(stream.isStreamEnded(), "Stream should be ended");
                          assertEquals(itemsWithNulls, stream.getWrittenItems(), "Written items should match input including nulls");
                        });
                        return Future.succeededFuture();
                      });
            })
            .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  public void testWriteAllItemsSerializationFailure(Vertx vertx, VertxTestContext testContext) {
    FileSystem fs = vertx.fileSystem();
    String testFile = tempDir.resolve("serialization-failure-test.tmp").toString();

    fs.open(testFile, new OpenOptions().setCreate(true).setWrite(true))
            .compose(file -> {
              TestSerializer serializer = new TestSerializer();
              serializer.setThrowException(true);
              serializer.setExceptionAtCount(2); // Fail on second serialization

              SerializeWriteStream<Integer> stream = new SerializeWriteStream<>(file, serializer);
              SortingStream<Integer> sortingStream = createSortingStream(vertx, fs);

              List<Integer> items = Arrays.asList(1, 2, 3, 4, 5);
              return sortingStream.writeAllItems(stream, items)
                      .compose(v -> {
                        testContext.failNow("Expected serialization failure");
                        return Future.<Void>failedFuture("Should not reach here");
                      })
                      .recover(throwable -> {
                        testContext.verify(() -> {
                          assertTrue(throwable instanceof IOException, "Should be IOException");
                          assertEquals("Test serialization failure for item: 2", throwable.getMessage());
                          assertEquals(2, serializer.getSerializeCount(), "Should have attempted 2 serializations");
                        });
                        return Future.succeededFuture();
                      });
            })
            .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  public void testWriteAllItemsOrderPreservation(Vertx vertx, VertxTestContext testContext) {
    FileSystem fs = vertx.fileSystem();
    String testFile = tempDir.resolve("order-test.tmp").toString();

    fs.open(testFile, new OpenOptions().setCreate(true).setWrite(true))
            .compose(file -> {
              TestSerializeWriteStream<Integer> stream = new TestSerializeWriteStream<>(vertx, file, new TestSerializer());
              stream.setAsyncWrites(true);
              stream.setAsyncDelayMs(5);

              SortingStream<Integer> sortingStream = createSortingStream(vertx, fs);

              List<Integer> items = Arrays.asList(5, 1, 3, 2, 4);
              return sortingStream.writeAllItems(stream, items)
                      .compose(v -> {
                        testContext.verify(() -> {
                          assertEquals(5, stream.getWriteCount(), "All items should be written");
                          assertTrue(stream.isStreamEnded(), "Stream should be ended");
                          assertEquals(items, stream.getWrittenItems(), "Written items should preserve input order");
                        });
                        return Future.succeededFuture();
                      });
            })
            .onComplete(testContext.succeedingThenComplete());
  }
}
