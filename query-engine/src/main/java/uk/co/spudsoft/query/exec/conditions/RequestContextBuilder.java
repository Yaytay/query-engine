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
package uk.co.spudsoft.query.exec.conditions;

import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.HostAndPort;
import io.vertx.ext.web.client.WebClient;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.jwtvalidatorvertx.Jwt;
import uk.co.spudsoft.jwtvalidatorvertx.JwtValidator;
import uk.co.spudsoft.jwtvalidatorvertx.OpenIdDiscoveryHandler;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;
import uk.co.spudsoft.query.web.LoginDao;


/**
 * Factory object for constructing filled-in {@link RequestContext} objects.
 * 
 * @author jtalbut
 */
public class RequestContextBuilder {
  
  private static final Logger logger = LoggerFactory.getLogger(RequestContextBuilder.class);
  
  private static final String BASIC = "Basic ";
  private static final String BEARER = "Bearer ";
  
  private final WebClient webClient;
  private final JwtValidator validator;
  private final OpenIdDiscoveryHandler discoverer;
  private final LoginDao loginDao;
  private final boolean enableBasicAuth;
  private final boolean enableBearerAuth;
  private final String openIdIntrospectionHeaderName;
  private final boolean deriveIssuerFromHost;
  private final String issuerHostPath;
  private final List<String> audList;
  private final String sessionCookieName;

  /**
   * Constructor.
   * 
   * Note that fundamentally the RequestContextBuilder does not require the request to have any authentication specified at all.
   * Conditions imposed using the RequestContext should ensure this.
   * 
   * @param webClient The WebClient that will be used.
   * The WebClient should be created specifically for use by the RequestContextBuilder.
   * @param validator The JWT Validator that will be used for validating all tokens.
   * @param discoverer The Open ID Discovery handler that will be used for locating the auth URL for the host.
   * This does not have to be the same discoverer as used by the validator, but it will be more efficient if it is (shared cache).
   * @param loginDao DAO for accessing tokens from cookies.
   * @param enableBasicAuth When set to false no basic authentication (client credentials grant) will be attempted.
   * @param enableBearerAuth When set to false no bearer authentication will be permitted.
   * @param openIdIntrospectionHeaderName The name of the header that will contain the payload from a token as Json (that may be base64 encoded or not).
   * @param deriveIssuerFromHost If true the issuer should be derived from the Host (or X-Forwarded-Host) header.
   * @param issuerHostPath  Path to be appended to the Host to derive the issuer.  See {@link uk.co.spudsoft.query.main.JwtValidationConfig#issuerHostPath}.
   * @param requiredAuds The audience that must be found in any token (any one of the provided audiences matching any aud in the token is acceptable).
   * @param sessionCookie The name of the session cookie that should contain the ID of a previously recorded JWT.  Only valid if login is enabled.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The WebClient should be created specifically for use by the RequestContextBuilder.")
  public RequestContextBuilder(WebClient webClient
          , JwtValidator validator
          , OpenIdDiscoveryHandler discoverer
          , LoginDao loginDao
          , boolean enableBasicAuth
          , boolean enableBearerAuth
          , String openIdIntrospectionHeaderName
          , boolean deriveIssuerFromHost
          , String issuerHostPath
          , List<String> requiredAuds
          , String sessionCookie
  ) {
    this.webClient = webClient;
    this.validator = validator;
    this.discoverer = discoverer;
    this.loginDao = loginDao;
    this.enableBasicAuth = enableBasicAuth;
    this.enableBearerAuth = enableBearerAuth;
    this.openIdIntrospectionHeaderName = openIdIntrospectionHeaderName;
    this.deriveIssuerFromHost = deriveIssuerFromHost;
    this.issuerHostPath = Strings.isNullOrEmpty(issuerHostPath) ? "" : issuerHostPath.startsWith("/") ? issuerHostPath : ("/" + issuerHostPath);
    this.audList = ImmutableCollectionTools.copy(requiredAuds);
    this.sessionCookieName = sessionCookie;
  }
  
  static String baseRequestUrl(HttpServerRequest request) {
    StringBuilder sb = new StringBuilder();
    String scheme = request.scheme();
    sb.append(scheme);
    sb.append("://");
    HostAndPort hap = request.authority();
    if (hap != null) {
      sb.append(hap.host());
      int port = hap.port();
      if (!isStandardHttpsPort(scheme, port) && !isStandardHttpPort(scheme, port)) {
        sb.append(":");
        sb.append(port);
      }
    }

    return sb.toString();
  }
  
  /**
   * Return true if the scheme is http and the port is 80.
   * @param scheme The HTTP request scheme.
   * @param port The port used in  the connection.
   * @return true if the scheme is http and the port is 80.
   */
  public static boolean isStandardHttpPort(String scheme, int port) {
    return "http".equals(scheme) && port == 80;
  }

  /**
   * Return true if the scheme is https and the port is 443.
   * @param scheme The HTTP request scheme.
   * @param port The port used in  the connection.
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
   * @param request The HttpServerRequest to process.
   * @return A Future that will be completed with a newly constructed RequestContext when it is ready.
   */
  public Future<RequestContext> buildRequestContext(HttpServerRequest request) {

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
                return build(request, jwt);
              }, ex -> {
                request.resume();
                logger.info("Failed to validate token from cookie {}: {}", sessionCookie.getValue(), ex.getMessage());
                return buildRequestContextWithoutCookie(request);
              });      
    } else {
      return buildRequestContextWithoutCookie(request);
    }
  }

  /**
   * Validate a JWT using the configured rules.
   * @param request The HTTP request, which may be used to derive the issuer (if so configured).
   * @param token The JWT.
   * @return A token that will be completed when the token has been validated, though possibly with an Exception if the validation fails.
   */
  public Future<Jwt> validateToken(HttpServerRequest request, String token) {
    return validator.validateToken(issuer(request), token, audList, false);
  }
    
  private Future<RequestContext> buildRequestContextWithoutCookie(HttpServerRequest request) {
    String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
   
    String openIdIntrospectionHeader = Strings.isNullOrEmpty(openIdIntrospectionHeaderName) ? null : request.getHeader(openIdIntrospectionHeaderName);
    
    if (!Strings.isNullOrEmpty(openIdIntrospectionHeader)) {
      
      return buildFromBase64Json(request, openIdIntrospectionHeader);
      
    } else if (Strings.isNullOrEmpty(authHeader)) {
      
      return build(request, null);
      
    } else if (authHeader.startsWith(BASIC)) {
      
      if (!enableBasicAuth) {
        return build(request, null);
      }
      
      String credentials = authHeader.substring(BASIC.length());
      credentials = new String(Base64.getUrlDecoder().decode(credentials), StandardCharsets.UTF_8);
      int colon = credentials.indexOf(":");
      String clientId = credentials.substring(0, colon);
      String clientSecret = credentials.substring(colon + 1);      
      request.pause();
      return performClientCredentialsGrant(baseRequestUrl(request), clientId, clientSecret)
              .compose(token -> {
                logger.debug("Login for {} got token: {}", clientId, token);
                request.resume();
                return validateToken(request, token);
              })
              .compose(jwt -> {
                return build(request, jwt);
              });
      
    } else if (authHeader.startsWith(BEARER)) {
      
      if (!enableBearerAuth) {
        return build(request, null);
      }
      
      String token = authHeader.substring(BEARER.length());
      request.pause();
      return validator.validateToken(issuer(request), token, audList, false)
              .compose(jwt -> {
                request.resume();
                return build(request, jwt);
              });
      
    } else {
      
      logger.warn("Unable to process auth header: {}", authHeader);
      return build(request, null);
      
    }
  }
  
  static Future<RequestContext> buildFromBase64Json(HttpServerRequest request, String base64Json) {
    try {
      String json = base64Json;
      try {
        // If the header is base64 encoded then decode it, otherwise leave it as is.
        json = new String(Base64.getDecoder().decode(base64Json.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
      } catch (Throwable ex) {        
      }
      Jwt jwt = new Jwt(null, new JsonObject(json), null, null);      
      RequestContext result = new RequestContext(request, jwt);
      return Future.succeededFuture(result);
    } catch (Throwable ex) {
      logger.warn("Failed to create RequestContext: ", ex);
      return Future.failedFuture(ex);
    }
  }
  
  static Future<RequestContext> build(HttpServerRequest request, Jwt jwt) {
    try {
      RequestContext result = new RequestContext(request, jwt);
      return Future.succeededFuture(result);
    } catch (Throwable ex) {
      logger.warn("Failed to create RequestContext: ", ex);
      return Future.failedFuture(ex);
    }
  }
  
  private Future<String> performClientCredentialsGrant(String issuer, String clientId, String clientSecret) {
    return discoverer.performOpenIdDiscovery(issuer)
            .compose(dd -> {
              String authEndpoint = dd.getAuthorizationEndpoint();
              
              MultiMap body = new HeadersMultiMap();
              body.add("grant_type", "client_credentials");
              body.add("client_id", clientId);
              body.add("client_secret", clientSecret);              
              return webClient.postAbs(authEndpoint)
                      .sendForm(body)
                      ;
            })
            .compose(response -> {
              try {
                JsonObject body = response.bodyAsJsonObject();
                String token = body.getString("access_token");
                logger.debug("Client {}@{} got token {}", clientId, issuer, token);
                return Future.succeededFuture(token);
              } catch (Throwable ex) {
                logger.warn("Failed to process client credentials grant for {}@{}: ", clientId, issuer, ex);
                return Future.failedFuture(ex);
              }
            });    
  }
  
}
