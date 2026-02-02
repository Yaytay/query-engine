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

import inet.ipaddr.IPAddressString;
import io.vertx.core.Future;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.matches;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;
import uk.co.spudsoft.query.defn.FormatDelimited;
import uk.co.spudsoft.query.exec.AuditorMemoryImpl;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.logging.RequestCollatingAppender;
import uk.co.spudsoft.query.main.Authenticator;
import uk.co.spudsoft.query.main.OperatorsInstance;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class QueryRouterTest {

  @Test
  public void testNotDeployed(Vertx vertx) {

    PipelineDefnLoader loader = mock(PipelineDefnLoader.class);
    Authenticator rcb = new Authenticator(null, null, null, null, null, null, true, null, false, null, Collections.singletonList("aud"), null);
    RequestCollatingAppender requestCollatingAppender = new RequestCollatingAppender();
    QueryRouter router = new QueryRouter(vertx, null, new AuditorMemoryImpl(vertx, new OperatorsInstance(null)), rcb, loader, null, requestCollatingAppender, System.getProperty("java.io.tmpdir"), 32768, 32, true, 2);

    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    when(routingContext.request()).thenReturn(request);
    when(request.method()).thenReturn(HttpMethod.HEAD);

    assertThat(assertThrows(IllegalStateException.class, () -> { router.handle(routingContext); }).getMessage(), containsString("QueryRouter#deploy not called"));
  }

  @Test
  public void testBadMethod(Vertx vertx) {

    PipelineDefnLoader loader = mock(PipelineDefnLoader.class);
    Authenticator rcb = new Authenticator(null, null, null, null, null, null, true, null, false, null, Collections.singletonList("aud"), null);
    RequestCollatingAppender requestCollatingAppender = new RequestCollatingAppender();
    QueryRouter router = new QueryRouter(vertx, null, new AuditorMemoryImpl(vertx, new OperatorsInstance(null)), rcb, loader, null, requestCollatingAppender, System.getProperty("java.io.tmpdir"), 32768, 32, true, 2);

    Future<Void> deployFuture = router.deploy();
    await().until(() -> deployFuture.isComplete());

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
    Authenticator rcb = new Authenticator(null, null, null, null, null, null, true, null, false, null, Collections.singletonList("aud"), null);
    PipelineExecutor pipelineExecutor = mock(PipelineExecutor.class);
    RequestCollatingAppender requestCollatingAppender = new RequestCollatingAppender();
    QueryRouter router = new QueryRouter(vertx, null, new AuditorMemoryImpl(vertx, new OperatorsInstance(null)), rcb, loader, pipelineExecutor, requestCollatingAppender, System.getProperty("java.io.tmpdir"), 32768, 32, true, 2);
    
    Future<Void> deployFuture = router.deploy();
    await().until(() -> deployFuture.isComplete());

    RequestContext requestContext = new RequestContext(null, "requestId", "url", "host", "", null, null, null, new IPAddressString("127.0.0.0"), null);
    
    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    when(routingContext.request()).thenReturn(request);
    when(request.path()).thenReturn("/query");
    when(request.method()).thenReturn(HttpMethod.GET);
    HttpServerResponse response = mock(HttpServerResponse.class);
    when(routingContext.response()).thenReturn(response);
    when(response.setStatusCode(400)).thenReturn(response);
    when(routingContext.get(RequestContext.class.getName())).thenReturn(requestContext);

    router.handle(routingContext);

    verify(response).setStatusCode(400);
    verify(response).end(matches("Invalid path \\(from ServiceException@uk\\.co\\.spudsoft\\.query\\.web\\.QueryRouter:\\d+\\)"));
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
  
  @Test
  void testRemoveMatrixParams() {
    // Basic cases - no matrix parameters
    assertEquals("", QueryRouter.removeMatrixParams(""));
    assertEquals("/", QueryRouter.removeMatrixParams("/"));
    assertEquals("/path", QueryRouter.removeMatrixParams("/path"));
    assertEquals("/path/to/resource", QueryRouter.removeMatrixParams("/path/to/resource"));
    assertEquals("https://example.com/path", QueryRouter.removeMatrixParams("https://example.com/path"));

    // Single matrix parameter at end
    assertEquals("/users", QueryRouter.removeMatrixParams("/users;type=admin"));
    assertEquals("/users", QueryRouter.removeMatrixParams("/users;type=admin;status=active"));

    // Multiple matrix parameters in middle of path
    assertEquals("/users/profile", QueryRouter.removeMatrixParams("/users;type=admin;status=active/profile"));
    assertEquals("/users/profile/data", QueryRouter.removeMatrixParams("/users;type=admin;status=active/profile;view=full;format=json/data"));

    // Matrix parameters at end of path
    assertEquals("/users/profile", QueryRouter.removeMatrixParams("/users/profile;view=full;format=json"));
    assertEquals("/users/profile", QueryRouter.removeMatrixParams("/users/profile;view=full"));

    // Matrix parameters followed by query string
    assertEquals("/users?query=value", QueryRouter.removeMatrixParams("/users;type=admin;status=active?query=value"));
    assertEquals("/users/profile?query=value&limit=10", QueryRouter.removeMatrixParams("/users;type=admin;status=active/profile;view=full?query=value&limit=10"));
    assertEquals("/users?query=value&limit=10", QueryRouter.removeMatrixParams("/users;type=admin;status=active?query=value&limit=10"));

    // Matrix parameters followed by fragment
    assertEquals("/users#fragment", QueryRouter.removeMatrixParams("/users;type=admin;status=active#fragment"));
    assertEquals("/users/profile#section", QueryRouter.removeMatrixParams("/users;type=admin;status=active/profile;view=full#section"));

    // Matrix parameters followed by both query and fragment
    assertEquals("/users?query=value#fragment", QueryRouter.removeMatrixParams("/users;type=admin;status=active?query=value#fragment"));
    assertEquals("/users/profile?query=value#section", QueryRouter.removeMatrixParams("/users;type=admin;status=active/profile;view=full?query=value#section"));

    // Multiple path segments with matrix parameters
    assertEquals("/users/profile/data", QueryRouter.removeMatrixParams("/users;type=admin/profile;view=full/data;format=json"));
    assertEquals("/api/v1/users/profile/data", QueryRouter.removeMatrixParams("/api;version=1/v1;build=123/users;type=admin/profile;view=full/data;format=json"));

    // Full URLs with matrix parameters
    assertEquals("https://example.com/users/profile", QueryRouter.removeMatrixParams("https://example.com/users;type=admin;status=active/profile"));
    assertEquals("https://api.example.com/v1/users/profile?query=value", QueryRouter.removeMatrixParams("https://api.example.com/v1;build=123/users;type=admin/profile;view=full?query=value"));
    assertEquals("https://example.com/users/profile?query=value&limit=10#section", QueryRouter.removeMatrixParams("https://example.com/users;type=admin/profile;view=full?query=value&limit=10#section"));

    // Edge cases with special characters in matrix parameters
    assertEquals("/users", QueryRouter.removeMatrixParams("/users;type=admin:user"));
    assertEquals("/users", QueryRouter.removeMatrixParams("/users;filter=name:john,age:30"));
    assertEquals("/users", QueryRouter.removeMatrixParams("/users;data=value%20with%20spaces"));
    assertEquals("/users", QueryRouter.removeMatrixParams("/users;empty="));
    assertEquals("/users", QueryRouter.removeMatrixParams("/users;novalue"));

    // Matrix parameters with equals signs in values
    assertEquals("/users", QueryRouter.removeMatrixParams("/users;equation=a=b+c"));
    assertEquals("/users", QueryRouter.removeMatrixParams("/users;complex=key=value&other=data"));

    // Matrix parameters with semicolons in values (should still work correctly)
    assertEquals("/users", QueryRouter.removeMatrixParams("/users;data=value"));
    assertEquals("/users/profile", QueryRouter.removeMatrixParams("/users;data=value/profile"));

    // Very long matrix parameters
    assertEquals("/users", QueryRouter.removeMatrixParams("/users;verylongparameter=verylongvaluethatgoesonforfairlyawhile"));

    // Matrix parameters at root
    assertEquals("/", QueryRouter.removeMatrixParams("/;param=value"));
    assertEquals("", QueryRouter.removeMatrixParams(";param=value"));

    // Multiple consecutive semicolons (malformed but should handle gracefully)
    assertEquals("/users", QueryRouter.removeMatrixParams("/users;;type=admin"));
    assertEquals("/users", QueryRouter.removeMatrixParams("/users;type=admin;;status=active"));

    // Matrix parameters with no path
    assertEquals("", QueryRouter.removeMatrixParams(";param=value"));
    assertEquals("?query=value", QueryRouter.removeMatrixParams(";param=value?query=value"));
    assertEquals("#fragment", QueryRouter.removeMatrixParams(";param=value#fragment"));

    // Complex real-world examples
    assertEquals("https://api.example.com/v1/reports/data",
            QueryRouter.removeMatrixParams("https://api.example.com/v1;build=123/reports;year=2023;month=12;type=sales/data;format=csv;compressed=true"));
    assertEquals("https://files.example.com/documents/folder/file.pdf",
            QueryRouter.removeMatrixParams("https://files.example.com/documents;owner=john;access=private/folder;created=2023;modified=2024/file.pdf"));
    assertEquals("https://shop.example.com/search?q=shoes&limit=20",
            QueryRouter.removeMatrixParams("https://shop.example.com/search;filters=color:red,size:large;page=1?q=shoes&limit=20"));

    // Empty matrix parameter values
    assertEquals("/users", QueryRouter.removeMatrixParams("/users;type=;status=active"));
    assertEquals("/users", QueryRouter.removeMatrixParams("/users;type=admin;status="));

    // Matrix parameters with only semicolons
    assertEquals("/users", QueryRouter.removeMatrixParams("/users;"));
    assertEquals("/users", QueryRouter.removeMatrixParams("/users;;"));

    // Preserve query parameters that contain semicolons
    assertEquals("/users?data=a;b;c", QueryRouter.removeMatrixParams("/users;type=admin?data=a;b;c"));
    assertEquals("/users?data=a;b;c&other=value", QueryRouter.removeMatrixParams("/users;type=admin?data=a;b;c&other=value"));

    // Preserve fragments that contain semicolons
    assertEquals("/users#section;subsection", QueryRouter.removeMatrixParams("/users;type=admin#section;subsection"));

    // Mixed scenarios
    assertEquals("/users?query=value#fragment;part", QueryRouter.removeMatrixParams("/users;type=admin;status=active?query=value#fragment;part"));
  }

}

