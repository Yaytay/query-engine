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

import com.google.common.cache.Cache;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import uk.co.spudsoft.jwtvalidatorvertx.AlgorithmAndKeyPair;
import uk.co.spudsoft.jwtvalidatorvertx.JsonWebAlgorithm;
import uk.co.spudsoft.jwtvalidatorvertx.JwkBuilder;
import uk.co.spudsoft.jwtvalidatorvertx.jdk.JdkTokenBuilder;

/**
 * Configurable mock OIDC Identity Provider for testing. Supports both front-channel and back-channel logout mechanisms.
 */
public class MockOidcServer {

  private static final Logger logger = LoggerFactory.getLogger(MockOidcServer.class);

  private Cache<String, AlgorithmAndKeyPair> keyCache = AlgorithmAndKeyPair.createCache(Duration.ofMinutes(1));
  private JdkTokenBuilder builder = new JdkTokenBuilder(keyCache);

  private final String issuer;
  private final int port;
  private final String clientId;
  private final String clientSecret;
  private final boolean supportsFrontChannelLogout;
  private final boolean supportsBackChannelLogout;
  private final String frontChannelLogoutUri;
  private final String backChannelLogoutUri;

  private HttpServer server;
  private final AtomicBoolean started = new AtomicBoolean(false);
  private final Map<String, String> authorizationCodes = new HashMap<>();
  private final Map<String, String> accessTokens = new HashMap<>();
  private final List<LogoutRequest> logoutRequests = new ArrayList<>();

  /**
   * Configuration builder for MockOidcServer.
   */
  public static class Builder {

    private String issuer;
    private int port = 0; // 0 means find unused port
    private String clientId = "test-client";
    private String clientSecret = "test-secret";
    private boolean supportsFrontChannelLogout = true;
    private boolean supportsBackChannelLogout = true;
    private String frontChannelLogoutUri;
    private String backChannelLogoutUri;

    public Builder issuer(String issuer) {
      this.issuer = issuer;
      return this;
    }

    public Builder port(int port) {
      this.port = port;
      return this;
    }

    public Builder clientId(String clientId) {
      this.clientId = clientId;
      return this;
    }

    public Builder clientSecret(String clientSecret) {
      this.clientSecret = clientSecret;
      return this;
    }

    public Builder supportsFrontChannelLogout(boolean supports) {
      this.supportsFrontChannelLogout = supports;
      return this;
    }

    public Builder supportsBackChannelLogout(boolean supports) {
      this.supportsBackChannelLogout = supports;
      return this;
    }

    public Builder frontChannelLogoutUri(String uri) {
      this.frontChannelLogoutUri = uri;
      return this;
    }

    public Builder backChannelLogoutUri(String uri) {
      this.backChannelLogoutUri = uri;
      return this;
    }

    public MockOidcServer build() throws Exception {
      if (issuer == null) {
        if (port == 0) {
          port = findUnusedPort();
        }
        issuer = "http://localhost:" + port + "/";
      }
      return new MockOidcServer(this);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Record representing a logout request.
   */
  public record LogoutRequest(boolean frontChannel, String sessionId, String idToken, String accessToken, String refreshToken, String postLogoutRedirectUri, Instant timestamp) {

  }

  private MockOidcServer(Builder builder) throws NoSuchAlgorithmException {
    this.issuer = builder.issuer;
    this.port = builder.port;
    this.clientId = builder.clientId;
    this.clientSecret = builder.clientSecret;
    this.supportsFrontChannelLogout = builder.supportsFrontChannelLogout;
    this.supportsBackChannelLogout = builder.supportsBackChannelLogout;
    this.frontChannelLogoutUri = builder.frontChannelLogoutUri;
    this.backChannelLogoutUri = builder.backChannelLogoutUri;
  }

  /**
   * Start the mock OIDC server.
   */
  public void start() throws IOException {
    if (started.compareAndSet(false, true)) {
      server = HttpServer.create(new InetSocketAddress(port), 0);

      // OpenID Connect Discovery endpoint
      server.createContext("/.well-known/openid-configuration", new DiscoveryHandler());

      // JWKS endpoint
      server.createContext("/jwks", new JwksHandler());

      // Authorization endpoint
      server.createContext("/auth", new AuthorizationHandler());

      // Token endpoint
      server.createContext("/token", new TokenHandler());

      // End session endpoint (logout)
      server.createContext("/logout", new LogoutHandler());

      // Back-channel logout endpoint
      if (supportsBackChannelLogout) {
        server.createContext("/backchannel-logout", new BackChannelLogoutHandler());
      }

      server.setExecutor(null);
      server.start();

      logger.info("Mock OIDC server started on port {} with issuer: {}", getActualPort(), issuer);
    }
  }

  /**
   * Stop the mock OIDC server.
   */
  public void stop() {
    if (started.compareAndSet(true, false)) {
      if (server != null) {
        server.stop(0);
        logger.info("Mock OIDC server stopped");
      }
    }
  }

  /**
   * Get the actual port the server is listening on.
   */
  public int getActualPort() {
    return server != null ? server.getAddress().getPort() : port;
  }

  /**
   * Get the issuer URL.
   */
  public String getIssuer() {
    return issuer;
  }

  /**
   * Get the client ID.
   */
  public String getClientId() {
    return clientId;
  }

  /**
   * Get the client secret.
   */
  public String getClientSecret() {
    return clientSecret;
  }

  /**
   * Get recorded logout requests for testing.
   */
  public List<LogoutRequest> getLogoutRequests() {
    return new ArrayList<>(logoutRequests);
  }

  /**
   * Clear recorded logout requests.
   */
  public void clearLogoutRequests() {
    logoutRequests.clear();
  }

  /**
   * Find an unused port.
   */
  public static int findUnusedPort() {
    Path targetDir = Paths.get("target", "reserved-ports");

    try {
      Files.createDirectories(targetDir);

      for (int attempt = 0; attempt < 100; attempt++) {
        try (ServerSocket socket = new ServerSocket(0)) {
          int port = socket.getLocalPort();
          Path lockFile = targetDir.resolve("port-" + port + ".lock");

          try {
            Files.createFile(lockFile);
            lockFile.toFile().deleteOnExit();
            return port;
          } catch (FileAlreadyExistsException e) {
            continue;
          }
        }
      }

      throw new RuntimeException("Could not find an unused port after 100 attempts");
    } catch (IOException ex) {
      throw new RuntimeException("Failed to reserve port", ex);
    }
  }

  private void sendResponse(HttpExchange exchange, int responseCode, String contentType, String body) throws IOException {
    byte[] responseBytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", contentType);
    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    exchange.sendResponseHeaders(responseCode, responseBytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(responseBytes);
    }
  }

  /**
   * Handler for OpenID Connect Discovery endpoint.
   */
  private class DiscoveryHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      JsonObject discovery = new JsonObject()
              .put("issuer", issuer)
              .put("authorization_endpoint", issuer + "auth")
              .put("token_endpoint", issuer + "token")
              .put("jwks_uri", issuer + "jwks")
              .put("response_types_supported", new io.vertx.core.json.JsonArray().add("code"))
              .put("subject_types_supported", new io.vertx.core.json.JsonArray().add("public"))
              .put("id_token_signing_alg_values_supported", new io.vertx.core.json.JsonArray().add("RS256"));

      if (supportsFrontChannelLogout) {
        discovery.put("frontchannel_logout_supported", true);
        discovery.put("end_session_endpoint", issuer + "logout");
        if (frontChannelLogoutUri != null) {
          discovery.put("frontchannel_logout_session_supported", true);
        }
      }

      if (supportsBackChannelLogout) {
        discovery.put("backchannel_logout_supported", true);
        discovery.put("backchannel_logout_session_supported", true);
        discovery.put("revocation_endpoint", issuer + "backchannel-logout");
      }

      sendResponse(exchange, 200, "application/json", discovery.encode());
    }
  }
  
  public String getLogoutUrl() {
    return issuer + "logout";
  }

  /**
   * Handler for JWKS endpoint.
   */
  private class JwksHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      JsonObject jwkSet = new JsonObject();
      JsonArray jwks = new JsonArray();
      jwkSet.put("keys", jwks);
      synchronized (keyCache) {
        keyCache.asMap().forEach((kid, akp) -> {
          PublicKey key = akp.getKeyPair().getPublic();
          try {
            JsonObject json = JwkBuilder.get(key).toJson(kid, akp.getAlgorithm().getName(), key);
            jwks.add(json);
          } catch (Exception ex) {
            logger.warn("Failed to add key {} to JWKS: ", kid, ex);
          }
        });
      }
      exchange.getResponseHeaders().add("cache-control", "max-age=100");
      sendResponse(exchange, 200, "application/json", jwkSet.encode());
    }
  }

  /**
   * Handler for authorization endpoint.
   */
  private class AuthorizationHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      String query = exchange.getRequestURI().getQuery();
      Map<String, String> params = parseQueryString(query);

      String redirectUri = params.get("redirect_uri");
      String state = params.get("state");
      String scope = params.get("scope");
      String code = UUID.randomUUID().toString();

      // Store the authorization code with scope for later token generation
      authorizationCodes.put(code, clientId + "|" + (scope != null ? scope : ""));

      String redirectUrl = redirectUri + "?code=" + code + "&state=" + state;

      exchange.getResponseHeaders().add("Location", redirectUrl);
      exchange.sendResponseHeaders(302, -1);
    }
  }

  /**
   * Handler for token endpoint.
   */
  private class TokenHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      try {
        // Parse the request body for authorization code flow
        String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> params = parseFormData(requestBody);

        String code = params.get("code");
        String clientIdFromRequest = params.get("client_id");

        // Validate the authorization code
        String storedData = authorizationCodes.get(code);
        if (storedData == null) {
          logger.warn("storedData not found for {}", code);
          sendResponse(exchange, 400, "application/json",
                  new JsonObject().put("error", "invalid_grant").encode());
          return;
        }

        String[] parts = storedData.split("\\|", 2);
        String storedClientId = parts[0];
        String scope = parts.length > 1 ? parts[1] : "";

        if (!clientId.equals(storedClientId) || !clientId.equals(clientIdFromRequest)) {
          sendResponse(exchange, 400, "application/json",
                  new JsonObject().put("error", "invalid_client").encode());
          return;
        }

        // Generate response
        JsonObject tokenResponse;
        try {
          String accessToken = createJwtAccessToken(scope);
          accessTokens.put(accessToken, clientId);

          tokenResponse = new JsonObject()
                  .put("access_token", accessToken)
                  .put("token_type", "Bearer")
                  .put("expires_in", 3600)
                  .put("scope", scope);

          // Only include ID token if scope includes "openid"
          if (scope.contains("openid")) {
            String idToken = createJwtIdToken(scope);
            tokenResponse.put("id_token", idToken);
          }
        } catch (Exception ex) {
          logger.error("Failed to create token: ", ex);
          throw new AccessDeniedException("Failed");
          
        }

        // Clean up the authorization code (single use)
        authorizationCodes.remove(code);

        sendResponse(exchange, 200, "application/json", tokenResponse.encode());
      } catch (IOException e) {
        logger.error("Error processing token request", e);
        sendResponse(exchange, 500, "application/json",
                new JsonObject().put("error", "server_error").encode());
      }
    }
  }

  /**
   * Handler for logout endpoint.
   */
  private class LogoutHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      String query = exchange.getRequestURI().getQuery();
      Map<String, String> params = parseQueryString(query);

      String idTokenHint = params.get("id_token_hint");
      String postLogoutRedirectUri = params.get("post_logout_redirect_uri");
      String sessionId = params.get("logout_hint"); // or extract from session

      // Record the logout request
      LogoutRequest logoutRequest = new LogoutRequest(
              true,
              sessionId,
              idTokenHint,
              null,
              null,
              postLogoutRedirectUri,
              Instant.now()
      );
      logoutRequests.add(logoutRequest);

      // Redirect back to post logout URI if provided
      if (postLogoutRedirectUri != null) {
        exchange.getResponseHeaders().add("Location", postLogoutRedirectUri);
        exchange.sendResponseHeaders(302, -1);
      } else {
        sendResponse(exchange, 200, "text/html",
                "<html><body><h1>Logged out successfully</h1></body></html>");
      }
    }
  }

  /**
   * Handler for back-channel logout endpoint.
   */
  private class BackChannelLogoutHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      // In a real implementation, would validate the logout token JWT      
      // For testing, just record that back-channel logout was called
      
      String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
      Map<String, String> params = parseQueryString(body);
      String token_type_hint = params.get("token_type_hint");
      String refreshToken = null;
      if ("refresh_token".equals(token_type_hint)) {
        refreshToken = params.get("token");
      }
      String accessToken = null;
      if ("access_token".equals(token_type_hint)) {
        accessToken = params.get("token");
      }
      String idToken = null;
      if ("id_token".equals(token_type_hint)) {
        idToken = params.get("token");
      }
      String sessionId = null;
      
      LogoutRequest logoutRequest = new LogoutRequest(
              false,
              sessionId,
              idToken,
              accessToken,
              refreshToken,
              null,
              Instant.now()
      );
      logoutRequests.add(logoutRequest);

      sendResponse(exchange, 200, "application/json", "{}");
    }
  }

  private Map<String, String> parseQueryString(String query) {
    Map<String, String> params = new HashMap<>();
    if (query != null && !query.isEmpty()) {
      String[] pairs = query.split("&");
      for (String pair : pairs) {
        String[] keyValue = pair.split("=", 2);
        if (keyValue.length == 2) {
          try {
            String key = java.net.URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
            String value = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
            params.put(key, value);
          } catch (Exception e) {
            // Skip malformed pairs
          }
        }
      }
    }
    return params;
  }

  private Map<String, String> parseFormData(String formData) {
    Map<String, String> params = new HashMap<>();
    if (formData != null && !formData.isEmpty()) {
      String[] pairs = formData.split("&");
      for (String pair : pairs) {
        String[] keyValue = pair.split("=", 2);
        if (keyValue.length == 2) {
          try {
            String key = java.net.URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
            String value = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
            params.put(key, value);
          } catch (Exception e) {
            // Skip malformed pairs
          }
        }
      }
    }
    return params;
  }

  private String createJwtAccessToken(String scope) throws Exception {
    
    long nowSeconds = Instant.now().getEpochSecond();
    return builder.buildToken(JsonWebAlgorithm.RS512
            , this.toString()
            , issuer
            , "test-user"
            , Arrays.asList("query-engine")
            , nowSeconds
            , nowSeconds + 100
            , null
    );
  }

  private String createJwtIdToken(String scope) throws Exception {
    
    long nowSeconds = Instant.now().getEpochSecond();
    Map<String, Object> payload = new HashMap<>();
    payload.put("auth_time", nowSeconds);
    payload.put("nonce", "test-nonce");
    payload.put("email", "test@example.com");
    payload.put("name", "Test User");
    payload.put("preferred_username", "testuser");

    // Add additional claims based on scope
    if (scope.contains("profile")) {
      payload.put("given_name", "Test");
      payload.put("family_name", "User");
      payload.put("picture", "https://example.com/avatar.png");
    }

    if (scope.contains("email")) {
      payload.put("email_verified", true);
    }
    
    return builder.buildToken(JsonWebAlgorithm.RS512
            , this.toString()
            , issuer
            , "test-user"
            , Arrays.asList("query-engine")
            , nowSeconds
            , nowSeconds + 100
            , payload
    );
  }
}
