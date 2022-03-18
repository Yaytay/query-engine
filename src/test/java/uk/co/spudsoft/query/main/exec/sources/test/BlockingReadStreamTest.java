/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.sources.test;

import com.google.common.collect.ImmutableMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class BlockingReadStreamTest {
  
  private static final Logger logger = LoggerFactory.getLogger(BlockingReadStreamTest.class);
  
  @Test
  public void testNoData(Vertx vertx, VertxTestContext testContext) throws Exception {
    assertNotNull(vertx);
    BlockingReadStream<JsonObject> impl = new BlockingReadStream<>(vertx.getOrCreateContext(), 1);    
    impl.endHandler(v -> {
      // Queue isn't used in this test
      assertEquals(0, impl.size());
      testContext.completeNow();
    });
    impl.end();
  }
  
  @Test
  public void testNoDataPaused(Vertx vertx, VertxTestContext testContext) throws Exception {
    assertNotNull(vertx);
    BlockingReadStream<JsonObject> impl = new BlockingReadStream<>(vertx.getOrCreateContext(), 1);    
    impl.endHandler(v -> {
      // Queue isn't used in this test
      assertEquals(0, impl.size());
      testContext.completeNow();
    });
    impl.pause();
    impl.end();
  }
  
  @Test
  public void testNoHandlerThrowsAwayValues(Vertx vertx, VertxTestContext testContext) throws Exception {
    assertNotNull(vertx);
    BlockingReadStream<JsonObject> impl = new BlockingReadStream<>(vertx.getOrCreateContext(), 1);    
    impl.endHandler(v -> {
      // Queue isn't used in this test
      assertEquals(0, impl.size());
    });
    impl.resume();
    for (int i = 0; i < 10; ++i) {
      JsonObject jo = new JsonObject(ImmutableMap.of("value", i));
      impl.add(jo);
    }
    impl.end();
    assertEquals(0, impl.size());
    testContext.completeNow();
  }
  
  @Test
  public void testSimpleFlow(Vertx vertx, VertxTestContext testContext) throws Exception {
    assertNotNull(vertx);
    BlockingReadStream<JsonObject> impl = new BlockingReadStream<>(vertx.getOrCreateContext(), 1);    
    impl.handler(jo -> logger.debug("testSimpleFlow Received: {}", jo));
    impl.endHandler(v -> {
      // Queue isn't used in this test
      assertEquals(0, impl.size());
    });
    
    // Ensure resume is happy being called multiple times    
    impl.resume();
    for (int i = 0; i < 10; ++i) {
      JsonObject jo = new JsonObject(ImmutableMap.of("value", i));
      impl.add(jo);
      logger.debug("testSimpleFlow Posted: {} (queue size: {})", jo, impl.size());
    }
    impl.end();
    assertEquals(0, impl.size());    
    testContext.completeNow();
  }
  
  @Test
  public void testSimpleFlowWithLotsOfResuming(Vertx vertx, VertxTestContext testContext) throws Exception {
    assertNotNull(vertx);
    BlockingReadStream<JsonObject> impl = new BlockingReadStream<>(vertx.getOrCreateContext(), 1);    
    impl.handler(jo -> logger.debug("testSimpleFlowWithLotsOfResuming Received: {}", jo));
    impl.endHandler(v -> {
      // Queue isn't used in this test
      assertEquals(0, impl.size());
    });
    // Ensure resume is happy being called multiple times
    for (int i =0; i < 1000; ++i) {
      impl.resume();
    }
    for (int i = 0; i < 10; ++i) {
      JsonObject jo = new JsonObject(ImmutableMap.of("value", i));
      impl.add(jo);
      logger.debug("testSimpleFlowWithLotsOfResuming Posted: {} (queue size: {})", jo, impl.size());
      impl.resume();
    }
    impl.end();
    assertEquals(0, impl.size());    
    testContext.completeNow();
  }
  
  @Test
  public void testSimpleQueuing(Vertx vertx, VertxTestContext testContext) throws Exception {
    assertNotNull(vertx);
    BlockingReadStream<JsonObject> impl = new BlockingReadStream<>(vertx.getOrCreateContext(), 10);    
    impl.pause();
    impl.endHandler(v -> {
      // Queue is used, must be empty before end handler is called
      assertEquals(0, impl.size());
    });    
    impl.handler(jo -> logger.debug("testSimpleQueuing Received: {}", jo));
    for (int i = 0; i < 10; ++i) {
      JsonObject jo = new JsonObject(ImmutableMap.of("value", i));
      impl.add(jo);
      logger.debug("testSimpleQueuing Posted: {} (queue size: {})", jo, impl.size());
    }
    impl.end();
    assertEquals(10, impl.size());    
    testContext.completeNow();
  }
  
  @Test
  public void testSimpleQueuingThenFetchSome(Vertx vertx, VertxTestContext testContext) throws Exception {
    assertNotNull(vertx);
    BlockingReadStream<JsonObject> impl = new BlockingReadStream<>(vertx.getOrCreateContext(), 10);    
    impl.pause();
    impl.handler(jo -> logger.debug("testSimpleQueuingThenFetchSome Received: {}", jo));
    impl.endHandler(v -> {
      // Queue is used, must be empty before end handler is called
      assertEquals(0, impl.size());
    });    
    for (int i = 0; i < 10; ++i) {
      JsonObject jo = new JsonObject(ImmutableMap.of("value", i));
      impl.add(jo);
      logger.debug("testSimpleQueuingThenFetchSome Posted: {} (queue size: {})", jo, impl.size());
    }
    impl.end();
    assertEquals(10, impl.size());
    impl.drainHandler(v -> {
      assertEquals(7, impl.size());
      vertx.setTimer(10, l -> {
        assertEquals(7, impl.size());        
        testContext.completeNow();        
      });

    });
    impl.fetch(3);
  }
  
  @Test
  public void testPauseResume(Vertx vertx, VertxTestContext testContext) throws Exception {
    assertNotNull(vertx);
    BlockingReadStream<JsonObject> impl = new BlockingReadStream<>(vertx.getOrCreateContext(), 10);    
    impl.pause();
    impl.handler(jo -> {
      logger.debug("testPauseResume Received: {} (queue size: {})", jo, impl.size());
      impl.pause();
      vertx.setTimer(10, l -> {
        impl.resume();
      });
    });
    impl.endHandler(v -> {
      // Queue is used, must be empty before end handler is called
      assertEquals(0, impl.size());
    });    
    impl.resume();
    for (int i = 0; i < 10; ++i) {
      JsonObject jo = new JsonObject(ImmutableMap.of("value", i));
      impl.add(jo);
      logger.debug("testPauseResume Posted: {} (queue size: {})", jo, impl.size());
    }
    impl.end();
    impl.drainHandler(v -> {
      assertEquals(0, impl.size());
      testContext.completeNow();        
    });    
  }
  
  
  @Test
  public void testPauseResumeAndBlock(Vertx vertx, VertxTestContext testContext) throws Exception {
    assertNotNull(vertx);
    BlockingReadStream<JsonObject> impl = new BlockingReadStream<>(vertx.getOrCreateContext(), 4);    
    impl.pause();
    impl.handler(jo -> {
      logger.debug("testPauseResumeAndBlock Received: {} (queue size: {})", jo, impl.size());
      impl.pause();
      vertx.setTimer(10, l -> {
        impl.resume();
      });
    });
    impl.endHandler(v -> {
      // Queue is used, must be empty before end handler is called
      assertEquals(0, impl.size());
    });    
    impl.resume();
    for (int i = 0; i < 10; ++i) {
      JsonObject jo = new JsonObject(ImmutableMap.of("value", i));
      impl.add(jo);
      logger.debug("testPauseResumeAndBlock Posted: {} (queue size: {})", jo, impl.size());
    }
    impl.end();
    impl.drainHandler(v -> {
      assertEquals(0, impl.size());
      testContext.completeNow();        
    });    
  }
  
}
