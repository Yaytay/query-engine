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
import java.util.Collections;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import uk.co.spudsoft.query.defn.FormatDelimited;
import uk.co.spudsoft.query.exec.AuditorMemoryImpl;
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
    RequestContextBuilder rcb = new RequestContextBuilder(null, null, null, null, null, true, null, false, null, Collections.singletonList("aud"), null, null);
    QueryRouter router = new QueryRouter(vertx, new AuditorMemoryImpl(), rcb, loader, null, System.getProperty("java.io.tmpdir"), true);

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
    RequestContextBuilder rcb = new RequestContextBuilder(null, null, null, null, null, true, null, false, null, Collections.singletonList("aud"), null, null);
    QueryRouter router = new QueryRouter(vertx, new AuditorMemoryImpl(), rcb, loader, null, System.getProperty("java.io.tmpdir"), true);

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

  @Test
  public void testErrorReportIllegalArgumentExceptionWithStack() {
    Throwable ex = new IllegalArgumentException("Bad arg");

    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(response);

    ArgumentCaptor<Integer> statusCaptor = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

    when(response.setStatusCode(statusCaptor.capture())).thenReturn(response);

    QueryRouter.internalError(ex, routingContext, true);
    verify(response).setStatusCode(statusCaptor.capture());
    verify(response).end(messageCaptor.capture());

    assertEquals(400, statusCaptor.getValue());
    assertThat(messageCaptor.getValue(), startsWith("Bad arg (from"));
    assertThat(messageCaptor.getValue(), containsString("IllegalArgumentException"));
  }

  @Test
  public void testErrorReportIllegalArgumentExceptionWithoutStack() {
    Throwable ex = new IllegalArgumentException("Bad arg");

    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(response);

    ArgumentCaptor<Integer> statusCaptor = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

    when(response.setStatusCode(statusCaptor.capture())).thenReturn(response);

    QueryRouter.internalError(ex, routingContext, false);
    verify(response).setStatusCode(statusCaptor.capture());
    verify(response).end(messageCaptor.capture());

    assertEquals(400, statusCaptor.getValue());
    assertEquals("Bad arg", messageCaptor.getValue());
  }

  @Test
  public void testErrorReportIllegalStateExceptionWithStack() {
    Throwable ex = new IllegalStateException("Bad state");

    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(response);

    ArgumentCaptor<Integer> statusCaptor = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

    when(response.setStatusCode(statusCaptor.capture())).thenReturn(response);

    QueryRouter.internalError(ex, routingContext, true);
    verify(response).setStatusCode(statusCaptor.capture());
    verify(response).end(messageCaptor.capture());

    assertEquals(500, statusCaptor.getValue());
    assertThat(messageCaptor.getValue(), startsWith("Bad state (from"));
    assertThat(messageCaptor.getValue(), containsString("IllegalStateException"));
  }

  @Test
  public void testErrorReportIllegalStateExceptionWithoutStack() {
    Throwable ex = new IllegalStateException("Bad state");

    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(response);

    ArgumentCaptor<Integer> statusCaptor = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

    when(response.setStatusCode(statusCaptor.capture())).thenReturn(response);

    QueryRouter.internalError(ex, routingContext, false);
    verify(response).setStatusCode(statusCaptor.capture());
    verify(response).end(messageCaptor.capture());

    assertEquals(500, statusCaptor.getValue());
    assertEquals("Failed", messageCaptor.getValue());
  }

  @Test
  public void testErrorReportServiceExceptionWithStack() {
    Throwable ex = new ServiceException(123, "Something special went wrong");

    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(response);

    ArgumentCaptor<Integer> statusCaptor = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

    when(response.setStatusCode(statusCaptor.capture())).thenReturn(response);

    QueryRouter.internalError(ex, routingContext, true);
    verify(response).setStatusCode(statusCaptor.capture());
    verify(response).end(messageCaptor.capture());

    assertEquals(123, statusCaptor.getValue());
    assertThat(messageCaptor.getValue(), startsWith("Something special went wrong (from"));
    assertThat(messageCaptor.getValue(), containsString("ServiceException"));
  }

  @Test
  public void testErrorReportServiceExceptionWithoutStack() {
    Throwable ex = new ServiceException(123, "Something special went wrong");

    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(response);

    ArgumentCaptor<Integer> statusCaptor = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

    when(response.setStatusCode(statusCaptor.capture())).thenReturn(response);

    QueryRouter.internalError(ex, routingContext, false);
    verify(response).setStatusCode(statusCaptor.capture());
    verify(response).end(messageCaptor.capture());

    assertEquals(123, statusCaptor.getValue());
    assertEquals("Something special went wrong", messageCaptor.getValue());
  }

  @Test
  public void testGetFilename() {
    
    assertEquals(null, QueryRouter.buildDesiredFilename(FormatDelimited.builder().build()));
    assertEquals(null, QueryRouter.buildDesiredFilename(FormatDelimited.builder().extension(".txt").build()));
    assertEquals("Bob.csv", QueryRouter.buildDesiredFilename(FormatDelimited.builder().filename("Bob").build()));
    assertEquals("Bob", QueryRouter.buildDesiredFilename(FormatDelimited.builder().filename("Bob").extension("").build()));
    assertEquals("Bob.thing", QueryRouter.buildDesiredFilename(FormatDelimited.builder().filename("Bob.thing").extension("txt").build()));
    assertEquals("Bob.txt", QueryRouter.buildDesiredFilename(FormatDelimited.builder().filename("Bob").extension("txt").build()));
    
  }
  
}

