/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
 */
package uk.co.spudsoft.query.pipeline;

import uk.co.spudsoft.query.pipeline.PipelineParsingErrorHandler;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.web.DummyParameters;

/**
 *
 * @author jtalbut
 */
public class PipelineParsingErrorHandlerTest {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(PipelineParsingErrorHandlerTest.class);

  private ObjectMapper mapper = createJsonMapper();
  
  private ObjectMapper createJsonMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    mapper.registerModule(new JavaTimeModule());
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    mapper.setDefaultMergeable(Boolean.TRUE);
    mapper.addHandler(new PipelineParsingErrorHandler());
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    return mapper;
  }
  
  @Test
  public void testHandleUnexpectedToken() throws Exception {
    String json = "{\"list\":{}}";
    logger.debug("testHandleUnexpectedToken: {}", json);
    DummyParameters result = mapper.readValue(json, DummyParameters.class);
    logger.debug("Value: {}", mapper.writeValueAsString(result));
  }

  @Test
  public void testHandleUnknownProperty() throws Exception {
    String json = "{\"fred\":{}}";
    logger.debug("testHandleUnknownProperty: {}", json);
    DummyParameters result = mapper.readValue(json, DummyParameters.class);
    logger.debug("Value: {}", mapper.writeValueAsString(result));
  }
  
}
