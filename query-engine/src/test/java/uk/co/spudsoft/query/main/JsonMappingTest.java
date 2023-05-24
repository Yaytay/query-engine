/*
 * Copyright (C) 2022 jtalbut
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.co.spudsoft.query.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.core.tracing.TracingOptions;
import io.vertx.tracing.zipkin.ZipkinTracingOptions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.params4j.Params4J;
import uk.co.spudsoft.params4j.Params4JSpi;
import uk.co.spudsoft.query.json.TracingOptionsMixin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author jtalbut
 */
public class JsonMappingTest {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(JsonMappingTest.class);  
  
  @Test
  public void testJson() throws Exception {
    
    DatabindCodec.mapper()
            .addMixIn(TracingOptions.class, TracingOptionsMixin.class)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .registerModule(new JavaTimeModule())
            ;
    
    VertxOptions vo = new VertxOptions()
            .setEventLoopPoolSize(8)
            .setTracingOptions(
                    new ZipkinTracingOptions()
                            .setServiceName("service")
            );
    String json = Json.encode(vo);
    logger.debug("Json: {}", json);
    String simpleJson = "{\"eventLoopPoolSize\":8,\"tracingOptions\": {\"serviceName\": \"service\"}}";
    VertxOptions vo2 = Json.decodeValue(simpleJson, VertxOptions.class);
    assertEquals(vo.toJson(), vo2.toJson());
    assertEquals(8, vo2.getEventLoopPoolSize());
    assertThat(vo2.getTracingOptions(), instanceOf(ZipkinTracingOptions.class));
    assertEquals("service", ((ZipkinTracingOptions) vo2.getTracingOptions()).getServiceName());
    
    Params4J<Parameters> p4j = Params4J.<Parameters>factory()
            .withConstructor(() -> new Parameters())
            .withMixIn(TracingOptions.class, TracingOptionsMixin.class)
            .withProblemHandler(new DeserializationProblemHandler(){})
            .create();
    ObjectMapper mapper = ((Params4JSpi) p4j).getJsonMapper();
    VertxOptions vo3 = mapper.readValue(simpleJson, VertxOptions.class);
    assertEquals(8, vo3.getEventLoopPoolSize());
    assertThat(vo3.getTracingOptions(), instanceOf(ZipkinTracingOptions.class));
    assertEquals("service", ((ZipkinTracingOptions) vo3.getTracingOptions()).getServiceName());
    logger.debug("vo3: {}", mapper.writeValueAsString(vo3));

    VertxOptions vo4 = new VertxOptions();    
    assertThat(vo4.getTracingOptions(), not(instanceOf(ZipkinTracingOptions.class)));
    ObjectReader reader = mapper.readerForUpdating(vo4);
    vo4 = reader.readValue(simpleJson);
    logger.debug("vo4: {}", mapper.writeValueAsString(vo4));
    assertEquals(8, vo4.getEventLoopPoolSize());
    assertThat(vo4.getTracingOptions(), instanceOf(ZipkinTracingOptions.class));
    assertEquals("service", ((ZipkinTracingOptions) vo4.getTracingOptions()).getServiceName());
    
    Parameters p1 = new Parameters();
    ObjectReader reader2 = mapper.readerForUpdating(p1);
    p1 = reader2.readValue("{\"vertxOptions\":{\"eventLoopPoolSize\":8,\"tracingOptions\": {\"serviceName\": \"service\"}}}");
    logger.debug("p1: {}", mapper.writeValueAsString(p1));
    assertEquals(8, p1.getVertxOptions().getEventLoopPoolSize());
    assertThat(p1.getVertxOptions().getTracingOptions(), instanceOf(ZipkinTracingOptions.class));
    assertEquals("service", ((ZipkinTracingOptions) p1.getVertxOptions().getTracingOptions()).getServiceName());
    
  }
  
}
