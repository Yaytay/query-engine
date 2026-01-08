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
package uk.co.spudsoft.query.main;

import com.google.common.collect.ImmutableMap;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.jwtvalidatorvertx.JsonWebAlgorithm;
import uk.co.spudsoft.jwtvalidatorvertx.TokenBuilder;
import uk.co.spudsoft.jwtvalidatorvertx.jdk.JdkJwksHandler;

public class MockOAuthServer {

  private static final Logger logger = LoggerFactory.getLogger(MockOAuthServer.class);

  private final TokenBuilder tokenBuilder;
  private final JdkJwksHandler jwks;
  private HttpServer server;
  private int port;
  private final Map<String, String> clientCredentials = new HashMap<>();

  public MockOAuthServer(TokenBuilder tokenBuilder, JdkJwksHandler jwks) throws IOException {
    this.tokenBuilder = tokenBuilder;
    this.jwks = jwks;
    this.port = findUnusedPort();
    this.server = HttpServer.create(new InetSocketAddress(port), 0);

    // Add test credentials - Username:Password as used in BasicAuthQueryIT
    clientCredentials.put("Username", "Password");

    setupEndpoints();
  }

  private void setupEndpoints() {
    // OAuth token endpoint for client credentials flow
    server.createContext("/token", new TokenHandler());

    // JWKS endpoint (minimal implementation)
    server.createContext("/jwks", new JwksHandler());

    // OpenID Connect discovery endpoint - note the exact path
    server.createContext("/.well-known/openid_configuration", new DiscoveryHandler());

    // Root handler for debugging
    server.createContext("/", new RootHandler());
  }

  public void start() {
    server.setExecutor(null);
    server.start();
    logger.info("Mock OAuth server started on port {}", port);
    logger.info("Discovery endpoint: {}", getBaseUrl() + "/.well-known/openid_configuration");
  }

  public void stop() {
    if (server != null) {
      server.stop(0);
    }
  }

  public int getPort() {
    return port;
  }

  public String getBaseUrl() {
    return "http://localhost:" + port;
  }

  private static int findUnusedPort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  private class RootHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      String path = exchange.getRequestURI().getPath();
      logger.debug("Root handler received request for path: {}", path);
      sendResponse(exchange, 404, "Not Found: " + path);
    }
  }

  private class TokenHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      logger.debug("Token handler received {} request", exchange.getRequestMethod());

      if (!"POST".equals(exchange.getRequestMethod())) {
        sendResponse(exchange, 405, "Method not allowed");
        return;
      }

      try {
        // Read the request body
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> params = parseFormData(body);

        String grantType = params.get("grant_type");
        String clientId = params.get("client_id");
        String clientSecret = params.get("client_secret");

        logger.debug("Token request: grant_type={}, client_id={}, client_secret={}",
                grantType, clientId, clientSecret != null ? "***" : null);

        if (!"client_credentials".equals(grantType)) {
          sendErrorResponse(exchange, "unsupported_grant_type", "Only client_credentials grant type is supported");
          return;
        }

        // Validate credentials
        if (clientId == null || clientSecret == null) {
          sendErrorResponse(exchange, "invalid_request", "Missing client credentials");
          return;
        }

        String expectedPassword = clientCredentials.get(clientId);
        if (expectedPassword == null || !expectedPassword.equals(clientSecret)) {
          sendErrorResponse(exchange, "invalid_client", "Invalid client credentials");
          return;
        }

        // Generate a simple access token (in real implementation, this would be a proper JWT)
        String accessToken = tokenBuilder.buildToken(JsonWebAlgorithm.RS512,
                 "AuthQueryIT-" + UUID.randomUUID().toString(),
                 jwks.getBaseUrl(),
                 "sub" + clientId,
                 Arrays.asList("query-engine"),
                 System.currentTimeMillis() / 1000,
                 System.currentTimeMillis() / 1000 + 2,
                 ImmutableMap.<String, Object>builder()
                        .put("name", "Full Name")
                        .put("preferred_username", clientId)
                        .build()
        );

        JsonObject tokenResponse = new JsonObject()
                .put("access_token", accessToken)
                .put("token_type", "Bearer")
                .put("expires_in", 3600)
                .put("scope", "query-engine");

        sendJsonResponse(exchange, 200, tokenResponse.encode());

      } catch (Exception e) {
        logger.error("Error processing token request", e);
        sendErrorResponse(exchange, "server_error", "Internal server error");
      }
    }
  }

  private class JwksHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      logger.debug("JWKS handler received {} request", exchange.getRequestMethod());

      // Handle both GET and HEAD requests
      if (!"GET".equals(exchange.getRequestMethod()) && !"HEAD".equals(exchange.getRequestMethod())) {
        sendResponse(exchange, 405, "Method not allowed");
        return;
      }

      // Minimal JWKS response - in real implementation you'd include actual keys
      JsonObject jwks = new JsonObject()
              .put("keys", new io.vertx.core.json.JsonArray());

      sendJsonResponse(exchange, 200, jwks.encode());
    }
  }

  private class DiscoveryHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      logger.debug("Discovery handler received {} request for path: {}",
              exchange.getRequestMethod(), exchange.getRequestURI().getPath());

      // Handle both GET and HEAD requests
      if (!"GET".equals(exchange.getRequestMethod())) {
        sendResponse(exchange, 405, "Method not allowed");
        return;
      }

      JsonObject discovery = new JsonObject()
              .put("issuer", getBaseUrl())
              .put("token_endpoint", getBaseUrl() + "/token")
              .put("jwks_uri", getBaseUrl() + "/jwks")
              .put("grant_types_supported", new io.vertx.core.json.JsonArray().add("client_credentials"))
              .put("token_endpoint_auth_methods_supported", new io.vertx.core.json.JsonArray().add("client_secret_post"))
              .put("response_types_supported", new io.vertx.core.json.JsonArray().add("token"));

      sendJsonResponse(exchange, 200, discovery.encode());
    }
  }

  private Map<String, String> parseFormData(String body) {
    Map<String, String> params = new HashMap<>();
    if (body != null && !body.isEmpty()) {
      String[] pairs = body.split("&");
      for (String pair : pairs) {
        String[] keyValue = pair.split("=", 2);
        if (keyValue.length == 2) {
          try {
            params.put(
                    URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8)
            );
          } catch (Exception e) {
            logger.warn("Failed to decode form parameter: {}", pair, e);
          }
        }
      }
    }
    return params;
  }

  private void sendResponse(HttpExchange exchange, int responseCode, String body) throws IOException {
    byte[] response = body.getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(responseCode, response.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(response);
    }
  }

  private void sendJsonResponse(HttpExchange exchange, int responseCode, String json) throws IOException {
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    sendResponse(exchange, responseCode, json);
  }

  private void sendErrorResponse(HttpExchange exchange, String error, String description) throws IOException {
    JsonObject errorResponse = new JsonObject()
            .put("error", error)
            .put("error_description", description);

    sendJsonResponse(exchange, 400, errorResponse.encode());
  }
}
