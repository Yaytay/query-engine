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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.jwtvalidatorvertx.JwtValidator;
import uk.co.spudsoft.jwtvalidatorvertx.OpenIdDiscoveryHandler;
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.logging.Log;
import uk.co.spudsoft.query.main.Authenticator;
import uk.co.spudsoft.query.main.AuthEndpoint;
import uk.co.spudsoft.query.main.Coalesce;
import uk.co.spudsoft.query.main.CookieConfig;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;
import uk.co.spudsoft.query.main.SessionConfig;
import uk.co.spudsoft.query.web.LoginDao.RequestData;

/**
 * Vert.x {@link io.vertx.core.Handler}&lt;{@link io.vertx.ext.web.RoutingContext}&gt; for handling login requests.
 *
 * @author jtalbut
 */
public class LoginRouter implements Handler<RoutingContext> {

  private static final Logger logger = LoggerFactory.getLogger(LoginRouter.class);

  private static final Base64.Encoder RAW_BASE64_URLENCODER = Base64.getUrlEncoder().withoutPadding();

  private static final SecureRandom RANDOM = getFastestSecureRandom();
  
  /**
   * Choose the best available SecureRandom implementation.
   * @return the best available SecureRandom implementation. 
   */
  public static SecureRandom getFastestSecureRandom() {
    String[] algorithms = {
      "NativePRNGNonBlocking", // Fastest, non-blocking
      "NativePRNG", // Fast, may block occasionally  
      "DRBG" // Java 9+ fallback
    };

    for (String algorithm : algorithms) {
      try {
        SecureRandom sr = SecureRandom.getInstance(algorithm);
        sr.nextBytes(new byte[1]); // Test it works
        return sr;
      } catch (NoSuchAlgorithmException e) {
        // Try next algorithm
      }
    }

    return new SecureRandom(); // Fallback to default
  }

  private final Vertx vertx;
  private final WebClient webClient;
  private final LoginDao loginDao;
  private final OpenIdDiscoveryHandler openIdDiscoveryHandler;
  private final JwtValidator jwtValidator;
  private final Authenticator requestContextBuilder;
  private final int stateLength;
  private final int codeVerifierLength;
  private final int nonceLength;
  private final ImmutableMap<String, AuthEndpoint> authEndpoints;
  private final boolean outputAllErrorMessages;
  private final boolean enableForceJwt;
  private final ImmutableList<String> requiredAuds;
  private final CookieConfig sessionCookie;
  /**
   * Factory method.
   *
   * @param vertx The Vert.x instance.
   * @param loginDao Datastore for login data.
   * @param openIdDiscoveryHandler Handler from <a href="https://github.com/Yaytay/jwt-validator-vertx">jwt-validator-vertx</a>
   * for performing OpenID Connect Discovery.
   * @param jwtValidator Handler from <a href="https://github.com/Yaytay/jwt-validator-vertx">jwt-validator-vertx</a> for
   * validating JWTs.
   * @param requestContextBuilder Creator of {@link uk.co.spudsoft.query.exec.context.RequestContext} objects, used here for
   * some utility functions.
   * @param sessionConfig Configuration data.
   * @param requiredAuds A valid JWT must contain at least one of these audience values.
   * @param outputAllErrorMessages In a production environment error messages should usually not leak information that may assist
   * a bad actor, set this to true to return full details in error responses.
   * <p>
   * Note that everything is always logged.
   * @param enableForceJwt If true, the path /login/forcejwt can be PUT to create a session based on the the JWT in the message body.
   * This should be secure even in a production environment, but for the sake of safety it defaults to being disabled.
   * @param sessionCookie Configuration data for the session cookie.
   * @return a newly created LoginRouter.
   */
  public static LoginRouter create(Vertx vertx,
           LoginDao loginDao,
           OpenIdDiscoveryHandler openIdDiscoveryHandler,
           JwtValidator jwtValidator,
           Authenticator requestContextBuilder,
           SessionConfig sessionConfig,
           List<String> requiredAuds,
           boolean outputAllErrorMessages,
           boolean enableForceJwt,
           CookieConfig sessionCookie
  ) {
    return new LoginRouter(vertx, loginDao, openIdDiscoveryHandler, jwtValidator, requestContextBuilder, sessionConfig, requiredAuds, outputAllErrorMessages, enableForceJwt, sessionCookie);
  }

  private static class RequestDataAndAuthEndpoint {

    private AuthEndpoint authEndpoint;
    private RequestData requestData;
  }

  static final String randomString(int length) {
    if (length <= 0) {
      throw new IllegalArgumentException("Length must be positive");
    }

    // Calculate how many bytes we need
    // Base64 encoding produces 4 characters for every 3 bytes
    // So we need ceil(length * 3/4) bytes to get at least 'length' characters
    int bytesNeeded = (length * 3 + 3) / 4;

    byte[] randomBytes = new byte[bytesNeeded];
    RANDOM.nextBytes(randomBytes);

    String encoded = RAW_BASE64_URLENCODER.encodeToString(randomBytes);

    // Trim to exact length if needed
    return encoded.length() > length ? encoded.substring(0, length) : encoded;
  }

  static final String createRandomSessionId() {
    return randomString(100);
  }

  /**
   * Constructor.
   *
   * @param vertx The Vert.x instance.
   * @param loginDao Datastore for login data.
   * @param openIdDiscoveryHandler Handler from <a href="https://github.com/Yaytay/jwt-validator-vertx">jwt-validator-vertx</a>
   * for performing OpenID Connect Discovery.
   * @param jwtValidator Handler from <a href="https://github.com/Yaytay/jwt-validator-vertx">jwt-validator-vertx</a> for
   * validating JWTs.
   * @param requestContextBuilder Creator of {@link uk.co.spudsoft.query.exec.context.RequestContext} objects, used here for
   * some utility functions.
   * @param sessionConfig Configuration data.
   * @param requiredAuds A valid JWT must contain at least one of these audience values.
   * @param outputAllErrorMessages In a production environment error messages should usually not leak information that may assist
   * a bad actor, set this to true to return full details in error responses.
   * <p>
   * Note that everything is always logged.
   * @param enableForceJwt If true, the path /login/forcejwt can be PUT to create a session based on the the JWT in the message body.
   * This should be secure even in a production environment, but for the sake of safety it defaults to being disabled.
   * @param sessionCookie Configuration data for the session cookie.
   */
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public LoginRouter(Vertx vertx,
           LoginDao loginDao,
           OpenIdDiscoveryHandler openIdDiscoveryHandler,
           JwtValidator jwtValidator,
           Authenticator requestContextBuilder,
           SessionConfig sessionConfig,
           List<String> requiredAuds,
           boolean outputAllErrorMessages,
           boolean enableForceJwt,
           CookieConfig sessionCookie
  ) {
    this.vertx = vertx;
    this.webClient = WebClient.create(vertx, new WebClientOptions().setConnectTimeout(60000));
    this.loginDao = loginDao;
    this.openIdDiscoveryHandler = openIdDiscoveryHandler;
    this.jwtValidator = jwtValidator;
    this.requestContextBuilder = requestContextBuilder;
    this.stateLength = sessionConfig.getStateLength();
    this.codeVerifierLength = sessionConfig.getCodeVerifierLength();
    this.nonceLength = sessionConfig.getNonceLength();
    // Deep copy of auth endpoints because they may be changed
    this.authEndpoints = deepCopyAuthEndpoints(sessionConfig.getOauth());
    this.requiredAuds = ImmutableCollectionTools.copy(requiredAuds);
    this.outputAllErrorMessages = outputAllErrorMessages;
    this.enableForceJwt = enableForceJwt;
    this.sessionCookie = sessionCookie;
  }

  static ImmutableMap<String, AuthEndpoint> deepCopyAuthEndpoints(Map<String, AuthEndpoint> authEndpoints) {
    ImmutableMap.Builder<String, AuthEndpoint> builder = ImmutableMap.<String, AuthEndpoint>builder();
    if (authEndpoints != null) {
      authEndpoints.entrySet().stream().forEach(e -> {
        builder.put(e.getKey(), new AuthEndpoint(e.getValue()));
      });
    }
    return builder.build();
  }
  
  /**
   * Return the base URL used to make this request with a path of /login/return.
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
    return OriginalUrl.get(
            request.scheme()
            , request.authority()
            , "/login/return"
            , null
            , request.headers()
    );
  }

  /**
   * Return the base URL used to make this request with a path of /.
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
  static String loggedOutUri(HttpServerRequest request) {
    return OriginalUrl.get(
            request.scheme()
            , request.authority()
            , "/"
            , null
            , request.headers()
    );
  }

  /**
   * Return the domain used to make this request.
   *
   * The X-Forwarded-Host header is used, if present. Any components of the URL not found in headers is taken from the request.
   *
   * @param request The request.
   * @return The domain that the client used to make the request.
   */
  static String domain(HttpServerRequest request) {
    String host = request.getHeader("X-Forwarded-Host");
    if (Strings.isNullOrEmpty(host)) {
      host = request.authority().host();
    }
    return host;
  }

  /**
   * Return whether the client made the request using TLS.
   *
   * The X-Forwarded-Proto header is used, if present. Any components of the URL not found in headers is taken from the request.
   *
   * @param request The request.
   * @return true if client made the request using TLS.
   */
  static boolean wasTls(HttpServerRequest request) {
    String proto = request.getHeader("X-Forwarded-Proto");
    if (!Strings.isNullOrEmpty(proto)) {
      return "https".equals(proto);
    } else {
      return request.isSSL();
    }
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
    } else if (this.enableForceJwt && event.request().path().endsWith("/login/forcejwt") && event.request().method() == HttpMethod.PUT) {
      handleForceJwt(event);
    } else if (event.request().path().endsWith("/logout")) {
      handleLogout(event);
    } else {
      event.next();
    }
  }

  private void handleLogout(RoutingContext event) {

    if (sessionCookie != null && !Strings.isNullOrEmpty(sessionCookie.getName())) {

      Cookie cookie = event.request().cookies(sessionCookie.getName()).stream().findFirst().orElse(null);
      
      RequestContext requestContext = RequestContext.retrieveRequestContext(event);
      
      loginDao.getProviderAndTokens(requestContext, cookie.getValue())
              .compose(providerAndTokens -> {
                return loginDao.removeToken(requestContext, providerAndTokens.sessionId()).map(v -> providerAndTokens);
              })
              .compose(providerAndTokens -> {
                AuthEndpoint authEndpoint = authEndpoints.get(providerAndTokens.provider());
                if (authEndpoint != null && !Strings.isNullOrEmpty(authEndpoint.getRevocationEndpoint())) {
                  return performBackChannelLogout(
                          requestContext
                          , authEndpoint
                          , providerAndTokens.accessToken()
                          , providerAndTokens.refreshToken()
                  ).transform(ar -> {
                    if (ar.failed()) {
                      Log.decorate(logger.atDebug(), requestContext).log("Back-channel logout failed: ", ar.cause());
                      return Future.succeededFuture(providerAndTokens);
                    } else {
                      Log.decorate(logger.atDebug(), requestContext).log("Back-channel logout succeeded");
                      return Future.succeededFuture();
                    }
                  });
                } else {
                  return Future.succeededFuture(providerAndTokens);
                }
              })
              .onComplete(ar -> {
                Cookie newCookie = createCookie(sessionCookie, 0, wasTls(event.request()), domain(event.request()), "");
                if (ar.succeeded() && ar.result() != null) {
                  LoginDao.ProviderAndTokens providerAndTokens = ar.result();
                  AuthEndpoint authEndpoint = authEndpoints.get(providerAndTokens.provider());
                  if (authEndpoint != null 
                          && !Strings.isNullOrEmpty(authEndpoint.getEndSessionEndpoint())) {
                    Log.decorate(logger.atDebug(), requestContext).log("Performing front-channel logout using {}", authEndpoint.getEndSessionEndpoint());
                    StringBuilder logoutUrl = new StringBuilder();
                    logoutUrl.append(authEndpoint.getEndSessionEndpoint());
                    logoutUrl.append(authEndpoint.getEndSessionEndpoint().contains("?") ? "&" : "?");
                    if (!Strings.isNullOrEmpty(providerAndTokens.idToken())) {
                      logoutUrl.append("id_token_hint=");
                      logoutUrl.append(URLEncoder.encode(providerAndTokens.idToken(), StandardCharsets.UTF_8));
                    }
                    if (!Strings.isNullOrEmpty(authEndpoint.getCredentials().getId())) {
                      logoutUrl.append("&client_id=");
                      logoutUrl.append(URLEncoder.encode(authEndpoint.getCredentials().getId(), StandardCharsets.UTF_8));
                    }
                    logoutUrl.append("&post_logout_redirect_uri=")
                            .append(URLEncoder.encode(loggedOutUri(event.request()), StandardCharsets.UTF_8));
                    event.response()
                            .addCookie(newCookie)
                            .putHeader("Location", logoutUrl.toString())
                            .setStatusCode(307)
                            .end();
                    return ;
                  }
                }
                if (ar.failed()) {
                  Log.decorate(logger.atWarn(), requestContext).log("Failure during logout: ", ar.cause());
                }
                event.response()
                        .addCookie(newCookie)
                        .putHeader("Location", "/")
                        .setStatusCode(307)
                        .end();
              });
    }
  }

  private Future<Void> performBackChannelLogout(RequestContext requestContext, AuthEndpoint authEndpoint, String accessToken, String refreshToken) {
    Log.decorate(logger.atDebug(), requestContext).log("Performing back-channel logout using {}", authEndpoint);
    MultiMap body = MultiMap.caseInsensitiveMultiMap();
    if (Strings.isNullOrEmpty(refreshToken)) {
      body.add("token_type_hint", "access_token");
      body.add("token", accessToken);
    } else {
      body.add("token_type_hint", "refresh_token");
      body.add("token", refreshToken);
    }
    return webClient.requestAbs(HttpMethod.POST, authEndpoint.getRevocationEndpoint())
            .basicAuthentication(authEndpoint.getCredentials().getId(), authEndpoint.getCredentials().getSecret())
            .putHeader("Accept", "application/json")
            .sendForm(body)
            .onComplete(ar -> {
              if (ar.failed()) {
                Log.decorate(logger.atWarn(), requestContext).log("Revocation request to {} failed: ", authEndpoint.getRevocationEndpoint(), ar.cause());
              } else {
                HttpResponse<Buffer> response = ar.result();
                Buffer responseBody = response.body();
                int statusCode = response.statusCode();
                if (statusCode >= 200 && statusCode < 300) {
                  Log.decorate(logger.atDebug(), requestContext).log("Revocation request to {} returned {} with {}"
                          , authEndpoint.getRevocationEndpoint()
                          , response.statusCode()
                          , responseBody
                          );
                } else {
                  Log.decorate(logger.atWarn(), requestContext).log("Revocation request to {} returned {} with {}"
                          , authEndpoint.getRevocationEndpoint()
                          , response.statusCode()
                          , responseBody
                          );
                }
              }
            })
            .map(v -> null);
  }

  private boolean handleLoginResponse(RoutingContext event) {
    RequestContext requestContext = RequestContext.retrieveRequestContext(event);
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
    Log.decorate(logger.atDebug(), requestContext).log("State: {}", state);
    String[] tokens = new String[3]; // {accessToken, refreshToken, idToken}
    loginDao.getRequestData(requestContext, state)
            .compose(requestData -> {
              requestDataAndAuthEndpoint.requestData = requestData;
              return loginDao.markUsed(requestContext, state);
            }).compose(v -> {
      requestDataAndAuthEndpoint.authEndpoint = authEndpoints.get(requestDataAndAuthEndpoint.requestData.provider());
      if (null == requestDataAndAuthEndpoint.authEndpoint) {
        Log.decorate(logger.atWarn(), requestContext).log("OAuth provider {} not found in {}", requestDataAndAuthEndpoint.requestData.provider(), authEndpoints.keySet());
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

      Log.decorate(logger.atDebug(), requestContext).log("Token endpoiont: {} and body: {}", requestDataAndAuthEndpoint.authEndpoint.getTokenEndpoint(), body);
      return webClient.requestAbs(HttpMethod.POST, requestDataAndAuthEndpoint.authEndpoint.getTokenEndpoint())
              .putHeader("Accept", "application/json")
              .sendForm(body);
    })
            .compose(codeResponse -> {
              Buffer body = codeResponse.body();
              if (codeResponse.statusCode() != 200) {
                Log.decorate(logger.atWarn(), requestContext).log("Failed to get access token from {} ({}): {}",
                         requestDataAndAuthEndpoint.authEndpoint.getTokenEndpoint(), codeResponse.statusCode(), body);
                return Future.failedFuture(new IllegalStateException("Failed to get access token from provider"));
              } else {
                JsonObject jsonBody;
                try {
                  jsonBody = body.toJsonObject();
                } catch (Throwable ex) {
                  Log.decorate(logger.atWarn(), requestContext).log("Failed to get access token ({}): {}", codeResponse.statusCode(), body);
                  return Future.failedFuture(new IllegalStateException("Failed to get access token from provider"));
                }
                Log.decorate(logger.atDebug(), requestContext).log("Access token response: {}", jsonBody);
                tokens[0] = jsonBody.getString("access_token");
                tokens[1] = jsonBody.getString("refresh_token");
                tokens[2] = jsonBody.getString("id_token");
                
                if (!Strings.isNullOrEmpty(tokens[0]) && Strings.isNullOrEmpty(tokens[2]) && !Strings.isNullOrEmpty(requestDataAndAuthEndpoint.authEndpoint.getRevocationEndpoint())) {
                  if (requestDataAndAuthEndpoint.authEndpoint.getScope() != null && requestDataAndAuthEndpoint.authEndpoint.getScope().contains("openid")) {
                    Log.decorate(logger.atWarn(), requestContext).log("No id_token found in response from OAuth provider that has revocation endpoint ({}), consult their documentation."
                            , requestDataAndAuthEndpoint.requestData.provider());
                  } else {
                    Log.decorate(logger.atWarn(), requestContext).log("No id_token found in response from OAuth provider that has revocation endpoint ({}), you probably need to add openid to the scope for the provider."
                            , requestDataAndAuthEndpoint.requestData.provider());
                  }
                }
                if (Strings.isNullOrEmpty(tokens[0])) {
                  Log.decorate(logger.atWarn(), requestContext).log("Failed to get access token ({}): {}", codeResponse.statusCode(), body);
                  return Future.failedFuture(new IllegalStateException("Failed to get access token from provider"));
                }

                return jwtValidator.validateToken(requestDataAndAuthEndpoint.authEndpoint.getIssuer(),
                         tokens[0],
                         requiredAuds,
                         outputAllErrorMessages
                );
              }
            })
            .compose(jwt -> {
              String id = createRandomSessionId();
              long maxAge = jwt.getExpiration() - (System.currentTimeMillis() / 1000);
              Cookie cookie = createCookie(sessionCookie, maxAge, wasTls(event.request()), domain(event.request()), id);

              return loginDao.storeTokens(requestContext
                      , id
                      , jwt.getExpirationLocalDateTime()
                      , tokens[0]
                      , requestDataAndAuthEndpoint.requestData.provider()
                      , tokens[1]
                      , tokens[2]
              )
                      .compose(v -> {
                        return event.response()
                                .addCookie(cookie)
                                .putHeader("Location", requestDataAndAuthEndpoint.requestData.targetUrl())
                                .setStatusCode(307)
                                .end();
                      });
            })
            .onFailure(ex -> {
              QueryRouter.internalError(ex, event, outputAllErrorMessages);
            });
    return false;
  }

  private boolean handleForceJwt(RoutingContext event) {
    RequestContext requestContext = RequestContext.retrieveRequestContext(event);
    String[] tokens = new String[3]; // {accessToken, refreshToken, idToken}
    
    event.request().body()
            .compose(body -> {
              tokens[0] = body.toString(StandardCharsets.UTF_8);
              if (tokens[0] == null) {
                tokens[0] = "";
              }
              return jwtValidator.validateToken(null,
                       tokens[0],
                       requiredAuds,
                       outputAllErrorMessages
              );
            })
            .compose(jwt -> {
              String id = createRandomSessionId();
              long maxAge = jwt.getExpiration() - (System.currentTimeMillis() / 1000);
              Cookie cookie = createCookie(sessionCookie, maxAge, wasTls(event.request()), domain(event.request()), id);

              return loginDao.storeTokens(requestContext
                      , id
                      , jwt.getExpirationLocalDateTime()
                      , tokens[0]
                      , "forced"
                      , tokens[1]
                      , tokens[2]
              )
                      .compose(v -> {
                        return event.response()
                                .addCookie(cookie)
                                .putHeader("Location", "/ui/")
                                .setStatusCode(307)
                                .end("Session started");
                      });
            })
            .onFailure(ex -> {
              QueryRouter.internalError(ex, event, outputAllErrorMessages);
            });
    return false;
  }

  private Cookie getSessionCookie(HttpServerRequest request) {
    if (sessionCookie != null && !Strings.isNullOrEmpty(sessionCookie.getName())) {
      Set<Cookie> cookies = request.cookies(sessionCookie.getName());
      for (Cookie cookie : cookies) {
        return cookie;
      }
    }
    return null;
  }

  
  
  /**
   * Create a cookie according to the passed in values.
   *
   * @param config General cookie configuration.
   * @param maxAge Maximum age of the cookie in seconds from now.
   * @param wasTls True if the request to this service was originally HTTPS.
   * @param domain The domain name in the original request to this service.
   * @param value The value to assign to the cookie.
   * @return A new Cookie.
   */
  static Cookie createCookie(CookieConfig config, long maxAge, boolean wasTls, String domain, String value) {
    Cookie cookie = Cookie.cookie(config.getName(), value);
    cookie.setHttpOnly(config.isHttpOnly() == null ? false : Boolean.TRUE.equals(config.isHttpOnly()));
    cookie.setSecure(config.isSecure() == null ? wasTls : Boolean.TRUE.equals(config.isSecure()));
    cookie.setDomain(config.getDomain() != null ? config.getDomain() : domain);
    cookie.setPath(Coalesce.coalesce(config.getPath(), "/"));
    if (config.getSameSite() != null) {
      cookie.setSameSite(config.getSameSite());
    }
    cookie.setMaxAge(maxAge);
    return cookie;
  }

  private String buildCompleteRedirectUrl(HttpServerRequest request, String path) {
    return OriginalUrl.get(
            request.scheme()
            , request.authority()
            , path
            , null
            , request.headers()
    );
  }
  
  private boolean handleLoginRequest(RoutingContext event) {
    RequestContext requestContext = RequestContext.retrieveRequestContext(event);
    
    String targetUrl = event.request().getParam("return");
    if (Strings.isNullOrEmpty(targetUrl)) {
      QueryRouter.internalError(new IllegalArgumentException("Return path not specified"), event, outputAllErrorMessages);
      return true;
    } else if (!targetUrl.startsWith("/")) {
      QueryRouter.internalError(new IllegalArgumentException("Return must be just a full path"), event, outputAllErrorMessages);
      return true;
    }
    // /If there is a valid session cookie just use that access token
    Cookie cookie = getSessionCookie(event.request());
    String redirectUrl = buildCompleteRedirectUrl(event.request(), targetUrl);
    if (cookie != null) {
      String token[] = new String[1];
      loginDao.getToken(requestContext, cookie.getValue())
              .compose(tokenFromDb -> {
                token[0] = tokenFromDb;
                return requestContextBuilder.validateToken(event.request(), token[0]);
              })
              .onFailure(ex -> {
                Log.decorate(logger.atDebug(), requestContext).log("Request has invalid session cookie, continuing with login");
                performOAuthRedirect(event, redirectUrl);
              })
              .onSuccess(jwt -> {
                Log.decorate(logger.atDebug(), requestContext).log("Request has valid session cookie, skipping login");

                event.response()
                        .putHeader("Location", redirectUrl)
                        .setStatusCode(307)
                        .end();
              });
      return false;
    } else {
      return performOAuthRedirect(event, redirectUrl);
    }
  }

  private boolean performOAuthRedirect(RoutingContext event, String targetUrl) {
    RequestContext requestContext = RequestContext.retrieveRequestContext(event);
    String provider = event.request().getParam("provider");
    if (Strings.isNullOrEmpty(provider)) {
      QueryRouter.internalError(new IllegalArgumentException("OAuth provider not specified"), event, outputAllErrorMessages);
      return true;
    }
    AuthEndpoint authEndpoint = authEndpoints.get(provider);
    if (null == authEndpoint) {
      Log.decorate(logger.atWarn(), requestContext).log("OAuth provider {} not found in {}", provider, authEndpoints.keySet());
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
                Log.decorate(logger.atWarn(), requestContext).log("Failed to get OpenID configuration for issuer {}: ", authEndpoint.getIssuer(), ex);
                QueryRouter.internalError(ex, event, outputAllErrorMessages);
              });
    } else {
      redirectToAuthEndpoint(event, authEndpoint, provider, targetUrl);
    }
    return false;
  }

  /**
   * Return true if this authentication endpoint should undergo
   * <a href="https://openid.net/specs/openid-connect-discovery-1_0.html">OpenID Connect Discovery</a>.
   * <p>
   * If the AuthEndpoint does not have an issuer this will always be false, because OpenID Connect Discovery requires an issuer.
   * Otherwise, returns true if the endpoint does not have both an authorization endpoint and a token endpoint or it is not within
   * its valid date.
   *
   * @param authEndpoint the configured (or already discovered) endpoint details.
   * @return true if this authentication endpoint should undergo OpenID Connect Discover.
   */
  static boolean shouldDiscover(AuthEndpoint authEndpoint) {
    return !Strings.isNullOrEmpty(authEndpoint.getIssuer())
            && (Strings.isNullOrEmpty(authEndpoint.getAuthorizationEndpoint())
            || Strings.isNullOrEmpty(authEndpoint.getTokenEndpoint())
            || authEndpoint.getInvalidDate() == null
            || authEndpoint.getInvalidDate().isBefore(LocalDateTime.now(ZoneOffset.UTC)));
  }

  private void redirectToAuthEndpoint(RoutingContext event, AuthEndpoint authEndpoint, String provider, String targetUrl) {
    RequestContext requestContext = RequestContext.retrieveRequestContext(event);

    if (Strings.isNullOrEmpty(authEndpoint.getAuthorizationEndpoint())) {
      Log.decorate(logger.atWarn(), requestContext).log("Provider {} has no authorization endpoint", provider);
      QueryRouter.internalError(new IllegalStateException("Provider has no authorization endpoint"), event, outputAllErrorMessages);
      return;
    }

    String state = randomString(stateLength);
    String codeVerifier = null;
    String nonce = null;
    String redirectUri = redirectUri(event.request());

    StringBuilder url = new StringBuilder();
    url.append(authEndpoint.getAuthorizationEndpoint())
            .append("?response_type=code")
            .append("&client_id=").append(URLEncoder.encode(authEndpoint.getCredentials().getId(), StandardCharsets.UTF_8))
            .append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8))
            .append("&state=").append(state);
    if (!Strings.isNullOrEmpty(authEndpoint.getScope())) {
      url.append("&scope=").append(URLEncoder.encode(authEndpoint.getScope(), StandardCharsets.UTF_8));
    }
    if (authEndpoint.isPkce()) {
      codeVerifier = randomString(codeVerifierLength);
      url.append("&code_challenge=").append(createCodeChallenge(codeVerifier));
      url.append("&code_challenge_method=S256");
    }
    if (authEndpoint.isNonce()) {
      nonce = randomString(nonceLength);
      url.append("&nonce=").append(nonce);
    }

    loginDao.store(requestContext, state, provider, codeVerifier, nonce, redirectUri, targetUrl)
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
