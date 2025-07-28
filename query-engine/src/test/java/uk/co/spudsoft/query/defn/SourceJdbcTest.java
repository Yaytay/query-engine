/*
 * Copyright (C) 2025 jtalbut
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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import uk.co.spudsoft.query.exec.sources.jdbc.SourceJdbcInstance;
import io.vertx.core.Vertx;
import io.vertx.core.Context;
import java.time.Duration;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import uk.co.spudsoft.query.exec.SharedMap;

/**
 *
 * @author jtalbut
 */
class SourceJdbcTest {

  @Test
  void testBuilderPropertiesAndGetters() {
    // Build the SourceJdbc with the builder (assuming builder is available)
    SourceJdbc src = SourceJdbc.builder()
            .name("mySource")
            .endpoint("dbEndpoint")
            .query("SELECT 1")
            .type(SourceType.JDBC)
            .processingBatchSize(500)
            .jdbcFetchSize(200)
            .build();

    assertEquals("mySource", src.getName());
    assertEquals("dbEndpoint", src.getEndpoint());
    assertEquals("SELECT 1", src.getQuery());
    assertEquals(SourceType.JDBC, src.getType());
    assertEquals(500, src.getProcessingBatchSize());
    assertEquals(200, src.getJdbcFetchSize());
  }

  @Test
  void testCreateInstanceUsesCorrectArgs() {
    Vertx vertx = mock(Vertx.class);
    Context ctx = mock(Context.class);
    SharedMap sharedMap = mock(SharedMap.class);

    SourceJdbc src = SourceJdbc.builder()
            .name("SourceX")
            .endpoint("db")
            .type(SourceType.JDBC)
            .build();

    SourceJdbcInstance inst = (SourceJdbcInstance) src.createInstance(vertx, ctx, null, sharedMap, "defaultName");
    assertNotNull(inst);
    assertEquals("SourceX", inst.getName());
  }

  @Test
  void testCreateInstanceUsesDefaultNameWhenNameIsNull() {
    Vertx vertx = mock(Vertx.class);
    Context ctx = mock(Context.class);
    SharedMap sharedMap = mock(SharedMap.class);

    SourceJdbc src = SourceJdbc.builder()
            .name(null)
            .endpoint("db")
            .type(SourceType.JDBC)
            .build();

    SourceJdbcInstance inst = (SourceJdbcInstance) src.createInstance(vertx, ctx, null, sharedMap, "Dflt");
    assertNotNull(inst);
    assertEquals("Dflt", inst.getName());
  }

  @Test
  void testValidate_succeedsWithMatchingType() {
    SourceJdbc src = SourceJdbc.builder()
            .name("foo")
            .endpoint("ep")
            .type(SourceType.JDBC)
            .query("select 1")
            .build();
    assertDoesNotThrow(src::validate);
  }

  @Test
  void testValidate_succeedsWithQueryTemplate() {
    SourceJdbc src = SourceJdbc.builder()
            .name("foo")
            .endpoint("ep")
            .type(SourceType.JDBC)
            .queryTemplate("select 2")
            .build();
    assertDoesNotThrow(src::validate);
  }

  @Test
  void testValidate_failsWithNoQuery() {
    SourceJdbc src = SourceJdbc.builder()
            .name("foo")
            .endpoint("ep")
            .type(SourceType.JDBC)
            .build();
    Exception ex = assertThrows(IllegalArgumentException.class, src::validate);
    assertThat(ex.getMessage().toLowerCase(), containsString("query nor querytemplate"));
  }

  @Test
  void testValidate_failsWithTwoQueries() {
    SourceJdbc src = SourceJdbc.builder()
            .name("foo")
            .endpoint("ep")
            .query("query")
            .queryTemplate("query")
            .type(SourceType.JDBC)
            .build();
    Exception ex = assertThrows(IllegalArgumentException.class, src::validate);
    assertThat(ex.getMessage().toLowerCase(), containsString("query and querytemplate"));
  }

  @Test
  void testValidate_failsWithTypeMismatch() {
    Exception ex = assertThrows(IllegalArgumentException.class, () -> {
      SourceJdbc.builder()
              .name("foo")
              .endpointTemplate("ep")
              .type(SourceType.TEST)
              .query("select 1")
              .build();
    });
    assertThat(ex.getMessage().toLowerCase(), containsString("jdbc"));
  }
  
  @Test
  void testValidate_failsWithNoEndpoint() {
    SourceJdbc src = SourceJdbc.builder()
            .name("foo")
            .type(SourceType.JDBC)
            .queryTemplate("select 2")
            .build();
    Exception ex = assertThrows(IllegalArgumentException.class, src::validate);
    assertThat(ex.getMessage().toLowerCase(), containsString("endpoint nor endpointtemplate"));
  }
  
  @Test
  void testValidate_failsWithNegativeTimeout() {
    SourceJdbc src = SourceJdbc.builder()
            .name("foo")
            .type(SourceType.JDBC)
            .endpoint("ep")
            .queryTemplate("select 2")
            .connectionTimeout(Duration.ofDays(-1))
            .build();
    Exception ex = assertThrows(IllegalArgumentException.class, src::validate);
    assertThat(ex.getMessage().toLowerCase(), containsString("connectiontimeout"));
  }
  
  @Test
  void testValidate_failsWithSmallBath() {
    SourceJdbc src = SourceJdbc.builder()
            .name("foo")
            .type(SourceType.JDBC)
            .endpoint("ep")
            .queryTemplate("select 2")
            .processingBatchSize(57)
            .build();
    Exception ex = assertThrows(IllegalArgumentException.class, src::validate);
    assertThat(ex.getMessage().toLowerCase(), containsString("processingbatchsize"));
  }
}
