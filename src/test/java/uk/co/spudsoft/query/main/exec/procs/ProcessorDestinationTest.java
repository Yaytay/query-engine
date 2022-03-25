/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.procs;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

/**
 *
 * @author jtalbut
 */
public class ProcessorDestinationTest {
  
  @Test
  public void testInitialize() {
    ProcessorDestination instance = new ProcessorDestination(null);
    // Initialize is a noop
    assertEquals(Future.succeededFuture(), instance.initialize(null, null));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetWriteStream() {
    WriteStream<JsonObject> stream = mock(WriteStream.class);
    ProcessorDestination instance = new ProcessorDestination(stream);
    assertSame(stream, instance.getWriteStream());
  }
  
}
