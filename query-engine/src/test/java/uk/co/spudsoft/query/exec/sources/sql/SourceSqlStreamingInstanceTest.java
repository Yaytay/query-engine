/*
 * Copyright (C) 2024 jtalbut
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
import inet.ipaddr.IPAddressString;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.PoolOptions;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.query.defn.Condition;
import uk.co.spudsoft.query.defn.Endpoint;
import uk.co.spudsoft.query.defn.Pipeline;
import uk.co.spudsoft.query.defn.SourceSql;
import uk.co.spudsoft.query.exec.FilterFactory;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineExecutorImpl;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.exec.fmts.FormatCaptureInstance;
import uk.co.spudsoft.query.main.ProtectedCredentials;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class SourceSqlStreamingInstanceTest {
  
  @Test
  public void testPrepareSqlStatement() {
  }

  @Test
  public void testInitializeEndpointNotFound(Vertx vertx, VertxTestContext testContext) {
    
    vertx.getOrCreateContext().runOnContext(v -> {
      Context context = vertx.getOrCreateContext();
      FilterFactory filterFactory = new FilterFactory(Collections.emptyList());
      PipelineExecutor pipelineExecutor = new PipelineExecutorImpl(filterFactory, ImmutableMap.<String, ProtectedCredentials>builder().build());
      
      SourceSql definition = SourceSql.builder()
              .endpoint("bob")
              .build();
      SourceSqlStreamingInstance instance = new SourceSqlStreamingInstance(vertx, context, pipelineExecutor, definition, "test");
      
      Pipeline pipeline = Pipeline.builder()
              .sourceEndpoints(
                      Arrays.asList(
                              Endpoint.builder()
                                      .name("fred")
                                      .build()                              
                              
                      )
              )
              .source(definition)
              .build();
      
      MultiMap params = new HeadersMultiMap();
      
      RequestContext req = new RequestContext(
              null
              , null
              , null
              , "localhost"
              , null
              , null
              , new HeadersMultiMap().add("Host", "localhost:123")
              , null
              , new IPAddressString("127.0.0.1")
              , null
      );
      
      PipelineInstance pipelineInstance = new PipelineInstance(
                        pipelineExecutor.prepareArguments(req, pipeline.getArguments(), params)
                        , pipeline.getSourceEndpointsMap()
                        , pipelineExecutor.createPreProcessors(vertx, Vertx.currentContext(), pipeline)
                        , instance
                        , Collections.emptyList()
                        , new FormatCaptureInstance()
                );
      
      Future<ReadStreamWithTypes> future = instance.initialize(pipelineExecutor, pipelineInstance);
      testContext.verify(() -> {
        assertTrue(future.isComplete());
        assertTrue(future.failed());
        assertEquals("Endpoint \"bob\" not found in [fred]", future.cause().getMessage());
      });
      testContext.completeNow();
    });
    
  }
  
  @Test
  public void testInitializeEndpointNotPermitted(Vertx vertx, VertxTestContext testContext) {
    
    vertx.getOrCreateContext().runOnContext(v -> {
      Context context = vertx.getOrCreateContext();
      FilterFactory filterFactory = new FilterFactory(Collections.emptyList());
      PipelineExecutor pipelineExecutor = new PipelineExecutorImpl(filterFactory, ImmutableMap.<String, ProtectedCredentials>builder().build());
      
      SourceSql definition = SourceSql.builder()
              .endpoint("bob")
              .build();
      SourceSqlStreamingInstance instance = new SourceSqlStreamingInstance(vertx, context, pipelineExecutor, definition, "test");
      
      Pipeline pipeline = Pipeline.builder()
              .sourceEndpoints(
                      Arrays.asList(
                              Endpoint.builder()
                                      .name("bob")
                                      .condition(
                                              new Condition("false")
                                      )
                                      .build()                              
                              
                      )
              )
              .source(definition)
              .build();
      
      MultiMap params = new HeadersMultiMap();
      
      RequestContext req = new RequestContext(
              null
              , null
              , null
              , "localhost"
              , null
              , null
              , new HeadersMultiMap().add("Host", "localhost:123")
              , null
              , new IPAddressString("127.0.0.1")
              , null
      );
      
      PipelineInstance pipelineInstance = new PipelineInstance(
                        pipelineExecutor.prepareArguments(req, pipeline.getArguments(), params)
                        , pipeline.getSourceEndpointsMap()
                        , pipelineExecutor.createPreProcessors(vertx, Vertx.currentContext(), pipeline)
                        , instance
                        , Collections.emptyList()
                        , new FormatCaptureInstance()
                );
      
      Future<ReadStreamWithTypes> future = instance.initialize(pipelineExecutor, pipelineInstance);
      testContext.verify(() -> {
        assertTrue(future.isComplete());
        assertTrue(future.failed());
        assertEquals("Endpoint \"bob\" not accessible", future.cause().getMessage());
      });
      testContext.completeNow();
    });
    
  }
  
  @Test
  public void testGetPreparer() {
    assertThat(SourceSqlStreamingInstance.getPreparer("sqlserver:nonsense"), instanceOf(MsSqlPreparer.class));
    assertThat(SourceSqlStreamingInstance.getPreparer("postgresql:nonsense"), instanceOf(PostgreSqlPreparer.class));
    assertThat(SourceSqlStreamingInstance.getPreparer("mysql:nonsense"), instanceOf(MySqlPreparer.class));
    assertThat(SourceSqlStreamingInstance.getPreparer("wibble"), instanceOf(MsSqlPreparer.class));
  }
  
  @Test
  public void testPoolOptions() {
    PoolOptions po = SourceSqlStreamingInstance.poolOptions(SourceSql.builder().build());
    assertEquals(PoolOptions.DEFAULT_CONNECTION_TIMEOUT, po.getConnectionTimeout());
    assertEquals(PoolOptions.DEFAULT_CONNECTION_TIMEOUT_TIME_UNIT, po.getConnectionTimeoutUnit());
    
    po = SourceSqlStreamingInstance.poolOptions(SourceSql.builder().connectionTimeout(Duration.ofMillis(Long.MAX_VALUE)).build());
    assertEquals(Integer.MAX_VALUE, po.getConnectionTimeout());
    assertEquals(TimeUnit.MILLISECONDS, po.getConnectionTimeoutUnit());
  }
  
  @Test
  public void testCoalesce() {
    assertEquals("two", SourceSqlStreamingInstance.coalesce(null, "two"));
    assertEquals("one", SourceSqlStreamingInstance.coalesce("one", "two"));
    assertNull(SourceSqlStreamingInstance.coalesce(null, null));
  }
  
}
