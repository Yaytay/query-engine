/*
 * Copyright (C) 2025 njt
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

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.CheckResult;
import io.vertx.ext.healthchecks.HealthChecks;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HealthCheckHandler.
 */
public class HealthCheckHandlerTest {

  @Test
  public void testConstructor() {
    HealthChecks checks = mock(HealthChecks.class);
    HealthCheckHandler handler = new HealthCheckHandler(checks);
    assertNotNull(handler);
  }

  @Test
  public void testHandleNonGetCallsNext() {
    HealthChecks checks = mock(HealthChecks.class);
    HealthCheckHandler handler = new HealthCheckHandler(checks);

    RoutingContext ctx = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    HttpServerResponse response = mock(HttpServerResponse.class);

    when(ctx.request()).thenReturn(request);
    when(ctx.response()).thenReturn(response);
    when(request.method()).thenReturn(HttpMethod.POST);

    handler.handle(ctx);

    verify(ctx, times(1)).next();
    verifyNoInteractions(checks);
    verify(response, never()).setStatusCode(anyInt());
  }

  @Test
  public void testHandleGetUpWithData200() {
    HealthChecks checks = mock(HealthChecks.class);
    HealthCheckHandler handler = new HealthCheckHandler(checks);

    RoutingContext ctx = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    HttpServerResponse response = mock(HttpServerResponse.class);

    when(ctx.request()).thenReturn(request);
    when(ctx.response()).thenReturn(response);
    when(request.method()).thenReturn(HttpMethod.GET);

    JsonObject data = new JsonObject().put("status", "ok");
    CheckResult result = mock(CheckResult.class);
    when(result.getUp()).thenReturn(true);
    when(result.getData()).thenReturn(data);

    when(checks.checkStatus()).thenReturn(Future.succeededFuture(result));

    when(response.putHeader(anyString(), anyString())).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);

    handler.handle(ctx);

    verify(checks, times(1)).checkStatus();
    verify(response).putHeader("Content-Type", "application/json");
    verify(response).setStatusCode(200);
    verify(response).end(eq(data.toBuffer()));
    verify(ctx, never()).next();
  }

  @Test
  public void testHandleGetUpNoData200EmptyBody() {
    HealthChecks checks = mock(HealthChecks.class);
    HealthCheckHandler handler = new HealthCheckHandler(checks);

    RoutingContext ctx = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    HttpServerResponse response = mock(HttpServerResponse.class);

    when(ctx.request()).thenReturn(request);
    when(ctx.response()).thenReturn(response);
    when(request.method()).thenReturn(HttpMethod.GET);

    CheckResult result = mock(CheckResult.class);
    when(result.getUp()).thenReturn(true);
    when(result.getData()).thenReturn(null);

    when(checks.checkStatus()).thenReturn(Future.succeededFuture(result));

    when(response.putHeader(anyString(), anyString())).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);

    handler.handle(ctx);

    verify(response).putHeader("Content-Type", "application/json");
    verify(response).setStatusCode(200);
    verify(response).end(eq(Buffer.buffer()));
  }

  @Test
  public void testHandleGetDownWithData503() {
    HealthChecks checks = mock(HealthChecks.class);
    HealthCheckHandler handler = new HealthCheckHandler(checks);

    RoutingContext ctx = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    HttpServerResponse response = mock(HttpServerResponse.class);

    when(ctx.request()).thenReturn(request);
    when(ctx.response()).thenReturn(response);
    when(request.method()).thenReturn(HttpMethod.GET);

    JsonObject data = new JsonObject().put("status", "down");
    CheckResult result = mock(CheckResult.class);
    when(result.getUp()).thenReturn(false);
    when(result.getData()).thenReturn(data);

    when(checks.checkStatus()).thenReturn(Future.succeededFuture(result));

    when(response.putHeader(anyString(), anyString())).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);

    handler.handle(ctx);

    verify(response).putHeader("Content-Type", "application/json");
    verify(response).setStatusCode(503);
    verify(response).end(eq(data.toBuffer()));
  }

  @Test
  public void testHandleGetDownNoData503EmptyBody() {
    HealthChecks checks = mock(HealthChecks.class);
    HealthCheckHandler handler = new HealthCheckHandler(checks);

    RoutingContext ctx = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    HttpServerResponse response = mock(HttpServerResponse.class);

    when(ctx.request()).thenReturn(request);
    when(ctx.response()).thenReturn(response);
    when(request.method()).thenReturn(HttpMethod.GET);

    CheckResult result = mock(CheckResult.class);
    when(result.getUp()).thenReturn(false);
    when(result.getData()).thenReturn(null);

    when(checks.checkStatus()).thenReturn(Future.succeededFuture(result));

    when(response.putHeader(anyString(), anyString())).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);

    handler.handle(ctx);

    verify(response).putHeader("Content-Type", "application/json");
    verify(response).setStatusCode(503);
    verify(response).end(eq(Buffer.buffer()));
  }

  @Test
  public void testHandleGetFailure500Message() {
    HealthChecks checks = mock(HealthChecks.class);
    HealthCheckHandler handler = new HealthCheckHandler(checks);

    RoutingContext ctx = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    HttpServerResponse response = mock(HttpServerResponse.class);

    when(ctx.request()).thenReturn(request);
    when(ctx.response()).thenReturn(response);
    when(request.method()).thenReturn(HttpMethod.GET);

    RuntimeException failure = new RuntimeException("boom");
    when(checks.checkStatus()).thenReturn(Future.failedFuture(failure));

    when(response.putHeader(anyString(), anyString())).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);

    handler.handle(ctx);

    verify(response).putHeader("Content-Type", "application/json");
    verify(response).setStatusCode(500);
    verify(response).end(eq("boom"));
  }
}
