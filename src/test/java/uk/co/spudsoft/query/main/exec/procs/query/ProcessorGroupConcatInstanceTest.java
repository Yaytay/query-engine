/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.procs.query;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.query.main.defn.DestinationLogger;
import uk.co.spudsoft.query.main.defn.Pipeline;
import uk.co.spudsoft.query.main.defn.ProcessorGroupConcat;
import uk.co.spudsoft.query.main.defn.ProcessorScript;
import uk.co.spudsoft.query.main.defn.SourcePipeline;
import uk.co.spudsoft.query.main.defn.SourceTest;
import uk.co.spudsoft.query.main.exec.PipelineExecutor;
import uk.co.spudsoft.query.main.exec.PipelineExecutorImpl;
import uk.co.spudsoft.query.main.exec.PipelineInstance;
import uk.co.spudsoft.query.main.exec.dests.logger.DestinationLoggerInstance;
import uk.co.spudsoft.query.main.exec.dests.logger.LoggingWriteStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProcessorGroupConcatInstanceTest {
  
  @Test
  public void testGetId() {
    assertNull(ProcessorGroupConcatInstance.getId(null, "id"));
    assertEquals(17L, ProcessorGroupConcatInstance.getId(new JsonObject().put("id", 17L), "id"));
    assertNull(ProcessorGroupConcatInstance.getId(new JsonObject().put("id", new Object()), "id"));
    assertNull(ProcessorGroupConcatInstance.getId(new JsonObject().put("id", null), "id"));
    assertNull(ProcessorGroupConcatInstance.getId(new JsonObject().put("id", 17L), "notId"));
  }
  
  @Test
  @Timeout(value = 5, timeUnit = TimeUnit.SECONDS)
  public void testChildEndsEarly(Vertx vertx, VertxTestContext testContext) {
    
    Pipeline pipeline = Pipeline.builder()
            .source(SourceTest.builder().rowCount(10).build())
            .processors(
                    Arrays.asList(
                            ProcessorGroupConcat.builder()
                                    .input(
                                            SourcePipeline.builder()
                                                    .source(SourceTest.builder().rowCount(7).build())
                                                    .build()                                            
                                    )
                                    .parentIdColumn("value")
                                    .childIdColumn("value")
                                    .childValueColumn("value")
                                    .parentValueColumn("child")
                                    .build()
                    )
            )
            .destination(DestinationLogger.builder().build())
            .build();
    
    PipelineExecutor executor = new PipelineExecutorImpl();
    
    DestinationLoggerInstance dest = (DestinationLoggerInstance) pipeline.getDestination().createInstance(vertx, vertx.getOrCreateContext());
    PipelineInstance pipelineInstance = new PipelineInstance(
            null
            , pipeline.getSourceEndpoints()
            , pipeline.getSource().createInstance(vertx, vertx.getOrCreateContext())
            , executor.createProcessors(vertx, vertx.getOrCreateContext(), pipeline)
            , dest
    );
    
    executor.initializePipeline(pipelineInstance)
            .compose(v -> pipelineInstance.getFinalPromise().future())
            .onComplete(ar -> {
              testContext.verify(() -> {
                assertEquals(10, ((LoggingWriteStream) dest.getWriteStream()).getCount());
              });
              testContext.<Void>succeedingThenComplete().handle(ar);
            })
            ;
  }  
  
  @Test
  @Timeout(value = 5, timeUnit = TimeUnit.SECONDS)
  public void testParentSuppliesEveryOther(Vertx vertx, VertxTestContext testContext) {
    
    Pipeline pipeline = Pipeline.builder()
            .source(SourceTest.builder().rowCount(10).name("parent").build())
            .processors(
                    Arrays.asList(
                            ProcessorScript.builder()
                                    .language("js")
                                    .predicate("data.value % 2 == 0")
                                    .process("data.count = data.count == null ? 1 : data.count + 1")
                                    .build()
                            , ProcessorGroupConcat.builder()
                                    .input(
                                            SourcePipeline.builder()
                                                    .source(SourceTest.builder().rowCount(7).name("child").build())
                                                    .processors(
                                                            Arrays.asList(
                                                                    ProcessorScript.builder()
                                                                            .language("js")
                                                                            .process("data.count = data.count == null ? 1 : data.count + 1")
                                                                            .build()
                                                            )
                                                    )
                                                    .build()                                            
                                    )
                                    .delimiter(", ")
                                    .parentIdColumn("value")
                                    .childIdColumn("value")
                                    .childValueColumn("value")
                                    .parentValueColumn("children")
                                    .build()
                    )
            )
            .destination(DestinationLogger.builder().build())
            .build();
    
    PipelineExecutor executor = new PipelineExecutorImpl();
    
    DestinationLoggerInstance dest = (DestinationLoggerInstance) pipeline.getDestination().createInstance(vertx, vertx.getOrCreateContext());
    PipelineInstance pipelineInstance = new PipelineInstance(
            null
            , pipeline.getSourceEndpoints()
            , pipeline.getSource().createInstance(vertx, vertx.getOrCreateContext())
            , executor.createProcessors(vertx, vertx.getOrCreateContext(), pipeline)
            , dest
    );
    
    executor.initializePipeline(pipelineInstance)
            .compose(v -> pipelineInstance.getFinalPromise().future())
            .onComplete(ar -> {
              testContext.verify(() -> {
                assertEquals(5, ((LoggingWriteStream) dest.getWriteStream()).getCount());
              });
              testContext.<Void>succeedingThenComplete().handle(ar);
            })
            ;
  }
   
  @Test
  @Timeout(value = 5, timeUnit = TimeUnit.SECONDS)
  public void testChildSuppliesEveryOther(Vertx vertx, VertxTestContext testContext) {
    
    Pipeline pipeline = Pipeline.builder()
            .source(SourceTest.builder().rowCount(10).build())
            .processors(
                    Arrays.asList(
                            ProcessorGroupConcat.builder()
                                    .input(
                                            SourcePipeline.builder()
                                                    .source(SourceTest.builder().rowCount(7).build())
                                                    .processors(
                                                            Arrays.asList(
                                                                    ProcessorScript.builder()
                                                                            .language("js")
                                                                            .predicate("data.value % 2 == 0")
                                                                            .build()
                                                            )
                                                    )
                                                    .build()                                            
                                    )
                                    .delimiter(", ")
                                    .parentIdColumn("value")
                                    .childIdColumn("value")
                                    .childValueColumn("value")
                                    .parentValueColumn("child")
                                    .build()
                    )
            )
            .destination(DestinationLogger.builder().build())
            .build();
    
    PipelineExecutor executor = new PipelineExecutorImpl();
    
    DestinationLoggerInstance dest = (DestinationLoggerInstance) pipeline.getDestination().createInstance(vertx, vertx.getOrCreateContext());
    PipelineInstance pipelineInstance = new PipelineInstance(
            null
            , pipeline.getSourceEndpoints()
            , pipeline.getSource().createInstance(vertx, vertx.getOrCreateContext())
            , executor.createProcessors(vertx, vertx.getOrCreateContext(), pipeline)
            , dest
    );
    
    executor.initializePipeline(pipelineInstance)
            .compose(v -> pipelineInstance.getFinalPromise().future())
            .onComplete(ar -> {
              testContext.verify(() -> {
                assertEquals(10, ((LoggingWriteStream) dest.getWriteStream()).getCount());
              });
              testContext.<Void>succeedingThenComplete().handle(ar);
            })
            ;
  }
  
}
