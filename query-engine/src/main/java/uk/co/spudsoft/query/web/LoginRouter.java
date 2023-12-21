/*
 * Copyright (C) 2023 jtalbut
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

import com.google.common.base.Strings;
import com.google.common.hash.Hashing;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.VertxContextPRNG;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.jwtvalidatorvertx.JwtValidator;
import uk.co.spudsoft.jwtvalidatorvertx.OpenIdDiscoveryHandler;
import uk.co.spudsoft.query.exec.conditions.RequestContextBuilder;
import uk.co.spudsoft.query.main.AuthEndpoint;
import uk.co.spudsoft.query.main.SessionConfig;
import uk.co.spudsoft.query.web.LoginDao.RequestData;

/**
 *
 * @author njt
 */
public class LoginRouter  implements Handler<RoutingContext> {
  
  private static final Logger logger = LoggerFactory.getLogger(LoginRouter.class);

  private static final Base64.Encoder RAW_BASE64_URLENCODER = Base64.getUrlEncoder().withoutPadding();
  
  private final Vertx vertx;
  private final WebClient webClient;
  private final LoginDao loginDao;
  private final OpenIdDiscoveryHandler openIdDiscoveryHandler;
  private final JwtValidator jwtValidator;
  private final int stateLength;
  private final int codeVerifierLength;
  private final int nonceLength;
  private final Map<String, AuthEndpoint>  authEndpoints;
  private final boolean outputAllErrorMessages;

  public static LoginRouter create(Vertx vertx
          , LoginDao loginDao
          , OpenIdDiscoveryHandler openIdDiscoveryHandler
          , JwtValidator jwtValidator
          , SessionConfig sessionConfig
          , boolean outputAllErrorMessages
  ) {
    return new LoginRouter(vertx, loginDao, openIdDiscoveryHandler, jwtValidator, sessionConfig, outputAllErrorMessages);
  }
  
  private static class RequestDataAndAuthEndpoint {
    private AuthEndpoint authEndpoint;
    private RequestData requestData;
  }

  public LoginRouter(Vertx vertx
          , LoginDao loginDao
          , OpenIdDiscoveryHandler openIdDiscoveryHandler
          , JwtValidator jwtValidator
          , SessionConfig sessionConfig
          , boolean outputAllErrorMessages
  ) {
    this.vertx = vertx;
    this.webClient = WebClient.create(vertx);
    this.loginDao = loginDao;
    this.openIdDiscoveryHandler = openIdDiscoveryHandler;
    this.jwtValidator = jwtValidator;
    this.stateLength = sessionConfig.getStateLength();
    this.codeVerifierLength = sessionConfig.getCodeVerifierLength();
    this.nonceLength = sessionConfig.getNonceLength();
    this.authEndpoints = new HashMap<>();
    // Deep copy of auth endpoints because they may be changed
    if (sessionConfig.getOauth() != null) {
      sessionConfig.getOauth().entrySet().stream().forEach(e -> {
        this.authEndpoints.put(e.getKey(), new AuthEndpoint(e.getValue()));
      });    
    }
    this.outputAllErrorMessages = outputAllErrorMessages;
  }

  /**
   * Return the base URL used to make this request with a suffix of /login/return.
   * 
   * The following headers are used, if present:
   * <UL>
   * <LI>X-Forwarded-Proto
   * <LI>X-Forwarded-Port
   * <LI>X-Forwarded-Host
   * </UL>
   * Any components of the URL not found in headers is taken from the request.
   * 
   * This method is not used when path hijacks are in operation (because login is not used when path hijacks are in operation).
   * 
   * @param request The request.
   * @return The URL that should be used by an OpenID redirect.
   */
  static String redirectUri(HttpServerRequest request) {
    String port = request.getHeader("X-Forwarded-Port");
    String scheme = request.getHeader("X-Forwarded-Proto");
    if (Strings.isNullOrEmpty(scheme)) {
      scheme = request.scheme();
      int portNum = request.authority().port();
      if (RequestContextBuilder.isStandardHttpsPort(scheme, portNum)) {
        port = "";
      } else if (RequestContextBuilder.isStandardHttpPort(scheme, portNum)) {
        port = "";
      } else {
        port = Integer.toString(portNum);
      }
    }
    if (Strings.isNullOrEmpty(port)) {
      port = "";
    } else {
      port = ":" + port;
    }
    String host = request.getHeader("X-Forwarded-Host");
    if (Strings.isNullOrEmpty(host)) {
      host = request.authority().host();
    }
    return scheme + "://" + host + port + "/login/return";
  }
  
  static String createCodeChallenge(String codeVerifier) {
    byte[] hash = Hashing.sha256().hashString(codeVerifier, StandardCharsets.US_ASCII).asBytes();
    return RAW_BASE64_URLENCODER.encodeToString(hash);
  }
  
  @Override
  public void handle(RoutingContext event) {
    if (event.request().path().endsWith("/login")) {
      handleLoginRequest(event);
    } else if (event.request().path().endsWith("/login/return")) {
      handleLoginResponse(event);
    } else {
      event.next();
    }
  }

  boolean handleLoginResponse(RoutingContext event) {
    String code = event.request().getParam("code");
    if (Strings.isNullOrEmpty(code)) {
      QueryRouter.internalError(new IllegalArgumentException("Code not specified"), event, outputAllErrorMessages);
      return true;
    }
    String state = event.request().getParam("state");
    if (Strings.isNullOrEmpty(state)) {
      QueryRouter.internalError(new IllegalArgumentException("State not specified"), event, outputAllErrorMessages);
      return true;
    }
    RequestDataAndAuthEndpoint requestDataAndAuthEndpoint = new RequestDataAndAuthEndpoint();
    logger.debug("State: {}", state);
    loginDao.getRequestData(state)
            .compose(requestData -> {
              requestDataAndAuthEndpoint.requestData = requestData;
              return loginDao.markUsed(state);
            }).compose(v -> {
              requestDataAndAuthEndpoint.authEndpoint = authEndpoints.get(requestDataAndAuthEndpoint.requestData.provider());
              if (null == requestDataAndAuthEndpoint.authEndpoint) {
                logger.warn("OAuth provider {} not found in {}", requestDataAndAuthEndpoint.requestData.provider(), authEndpoints.keySet());
                return Future.failedFuture(new IllegalArgumentException("OAuth provider not known"));
              }
              
              MultiMap body = MultiMap.caseInsensitiveMultiMap();
              body.add("client_id", requestDataAndAuthEndpoint.authEndpoint.getCredentials().getId());
              body.add("client_secret", requestDataAndAuthEndpoint.authEndpoint.getCredentials().getSecret());
              body.add("code", code);
              if (!Strings.isNullOrEmpty(requestDataAndAuthEndpoint.authEndpoint.getScope())) {
                body.add("scope", requestDataAndAuthEndpoint.authEndpoint.getScope());
              }
              body.add("redirect_uri", requestDataAndAuthEndpoint.requestData.redirectUri());
              body.add("grant_type", "authorization_code");
              if (requestDataAndAuthEndpoint.authEndpoint.isPkce()) {
                body.add("code_verifier", requestDataAndAuthEndpoint.requestData.codeVerifier());
              }
              
              logger.debug("Token endpoiont: {} and body: {}", requestDataAndAuthEndpoint.authEndpoint.getTokenEndpoint(), body);
              return webClient.requestAbs(HttpMethod.POST, requestDataAndAuthEndpoint.authEndpoint.getTokenEndpoint())
                      .putHeader("Accept", "application/json")
                      .sendForm(body)
                      ;
            })
            .compose(codeResponse -> {
              if (codeResponse.statusCode() != 200) {
                String responseBody = codeResponse.bodyAsString();
                logger.warn("Failed to get access token from {} ({}): {}"
                        , requestDataAndAuthEndpoint.authEndpoint.getTokenEndpoint(), codeResponse.statusCode(), responseBody);
                return Future.failedFuture(new IllegalStateException("Failed to get access token from provider"));
              } else {
                JsonObject body;
                try {
                  body = codeResponse.bodyAsJsonObject();
                } catch (Throwable ex) {
                  String stringBody = codeResponse.bodyAsString();
                  logger.warn("Failed to get access token ({}): {}", codeResponse.statusCode(), stringBody);
                  return Future.failedFuture(new IllegalStateException("Failed to get access token from provider"));
                }
                logger.debug("Access token response: {}", body);
                String accessToken = body.getString("access_token");
                if (Strings.isNullOrEmpty(accessToken)) {
                  String stringBody = codeResponse.bodyAsString();
                  logger.warn("Failed to get access token ({}): {}", codeResponse.statusCode(), stringBody);
                  return Future.failedFuture(new IllegalStateException("Failed to get access token from provider"));
                }
                
                String targetUrl = requestDataAndAuthEndpoint.requestData.targetUrl();
                targetUrl = targetUrl
                        + (targetUrl.contains("?") ? "&" : "?")
                        + "access_token=" + accessToken
                        ;
                
                event.response()
                        .putHeader("Location", targetUrl)
                        .setStatusCode(307)
                        .end();
              }
              return Future.succeededFuture();
            })
            .onFailure(ex -> {
              QueryRouter.internalError(ex, event, outputAllErrorMessages);
            })
            ;
    return false;
  }

  boolean handleLoginRequest(RoutingContext event) {
    String targetUrl = event.request().getParam("return");
    if (Strings.isNullOrEmpty(targetUrl)) {
      QueryRouter.internalError(new IllegalArgumentException("Target URL not specified"), event, outputAllErrorMessages);
      return true;
    }
    String provider = event.request().getParam("provider");
    if (Strings.isNullOrEmpty(provider)) {
      QueryRouter.internalError(new IllegalArgumentException("OAuth provider not specified"), event, outputAllErrorMessages);
      return true;
    }
    AuthEndpoint authEndpoint = authEndpoints.get(provider);
    if (null == authEndpoint) {
      logger.warn("OAuth provider {} not found in {}", provider, authEndpoints.keySet());
      QueryRouter.internalError(new IllegalArgumentException("OAuth provider not known"), event, outputAllErrorMessages);
      return true;
    }
    if (shouldDiscover(authEndpoint)) {
      openIdDiscoveryHandler.performOpenIdDiscovery(authEndpoint.getIssuer())
              .onSuccess(discoveryData -> {
                authEndpoint.updateFromOpenIdConfiguration(discoveryData);
                redirectToAuthEndpoint(event, authEndpoint, provider, targetUrl);
              })
              .onFailure(ex -> {
                logger.warn("Failed to OpenID configuration for issuer {}: ", authEndpoint.getIssuer(), ex);
                QueryRouter.internalError(ex, event, outputAllErrorMessages);
              })
              ;
    } else {
      redirectToAuthEndpoint(event, authEndpoint, provider, targetUrl);
    }
    return false;
  }

  static boolean shouldDiscover(AuthEndpoint authEndpoint) {
    return !Strings.isNullOrEmpty(authEndpoint.getIssuer())
            && (
               Strings.isNullOrEmpty(authEndpoint.getAuthorizationEndpoint())
            || Strings.isNullOrEmpty(authEndpoint.getTokenEndpoint())
            || authEndpoint.getInvalidDate() == null
            || authEndpoint.getInvalidDate().isBefore(LocalDateTime.now())
            );
  }

  private void redirectToAuthEndpoint(RoutingContext event, AuthEndpoint authEndpoint, String provider, String targetUrl) {
    if (Strings.isNullOrEmpty(authEndpoint.getAuthorizationEndpoint())) {
      logger.warn("Provider {} has no authorization endpoint", provider);
      QueryRouter.internalError(new IllegalStateException("Provider has no authorization endpoint"), event, outputAllErrorMessages);
      return ;
    }
  
    VertxContextPRNG prng = VertxContextPRNG.current(vertx);
    String state = prng.nextString(stateLength);
    String codeVerifier = null;
    String nonce = null;
    String redirectUri = redirectUri(event.request());
    
    StringBuilder url = new StringBuilder();
    url.append(authEndpoint.getAuthorizationEndpoint())
            .append("?response_type=code")
            .append("&client_id=").append(URLEncoder.encode(authEndpoint.getCredentials().getId(), StandardCharsets.UTF_8))
            .append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8))
            .append("&state=").append(state)
            ;
    if (!Strings.isNullOrEmpty(authEndpoint.getScope())) {
      url.append("&scope=").append(URLEncoder.encode(authEndpoint.getScope(), StandardCharsets.UTF_8));
    }
    if (authEndpoint.isPkce()) {
      codeVerifier = prng.nextString(codeVerifierLength);
      url.append("&code_challenge=").append(createCodeChallenge(codeVerifier));
      url.append("&code_challenge_method=S256");
    }
    if (authEndpoint.isNonce()) {
      nonce = prng.nextString(nonceLength);
      url.append("&nonce=").append(nonce);
    }
    
    loginDao.store(state, provider, codeVerifier, nonce, redirectUri, targetUrl)
            .onSuccess(v -> {
              event.response()
                      .putHeader("Location", url.toString())
                      .setStatusCode(307)
                      .end("");
            })
            .onFailure(ex -> {
              QueryRouter.internalError(ex, event, outputAllErrorMessages);
            });
  }
  
}
