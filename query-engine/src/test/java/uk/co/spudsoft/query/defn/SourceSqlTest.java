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
package uk.co.spudsoft.query.defn;

import io.vertx.core.json.Json;
import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jtalbut
 */
public class SourceSqlTest {
  
  private static final Logger logger = LoggerFactory.getLogger(SourceSqlTest.class);
  
  @Test
  public void testValidate() {
    assertThrows(IllegalArgumentException.class
            , () -> {
              SourceSql.builder().endpoint(null).endpointTemplate(null).build().validate();
            });
    assertThrows(IllegalArgumentException.class
            , () -> {
              SourceSql.builder().endpoint(null).endpointTemplate("endpointTemplate").query(null).build().validate();
            });
    assertThrows(IllegalArgumentException.class
            , () -> {
              SourceSql.builder().endpoint(null).endpointTemplate("endpointTemplate").query("").build().validate();
            });
    assertThrows(IllegalArgumentException.class
            , () -> {
              SourceSql.builder().endpoint(null).endpointTemplate("endpointTemplate").query("select").maxPoolSize(-1).build().validate();
            });
    assertThrows(IllegalArgumentException.class
            , () -> {
              SourceSql.builder().endpoint(null).endpointTemplate("endpointTemplate").query("select").maxPoolWaitQueueSize(-1).build().validate();
            });
    assertThrows(IllegalArgumentException.class
            , () -> {
              SourceSql.builder().endpoint(null).endpointTemplate("endpointTemplate").query("select").idleTimeout(Duration.ofMillis(-10)).build().validate();
            });
    assertThrows(IllegalArgumentException.class
            , () -> {
              SourceSql.builder().endpoint(null).endpointTemplate("endpointTemplate").query("select").connectionTimeout(Duration.ofMillis(-10)).build().validate();
            });
    SourceSql.builder()
            .endpoint(null)
            .endpointTemplate("endpointTemplate")
            .query("select")
            .maxPoolSize(1)
            .maxPoolWaitQueueSize(1)
            .idleTimeout(Duration.ofMillis(10))
            .connectionTimeout(Duration.ofMillis(10))
            .build()
            .validate();
  }
  
  @Test
  public void testGetType() {
    SourceSql instance = SourceSql.builder().build();
    assertEquals(SourceType.SQL, instance.getType());
  }

  @Test
  public void testSetType() {
    SourceSql instance = SourceSql.builder().type(SourceType.SQL).build();
    assertEquals(SourceType.SQL, instance.getType());
    try {
      SourceSql.builder().type(SourceType.HTTP).build();
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
    }
  }

  @Test
  public void testGetEndpoint() {
    SourceSql instance = SourceSql.builder().endpoint("endpoint").build();
    assertEquals("endpoint", instance.getEndpoint());
  }

  @Test
  public void testGetQuery() {
    SourceSql instance = SourceSql.builder().query("query").build();
    assertEquals("query", instance.getQuery());
  }

  @Test
  public void testGetStreamingFetchSize() {
    SourceSql instance = SourceSql.builder().streamingFetchSize(2345).build();
    assertEquals(2345, instance.getStreamingFetchSize());
  }
  
  @Test
  public void toFromJson() {
    SourceSql instance = SourceSql.builder().build();
    String json = Json.encode(instance);
    logger.debug("Json: {}", json);
    Source instance2 = Json.decodeValue(json, Source.class);
    logger.debug("Source: {}", instance2);
  }    
  
}
