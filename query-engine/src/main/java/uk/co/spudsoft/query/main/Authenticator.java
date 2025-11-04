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
package uk.co.spudsoft.query.main;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.reactiverse.contextual.logging.ContextualData;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.HostAndPort;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.jwtvalidatorvertx.Jwt;
import uk.co.spudsoft.jwtvalidatorvertx.JwtValidator;
import uk.co.spudsoft.jwtvalidatorvertx.OpenIdDiscoveryHandler;
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.web.LoginDao;
import uk.co.spudsoft.query.web.ServiceException;

/**
 * Factory object for constructing filled-in {@link RequestContext} objects.
 *
 * @author jtalbut
 */
public class Authenticator {

  private static final Logger logger = LoggerFactory.getLogger(Authenticator.class);

  private static final String BASIC = "Basic ";
  private static final String BEARER = "Bearer ";

  private final WebClient webClient;
  private final JwtValidator validator;
  private final OpenIdDiscoveryHandler discoverer;
  private final LoginDao loginDao;
  private final BasicAuthConfig basicAuthConfig;
  private final boolean enableBearerAuth;
  private final String openIdIntrospectionHeaderName;
  private final boolean deriveIssuerFromHost;
  private final String issuerHostPath;
  private final ImmutableList<String> audList;
  private final String sessionCookieName;
  
  private record TimestampedToken(LocalDateTime expiry, String token){};
  
  private final MeterRegistry meterRegistry;
  
  private final Map<String, TimestampedToken> credentialsCache = new HashMap<>();


  /**
   * Constructor.
   *
   * Note that fundamentally the RequestContextBuilder does not require the request to have any authentication specified at all.
   * Conditions imposed using the RequestContext should ensure this.
   *
   * @param webClient The WebClient that will be used. The WebClient should be created specifically for use by the
   * RequestContextBuilder.
   * @param validator The JWT Validator that will be used for validating all tokens.
   * @param discoverer The Open ID Discovery handler that will be used for locating the auth URL for the host. This does not have
   * to be the same discoverer as used by the validator, but it will be more efficient if it is (shared cache).
   * @param loginDao DAO for accessing tokens from cookies.
   * @param meterRegistry MeterRegistry for production of metrics.
   * @param basicAuthConfig Configuration of the handling of Basic Auth credentials in requests.
   * @param enableBearerAuth When set to false no bearer authentication will be permitted.
   * @param openIdIntrospectionHeaderName The name of the header that will contain the payload from a token as Json (that may be
   * base64 encoded or not).
   * @param deriveIssuerFromHost If true the issuer should be derived from the Host (or X-Forwarded-Host) header.
   * @param issuerHostPath Path to be appended to the Host to derive the issuer. See
   * {@link uk.co.spudsoft.query.main.JwtValidationConfig#issuerHostPath}.
   * @param requiredAuds The audience that must be found in any token (any one of the provided audiences matching any aud in the
   * token is acceptable).
   * @param sessionCookie The name of the session cookie that should contain the ID of a previously recorded JWT. Only valid if
   * login is enabled.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The WebClient should be created specifically for use by the RequestContextBuilder.")
  public Authenticator(WebClient webClient,
           JwtValidator validator,
           OpenIdDiscoveryHandler discoverer,
           LoginDao loginDao,
           MeterRegistry meterRegistry,
           BasicAuthConfig basicAuthConfig,
           boolean enableBearerAuth,
           String openIdIntrospectionHeaderName,
           boolean deriveIssuerFromHost,
           String issuerHostPath,
           List<String> requiredAuds,
           String sessionCookie
  ) {
    this.webClient = webClient;
    this.validator = validator;
    this.discoverer = discoverer;
    this.loginDao = loginDao;
    this.meterRegistry = meterRegistry;
    this.basicAuthConfig = basicAuthConfig;
    this.enableBearerAuth = enableBearerAuth;
    this.openIdIntrospectionHeaderName = openIdIntrospectionHeaderName;
    this.deriveIssuerFromHost = deriveIssuerFromHost;
    this.issuerHostPath = ensureNonBlankStartsWith(issuerHostPath, "/");
    this.audList = ImmutableCollectionTools.copy(requiredAuds);
    this.sessionCookieName = sessionCookie;
    
    if (meterRegistry != null) {
      meterRegistry.gauge("queryengine.cache.size"
              , Arrays.asList(
                      Tag.of("cachename", "credentials")
              )
              , credentialsCache, cache -> {
        synchronized (cache) {
          return cache.size();
        }
      });
    }
  }

  static String ensureNonBlankStartsWith(String path, String start) {
    return Strings.isNullOrEmpty(path) ? "" : path.startsWith(start) ? path : (start + path);
  }

  static String baseRequestUrl(HttpServerRequest request) {
    StringBuilder sb = new StringBuilder();
    
    MultiMap headers = request.headers();
    
    String proto = headers == null ? null : headers.get("X-Forwarded-Proto");
    if (Strings.isNullOrEmpty(proto) || (!"https".equals(proto) && !"http".equals(proto))) {
      proto = request.scheme();
    }
    
    sb.append(proto);
    sb.append("://");
    HostAndPort hap = request.authority();
    if (hap != null) {
      sb.append(hap.host());
      int port = hap.port();
      if (port > 0 && !isStandardHttpsPort(proto, port) && !isStandardHttpPort(proto, port)) {
        sb.append(":");
        sb.append(port);
      }
    }

    return sb.toString();
  }

  /**
   * Return true if the scheme is http and the port is 80.
   *
   * @param scheme The HTTP request scheme.
   * @param port The port used in the connection.
   * @return true if the scheme is http and the port is 80.
   */
  public static boolean isStandardHttpPort(String scheme, int port) {
    return "http".equals(scheme) && port == 80;
  }

  /**
   * Return true if the scheme is https and the port is 443.
   *
   * @param scheme The HTTP request scheme.
   * @param port The port used in the connection.
   * @return true if the scheme is https and the port is 443.
   */
  public static boolean isStandardHttpsPort(String scheme, int port) {
    return "https".equals(scheme) && port == 443;
  }

  private String issuer(HttpServerRequest request) {
    if (deriveIssuerFromHost) {
      return baseRequestUrl(request) + issuerHostPath;
    } else {
      return null;
    }
  }

  private Cookie getSessionCookie(HttpServerRequest request) {
    if (!Strings.isNullOrEmpty(sessionCookieName)) {

      Set<Cookie> cookies = request.cookies(sessionCookieName);
      for (Cookie cookie : cookies) {
        return cookie;
      }
    }
    return null;
  }

  /**
   * Create a RequestContext from an HttpServerRequest.
   *
   * This builder may perform multiple asynchronous authentication requests.
   *
   * @param routingContext The Vert.x RoutingContext.
   * @param requestContext The request context.
   * @return A Future that will be completed with a newly constructed RequestContext when it is ready.
   */
  public Future<RequestContext> authenticate(RoutingContext routingContext, RequestContext requestContext) {

    HttpServerRequest request = routingContext.request();    

    String runId = request.params() == null ? null : request.params().get("_runid");
    if (runId != null) {
      ContextualData.put("runId", runId);
    }
    if (!Strings.isNullOrEmpty(runId)) {
      logger.info("RunID: {}", runId);
    }
    
    Cookie sessionCookie = getSessionCookie(request);
    if (sessionCookie != null) {

      request.pause();
      
      return loginDao.getToken(sessionCookie.getValue())
              .compose(token -> {
                if (token == null) {
                  logger.info("No valid token for cookie {}", sessionCookie.getValue());
                  return Future.failedFuture(new IllegalArgumentException("No valid token for cookie \"" + sessionCookie.getValue() + "\""));
                } else {
                  return validateToken(request, token);
                }
              })
              .compose(jwt -> {
                request.resume();
                requestContext.setJwt(jwt);
                return Future.succeededFuture(requestContext);
              }, ex -> {
                request.resume();
                logger.info("Failed to validate token from cookie {}: {}", sessionCookie.getValue(), ex.getMessage());
                return buildRequestContextWithoutCookie(routingContext, requestContext);
              });
    } else {
      return buildRequestContextWithoutCookie(routingContext, requestContext);
    }
  }

  private String getCachedToken(String credentials) {
    int colonPos = credentials.indexOf(":");
    String username = colonPos > 0 ? credentials.substring(0, colonPos) : credentials;            
    synchronized (credentialsCache) {
      TimestampedToken timestampedToken = credentialsCache.get(credentials);
      if (timestampedToken == null) {
        logger.debug("No token for {} in cache", username);
        return null;
      } else {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (timestampedToken.expiry.isAfter(now)) {
          logger.debug("Found token for {} in cache", username);
          return timestampedToken.token;
        } else {
          // Remove all expired tokens now
          credentialsCache.entrySet().removeIf(entry -> entry.getValue().expiry.isBefore(now));
          logger.debug("Found token for {} in cache but it expired at {}, there are now {} entries in the cache"
                  , username, timestampedToken.token, credentialsCache.size());
          return null;
        }
      }
    }
  }
  
  private void cacheToken(String credentials, LocalDateTime expiration, String token) {
    if (expiration != null) {
      LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
      // Only cache tokens that are going to last for at least another minute
      if (expiration.isAfter(now.plusMinutes(1))) {
        synchronized (token) {
          credentialsCache.put(credentials, new TimestampedToken(expiration, token));
        }
      }
    }
  }
  
  /**
   * Validate a JWT using the configured rules.
   *
   * @param request The HTTP request, which may be used to derive the issuer (if so configured).
   * @param token The JWT.
   * @return A token that will be completed when the token has been validated, though possibly with an Exception if the validation
   * fails.
   */
  public Future<Jwt> validateToken(HttpServerRequest request, String token) {
    return validator.validateToken(issuer(request), token, audList, false);
  }

  private Future<RequestContext> buildRequestContextWithoutCookie(RoutingContext routingContext, RequestContext requestContext) {
    
    HttpServerRequest request = routingContext.request();
    
    String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

    String openIdIntrospectionHeader = Strings.isNullOrEmpty(openIdIntrospectionHeaderName) ? null : request.getHeader(openIdIntrospectionHeaderName);

    if (!Strings.isNullOrEmpty(openIdIntrospectionHeader)) {

      return buildFromBase64Json(requestContext, openIdIntrospectionHeader);

    } else if (Strings.isNullOrEmpty(authHeader)) {

      return Future.succeededFuture(requestContext);

    } else if (authHeader.startsWith(BASIC)) {

      request.pause();

      if (basicAuthConfig == null || (basicAuthConfig.getGrantType() == null)) {
        logger.warn("Request includes basic authentication credentials but system is not configured to handle them.");
        return Future.failedFuture(new ServiceException(401, "Forbidden"));
      }

      String encodedCredentials = authHeader.substring(BASIC.length());
      String decodedCredentials;
      try {
        decodedCredentials = new String(Base64.getDecoder().decode(encodedCredentials), StandardCharsets.UTF_8);
      } catch (Throwable ex) {
        logger.warn("Failed to decode credentials using base64, will try again with base64url: ", ex);
        decodedCredentials = new String(Base64.getUrlDecoder().decode(encodedCredentials), StandardCharsets.UTF_8);
      }
      
      int colon = decodedCredentials.indexOf(":");
      String username = decodedCredentials.substring(0, colon);
      String password = decodedCredentials.substring(colon + 1);

      Future<String> tokenFuture = null;

      String cachedToken = getCachedToken(decodedCredentials);
      if (cachedToken != null) {
        tokenFuture = Future.succeededFuture(cachedToken);
      } else {
        int at = username.indexOf("@");
        String domain = null;
        String usernameWithoutDomain = username;
        if (at > 0) {
          domain = username.substring(at + 1);
          usernameWithoutDomain = username.substring(0, at);
        }
        if (basicAuthConfig.getIdpMap() == null || basicAuthConfig.getIdpMap().isEmpty()) {
          if (basicAuthConfig.getDefaultIdp() == null && Strings.isNullOrEmpty(basicAuthConfig.getAuthorizationPath())) {
            tokenFuture = discoverer.performOpenIdDiscovery(baseRequestUrl(request))
                    .compose(dd -> {
                      logger.debug("Discovery data: {}", dd);
                      if (basicAuthConfig.getGrantType() == BasicAuthGrantType.clientCredentials) {
                        return performClientCredentialsGrant(dd.getTokenEndpoint(), username, password);
                      } else {
                        Endpoint endpoint = new Endpoint();
                        endpoint.setUrl(dd.getAuthorizationEndpoint());
                        endpoint.setCredentials(basicAuthConfig.getDiscoveryEndpointCredentials());
                        return performResourceOwnerPasswordCredentials(endpoint, username, password);
                      }
                    });
          }
        }
        if (tokenFuture == null) {
          Endpoint authEndpoint;
          try {
            authEndpoint = findAuthEndpoint(basicAuthConfig, domain, baseRequestUrl(request), basicAuthConfig.getGrantType() == BasicAuthGrantType.resourceOwnerPasswordCredentials);
          } catch (Throwable ex) {
            return Future.failedFuture(ex);
          }
          if (basicAuthConfig.getGrantType() == BasicAuthGrantType.clientCredentials) {
            tokenFuture = performClientCredentialsGrant(authEndpoint.getUrl(), usernameWithoutDomain, password);
          } else {
            tokenFuture = performResourceOwnerPasswordCredentials(authEndpoint, usernameWithoutDomain, password);
          }
        }
      }

      return tokenFuture
              .compose(token -> {
                if (token == null) {
                  logger.info("Login for {} failed", username);
                  return Future.failedFuture(new ServiceException(401, "Invalid credentials"));
                } else {
                  logger.debug("Login for {} got token: {}", username, token);
                  return validateToken(request, token)
                          .onComplete(ar -> {
                            if (ar.succeeded()) {
                              cacheToken(encodedCredentials, ar.result().getExpirationLocalDateTime(), token);
                              requestContext.setJwt(ar.result());
                            }
                            request.resume();
                          });
                }
              })
              .onFailure(ex -> {
                request.resume();
              })
              .map(jwt -> requestContext);

    } else if (authHeader.startsWith(BEARER)) {

      if (!enableBearerAuth) {
        logger.warn("Request includes bearer authentication but system is not configured to handle them.");
        return Future.failedFuture(new ServiceException(401, "Forbiedden"));
      }

      
      String token = authHeader.substring(BEARER.length());
      request.pause();
      return validator.validateToken(issuer(request), token, audList, false)
              .onFailure(ex -> {
                request.resume();
              })
              .compose(jwt -> {
                logger.trace("Storing JWT in RequestContext@{}", System.identityHashCode(requestContext));
                requestContext.setJwt(jwt);
                request.resume();
                return Future.succeededFuture(requestContext);
              });

    } else {

      logger.warn("Unable to process auth header: {}", authHeader);
      return Future.succeededFuture(requestContext);

    }
  }

  static Future<RequestContext> buildFromBase64Json(RequestContext requestContext, String base64Json) {
    try {
      String json = base64Json;
      try {
        // If the header is base64 encoded then decode it, otherwise leave it as is.
        json = new String(Base64.getDecoder().decode(base64Json.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
      } catch (Throwable ex) {
      }
      Jwt jwt = new Jwt(null, new JsonObject(json), null, null);
      requestContext.setJwt(jwt);
      return Future.succeededFuture(requestContext);
    } catch (Throwable ex) {
      logger.warn("Failed to create RequestContext: ", ex);
      return Future.failedFuture(ex);
    }
  }

  /**
   * Determines the authentication endpoint URL based on the provided domain. It retrieves the URL from a configured mapping of
   * domains to IdP URLs or defaults to a globally configured default IdP URL if no specific mapping exists for the domain. Throws
   * an exception if no valid IdP configuration is found.
   *
   * @param basicAuthConfig The configuration data for basic auth.
   * @param domain The domain used to look up a mapped IdP URL. If null or empty, the default IdP is used.
   * @param baseUrl The URL used to make this request, with no path or anything thereafter.
   * @return The authentication endpoint URL as a String.
   * @throws IllegalStateException If no valid IdP configuration can be determined.
   */
  static Endpoint findAuthEndpoint(BasicAuthConfig basicAuthConfig, String domain, String baseUrl, boolean requireCredentials) throws IllegalStateException {
    Endpoint authEndpoint;
    if (!Strings.isNullOrEmpty(basicAuthConfig.getAuthorizationPath())) {
      StringBuilder authUrlBuilder = new StringBuilder();
      authUrlBuilder.append(baseUrl);
      if (baseUrl.endsWith("/")) {
        if (basicAuthConfig.getAuthorizationPath().startsWith("/")) {
          authUrlBuilder.append(basicAuthConfig.getAuthorizationPath().substring(1));
        } else {
          authUrlBuilder.append(basicAuthConfig.getAuthorizationPath());
        }
      } else {
        if (basicAuthConfig.getAuthorizationPath().startsWith("/")) {
          authUrlBuilder.append(basicAuthConfig.getAuthorizationPath());
        } else {
          authUrlBuilder.append("/").append(basicAuthConfig.getAuthorizationPath());
        }
      }
      authEndpoint = new Endpoint(authUrlBuilder.toString(), basicAuthConfig.getDiscoveryEndpointCredentials());
      return authEndpoint;
    }
    if (basicAuthConfig.getIdpMap() == null || basicAuthConfig.getIdpMap().isEmpty()) {
      authEndpoint = basicAuthConfig.getDefaultIdp();
      if (authEndpoint == null || Strings.isNullOrEmpty(authEndpoint.getUrl())) {
        throw new IllegalStateException("No default IdP configured");
      }
    } else {
      if (Strings.isNullOrEmpty(domain)) {
        authEndpoint = basicAuthConfig.getDefaultIdp();
        if (authEndpoint == null || Strings.isNullOrEmpty(authEndpoint.getUrl())) {
          throw new IllegalStateException("No default IdP configured for no domain");
        }
      } else {
        authEndpoint = basicAuthConfig.getIdpMap().get(domain);
        if (authEndpoint == null || Strings.isNullOrEmpty(authEndpoint.getUrl())) {
          authEndpoint = basicAuthConfig.getDefaultIdp();
          if (authEndpoint == null || Strings.isNullOrEmpty(authEndpoint.getUrl())) {
            throw new IllegalStateException("No default IdP configured and no mapped IdP configured");
          }
        }
      }
    }
    if (requireCredentials) {
      if ((authEndpoint.getCredentials() == null) || (Strings.isNullOrEmpty(authEndpoint.getCredentials().getUsername()))) {
        logger.debug("No credentials found for {}", authEndpoint.getUrl());
        throw new IllegalStateException("No credentials configured for IdP");
      }
    }
    return authEndpoint;
  }

  private Future<String> performClientCredentialsGrant(String tokenEndpoint, String clientId, String clientSecret) {
    logger.debug("Performing client_credentials request to {}", tokenEndpoint);
    MultiMap form = HeadersMultiMap.httpHeaders();
    form.add("grant_type", "client_credentials");
    form.add("client_id", clientId);
    form.add("client_secret", clientSecret);
    return webClient.postAbs(tokenEndpoint)
            .sendForm(form)
            .compose(response -> {
              Buffer body = response.body();
              try {
                logger.trace("CCG request to {} returned: {} {}", tokenEndpoint, response.statusCode(), body);
                JsonObject bodyJson = body.toJsonObject();
                String token = bodyJson.getString("access_token");
                logger.debug("Client {}@{} got token {}", clientId, tokenEndpoint, token);
                return Future.succeededFuture(token);
              } catch (Throwable ex) {
                logger.warn("CCG request to {} returned: {} {}", tokenEndpoint, response.statusCode(), body);
                logger.warn("Failed to process client credentials grant for {}@{}: ", clientId, tokenEndpoint, ex);
                return Future.failedFuture(ex);
              }
            });
  }

  private Future<String> performResourceOwnerPasswordCredentials(Endpoint authEndpoint, String username, String password) {
    logger.debug("Performing password request to {}", authEndpoint);
    MultiMap form = HeadersMultiMap.httpHeaders();
    form.add("grant_type", "password");
    form.add("username", username);
    form.add("password", password);
    HttpRequest<Buffer> request = webClient.postAbs(authEndpoint.getUrl());
    if (authEndpoint.getCredentials() != null) {
      request.basicAuthentication(authEndpoint.getCredentials().getUsername(), authEndpoint.getCredentials().getPassword());
    }
    return request
            .sendForm(form)
            .compose(response -> {
              Buffer body = response.body();
              try {
                logger.trace("ROPC request to {} returned: {} {}", authEndpoint, response.statusCode(), body);
                JsonObject bodyJson = body.toJsonObject();
                String token = bodyJson.getString("access_token");
                logger.debug("User {}@{} got token {}", username, authEndpoint, token);
                return Future.succeededFuture(token);
              } catch (Throwable ex) {
                logger.warn("ROPC request to {} returned: {} {}", authEndpoint, response.statusCode(), body);
                logger.warn("Failed to process client credentials grant for {}@{}: ", username, authEndpoint, ex);
                return Future.failedFuture(ex);
              }
            });
  }

}
