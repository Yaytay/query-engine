/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.main.defn.Pipeline;
import uk.co.spudsoft.query.main.json.TemplateDeserializerModule;
import uk.co.spudsoft.query.main.testcontainers.ServerProviderMySQL;

import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class JsonToPipelineIT {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(JsonToPipelineIT.class);

  private final ServerProviderMySQL serverProvider = new ServerProviderMySQL();
  
  @BeforeAll
  public void prepData(Vertx vertx, VertxTestContext testContext) {
    Future<Void> future = 
            serverProvider
            .prepareContainer(vertx)
            .compose(v -> serverProvider.prepareTestDatabase(vertx))
            .onComplete(ar -> {
              logger.debug("Data prepped");
            })
            .onComplete(testContext.succeedingThenComplete())
            ;
    
  }
  
  @Test
  @Timeout(value = 1, timeUnit = TimeUnit.MINUTES)
  public void testParsingJsonToPipelineStreaming(Vertx vertx, VertxTestContext testContext) throws Throwable {
    String jsonString;
    try (InputStream stream = this.getClass().getResourceAsStream("/JsonToPipelineIT.json")) {
      jsonString = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
    ObjectMapper objectMapper = DatabindCodec.mapper();
    objectMapper.registerModule(new TemplateDeserializerModule());
    
    Map<String, String> args = ImmutableMap.<String, String>builder()
            .put("key", serverProvider.getName())
            .put("port", Integer.toString(serverProvider.getPort()))
            .build();
    vertx.getOrCreateContext().runOnContext(v -> {
      Vertx.currentContext().put("ARGUMENTS", args);

      Pipeline pipeline = parsePipelineDefinition(jsonString, testContext);
      logger.debug("Pipeline: {}", Json.encode(pipeline));
      assertNotNull(pipeline);

      PipelineExecutor executor = new PipelineExecutor();      
      PipelineInstance instance = buildPipelineInstance(executor, vertx, pipeline, testContext);
      assertNotNull(instance);

      executor.executePipeline(instance)
              .onComplete(testContext.succeedingThenComplete());
    });
  }

  @Test
  @Timeout(value = 1, timeUnit = TimeUnit.MINUTES)
  public void testParsingJsonToPipelineBlocking(Vertx vertx, VertxTestContext testContext) throws Throwable {
    String jsonString;
    try (InputStream stream = this.getClass().getResourceAsStream("/JsonToPipelineIT.json")) {
      jsonString = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
              .replaceAll("\"blockingProcessor\": false", "\"blockingProcessor\": true");
    }
    ObjectMapper objectMapper = DatabindCodec.mapper();
    objectMapper.registerModule(new TemplateDeserializerModule());
    
    Map<String, String> args = ImmutableMap.<String, String>builder()
            .put("key", serverProvider.getName())
            .put("port", Integer.toString(serverProvider.getPort()))
            .build();
    vertx.getOrCreateContext().runOnContext(v -> {
      Vertx.currentContext().put("ARGUMENTS", args);

      Pipeline pipeline = parsePipelineDefinition(jsonString, testContext);
      logger.debug("Pipeline: {}", Json.encode(pipeline));
      assertNotNull(pipeline);

      PipelineExecutor executor = new PipelineExecutor();      
      PipelineInstance instance = buildPipelineInstance(executor, vertx, pipeline, testContext);
      assertNotNull(instance);
      
      executor.executePipeline(instance)
              .onComplete(testContext.succeedingThenComplete());
    });
  }

  protected Pipeline parsePipelineDefinition(String jsonString, VertxTestContext testContext) {
    Pipeline pipeline = null;
    try {
      pipeline = Json.decodeValue(jsonString, Pipeline.class);
    } catch(Throwable ex) {
      logger.debug("Failed to parse JSON: ", ex);
      testContext.failNow(ex);
    }
    return pipeline;
  }

  protected PipelineInstance buildPipelineInstance(PipelineExecutor executor, Vertx vertx, Pipeline pipeline, VertxTestContext testContext) {
    PipelineInstance instance = null;
    try {
      instance = executor.buildPipelineFromDefinition(vertx, vertx.getOrCreateContext(), pipeline);
    } catch(Throwable ex) {
      testContext.failNow(ex);
    }
    return instance;
  }
}
