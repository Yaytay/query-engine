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
package uk.co.spudsoft.query.web;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.net.impl.HostAndPortImpl;
import io.vertx.core.net.impl.SocketAddressImpl;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import uk.co.spudsoft.query.exec.AuditorMemoryImpl;

@ExtendWith(MockitoExtension.class)
class LoggingRouterTest {

  @Test
  @SuppressWarnings("unchecked")
  void testHandleRegistersHandlersAndCallsNext() {
    // Arrange
    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    HttpServerResponse response = mock(HttpServerResponse.class);

    when(routingContext.request()).thenReturn(request);
    when(request.method()).thenReturn(HttpMethod.GET);
    when(request.uri()).thenReturn("/test/path");
    when(request.remoteAddress()).thenReturn(new SocketAddressImpl(37, "127.0.0.0"));
    when(request.scheme()).thenReturn("http");
    when(request.authority()).thenReturn(HostAndPortImpl.parseAuthority("localhost", 80));

    LoggingRouter loggingRouter = new LoggingRouter(null);

    // Act
    loggingRouter.handle(routingContext);

    // Assert that handlers are registered
    verify(routingContext).addHeadersEndHandler(any(Handler.class));
    verify(routingContext).addBodyEndHandler(any(Handler.class));
    verify(routingContext).addEndHandler(any(Handler.class));

    // Assert that next() is called
    verify(routingContext).next();
  }

  @Test
  @SuppressWarnings("unchecked")
  void testHandlersExecuteCorrectly() {
    // Arrange
    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    HttpServerResponse response = mock(HttpServerResponse.class);

    when(routingContext.request()).thenReturn(request);
    when(routingContext.response()).thenReturn(response);
    when(request.method()).thenReturn(HttpMethod.GET);
    when(request.uri()).thenReturn("/test/path");
    when(response.getStatusCode()).thenReturn(200);
    when(response.bytesWritten()).thenReturn(1024L);
    when(request.remoteAddress()).thenReturn(new SocketAddressImpl(37, "127.0.0.0"));
    when(request.scheme()).thenReturn("http");
    when(request.authority()).thenReturn(HostAndPortImpl.parseAuthority("localhost", 80));

    LoggingRouter loggingRouter = new LoggingRouter(null);

    // Capture the handlers
    ArgumentCaptor<Handler<Void>> headersEndCaptor = ArgumentCaptor.forClass(Handler.class);
    ArgumentCaptor<Handler<Void>> bodyEndCaptor = ArgumentCaptor.forClass(Handler.class);
    ArgumentCaptor<Handler<AsyncResult<Void>>> endCaptor = ArgumentCaptor.forClass(Handler.class);

    // Act
    loggingRouter.handle(routingContext);

    // Verify handlers are captured
    verify(routingContext).addHeadersEndHandler(headersEndCaptor.capture());
    verify(routingContext).addBodyEndHandler(bodyEndCaptor.capture());
    verify(routingContext).addEndHandler(endCaptor.capture());

    // Execute the captured handlers to ensure they don't throw exceptions
    assertDoesNotThrow(() -> headersEndCaptor.getValue().handle(null));
    assertDoesNotThrow(() -> bodyEndCaptor.getValue().handle(null));
    assertDoesNotThrow(() -> endCaptor.getValue().handle(null));

    // Verify response methods are called when handlers execute
    verify(response, atLeast(3)).getStatusCode();
    verify(response, atLeast(2)).bytesWritten();
  }

  @Test
  void testConstructor() {
    // Test that constructor doesn't throw any exceptions
    assertDoesNotThrow(() -> new LoggingRouter(null));
  }
}
