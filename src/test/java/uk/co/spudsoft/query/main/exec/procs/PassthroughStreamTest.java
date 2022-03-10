/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.procs;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PassthroughStreamTest {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(PassthroughStreamTest.class);

  private Future<Void> writeData(Vertx vertx, WriteStream<JsonObject> instance, int value, boolean queueEnd) {
    if (value < 0) {
      if (!queueEnd) {
        instance.end();
      }
      return Future.succeededFuture();
    } else {
      JsonObject data = new JsonObject().put("value", value);
      if (instance.writeQueueFull()) {
        Promise<Void> promise = Promise.promise();
        instance.drainHandler(v2 -> {
          instance.write(data, promise);
          instance.drainHandler(null);
        });
        if (value == 0 && queueEnd) {
          instance.end();
        }
        return promise.future().compose(v -> writeData(vertx, instance, value - 1, queueEnd));
      } else {
        instance.write(data);
        if (value == 0 && queueEnd) {
          instance.end();
        }
        return writeData(vertx, instance, value - 1, queueEnd);
      }
    }
  }
  
  @Test
  public void testQuickProcessor(Vertx vertx, VertxTestContext testContext) {    
    logger.info("testQuickProcessor");
    vertx.getOrCreateContext().runOnContext(v -> {
      long start = System.currentTimeMillis();
      PassthroughStream<JsonObject> instance = new PassthroughStream<>(
              row -> {
                return Future.succeededFuture(row);
              }
              , vertx.getOrCreateContext()
              , 3
      );
      instance.drainHandler(v2 -> {
        logger.info("Drained");
      }).exceptionHandler(ex -> {
        logger.warn("Failure: ", ex);
      });
      instance.getReadStream().handler(jo -> {
        logger.info("Received: {}", jo);
      }).endHandler(v2 -> {
        logger.info("Ended");
        testContext.completeNow();
        testContext.verify(() -> {
          assertThat(System.currentTimeMillis() - start, lessThan(1000L));
        });
        testContext.completeNow();
      });
      
      writeData(vertx, instance, 7, true)
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
      PassthroughStream<JsonObject> instance = new PassthroughStream<>(
              row -> {
                return Future.succeededFuture(row);
              }
              , vertx.getOrCreateContext()
              , 3
      );
      instance.drainHandler(v2 -> {
        logger.info("Drained");
      }).exceptionHandler(ex -> {
        logger.warn("Failure: ", ex);
      });
      instance.getReadStream().handler(jo -> {
        logger.info("Received: {}", jo);
        try {
          // This is clearly bad as this will run on a vertx thread (make enough repetitions and the blocked thread detector will trigger - it's not really blocked, but it does have the same stack trace)
          Thread.sleep(250);
        } catch(Throwable ex) {          
        }
      }).endHandler(v2 -> {
        logger.info("Ended");
        testContext.completeNow();
        testContext.verify(() -> {
          assertThat(System.currentTimeMillis() - start, greaterThan(1000L));
        });
        testContext.completeNow();
      });
      
      writeData(vertx, instance, 10, true)
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
      PassthroughStream<JsonObject> instance = new PassthroughStream<>(
              row -> {
                Promise<JsonObject> promise = Promise.promise();
                vertx.setTimer(250, l -> {
                  promise.complete(row);
                });
                return promise.future();
              }
              , vertx.getOrCreateContext()
              , 3
      );
      instance.drainHandler(v2 -> {
        logger.info("Drained");
      }).exceptionHandler(ex -> {
        logger.warn("Failure: ", ex);
      });
      instance.getReadStream().handler(jo -> {
        logger.info("Received: {}", jo);
      }).endHandler(v2 -> {
        logger.info("Ended");
        testContext.completeNow();
        testContext.verify(() -> {
          assertThat(System.currentTimeMillis() - start, greaterThan(1000L));
        });
        testContext.completeNow();
      });
      
      writeData(vertx, instance, 7, false)
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
      PassthroughStream<JsonObject> instance = new PassthroughStream<>(
              row -> {
                return Future.failedFuture("It didn't work");
              }
              , vertx.getOrCreateContext()
              , 3
      );
      instance.drainHandler(v2 -> {
        logger.info("Drained");
      }).exceptionHandler(ex -> {
        logger.warn("Failure: ", ex);
      });
      instance.getReadStream().handler(jo -> {
        logger.info("Received: {}", jo);
      }).endHandler(v2 -> {
        logger.info("Ended");
        testContext.completeNow();
        testContext.verify(() -> {
          assertThat(System.currentTimeMillis() - start, lessThan(1000L));
        });
        testContext.completeNow();
      });
      
      writeData(vertx, instance, 7, false)
              .onSuccess(v2 -> {
                logger.info("All data written");
              })
              .onFailure(ex -> {
                testContext.failNow(ex);
              })
              ;
    });
  }

}
