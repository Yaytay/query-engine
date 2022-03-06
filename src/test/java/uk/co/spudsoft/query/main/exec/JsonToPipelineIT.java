/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.main.defn.Pipeline;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class JsonToPipelineIT {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(JsonToPipelineIT.class);
  
  @Test
  public void testParsingJsonToPipeline(Vertx vertx, VertxTestContext testContext) throws Throwable {
    
    String jsonString;
    try (InputStream stream = this.getClass().getResourceAsStream("/JsonToPipelineIT.json")) {
      jsonString = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
    Pipeline pipeline = Json.decodeValue(jsonString, Pipeline.class);
    logger.debug("Pipeline: {}", Json.encode(pipeline));
    
    PipelineExecutor executor = new PipelineExecutor();
    
    testContext.completeNow();
  }
}
