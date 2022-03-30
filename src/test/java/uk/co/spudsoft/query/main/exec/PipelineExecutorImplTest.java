/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import com.google.common.collect.ImmutableMap;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.query.main.defn.Argument;
import uk.co.spudsoft.query.main.defn.ArgumentType;
import uk.co.spudsoft.query.main.defn.DestinationLogger;
import uk.co.spudsoft.query.main.defn.Pipeline;
import uk.co.spudsoft.query.main.defn.ProcessorLimit;
import uk.co.spudsoft.query.main.defn.SourceTest;
import uk.co.spudsoft.query.main.exec.procs.limit.ProcessorLimitInstance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PipelineExecutorImplTest {
  
  @Test
  public void testValidatePipeline(Vertx vertx, VertxTestContext testContext) {
    Pipeline definition = Pipeline.builder().build();
    PipelineExecutorImpl instance = new PipelineExecutorImpl();
    instance.validatePipeline(definition).onComplete(testContext.succeedingThenComplete());
  }

  @Test
  public void testCreateProcessors(Vertx vertx) {
    Pipeline definition = Pipeline.builder()
            .processors(
                    Arrays.asList(
                            ProcessorLimit.builder().limit(1).build()
                            , ProcessorLimit.builder().limit(2).build()
                    )
            )
            .build();
    PipelineExecutorImpl instance = new PipelineExecutorImpl();
    List<ProcessorInstance> results = instance.createProcessors(vertx, vertx.getOrCreateContext(), definition);
    assertThat(results, hasSize(2));
    assertEquals(1, ((ProcessorLimitInstance) results.get(0)).getLimit());
    assertEquals(2, ((ProcessorLimitInstance) results.get(1)).getLimit());
  }

  @Test
  public void testPrepareArguments() {
    PipelineExecutorImpl instance = new PipelineExecutorImpl();
    Map<String, ArgumentInstance> result = instance.prepareArguments(
            ImmutableMap.<String, Argument>builder()
                    .put("arg1", Argument.builder().type(ArgumentType.Long).defaultValue("12").build())
                    .put("arg2", Argument.builder().type(ArgumentType.String).defaultValue("message").build())
                    .put("arg3", Argument.builder().type(ArgumentType.String).build())
                    .build()
            , 
            ImmutableMap.<String, String>builder()
                    .build()
            );
    assertEquals(3, result.size());
    assertEquals("12", result.get("arg1").getValue());
    assertEquals("message", result.get("arg2").getValue());
    assertNull(result.get("arg3").getValue());
    
    result = instance.prepareArguments(
            ImmutableMap.<String, Argument>builder()
                    .put("arg1", Argument.builder().type(ArgumentType.Long).defaultValue("12").build())
                    .put("arg2", Argument.builder().type(ArgumentType.String).defaultValue("message").build())
                    .put("arg3", Argument.builder().type(ArgumentType.String).build())
                    .build()
            , 
            ImmutableMap.<String, String>builder()
                    .put("arg1", "17")
                    .put("arg2", "second")
                    .put("arg3", "third")
                    .build()
            );
    assertEquals(3, result.size());
    assertEquals("17", result.get("arg1").getValue());
    assertEquals("second", result.get("arg2").getValue());
    assertEquals("third", result.get("arg3").getValue());
  }

  @Test
  public void testInitializePipeline(Vertx vertx, VertxTestContext testContext) {
    
    Pipeline definition = Pipeline.builder()
            .processors(
                    Arrays.asList(
                            ProcessorLimit.builder().limit(3).build()
                            , ProcessorLimit.builder().limit(1).build()
                    )
            )
            .build();
    PipelineExecutorImpl instance = new PipelineExecutorImpl();
    List<ProcessorInstance> processors = instance.createProcessors(vertx, vertx.getOrCreateContext(), definition);
    
    Map<String, ArgumentInstance> arguments = instance.prepareArguments(
            ImmutableMap.<String, Argument>builder()
                    .put("arg1", Argument.builder().type(ArgumentType.Long).defaultValue("12").build())
                    .put("arg2", Argument.builder().type(ArgumentType.String).defaultValue("message").build())
                    .put("arg3", Argument.builder().type(ArgumentType.String).build())
                    .build()
            , 
            ImmutableMap.<String, String>builder()
                    .build()
            );
    
    SourceTest sourceDefn = SourceTest.builder().rowCount(7).build();
    SourceInstance source = sourceDefn.createInstance(vertx, vertx.getOrCreateContext());
    DestinationLogger destDefn = DestinationLogger.builder().build();
    DestinationInstance dest = destDefn.createInstance(vertx, vertx.getOrCreateContext());
    
    PipelineInstance pi = new PipelineInstance(arguments, null, source, processors, dest);
    
    instance.initializePipeline(pi);
    pi.getFinalPromise().future().onComplete(testContext.succeedingThenComplete());
    
  }
  
}
