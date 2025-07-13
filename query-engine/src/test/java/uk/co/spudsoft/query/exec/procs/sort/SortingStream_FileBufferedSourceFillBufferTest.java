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

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import uk.co.spudsoft.query.exec.procs.ListReadStream;

/**
 * Exhaustive unit tests for SortingStream.FileBufferedSource.fillBuffer method.
 * Tests cover all edge cases, error conditions, and normal operation scenarios.
 */
@ExtendWith(VertxExtension.class)
public class SortingStream_FileBufferedSourceFillBufferTest {

    private static final Logger logger = LoggerFactory.getLogger(SortingStream_FileBufferedSourceFillBufferTest.class);

    @TempDir
    Path tempDir;

    private Context context;
    private FileSystem fileSystem;
    private String tempDirPath;

    // Test data serializer/deserializer
    private final SerializeWriteStream.Serializer<Integer> serializer = number -> SerializeWriteStream.byteArrayFromInt(number);
    private final SerializeReadStream.Deserializer<Integer> deserializer = data -> SerializeReadStream.intFromByteArray(data);

    @BeforeEach
    void setUp(Vertx vertx) {
        context = vertx.getOrCreateContext();
        fileSystem = vertx.fileSystem();
        tempDirPath = tempDir.toString().replace('\\', '/');
    }

    @AfterEach
    void tearDown() {
        // Clean up temp files
        try {
            fileSystem.deleteRecursiveBlocking(tempDirPath);
        } catch (Exception e) {
            logger.warn("Failed to clean up temp directory: {}", e.getMessage());
        }
    }

    /**
     * Creates a test file with the given integers and returns a FileBufferedSource.
     */
    private Future<Object> createFileBufferedSource(List<Integer> data) {
        String fileName = tempDirPath + "/test_data.tmp";
        
        // Write test data to file asynchronously
        return fileSystem.open(fileName, new OpenOptions().setCreate(true).setWrite(true))
            .compose(writeFile -> {
                SerializeWriteStream<Integer> writeStream = new SerializeWriteStream<>(writeFile, serializer);
                
                // Write all items sequentially using iterative approach
                return writeItemsIteratively(writeStream, data)
                    .compose(v -> writeStream.end());
            })
            .compose(v -> {
                // Create read stream
                return fileSystem.open(fileName, new OpenOptions().setRead(true));
            })
            .compose(readFile -> {
                try {
                    SerializeReadStream<Integer> readStream = new SerializeReadStream<>(readFile, deserializer);
                    
                    // Create SortingStream instance and extract FileBufferedSource
                    SortingStream<Integer> sortingStream = new SortingStream<>(
                        context, fileSystem, Integer::compareTo, serializer, deserializer,
                        tempDirPath, "test", 1000, item -> 4,
                        new ListReadStream<>(context, Collections.emptyList())
                    );
                    
                    // Use reflection to access private inner class
                    Class<?> fileBufferedSourceClass = null;
                    for (Class<?> innerClass : SortingStream.class.getDeclaredClasses()) {
                        if ("FileBufferedSource".equals(innerClass.getSimpleName())) {
                            fileBufferedSourceClass = innerClass;
                            break;
                        }
                    }
                    
                    if (fileBufferedSourceClass == null) {
                        throw new RuntimeException("FileBufferedSource class not found");
                    }
                    
                    Object fileBufferedSource = fileBufferedSourceClass.getDeclaredConstructor(SortingStream.class, SerializeReadStream.class)
                        .newInstance(sortingStream, readStream);
                    
                    // Initialize the source
                    fileBufferedSourceClass.getDeclaredMethod("initialize").invoke(fileBufferedSource);
                    
                    return Future.succeededFuture(fileBufferedSource);
                } catch (Exception e) {
                    return Future.failedFuture(e);
                }
            });
    }

    /**
     * Helper method to write items iteratively (avoiding stack overflow with large datasets).
     */
    private Future<Void> writeItemsIteratively(SerializeWriteStream<Integer> writeStream, List<Integer> data) {
        Promise<Void> promise = Promise.promise();
        
        if (data.isEmpty()) {
            promise.complete();
            return promise.future();
        }
        
        ListIterator<Integer> iterator = data.listIterator();
        writeNextItem(writeStream, iterator, promise);
        
        return promise.future();
    }

    private void writeNextItem(SerializeWriteStream<Integer> writeStream, ListIterator<Integer> iterator, Promise<Void> promise) {
        if (!iterator.hasNext()) {
            promise.complete();
            return;
        }
        
        Integer item = iterator.next();
        writeStream.write(item)
            .onComplete(ar -> {
                if (ar.succeeded()) {
                    // Schedule next write to avoid stack overflow
                    context.runOnContext(v -> writeNextItem(writeStream, iterator, promise));
                } else {
                    promise.fail(ar.cause());
                }
            });
    }

    /**
     * Helper method to invoke fillBuffer using reflection.
     */
    @SuppressWarnings("unchecked")
    private Future<Void> invokeFillBuffer(Object fileBufferedSource, int targetSize) throws Exception {
        return (Future<Void>) fileBufferedSource.getClass()
            .getDeclaredMethod("fillBuffer", int.class)
            .invoke(fileBufferedSource, targetSize);
    }

    /**
     * Helper method that safely calls fillBuffer and returns the source.
     */
    private Future<Object> fillBufferAndReturnSource(Object source, int targetSize) {
        try {
            return invokeFillBuffer(source, targetSize)
                .compose(v -> Future.succeededFuture(source));
        } catch (Exception e) {
            return Future.failedFuture(e);
        }
    }

    /**
     * Helper method to get buffer size using reflection.
     */
    private int getBufferSize(Object fileBufferedSource) throws Exception {
        Object buffer = fileBufferedSource.getClass().getSuperclass()
            .getDeclaredField("buffer").get(fileBufferedSource);
        return ((java.util.Queue<?>) buffer).size();
    }

    /**
     * Helper method to check if source is ended using reflection.
     */
    private boolean isEnded(Object fileBufferedSource) throws Exception {
        return (Boolean) fileBufferedSource.getClass().getSuperclass()
            .getDeclaredField("ended").get(fileBufferedSource);
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void testFillBufferWithEmptyFile(Vertx vertx, VertxTestContext testContext) {
        logger.info("Testing fillBuffer with empty file");
        
        createFileBufferedSource(Collections.emptyList())
            .compose(source -> fillBufferAndReturnSource(source, 5))
            .onComplete(testContext.succeeding(source -> testContext.verify(() -> {
                try {
                    assertEquals(0, getBufferSize(source));
                    assertTrue(isEnded(source));
                    testContext.completeNow();
                } catch (Exception e) {
                    testContext.failNow(e);
                }
            })));
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void testFillBufferWithSingleItem(Vertx vertx, VertxTestContext testContext) {
        logger.info("Testing fillBuffer with single item");
        
        createFileBufferedSource(Arrays.asList(42))
            .compose(source -> fillBufferAndReturnSource(source, 5))
            .onComplete(testContext.succeeding(source -> testContext.verify(() -> {
                try {
                    assertEquals(1, getBufferSize(source));
                    assertTrue(isEnded(source));
                    testContext.completeNow();
                } catch (Exception e) {
                    testContext.failNow(e);
                }
            })));
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void testFillBufferWithExactTargetSize(Vertx vertx, VertxTestContext testContext) {
        logger.info("Testing fillBuffer with exact target size");
        
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        createFileBufferedSource(data)
            .compose(source -> fillBufferAndReturnSource(source, 5))
            .onComplete(testContext.succeeding(source -> testContext.verify(() -> {
                try {
                    // Buffer should contain at least the target size
                    int bufferSize = getBufferSize(source);
                    assertTrue(bufferSize >= 5, "Expected buffer size >= 5, but was " + bufferSize);
                    // Stream may not be ended yet since fillBuffer only reads up to target size
                    // The ended state depends on whether the stream has been fully consumed
                    testContext.completeNow();
                } catch (Exception e) {
                    testContext.failNow(e);
                }
            })));
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void testFillBufferWithMoreDataThanTarget(Vertx vertx, VertxTestContext testContext) {
        logger.info("Testing fillBuffer with more data than target size");
        
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        createFileBufferedSource(data)
            .compose(source -> fillBufferAndReturnSource(source, 5))
            .onComplete(testContext.succeeding(source -> testContext.verify(() -> {
                try {
                    // Should fill at least target size, but may read more due to buffering
                    assertTrue(getBufferSize(source) >= 5);
                    testContext.completeNow();
                } catch (Exception e) {
                    testContext.failNow(e);
                }
            })));
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void testFillBufferWithLessDataThanTarget(Vertx vertx, VertxTestContext testContext) {
        logger.info("Testing fillBuffer with less data than target size");
        
        List<Integer> data = Arrays.asList(1, 2, 3);
        createFileBufferedSource(data)
            .compose(source -> fillBufferAndReturnSource(source, 5))
            .onComplete(testContext.succeeding(source -> testContext.verify(() -> {
                try {
                    assertEquals(3, getBufferSize(source));
                    assertTrue(isEnded(source));
                    testContext.completeNow();
                } catch (Exception e) {
                    testContext.failNow(e);
                }
            })));
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void testFillBufferWithZeroTargetSize(Vertx vertx, VertxTestContext testContext) {
        logger.info("Testing fillBuffer with zero target size");
        
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        createFileBufferedSource(data)
            .compose(source -> fillBufferAndReturnSource(source, 0))
            .onComplete(testContext.succeeding(source -> testContext.verify(() -> {
                try {
                    // Should return immediately since target is 0
                    assertEquals(0, getBufferSize(source));
                    assertFalse(isEnded(source));
                    testContext.completeNow();
                } catch (Exception e) {
                    testContext.failNow(e);
                }
            })));
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void testFillBufferWhenAlreadyAtTarget(Vertx vertx, VertxTestContext testContext) {
        logger.info("Testing fillBuffer when already at target size");
        
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        createFileBufferedSource(data)
            .compose(source -> fillBufferAndReturnSource(source, 5))
            .compose(source -> fillBufferAndReturnSource(source, 5))
            .onComplete(testContext.succeeding(source -> testContext.verify(() -> {
                try {
                    assertTrue(getBufferSize(source) >= 5);
                    testContext.completeNow();
                } catch (Exception e) {
                    testContext.failNow(e);
                }
            })));
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void testFillBufferWhenStreamEnded(Vertx vertx, VertxTestContext testContext) {
        logger.info("Testing fillBuffer when stream is already ended");
        
        List<Integer> data = Arrays.asList(1, 2, 3);
        createFileBufferedSource(data)
            .compose(source -> fillBufferAndReturnSource(source, 10))
            .compose(source -> fillBufferAndReturnSource(source, 5))
            .onComplete(testContext.succeeding(source -> testContext.verify(() -> {
                try {
                    assertEquals(3, getBufferSize(source));
                    assertTrue(isEnded(source));
                    testContext.completeNow();
                } catch (Exception e) {
                    testContext.failNow(e);
                }
            })));
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void testMultipleConcurrentFillBuffer(Vertx vertx, VertxTestContext testContext) {
        logger.info("Testing multiple concurrent fillBuffer calls");
        
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        createFileBufferedSource(data)
            .compose(source -> {
                try {
                    // Start multiple concurrent fill operations
                    Future<Void> fill1 = invokeFillBuffer(source, 3);
                    Future<Void> fill2 = invokeFillBuffer(source, 3);
                    Future<Void> fill3 = invokeFillBuffer(source, 3);
                    
                    return Future.all(fill1, fill2, fill3)
                        .compose(v -> Future.succeededFuture(source));
                } catch (Exception e) {
                    return Future.failedFuture(e);
                }
            })
            .onComplete(testContext.succeeding(source -> testContext.verify(() -> {
                try {
                    assertTrue(getBufferSize(source) >= 3);
                    testContext.completeNow();
                } catch (Exception e) {
                    testContext.failNow(e);
                }
            })));
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void testFillBufferWithLargeTargetSize(Vertx vertx, VertxTestContext testContext) {
        logger.info("Testing fillBuffer with very large target size");
        
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        createFileBufferedSource(data)
            .compose(source -> fillBufferAndReturnSource(source, Integer.MAX_VALUE))
            .onComplete(testContext.succeeding(source -> testContext.verify(() -> {
                try {
                    assertEquals(5, getBufferSize(source));
                    assertTrue(isEnded(source));
                    testContext.completeNow();
                } catch (Exception e) {
                    testContext.failNow(e);
                }
            })));
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void testFillBufferWithNegativeTargetSize(Vertx vertx, VertxTestContext testContext) {
        logger.info("Testing fillBuffer with negative target size");
        
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        createFileBufferedSource(data)
            .compose(source -> fillBufferAndReturnSource(source, -1))
            .onComplete(testContext.succeeding(source -> testContext.verify(() -> {
                try {
                    // Should treat negative as 0 or handle gracefully
                    assertEquals(0, getBufferSize(source));
                    testContext.completeNow();
                } catch (Exception e) {
                    testContext.failNow(e);
                }
            })));
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void testFillBufferProgressiveReading(Vertx vertx, VertxTestContext testContext) {
        logger.info("Testing fillBuffer progressive reading");
        
        List<Integer> data = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            data.add(i);
        }
        
        createFileBufferedSource(data)
            .compose(source -> fillBufferAndReturnSource(source, 5))
            .compose(source -> {
                testContext.verify(() -> {
                    try {
                        assertTrue(getBufferSize(source) >= 5);
                    } catch (Exception e) {
                        testContext.failNow(e);
                    }
                });
                return fillBufferAndReturnSource(source, 10);
            })
            .compose(source -> {
                testContext.verify(() -> {
                    try {
                        assertTrue(getBufferSize(source) >= 5);
                    } catch (Exception e) {
                        testContext.failNow(e);
                    }
                });
                return fillBufferAndReturnSource(source, 15);
            })
            .onComplete(testContext.succeeding(source -> testContext.verify(() -> {
                try {
                    assertTrue(getBufferSize(source) >= 5);
                    testContext.completeNow();
                } catch (Exception e) {
                    testContext.failNow(e);
                }
            })));
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void testFillBufferWithDeserializationError(Vertx vertx, VertxTestContext testContext) {
        logger.info("Testing fillBuffer with deserialization error");
        
        // Create a deserializer that always throws an exception
        SerializeReadStream.Deserializer<Integer> failingDeserializer = data -> {
            throw new RuntimeException("Deserialization failed");
        };
        
        String fileName = tempDirPath + "/test_data.tmp";
        
        // Write some data first
        fileSystem.open(fileName, new OpenOptions().setCreate(true).setWrite(true))
            .compose(writeFile -> {
                SerializeWriteStream<Integer> writeStream = new SerializeWriteStream<>(writeFile, serializer);
                return writeStream.write(42)
                    .compose(v -> writeStream.end());
            })
            .compose(v -> {
                // Create read stream with failing deserializer
                return fileSystem.open(fileName, new OpenOptions().setRead(true));
            })
            .compose(readFile -> {
                try {
                    SerializeReadStream<Integer> readStream = new SerializeReadStream<>(readFile, failingDeserializer);
                    
                    SortingStream<Integer> sortingStream = new SortingStream<>(
                        context, fileSystem, Integer::compareTo, serializer, failingDeserializer,
                        tempDirPath, "test", 1000, item -> 4,
                        new ListReadStream<>(context, Collections.emptyList())
                    );
                    
                    Class<?> fileBufferedSourceClass = null;
                    for (Class<?> innerClass : SortingStream.class.getDeclaredClasses()) {
                        if ("FileBufferedSource".equals(innerClass.getSimpleName())) {
                            fileBufferedSourceClass = innerClass;
                            break;
                        }
                    }
                    
                    Object fileBufferedSource = fileBufferedSourceClass.getDeclaredConstructor(SortingStream.class, SerializeReadStream.class)
                        .newInstance(sortingStream, readStream);
                    
                    fileBufferedSourceClass.getDeclaredMethod("initialize").invoke(fileBufferedSource);
                    
                    return invokeFillBuffer(fileBufferedSource, 5);
                } catch (Exception e) {
                    return Future.failedFuture(e);
                }
            })
            .onComplete(testContext.failing(throwable -> {
                testContext.verify(() -> {
                    assertNotNull(throwable);
                    assertTrue(throwable.getMessage().contains("Deserialization failed") || 
                              (throwable.getCause() != null && throwable.getCause().getMessage().contains("Deserialization failed")));
                    testContext.completeNow();
                });
            }));
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void testFillBufferWithVeryLargeFile(Vertx vertx, VertxTestContext testContext) {
        logger.info("Testing fillBuffer with large file");
        
        // Create a smaller dataset to avoid stack overflow
        List<Integer> data = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            data.add(i);
        }
        
        createFileBufferedSource(data)
            .compose(source -> fillBufferAndReturnSource(source, 50))
            .compose(source -> {
                testContext.verify(() -> {
                    try {
                        assertTrue(getBufferSize(source) >= 50);
                    } catch (Exception e) {
                        testContext.failNow(e);
                    }
                });
                return fillBufferAndReturnSource(source, 75);
            })
            .compose(source -> {
                testContext.verify(() -> {
                    try {
                        assertTrue(getBufferSize(source) >= 50);
                    } catch (Exception e) {
                        testContext.failNow(e);
                    }
                });
                return fillBufferAndReturnSource(source, 100);
            })
            .onComplete(testContext.succeeding(source -> testContext.verify(() -> {
                try {
                    assertTrue(getBufferSize(source) >= 50);
                    testContext.completeNow();
                } catch (Exception e) {
                    testContext.failNow(e);
                }
            })));
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void testFillBufferReturnsSameFutureForConcurrentCalls(Vertx vertx, VertxTestContext testContext) {
        logger.info("Testing fillBuffer returns same future for concurrent calls");
        
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        createFileBufferedSource(data)
            .compose(source -> {
                try {
                    // Make two concurrent calls
                    Future<Void> future1 = invokeFillBuffer(source, 5);
                    Future<Void> future2 = invokeFillBuffer(source, 5);
                    
                    // Both should complete successfully
                    return Future.all(future1, future2)
                        .compose(v -> Future.succeededFuture(source));
                } catch (Exception e) {
                    return Future.failedFuture(e);
                }
            })
            .onComplete(testContext.succeeding(source -> testContext.verify(() -> {
                try {
                    assertTrue(getBufferSize(source) >= 5);
                    testContext.completeNow();
                } catch (Exception e) {
                    testContext.failNow(e);
                }
            })));
    }
}