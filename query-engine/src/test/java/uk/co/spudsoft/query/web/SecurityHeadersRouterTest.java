
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

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SecurityHeadersRouter.
 * 
 * @author jtalbut
 */
class SecurityHeadersRouterTest {

  @Test
  void testConstructorWithDefaults() {
    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    MultiMap requestHeaders = mock(MultiMap.class);
    MultiMap responseHeaders = mock(MultiMap.class);

    SecurityHeadersRouter router = new SecurityHeadersRouter(null, null, null, null, null, null, null);

    setupMocks(routingContext, request, response, requestHeaders, responseHeaders, "https", null);

    router.handle(routingContext);

    // Capture and execute the headers end handler
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Handler<Void>> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
    verify(routingContext).addHeadersEndHandler(handlerCaptor.capture());
    handlerCaptor.getValue().handle(null);

    verify(responseHeaders).add("X-Frame-Options", "DENY");
    verify(responseHeaders).add("Referrer-Policy", "same-origin");
    verify(responseHeaders).contains("Permissions-Policy");
    verify(routingContext).next();
  }

  @Test
  void testConstructorWithCustomValues() {
    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    MultiMap requestHeaders = mock(MultiMap.class);
    MultiMap responseHeaders = mock(MultiMap.class);

    SecurityHeadersRouter router = new SecurityHeadersRouter(
      null,
      null,
      null,
      null,
      "SAMEORIGIN",
      "no-referrer",
      "geolocation=(), camera=()"
    );

    setupMocks(routingContext, request, response, requestHeaders, responseHeaders, "https", null);

    router.handle(routingContext);

    // Capture and execute the headers end handler
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Handler<Void>> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
    verify(routingContext).addHeadersEndHandler(handlerCaptor.capture());
    handlerCaptor.getValue().handle(null);

    verify(responseHeaders).add("X-Frame-Options", "SAMEORIGIN");
    verify(responseHeaders).add("Referrer-Policy", "no-referrer");
    verify(responseHeaders).add("Permissions-Policy", "geolocation=(), camera=()");
    verify(routingContext).next();
  }

  @Test
  void testConstructorWithInvalidXFrameOptions() {
    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    MultiMap requestHeaders = mock(MultiMap.class);
    MultiMap responseHeaders = mock(MultiMap.class);

    SecurityHeadersRouter router = new SecurityHeadersRouter(
      null,
      null,
      Arrays.asList("http://nonexistant/"),
      null,
      "INVALID",
      null,
      null
    );

    setupMocks(routingContext, request, response, requestHeaders, responseHeaders, "https", null);

    router.handle(routingContext);

    // Capture and execute the headers end handler
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Handler<Void>> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
    verify(routingContext).addHeadersEndHandler(handlerCaptor.capture());
    handlerCaptor.getValue().handle(null);

    // Should fall back to default
    verify(responseHeaders).add("X-Frame-Options", "DENY");
    verify(routingContext).next();
  }

  @Test
  void testConstructorWithInvalidReferrerPolicy() {
    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    MultiMap requestHeaders = mock(MultiMap.class);
    MultiMap responseHeaders = mock(MultiMap.class);

    SecurityHeadersRouter router = new SecurityHeadersRouter(
      null,
      null,
      null,
      null,
      null,
      "invalid-policy",
      null
    );

    setupMocks(routingContext, request, response, requestHeaders, responseHeaders, "https", null);

    router.handle(routingContext);

    // Capture and execute the headers end handler
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Handler<Void>> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
    verify(routingContext).addHeadersEndHandler(handlerCaptor.capture());
    handlerCaptor.getValue().handle(null);

    // Should fall back to default
    verify(responseHeaders).add("Referrer-Policy", "same-origin");
    verify(routingContext).next();
  }

  @Test
  void testConstructorWithLogoUrls() {
    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    MultiMap requestHeaders = mock(MultiMap.class);
    MultiMap responseHeaders = mock(MultiMap.class);

    List<String> logoUrls = Arrays.asList(
      "https://example.com/logo.png",
      "https://cdn.example.com/assets/logo.svg"
    );

    SecurityHeadersRouter router = new SecurityHeadersRouter(logoUrls, null, null, null, null, null, null);

    setupMocks(routingContext, request, response, requestHeaders, responseHeaders, "https", null);

    router.handle(routingContext);

    // Capture and execute the headers end handler
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Handler<Void>> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
    verify(routingContext).addHeadersEndHandler(handlerCaptor.capture());
    handlerCaptor.getValue().handle(null);

    verify(responseHeaders).add(eq("Content-Security-Policy"), contains("example.com"));
    verify(responseHeaders).add(eq("Content-Security-Policy"), contains("cdn.example.com"));
    verify(routingContext).next();
  }

  @Test
  void testHandleWithHttpsRequest() {
    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    MultiMap requestHeaders = mock(MultiMap.class);
    MultiMap responseHeaders = mock(MultiMap.class);

    SecurityHeadersRouter router = new SecurityHeadersRouter(null, null, null, null, null, null, null);

    setupMocks(routingContext, request, response, requestHeaders, responseHeaders, "https", null);

    router.handle(routingContext);

    // Capture and execute the headers end handler
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Handler<Void>> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
    verify(routingContext).addHeadersEndHandler(handlerCaptor.capture());
    handlerCaptor.getValue().handle(null);

    verify(responseHeaders).add("Strict-Transport-Security", "max-age=63072000");
    verify(responseHeaders).add("Content-Security-Policy", "default-src 'self'; img-src 'self'; style-src 'self'; connect-src 'self'; script-src 'self'");
    verify(responseHeaders).add("X-Frame-Options", "DENY");
    verify(responseHeaders).add("X-Content-Type-Options", "nosniff");
    verify(responseHeaders).add("Referrer-Policy", "same-origin");
    verify(responseHeaders).contains("Permissions-Policy");
    verify(routingContext).next();
  }

  @Test
  void testHandleWithHttpRequest() {
    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    MultiMap requestHeaders = mock(MultiMap.class);
    MultiMap responseHeaders = mock(MultiMap.class);

    SecurityHeadersRouter router = new SecurityHeadersRouter(null, null, null, null, null, null, null);

    setupMocks(routingContext, request, response, requestHeaders, responseHeaders, "http", null);

    router.handle(routingContext);

    // Capture and execute the headers end handler
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Handler<Void>> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
    verify(routingContext).addHeadersEndHandler(handlerCaptor.capture());
    handlerCaptor.getValue().handle(null);

    // Should not add HSTS for HTTP requests
    verify(responseHeaders, never()).add("Strict-Transport-Security", "max-age=63072000");
    verify(responseHeaders).add("Content-Security-Policy", "default-src 'self'; img-src 'self'; style-src 'self'; connect-src 'self'; script-src 'self'");
    verify(responseHeaders).add("X-Frame-Options", "DENY");
    verify(responseHeaders).add("X-Content-Type-Options", "nosniff");
    verify(responseHeaders).add("Referrer-Policy", "same-origin");
    verify(responseHeaders).contains("Permissions-Policy");
    verify(routingContext).next();
  }
  
  @Test
  void testHandleWithXForwardedProtoHttps() {
    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    MultiMap requestHeaders = mock(MultiMap.class);
    MultiMap responseHeaders = mock(MultiMap.class);

    SecurityHeadersRouter router = new SecurityHeadersRouter(null, null, null, null, null, null, null);

    setupMocks(routingContext, request, response, requestHeaders, responseHeaders, "http", "https");

    router.handle(routingContext);

    // Capture and execute the headers end handler
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Handler<Void>> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
    verify(routingContext).addHeadersEndHandler(handlerCaptor.capture());
    handlerCaptor.getValue().handle(null);

    // Should add HSTS when X-Forwarded-Proto is https
    verify(responseHeaders).add("Strict-Transport-Security", "max-age=63072000");
    verify(routingContext).next();
  }

  @Test
  void testHandleDoesNotOverrideExistingHeaders() {
    RoutingContext routingContext = mock(RoutingContext.class);
    HttpServerRequest request = mock(HttpServerRequest.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    MultiMap requestHeaders = mock(MultiMap.class);
    MultiMap responseHeaders = mock(MultiMap.class);

    SecurityHeadersRouter router = new SecurityHeadersRouter(null, null, null, null, null, null, null);

    setupMocks(routingContext, request, response, requestHeaders, responseHeaders, "https", null);

    // Mock that headers already exist
    when(responseHeaders.contains("Strict-Transport-Security")).thenReturn(true);
    when(responseHeaders.contains("Content-Security-Policy")).thenReturn(true);
    when(responseHeaders.contains("X-Frame-Options")).thenReturn(true);
    when(responseHeaders.contains("X-Content-Type-Options")).thenReturn(true);
    when(responseHeaders.contains("Referrer-Policy")).thenReturn(true);
    when(responseHeaders.contains("Permissions-Policy")).thenReturn(true);

    router.handle(routingContext);

    // Capture and execute the headers end handler
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Handler<Void>> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
    verify(routingContext).addHeadersEndHandler(handlerCaptor.capture());
    handlerCaptor.getValue().handle(null);

    // Should not add headers that already exist
    verify(responseHeaders, never()).add(eq("Strict-Transport-Security"), anyString());
    verify(responseHeaders, never()).add(eq("Content-Security-Policy"), anyString());
    verify(responseHeaders, never()).add(eq("X-Frame-Options"), anyString());
    verify(responseHeaders, never()).add(eq("X-Content-Type-Options"), anyString());
    verify(responseHeaders, never()).add(eq("Referrer-Policy"), anyString());
    verify(responseHeaders, never()).add(eq("Permissions-Policy"), anyString());
    verify(routingContext).next();
  }

  @Test
  void testWasHttpsWithHttpsScheme() {
    HttpServerRequest request = mock(HttpServerRequest.class);
    MultiMap requestHeaders = mock(MultiMap.class);

    when(request.scheme()).thenReturn("https");
    when(request.headers()).thenReturn(requestHeaders);
    when(requestHeaders.get("X-Forwarded-Proto")).thenReturn(null);

    assertTrue(SecurityHeadersRouter.wasHttps(request));
  }

  @Test
  void testWasHttpsWithHttpScheme() {
    HttpServerRequest request = mock(HttpServerRequest.class);
    MultiMap requestHeaders = mock(MultiMap.class);

    when(request.scheme()).thenReturn("http");
    when(request.headers()).thenReturn(requestHeaders);
    when(requestHeaders.get("X-Forwarded-Proto")).thenReturn(null);

    assertFalse(SecurityHeadersRouter.wasHttps(request));
  }

  @Test
  void testWasHttpsWithXForwardedProtoHttps() {
    HttpServerRequest request = mock(HttpServerRequest.class);
    MultiMap requestHeaders = mock(MultiMap.class);

    when(request.scheme()).thenReturn("http");
    when(request.headers()).thenReturn(requestHeaders);
    when(requestHeaders.get("X-Forwarded-Proto")).thenReturn("https");

    assertTrue(SecurityHeadersRouter.wasHttps(request));
  }

  @Test
  void testWasHttpsWithXForwardedProtoHttp() {
    HttpServerRequest request = mock(HttpServerRequest.class);
    MultiMap requestHeaders = mock(MultiMap.class);

    when(request.scheme()).thenReturn("https");
    when(request.headers()).thenReturn(requestHeaders);
    when(requestHeaders.get("X-Forwarded-Proto")).thenReturn("http");

    assertFalse(SecurityHeadersRouter.wasHttps(request));
  }

  @Test
  void testAllValidReferrerPolicies() {
    String[] validPolicies = {
      "no-referrer",
      "no-referrer-when-downgrade",
      "origin",
      "origin-when-cross-origin",
      "same-origin",
      "strict-origin",
      "strict-origin-when-cross-origin",
      "unsafe-url"
    };

    for (String policy : validPolicies) {
      RoutingContext routingContext = mock(RoutingContext.class);
      HttpServerRequest request = mock(HttpServerRequest.class);
      HttpServerResponse response = mock(HttpServerResponse.class);
      MultiMap requestHeaders = mock(MultiMap.class);
      MultiMap responseHeaders = mock(MultiMap.class);

      SecurityHeadersRouter router = new SecurityHeadersRouter(null, null, null, null, null, policy, null);

      setupMocks(routingContext, request, response, requestHeaders, responseHeaders, "https", null);

      router.handle(routingContext);

      // Capture and execute the headers end handler
      @SuppressWarnings("unchecked")
      ArgumentCaptor<Handler<Void>> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
      verify(routingContext).addHeadersEndHandler(handlerCaptor.capture());
      handlerCaptor.getValue().handle(null);

      verify(responseHeaders).add("Referrer-Policy", policy);
    }
  }

  @Test
  void testValidXFrameOptions() {
    String[] validOptions = {"DENY", "SAMEORIGIN", "deny", "sameorigin"};
    String[] expectedOptions = {"DENY", "SAMEORIGIN", "DENY", "SAMEORIGIN"};

    for (int i = 0; i < validOptions.length; i++) {
      RoutingContext routingContext = mock(RoutingContext.class);
      HttpServerRequest request = mock(HttpServerRequest.class);
      HttpServerResponse response = mock(HttpServerResponse.class);
      MultiMap requestHeaders = mock(MultiMap.class);
      MultiMap responseHeaders = mock(MultiMap.class);

      SecurityHeadersRouter router = new SecurityHeadersRouter(
              null,
              null,
              null,
              null,
              validOptions[i],
              null,
              null
      );

      setupMocks(routingContext, request, response, requestHeaders, responseHeaders, "https", null);

      router.handle(routingContext);

      // Capture and execute the headers end handler
      @SuppressWarnings("unchecked")
      ArgumentCaptor<Handler<Void>> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
      verify(routingContext).addHeadersEndHandler(handlerCaptor.capture());
      handlerCaptor.getValue().handle(null);

      verify(responseHeaders).add("X-Frame-Options", expectedOptions[i]);
    }
  }

  private void setupMocks(RoutingContext routingContext, HttpServerRequest request, HttpServerResponse response,
                          MultiMap requestHeaders, MultiMap responseHeaders, String scheme, String xForwardedProto) {
    when(routingContext.request()).thenReturn(request);
    when(routingContext.response()).thenReturn(response);
    when(request.scheme()).thenReturn(scheme);
    when(request.headers()).thenReturn(requestHeaders);
    when(requestHeaders.get("X-Forwarded-Proto")).thenReturn(xForwardedProto);
    when(response.headers()).thenReturn(responseHeaders);
    when(responseHeaders.contains(anyString())).thenReturn(false);
    when(responseHeaders.add(anyString(), anyString())).thenReturn(responseHeaders);
  }
}