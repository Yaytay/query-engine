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
package uk.co.spudsoft.query.exec.sources.jdbc;

import com.google.common.collect.ImmutableMap;
import inet.ipaddr.IPAddressString;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.query.defn.Condition;
import uk.co.spudsoft.query.defn.Endpoint;
import uk.co.spudsoft.query.defn.Pipeline;
import uk.co.spudsoft.query.defn.SourceJdbc;
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
public class SourceJdbcInstanceTest {
  
  @Test
  public void testPrepareSqlStatement() {
  }

  @Test
  public void testInitializeEndpointNotFound(Vertx vertx, VertxTestContext testContext) {
    
    vertx.getOrCreateContext().runOnContext(v -> {
      Context context = vertx.getOrCreateContext();
      FilterFactory filterFactory = new FilterFactory(Collections.emptyList());
      PipelineExecutor pipelineExecutor = new PipelineExecutorImpl(filterFactory, ImmutableMap.<String, ProtectedCredentials>builder().build());
      
      SourceJdbc definition = SourceJdbc.builder()
              .endpoint("bob")
              .build();
      SourceJdbcInstance instance = new SourceJdbcInstance(vertx, context, definition, "test");
      
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
      
      SourceJdbc definition = SourceJdbc.builder()
              .endpoint("bob")
              .build();
      SourceJdbcInstance instance = new SourceJdbcInstance(vertx, context, definition, "test");
      
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
  
}
