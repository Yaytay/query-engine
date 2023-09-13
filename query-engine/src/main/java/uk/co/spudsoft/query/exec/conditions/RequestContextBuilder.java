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
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.HostAndPort;
import io.vertx.ext.web.client.WebClient;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.jwtvalidatorvertx.JWT;
import uk.co.spudsoft.jwtvalidatorvertx.OpenIdDiscoveryHandler;
import uk.co.spudsoft.jwtvalidatorvertx.JwtValidatorVertx;

/**
 *
 * @author jtalbut
 */
public class RequestContextBuilder {
  
  private static final Logger logger = LoggerFactory.getLogger(RequestContextBuilder.class);
  
  private static final String BASIC = "Basic ";
  private static final String BEARER = "Bearer ";
  
  private final WebClient webClient;
  private final JwtValidatorVertx validator;
  private final OpenIdDiscoveryHandler discoverer;
  private final String openIdIntrospectionHeaderName;
  private final List<String> audList;

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
   * @param openIdIntrospectionHeaderName The name of the header that will contain the payload from a token as Json (that may be base64 encoded or not).
   * @param aud The audience that must be found in any token.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The WebClient should be created specifically for use by the RequestContextBuilder.")
  public RequestContextBuilder(WebClient webClient, JwtValidatorVertx validator, OpenIdDiscoveryHandler discoverer, String openIdIntrospectionHeaderName, String aud) {
    this.webClient = webClient;
    this.validator = validator;
    this.discoverer = discoverer;
    this.openIdIntrospectionHeaderName = openIdIntrospectionHeaderName;
    this.audList = Collections.singletonList(aud);
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
      if (!("https".equals(scheme) && port == 443) && !("http".equals(scheme) && port == 80)) {
        sb.append(":");
        sb.append(port);
      }
    }

    return sb.toString();
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

    String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
   
    String openIdIntrospectionHeader = Strings.isNullOrEmpty(openIdIntrospectionHeaderName) ? null : request.getHeader(openIdIntrospectionHeaderName);
    
    if (!Strings.isNullOrEmpty(openIdIntrospectionHeader)) {
      
      return buildFromBase64Json(request, openIdIntrospectionHeader);
      
    } else if (Strings.isNullOrEmpty(authHeader)) {
      
      return build(request, null);
      
    } else if (authHeader.startsWith(BASIC)) {
      
      String credentials = authHeader.substring(BASIC.length());
      credentials = new String(Base64.getUrlDecoder().decode(credentials), StandardCharsets.UTF_8);
      int colon = credentials.indexOf(":");
      String clientId = credentials.substring(0, colon);
      String clientSecret = credentials.substring(colon + 1);      
      return performClientCredentialsGrant(baseRequestUrl(request), clientId, clientSecret)
              .compose(token -> validator.validateToken(token, audList, true))
              .compose(jwt -> build(request, jwt));
      
    } else if (authHeader.startsWith(BEARER)) {
      
      String token = authHeader.substring(BEARER.length());
      return validator.validateToken(token, audList, true)
              .compose(jwt -> build(request, jwt));
      
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
      JWT jwt = new JWT(null, new JsonObject(json), null, null);      
      RequestContext result = new RequestContext(request, jwt);
      return Future.succeededFuture(result);
    } catch (Throwable ex) {
      logger.warn("Failed to create RequestContext: ", ex);
      return Future.failedFuture(ex);
    }
  }
  
  static Future<RequestContext> build(HttpServerRequest request, JWT jwt) {
    try {
      RequestContext result = new RequestContext(request, jwt);
      return Future.succeededFuture(result);
    } catch (Throwable ex) {
      logger.warn("Failed to create RequestContext: ", ex);
      return Future.failedFuture(ex);
    }
  }
  
  Future<String> performClientCredentialsGrant(String issuer, String clientId, String clientSecret) {
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
