/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.procs;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;



/**
 * Some additional coverage tests for the PassthroughReadStream class.
 * Ideally this class would be entirely tested via the PassthroughStream, but there are some conditions that are difficult to achieve that way.
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class PassthroughReadStreamTest {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(PassthroughReadStreamTest.class);
  
  @Test
  public void testHandle(Vertx vertx, VertxTestContext testContext) {
    AtomicLong counter = new AtomicLong();
    PassthroughReadStream<JsonObject> instance = new PassthroughReadStream<>(vertx.getOrCreateContext(), 100);
    instance.handler(jo -> {
      logger.debug("Receiving on this thread ({})", jo);
      counter.incrementAndGet();
    });
    instance.endHandler(v -> {
      logger.debug("Ended with counter={}", counter.get());
      testContext.verify(() -> {
        assertThat(counter.get(), equalTo(100L));
      });
      testContext.completeNow();
    });
    instance.pause();
    vertx.executeBlocking(p -> {
      for (int i = 0; i < 100; ++i) {
        logger.debug("Adding on this thread ({})", i);
        instance.handle(new JsonObject().put("i", i));
      }
    });
    instance.fetch(10);
    Awaitility.await().atMost(1, TimeUnit.MINUTES).until(() -> counter.get() >= 10);
    try {
      Thread.sleep(100);
    } catch(InterruptedException ex) {        
    }
    assertThat(counter.get(), equalTo(10L));
    instance.fetch(13);
    Awaitility.await().atMost(1, TimeUnit.MINUTES).until(() -> counter.get() >= 23);
    try {
      Thread.sleep(100);
    } catch(InterruptedException ex) {        
    }
    assertThat(counter.get(), equalTo(23L));
    instance.end();
    instance.resume();
  }
  
  @Test
  public void testWithoutEndHandler(Vertx vertx) {
    AtomicLong counter = new AtomicLong();
    PassthroughReadStream<JsonObject> instance = new PassthroughReadStream<>(vertx.getOrCreateContext(), 100);
    instance.handler(jo -> {
      logger.debug("Receiving on this thread ({})", jo);
      counter.incrementAndGet();
    });
    instance.pause();
    vertx.executeBlocking(p -> {
      for (int i = 0; i < 2; ++i) {
        logger.debug("Adding on this thread ({})", i);
        instance.handle(new JsonObject().put("i", i));
      }
    });
    instance.end();
    instance.resume();
    Awaitility.await().atMost(1, TimeUnit.MINUTES).until(() -> counter.get() >= 2);
  }

}
