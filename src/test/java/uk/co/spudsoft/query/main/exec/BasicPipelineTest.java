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
import uk.co.spudsoft.query.main.exec.dests.logger.DestinationLoggerInstance;
import uk.co.spudsoft.query.main.exec.sources.test.SourceTestInstance;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class BasicPipelineTest {
  
  @Test
  public void testBasicPipeline(Vertx vertx, VertxTestContext testContext) {
    PipelineInstance pipeline = new PipelineInstance(null
            , null
            , new SourceTestInstance(vertx.getOrCreateContext(), SourceTest.builder().rowCount(100).build())
            , null
            , new DestinationLoggerInstance()
    );
    PipelineExecutorImpl executor = new PipelineExecutorImpl();
    executor.initializePipeline(pipeline)
            .onSuccess(v -> {
              testContext.completeNow();
            })
            .onFailure(ex -> {
              testContext.failNow(ex);
            })
            ;
  }
  
}
