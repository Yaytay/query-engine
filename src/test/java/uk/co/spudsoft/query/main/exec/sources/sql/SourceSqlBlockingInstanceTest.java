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
import io.vertx.sqlclient.PoolOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.query.main.defn.Endpoint;
import uk.co.spudsoft.query.main.defn.EndpointType;
import uk.co.spudsoft.query.main.defn.SourceSql;

import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class SourceSqlBlockingInstanceTest {
  
  @Test
  public void testInitializeWithBadEndpoint(Vertx vertx) {
    SourceSql definition = SourceSql.builder()
            .endpoint("none")
            .rowsQueuedBeforeDiscard(1)
            .build();
    SourceSqlBlockingInstance instance = new SourceSqlBlockingInstance(vertx, vertx.getOrCreateContext(), definition);
    Future<Void> future = instance.initialize(ImmutableMap.<String, Endpoint>builder().build());
    assertTrue(future.failed());
  }

  @Test
  public void testInitializeWithPoolOptions(Vertx vertx, VertxTestContext testContext) {
    SourceSql definition = SourceSql.builder()
            .endpoint("e")
            .rowsQueuedBeforeDiscard(1)
            .poolOptions(new PoolOptions().setConnectionTimeout(4))
            .build();
    Endpoint endpoint = Endpoint.builder()
            .type(EndpointType.SQL)
            .url("sqlserver://nonexistant:1234/test")
            .build();
    SourceSqlBlockingInstance instance = new SourceSqlBlockingInstance(vertx, vertx.getOrCreateContext(), definition);    
    Future<Void> future = instance.initialize(ImmutableMap.<String, Endpoint>builder().put("e", endpoint).build());
    // This is still going to fail because the pool cannot be created
    future.onComplete(testContext.failingThenComplete());
  }
}
