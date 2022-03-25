/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.procs.limit;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.main.defn.ProcessorLimit;
import uk.co.spudsoft.query.main.exec.procs.PassthroughStreamTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProcessorLimitInstanceTest {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ProcessorLimitInstanceTest.class);
  
  @Test
  public void testInitialize(Vertx vertx) {
    ProcessorLimitInstance instance = new ProcessorLimitInstance(vertx, vertx.getOrCreateContext(), null);
    assertEquals(Future.succeededFuture(), instance.initialize(null, null));
  }
  
  
  
  @Test
  public void testStream(Vertx vertx, VertxTestContext testContext) {
    
    ProcessorLimit definition = ProcessorLimit.builder()
            .limit(3)
            .build();
    ProcessorLimitInstance instance = new ProcessorLimitInstance(vertx, vertx.getOrCreateContext(), definition);
    WriteStream<JsonObject> write = instance.getWriteStream();
    ReadStream<JsonObject> read = instance.getReadStream();
    List<JsonObject> received = new ArrayList<>();
    read.fetch(12);
    read.handler(jo -> {
      received.add(jo);
    });
    read.exceptionHandler(ex -> {
      logger.debug("Exception: ", ex);
    });
    read.endHandler(v -> {
      testContext.verify(() -> {
        assertThat(received, hasSize(3));
        assertThat(received.get(0).getInteger("value"), equalTo(6));
        assertThat(received.get(1).getInteger("value"), equalTo(5));
        assertThat(received.get(2).getInteger("value"), equalTo(4));
      });
      testContext.completeNow();
    });
    PassthroughStreamTest.writeData(vertx, write, 6);
  }

}
