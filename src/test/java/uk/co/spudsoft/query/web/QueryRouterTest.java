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
package uk.co.spudsoft.query.web;

import uk.co.spudsoft.query.pipeline.PipelineDefnLoader;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import uk.co.spudsoft.query.exec.NullAuditor;
import uk.co.spudsoft.query.exec.conditions.RequestContextBuilder;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class QueryRouterTest {
  
  @Test
  public void testBadMethod(Vertx vertx) {
    
    PipelineDefnLoader loader = mock(PipelineDefnLoader.class);
    RequestContextBuilder rcb = new RequestContextBuilder(null, null, null, null, "aud");
    QueryRouter router = new QueryRouter(vertx, new NullAuditor(), rcb, loader, null, true);
    
    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    when(routingContext.request()).thenReturn(request);
    when(request.method()).thenReturn(HttpMethod.HEAD);
    
    router.handle(routingContext);
    
    verify(routingContext).next();
  }
  
  @Test
  public void testShortPath(Vertx vertx) {
    
    PipelineDefnLoader loader = mock(PipelineDefnLoader.class);
    RequestContextBuilder rcb = new RequestContextBuilder(null, null, null, null, "aud");
    QueryRouter router = new QueryRouter(vertx, new NullAuditor(), rcb, loader, null, true);
    
    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    when(routingContext.request()).thenReturn(request);
    when(request.path()).thenReturn("/query");
    when(request.method()).thenReturn(HttpMethod.GET);
    HttpServerResponse response = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(400)).thenReturn(response);
    
    router.handle(routingContext);
    
    verify(response).setStatusCode(400);
    verify(response).send("Invalid path");    
  }
  
}
