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
package uk.co.spudsoft.query.exec.sources.sql;

import com.google.common.collect.ImmutableMap;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.PreparedStatement;
import io.vertx.sqlclient.SqlConnectOptions;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.query.defn.Endpoint;
import uk.co.spudsoft.query.defn.EndpointType;
import uk.co.spudsoft.query.defn.SourceSql;
import uk.co.spudsoft.query.exec.PipelineInstance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import uk.co.spudsoft.query.exec.SharedMap;


/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class SourceSqlStreamingInstanceTest {
  
  private static class ShareMapImpl implements SharedMap {
    private final Map<String, Object> map = new HashMap<>();

    @Override
    public Object get(String name) {
      return map.get(name);
    }

    @Override
    public void put(String name, Object value) {
      map.put(name, value);
    }
  }
  
  @Test
  public void testInitializeWithBadEndpoint(Vertx vertx) {
    SourceSql definition = SourceSql.builder()
            .endpoint("none")
            .build();
    SourceSqlStreamingInstance instance = new SourceSqlStreamingInstance(vertx, vertx.getOrCreateContext(), new ShareMapImpl(), definition, "source");
    Future<Void> future = instance.initialize(null, new PipelineInstance(null, null, null, null, null, null));
    assertTrue(future.failed());
  }

  @Test
  public void testInitializeWithPoolOptions(Vertx vertx, VertxTestContext testContext) {
    SourceSql definition = SourceSql.builder()
            .endpoint("e")
            .streamingFetchSize(1)
            .connectionTimeout(Duration.ofMinutes(4))
            .build();
    Endpoint endpoint = Endpoint.builder()
            .type(EndpointType.SQL)
            .url("sqlserver://nonexistant:1234/test")
            .build();
    SourceSqlStreamingInstance instance = new SourceSqlStreamingInstance(vertx, vertx.getOrCreateContext(), new ShareMapImpl(), definition, "source");    
    Future<Void> future = instance.initialize(null, new PipelineInstance(null, ImmutableMap.<String, Endpoint>builder().put("e", endpoint).build(), null, null, null, null));
    // This is still going to fail because the pool cannot be created
    future.onComplete(ar -> {
      testContext.verify(() -> {
        assertNull(instance.getReadStream());
      });
      testContext.<Void>failingThenComplete().handle(ar);
    });    
  }

  @Test
  public void testInitializeWithCredentials(Vertx vertx, VertxTestContext testContext) {
    SourceSql definition = SourceSql.builder()
            .endpoint("e")
            .streamingFetchSize(1)
            .build();
    Endpoint endpoint = Endpoint.builder()
            .type(EndpointType.SQL)
            .url("sqlserver://nonexistant:1234/test")
            .username("user")
            .password("pass")
            .build();
    SourceSqlStreamingInstance instance = new SourceSqlStreamingInstance(vertx, vertx.getOrCreateContext(), new ShareMapImpl(), definition, "source");    
    Future<Void> future = instance.initialize(null, new PipelineInstance(null, ImmutableMap.<String, Endpoint>builder().put("e", endpoint).build(), null, null, null, null));
    // This is still going to fail because the pool cannot be created
    future.onComplete(ar -> {
      testContext.verify(() -> {
        assertNull(instance.getReadStream());
      });
      testContext.<Void>failingThenComplete().handle(ar);
    });    
  }

  private static class MockPoolCreator extends PoolCreator {

    @Override
    public Pool pool(Vertx vertx, SqlConnectOptions database, PoolOptions options) {
      Pool mockPool = mock(Pool.class);
      SqlConnection mockConnection = mock(SqlConnection.class);
      when(mockPool.getConnection()).thenReturn(Future.succeededFuture(mockConnection));      
      Transaction mockTransaction = mock(Transaction.class);
      when(mockConnection.begin()).thenReturn(Future.succeededFuture(mockTransaction));
      PreparedStatement mockStatement = mock(PreparedStatement.class);
      when(mockConnection.prepare(any())).thenReturn(Future.succeededFuture(mockStatement));
      return mockPool;
    }
    
  }
  

  @Test
  public void testInitializeWithMockPool(Vertx vertx, VertxTestContext testContext) {
    SourceSql definition = SourceSql.builder()
            .endpoint("e")
            .build();
    Endpoint endpoint = Endpoint.builder()
            .type(EndpointType.SQL)
            .url("sqlserver://nonexistant:1234/test") // This has to be a valid URL, but it will not be accessed
            .build();
    ShareMapImpl sharedMap = new ShareMapImpl();
    sharedMap.put(PoolCreator.class.toString(), new MockPoolCreator());
    SourceSqlStreamingInstance instance = new SourceSqlStreamingInstance(vertx, vertx.getOrCreateContext(), sharedMap, definition, "source");    
    Future<Void> future = instance.initialize(null, new PipelineInstance(null, ImmutableMap.<String, Endpoint>builder().put("e", endpoint).build(), null, null, null, null));
    // This is still going to fail because the pool cannot be created
    future.onComplete(ar -> {
      testContext.verify(() -> {
        assertThat(instance.getReadStream(), instanceOf(RowStreamWrapper.class));
      });
      testContext.<Void>succeedingThenComplete().handle(ar);
    });    
  }

  @Test
  public void testPrepareStatement(Vertx vertx) {
    SourceSql definition = SourceSql.builder()
            .query("query")
            .build();
    SourceSqlStreamingInstance instance = new SourceSqlStreamingInstance(vertx, vertx.getOrCreateContext(), new ShareMapImpl(), definition, "source");
    
    SqlConnection  conn = mock(SqlConnection.class);
    instance.prepareSqlStatement(conn, definition.getQuery());
    verify(conn).prepare("query");
    assertNull(instance.getReadStream());
  }
}
