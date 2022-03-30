/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.sources.sql;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.query.main.defn.Endpoint;
import uk.co.spudsoft.query.main.defn.EndpointType;
import uk.co.spudsoft.query.main.defn.SourceSql;
import uk.co.spudsoft.query.main.exec.PipelineInstance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class SourceSqlStreamingInstanceTest {
  
  @Test
  public void testInitializeWithBadEndpoint(Vertx vertx) {
    SourceSql definition = SourceSql.builder()
            .endpoint("none")
            .build();
    SourceSqlStreamingInstance instance = new SourceSqlStreamingInstance(vertx, vertx.getOrCreateContext(), definition);
    Future<Void> future = instance.initialize(null, new PipelineInstance(null, null, null, null, null));
    assertTrue(future.failed());
  }

  @Test
  public void testInitializeWithPoolOptions(Vertx vertx, VertxTestContext testContext) {
    SourceSql definition = SourceSql.builder()
            .endpoint("e")
            .streamingFetchSize(1)
            .poolOptions(new PoolOptions().setConnectionTimeout(4))
            .build();
    Endpoint endpoint = Endpoint.builder()
            .type(EndpointType.SQL)
            .url("sqlserver://nonexistant:1234/test")
            .build();
    SourceSqlStreamingInstance instance = new SourceSqlStreamingInstance(vertx, vertx.getOrCreateContext(), definition);    
    Future<Void> future = instance.initialize(null, new PipelineInstance(null, ImmutableMap.<String, Endpoint>builder().put("e", endpoint).build(), null, null, null));
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
    SourceSqlStreamingInstance instance = new SourceSqlStreamingInstance(vertx, vertx.getOrCreateContext(), definition);    
    Future<Void> future = instance.initialize(null, new PipelineInstance(null, ImmutableMap.<String, Endpoint>builder().put("e", endpoint).build(), null, null, null));
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
    SourceSqlStreamingInstance instance = new SourceSqlStreamingInstance(vertx, vertx.getOrCreateContext(), definition);    
    instance.setPoolCreator(new MockPoolCreator());
    Future<Void> future = instance.initialize(null, new PipelineInstance(null, ImmutableMap.<String, Endpoint>builder().put("e", endpoint).build(), null, null, null));
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
    SourceSqlStreamingInstance instance = new SourceSqlStreamingInstance(vertx, vertx.getOrCreateContext(), definition);
    
    SqlConnection  conn = mock(SqlConnection.class);
    instance.prepareSqlStatement(conn);
    verify(conn).prepare("query");
    assertNull(instance.getReadStream());
  }
}
