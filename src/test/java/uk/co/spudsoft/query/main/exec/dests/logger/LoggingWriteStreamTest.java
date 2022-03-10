/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.dests.logger;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * @author jtalbut
 */
public class LoggingWriteStreamTest {
  
  public LoggingWriteStreamTest() {
  }

  @Test
  // The LoggingWriteStream cannot throw any exceptions, so this is testing a noop for coverage.
  public void testExceptionHandler() {
    LoggingWriteStream<JsonObject> instance = new LoggingWriteStream<>();
    instance.exceptionHandler(ex -> {});
  }

  @Test
  public void testWrite() {
    LoggingWriteStream<JsonObject> instance = new LoggingWriteStream<>();
    Future<Void> future = instance.write(new JsonObject().put("test", "value"));
    assertTrue(future.succeeded());
  }

  @Test
  // The LoggingWriteStream has no write queue, so this is testing a noop for coverage.
  public void testSetWriteQueueMaxSize() {
    LoggingWriteStream<JsonObject> instance = new LoggingWriteStream<>();
    instance.setWriteQueueMaxSize(-3);
  }

  @Test
  // The LoggingWriteStream has no implemented drain handler, because it has no write queue, so this is testing a noop for coverage.
  public void testDrainHandler() {
    LoggingWriteStream<JsonObject> instance = new LoggingWriteStream<>();
    try {
      instance.drainHandler(v -> {});
      fail("Should have thrown");
    } catch(UnsupportedOperationException ex) {
    }
  }
  
}
