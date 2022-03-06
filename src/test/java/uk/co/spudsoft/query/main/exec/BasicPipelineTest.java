/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.query.main.defn.SourceTest;
import uk.co.spudsoft.query.main.exec.dests.logger.DestinationLogger;
import uk.co.spudsoft.query.main.exec.sources.test.TestSource;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class BasicPipelineTest {
  
  @Test
  public void testBasicPipeline(Vertx vertx, VertxTestContext testContext) {
    PipelineInstance pipeline = PipelineInstance.builder()
            .source(new TestSource(vertx.getOrCreateContext(), SourceTest.builder().rowCount(100).build()))
            .sink(new DestinationLogger())
            .build();
    PipelineExecutor executor = new PipelineExecutor();
    executor.executePipeline(pipeline)
            .onSuccess(v -> {
              testContext.completeNow();
            })
            .onFailure(ex -> {
              testContext.failNow(ex);
            })
            ;
  }
  
}
