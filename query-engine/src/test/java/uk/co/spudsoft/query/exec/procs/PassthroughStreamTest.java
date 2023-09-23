/*
 * Copyright (C) 2022 jtalbut
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
package uk.co.spudsoft.query.exec.procs;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.streams.WriteStream;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.DataRow;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import uk.co.spudsoft.query.exec.ColumnDefn;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PassthroughStreamTest {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(PassthroughStreamTest.class);

  private static List<ColumnDefn> types = new ArrayList<>();
  
  /**
   * Write simple DataRows to a WriteStream.
   * 
   * A total of value+1 items will be written to the stream.
   * Each item will have the structure: {"value":value}
   * The value will be decremented until value is zero (the zeroth value will be added to the stream) and this function will be called recursively.
   * 
   * Writing data to a stream is non-trivial as the writer should wait until the queue is not full before writing more data and should not call end until all data has been added.
   * 
   * @param vertx Vertx instance.
   * @param instance Stream that the data is to be written to.
   * @param value The current value to add to the stream.
   * @return A Future that will be completed when all data has been written.
   */
  public static Future<Void> writeData(Vertx vertx, WriteStream<DataRow> instance, int value) {
    if (value < 0) {
      instance.end();
      return Future.succeededFuture();
    } else {
      DataRow data = DataRow.create(types);
      data.put("value", value);
      if (instance.writeQueueFull()) {
        Promise<Void> promise = Promise.promise();
        instance.drainHandler(v2 -> {
          instance.write(data, promise);
          instance.drainHandler(null);
        });
        if (value == 0) {
          instance.end();
        }        
        Promise<Void> success = Promise.promise();        
        promise.future().
                onComplete(ar -> {
                  writeData(vertx, instance, value - 1);
                  success.complete();
                })
                ;
        return  success.future();
      } else {
        instance.write(data);
        if (value == 0) {
          instance.end();
        }
        return writeData(vertx, instance, value - 1);
      }
    }
  }
  
  @Test
  public void testQuickProcessor(Vertx vertx, VertxTestContext testContext) {    
    logger.info("testQuickProcessor");
    vertx.getOrCreateContext().runOnContext(v -> {
      long start = System.currentTimeMillis();
      PassthroughStream instance = new PassthroughStream(ctx -> {}
              ,
              (row, chain) -> {
                return chain.handle(row);
              }
              , vertx.getOrCreateContext()
      );
      instance.writeStream().drainHandler(v2 -> {
        logger.info("Drained");
      }).exceptionHandler(ex -> {
        logger.warn("Failure: ", ex);
      });
      instance.readStream().handler(jo -> {
        logger.info("Received: {}", jo);
      }).endHandler(v2 -> {
        logger.info("Ended");
        testContext.completeNow();
        testContext.verify(() -> {
          assertThat(System.currentTimeMillis() - start, lessThan(1000L));
        });
        testContext.completeNow();
      });
      
      writeData(vertx, instance.writeStream(), 7)
              .onSuccess(v2 -> {
                logger.info("All data written");
              })
              .onFailure(ex -> {
                testContext.failNow(ex);
              })
              ;
    });
  }

  @Test
  public void testSlowReader(Vertx vertx, VertxTestContext testContext) {    
    logger.info("testSlowReader");
    vertx.getOrCreateContext().runOnContext(v -> {
      long start = System.currentTimeMillis();
      PassthroughStream instance = new PassthroughStream(ctx -> {}
              ,
              (row, chain) -> {
                return chain.handle(row);
              }
              , vertx.getOrCreateContext()
      );
      instance.writeStream().drainHandler(v2 -> {
        logger.info("Drained");
      }).exceptionHandler(ex -> {
        logger.warn("Failure: ", ex);
      });
      instance.readStream().handler(jo -> {
        logger.info("Received: {}", jo);
        try {
          // This is clearly bad as this will run on a vertx thread (make enough repetitions and the blocked thread detector will trigger - it's not really blocked, but it does have the same stack trace)
          Thread.sleep(250);
        } catch (Throwable ex) {          
        }
      }).endHandler(v2 -> {
        logger.info("Ended");
        testContext.completeNow();
        testContext.verify(() -> {
          assertThat(System.currentTimeMillis() - start, greaterThan(1000L));
        });
        testContext.completeNow();
      });
      
      writeData(vertx, instance.writeStream(), 10)
              .onSuccess(v2 -> {
                logger.info("All data written");
              })
              .onFailure(ex -> {
                testContext.failNow(ex);
              })
              ;
    });
  }

  @Test
  public void testBadReader(Vertx vertx, VertxTestContext testContext) {    
    logger.info("testBadReader");
    vertx.getOrCreateContext().runOnContext(v -> {
      long start = System.currentTimeMillis();
      PassthroughStream instance = new PassthroughStream(ctx -> {}
              ,
              (row, chain) -> {
                return chain.handle(row);
              }
              , vertx.getOrCreateContext()
      );
      instance.writeStream().drainHandler(v2 -> {
        logger.info("Drained");
      }).exceptionHandler(ex -> {
        logger.warn("Failure: ", ex);
      });
      instance.readStream().handler(jo -> {
        logger.info("Received: {}", jo);
        throw new IllegalStateException("Bad reader");
      }).endHandler(v2 -> {
        logger.info("Ended");
        testContext.completeNow();
      }).exceptionHandler(ex -> {
        logger.warn("Exception: ", ex);
      });
      
      writeData(vertx, instance.writeStream(), 10)
              .onSuccess(v2 -> {
                logger.info("All data written");
              })
              .onFailure(ex -> {
                testContext.failNow(ex);
              })
              ;
    });
  }


  @Test
  public void testBadReaderWithoutExceptionHandler(Vertx vertx, VertxTestContext testContext) {    
    logger.info("testBadReaderWithoutExceptionHandler");
    vertx.getOrCreateContext().runOnContext(v -> {
      long start = System.currentTimeMillis();
      PassthroughStream instance = new PassthroughStream(ctx -> {}
              ,
              (row, chain) -> {
                return chain.handle(row);
              }
              , vertx.getOrCreateContext()
      );
      instance.writeStream().drainHandler(v2 -> {
        logger.info("Drained");
      });
      instance.readStream().handler(jo -> {
        logger.info("Received: {}", jo);
        throw new IllegalStateException("Bad reader");
      }).endHandler(v2 -> {
        logger.info("Ended");
        testContext.completeNow();
        testContext.completeNow();
      });
      
      writeData(vertx, instance.writeStream(), 10)
              .onSuccess(v2 -> {
                logger.info("All data written");
              })
              .onFailure(ex -> {
                testContext.failNow(ex);
              })
              ;
    });
  }

  @Test
  public void testSlowProcessor(Vertx vertx, VertxTestContext testContext) {
    logger.info("testSlowProcessor");
    vertx.getOrCreateContext().runOnContext(v -> {
      long start = System.currentTimeMillis();
      PassthroughStream instance = new PassthroughStream(ctx -> {}
              ,
              (row, chain) -> {
                Promise<Void> promise = Promise.promise();
                vertx.setTimer(250, l -> {
                  chain.handle(row)
                          .onComplete(promise);
                });
                return promise.future();
              }
              , vertx.getOrCreateContext()
      );
      instance.writeStream().drainHandler(v2 -> {
        logger.info("Drained");
      }).exceptionHandler(ex -> {
        logger.warn("Failure: ", ex);
      });
      instance.readStream().handler(jo -> {
        logger.info("Received: {}", jo);
      }).endHandler(v2 -> {
        logger.info("Ended");
        testContext.completeNow();
        testContext.verify(() -> {
          assertThat(System.currentTimeMillis() - start, greaterThan(1000L));
        });
        testContext.completeNow();
      });
      
      writeData(vertx, instance.writeStream(), 7)
              .onSuccess(v2 -> {
                logger.info("All data written");
              })
              .onFailure(ex -> {
                testContext.failNow(ex);
              })
              ;
    });
  }
    
  @Test
  public void testNoChainProcessor(Vertx vertx, VertxTestContext testContext) {
    logger.info("testNoChainProcessor");
    vertx.getOrCreateContext().runOnContext(v -> {
      long start = System.currentTimeMillis();
      PassthroughStream instance = new PassthroughStream(ctx -> {}
              ,
              (row, chain) -> {
                return Future.succeededFuture();
              }
              , vertx.getOrCreateContext()
      );
      instance.writeStream().drainHandler(v2 -> {
        logger.info("Drained");
      }).exceptionHandler(ex -> {
        logger.warn("Failure: ", ex);
      });
      instance.readStream().handler(jo -> {
        logger.info("Received: {}", jo);
      }).endHandler(v2 -> {
        logger.info("Ended");
        testContext.completeNow();
        testContext.verify(() -> {
          assertThat(System.currentTimeMillis() - start, lessThan(1000L));
        });
        testContext.completeNow();
      });
      
      writeData(vertx, instance.writeStream(), 7)
              .onSuccess(v2 -> {
                logger.info("All data written");
              })
              .onFailure(ex -> {
                testContext.failNow(ex);
              })
              ;
    });
  }
    
  @Test
  public void testBadProcessor(Vertx vertx, VertxTestContext testContext) {    
    logger.info("testBadProcessor");
    vertx.getOrCreateContext().runOnContext(v -> {
      long start = System.currentTimeMillis();
      PassthroughStream instance = new PassthroughStream(ctx -> {}
              ,
              (row, chain) -> {
                return Future.failedFuture("It didn't work");
              }
              , vertx.getOrCreateContext()
      );      
      instance.writeStream().drainHandler(v2 -> {
        logger.info("Drained");
      }).exceptionHandler(ex -> {
        logger.warn("Failure: ", ex);
      });
      AtomicLong received = new AtomicLong();
      instance.readStream().handler(jo -> {
        logger.info("Received: {}", jo);
        received.incrementAndGet();
      }).endHandler(v2 -> {
        logger.info("Ended");
        testContext.completeNow();
        testContext.verify(() -> {
          assertThat(System.currentTimeMillis() - start, lessThan(1000L));
        });
        testContext.completeNow();
      });
      instance.readStream().resume();
      try {
        instance.readStream().fetch(-1);
        testContext.failNow("Should have thrown IllegalArgumentException");
      } catch (IllegalArgumentException ex) {
      }
      
      
      writeData(vertx, instance.writeStream(), 7)
              .onSuccess(v2 -> {
                testContext.verify(() -> {
                  assertEquals(0, received.get());
                });
                testContext.completeNow();
              })
              .onFailure(ex -> {
                testContext.failNow(ex);
              })
              ;
    });
  }

  @Test
  public void testVeryBadProcessor(Vertx vertx, VertxTestContext testContext) {    
    logger.info("testVeryBadProcessor");
    vertx.getOrCreateContext().runOnContext(v -> {
      long start = System.currentTimeMillis();
      PassthroughStream instance = new PassthroughStream(ctx -> {}
              ,
              (row, chain) -> {
                throw new IllegalStateException("Very bad");
              }
              , vertx.getOrCreateContext()
      );      
      instance.writeStream().drainHandler(v2 -> {
        logger.info("Drained");
      }).exceptionHandler(ex -> {
        logger.warn("Failure: ", ex);
      });
      AtomicLong received = new AtomicLong();
      instance.readStream().handler(jo -> {
        logger.info("Received: {}", jo);
        received.incrementAndGet();
      }).endHandler(v2 -> {
        logger.info("Ended");
        testContext.completeNow();
        testContext.verify(() -> {
          assertThat(System.currentTimeMillis() - start, lessThan(1000L));
        });
        testContext.completeNow();
      }).exceptionHandler(ex -> {
        logger.warn("Exception: ", ex);
      });
      instance.readStream().pause().fetch(12);
      
      writeData(vertx, instance.writeStream(), 7)
              .onSuccess(v2 -> {
                testContext.verify(() -> {
                  assertEquals(0, received.get());
                });
                testContext.completeNow();
              })
              .onFailure(ex -> {
                testContext.failNow(ex);
              })
              ;
    });
  }


  @Test
  public void testVeryBadProcessorWithoutExceptionHandler(Vertx vertx, VertxTestContext testContext) {    
    logger.info("testVeryBadProcessorWithoutExceptionHandler");
    vertx.getOrCreateContext().runOnContext(v -> {
      long start = System.currentTimeMillis();
      PassthroughStream instance = new PassthroughStream(ctx -> {}
              ,
              (row, chain) -> {
                throw new IllegalStateException("Very bad");
              }
              , vertx.getOrCreateContext()
      );      
      instance.writeStream().drainHandler(v2 -> {
        logger.info("Drained");
      });
      AtomicLong received = new AtomicLong();
      instance.readStream().handler(jo -> {
        logger.info("Received: {}", jo);
        received.incrementAndGet();
      }).endHandler(v2 -> {
        logger.info("Ended");
        testContext.completeNow();
        testContext.verify(() -> {
          assertThat(System.currentTimeMillis() - start, lessThan(1000L));
        });
        testContext.completeNow();
      });
      instance.readStream().pause().fetch(12);
      
      writeData(vertx, instance.writeStream(), 7)
              .onSuccess(v2 -> {
                testContext.verify(() -> {
                  assertEquals(0, received.get());
                });
                testContext.completeNow();
              })
              .onFailure(ex -> {
                testContext.failNow(ex);
              })
              ;
    });
  }

  @Test
  public void basicCoverageChecks() {
    PassthroughStream instance = new PassthroughStream(ctx -> {}, (d,c) -> Future.succeededFuture(), null);
    try {
      instance.writeStream().setWriteQueueMaxSize(0);
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException ex) {      
    }
    instance.readStream().pause().resume().resume();
    
    instance.readStream().pause();
    instance.writeStream().write(DataRow.create(types));
    
    instance.readStream().endHandler(null);
    instance.writeStream().end();
    
    assertTrue(instance.writeStream().writeQueueFull());
    
    try {
      instance.writeStream().write(DataRow.create(types));
      fail("Expected IllegalStateException");
    } catch (IllegalStateException ex) {
    }
  }
  

}
