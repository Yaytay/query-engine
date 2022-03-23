/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.procs;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PassthroughWriteStreamTest {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(PassthroughWriteStreamTest.class);

  @Test
  public void testQuickProcessor(Vertx vertx, VertxTestContext testContext) {    
    logger.info("testQuickProcessor");
    vertx.getOrCreateContext().runOnContext(v -> {
      long start = System.currentTimeMillis();
      PassthroughWriteStream<JsonObject> instance = new PassthroughWriteStream<>(
              (row) -> {
                return Future.succeededFuture();
              }
              , vertx.getOrCreateContext()
      );
      instance.writeStream().drainHandler(v2 -> {
        logger.info("Drained");
      }).exceptionHandler(ex -> {
        logger.warn("Failure: ", ex);
      });
      instance.endHandler(v2 -> {
        logger.info("Ended");
        testContext.completeNow();
        testContext.verify(() -> {
          assertThat(System.currentTimeMillis() - start, lessThan(1000L));
        });
        testContext.completeNow();
      });
      
      PassthroughStreamTest.writeData(vertx, instance.writeStream(), 7)
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
  public void testVeryBadProcessor(Vertx vertx, VertxTestContext testContext) {    
    logger.info("testQuickProcessor");
    vertx.getOrCreateContext().runOnContext(v -> {
      long start = System.currentTimeMillis();
      PassthroughWriteStream<JsonObject> instance = new PassthroughWriteStream<>(
              (row) -> {
                throw new IllegalStateException("Test");
              }
              , vertx.getOrCreateContext()
      );
      instance.writeStream().drainHandler(v2 -> {
        logger.info("Drained");
      }).exceptionHandler(ex -> {
        logger.warn("Failure: ", ex);
      });
      instance.endHandler(v2 -> {
        logger.info("Ended");
        testContext.completeNow();
        testContext.verify(() -> {
          assertThat(System.currentTimeMillis() - start, lessThan(1000L));
        });
        testContext.completeNow();
      });
      
      PassthroughStreamTest.writeData(vertx, instance.writeStream(), 7)
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
  public void basicCoverageChecks(Vertx vertx) {
    AsyncHandler<JsonObject> handler = d -> {
      logger.debug("Handling: {}", d);
      return Future.succeededFuture();
    };
    PassthroughWriteStream<JsonObject> instance = new PassthroughWriteStream<>(handler, vertx.getOrCreateContext());
    try {
      instance.writeStream().setWriteQueueMaxSize(0);
      fail("Expected UnsupportedOperationException");
    } catch(UnsupportedOperationException ex) {      
    }
    instance.writeStream().write(new JsonObject());    
    instance.writeStream().end();    
    assertTrue(instance.writeStream().writeQueueFull());
  }
  
}
