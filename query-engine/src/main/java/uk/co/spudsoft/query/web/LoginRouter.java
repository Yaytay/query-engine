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
import com.google.common.hash.Hashing;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.jwtvalidatorvertx.JwtValidator;
import uk.co.spudsoft.jwtvalidatorvertx.OpenIdDiscoveryHandler;
import uk.co.spudsoft.query.exec.conditions.RequestContextBuilder;
import uk.co.spudsoft.query.main.AuthEndpoint;
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
  private final RequestContextBuilder requestContextBuilder;
  private final int stateLength;
  private final int codeVerifierLength;
  private final int nonceLength;
  private final Map<String, AuthEndpoint> authEndpoints;
  private final boolean outputAllErrorMessages;
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
   * @param requestContextBuilder Creator of {@link uk.co.spudsoft.query.exec.conditions.RequestContext} objects, used here for
   * some utility functions.
   * @param sessionConfig Configuration data.
   * @param requiredAuds A valid JWT must contain at least one of these audience values.
   * @param outputAllErrorMessages In a production environment error messages should usually not leak information that may assist
   * a bad actor, set this to true to return full details in error responses.
   * <p>
   * Note that everything is always logged.
   * @param sessionCookie Configuration data for the session cookie.
   * @return a newly created LoginRouter.
   */
  public static LoginRouter create(Vertx vertx,
           LoginDao loginDao,
           OpenIdDiscoveryHandler openIdDiscoveryHandler,
           JwtValidator jwtValidator,
           RequestContextBuilder requestContextBuilder,
           SessionConfig sessionConfig,
           List<String> requiredAuds,
           boolean outputAllErrorMessages,
           CookieConfig sessionCookie
  ) {
    return new LoginRouter(vertx, loginDao, openIdDiscoveryHandler, jwtValidator, requestContextBuilder, sessionConfig, requiredAuds, outputAllErrorMessages, sessionCookie);
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
   * @param requestContextBuilder Creator of {@link uk.co.spudsoft.query.exec.conditions.RequestContext} objects, used here for
   * some utility functions.
   * @param sessionConfig Configuration data.
   * @param requiredAuds A valid JWT must contain at least one of these audience values.
   * @param outputAllErrorMessages In a production environment error messages should usually not leak information that may assist
   * a bad actor, set this to true to return full details in error responses.
   * <p>
   * Note that everything is always logged.
   * @param sessionCookie Configuration data for the session cookie.
   */
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public LoginRouter(Vertx vertx,
           LoginDao loginDao,
           OpenIdDiscoveryHandler openIdDiscoveryHandler,
           JwtValidator jwtValidator,
           RequestContextBuilder requestContextBuilder,
           SessionConfig sessionConfig,
           List<String> requiredAuds,
           boolean outputAllErrorMessages,
           CookieConfig sessionCookie
  ) {
    this.vertx = vertx;
    this.webClient = WebClient.create(vertx);
    this.loginDao = loginDao;
    this.openIdDiscoveryHandler = openIdDiscoveryHandler;
    this.jwtValidator = jwtValidator;
    this.requestContextBuilder = requestContextBuilder;
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
    this.requiredAuds = ImmutableCollectionTools.copy(requiredAuds);
    this.outputAllErrorMessages = outputAllErrorMessages;
    this.sessionCookie = sessionCookie;
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

    int portNum = -1;
    if (!Strings.isNullOrEmpty(port)) {
      try {
        portNum = Integer.parseInt(port);
      } catch (Throwable ex) {
        logger.warn("Illegal X-Forward-Port header received: {}", port);
      }
    }

    if (Strings.isNullOrEmpty(scheme)) {
      scheme = request.scheme();
    }
    if (portNum < 0) {
      portNum = request.authority().port();
    }
    if (RequestContextBuilder.isStandardHttpsPort(scheme, portNum)) {
      port = "";
    } else if (RequestContextBuilder.isStandardHttpPort(scheme, portNum)) {
      port = "";
    } else {
      port = ":" + Integer.toString(portNum);
    }

    String host = request.getHeader("X-Forwarded-Host");
    if (Strings.isNullOrEmpty(host)) {
      host = request.authority().host();
    }
    return scheme + "://" + host + port + "/login/return";
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
    if (Strings.isNullOrEmpty(proto)) {
      return "https".equals(proto);
    }
    return request.isSSL();
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
    } else if (event.request().path().endsWith("/login/logout")) {
      handleLogout(event);
    } else {
      event.next();
    }
  }

  private void handleLogout(RoutingContext event) {

    if (sessionCookie != null && !Strings.isNullOrEmpty(sessionCookie.getName())) {

      Set<Cookie> cookies = event.request().cookies(sessionCookie.getName());
      List<Future<Void>> futures = new ArrayList<>();
      for (Cookie cookie : cookies) {
        futures.add(loginDao.removeToken(cookie.getValue()));
      }

      Future.all(futures)
              .onComplete(ar -> {
                Cookie cookie = createCookie(sessionCookie, 0, wasTls(event.request()), domain(event.request()), "");

                event.response()
                        .addCookie(cookie)
                        .putHeader("Location", "/")
                        .setStatusCode(307)
                        .end();
              });
    }
  }

  private boolean handleLoginResponse(RoutingContext event) {
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
    String[] accessTokenPtr = new String[1];
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
              .sendForm(body);
    })
            .compose(codeResponse -> {
              if (codeResponse.statusCode() != 200) {
                String responseBody = codeResponse.bodyAsString();
                logger.warn("Failed to get access token from {} ({}): {}",
                         requestDataAndAuthEndpoint.authEndpoint.getTokenEndpoint(), codeResponse.statusCode(), responseBody);
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
                accessTokenPtr[0] = body.getString("access_token");
                if (Strings.isNullOrEmpty(accessTokenPtr[0])) {
                  String stringBody = codeResponse.bodyAsString();
                  logger.warn("Failed to get access token ({}): {}", codeResponse.statusCode(), stringBody);
                  return Future.failedFuture(new IllegalStateException("Failed to get access token from provider"));
                }

                return jwtValidator.validateToken(requestDataAndAuthEndpoint.authEndpoint.getIssuer(),
                         accessTokenPtr[0],
                         requiredAuds,
                         outputAllErrorMessages
                );
              }
            })
            .compose(jwt -> {
              String targetUrl = buildTargetUrlWithAccessToken(requestDataAndAuthEndpoint.requestData.targetUrl(), accessTokenPtr[0]);

              String id = createRandomSessionId();
              long maxAge = jwt.getExpiration() - (System.currentTimeMillis() / 1000);
              Cookie cookie = createCookie(sessionCookie, maxAge, wasTls(event.request()), domain(event.request()), id);

              return loginDao.storeToken(id, jwt.getExpirationLocalDateTime(), accessTokenPtr[0])
                      .compose(v -> {
                        return event.response()
                                .addCookie(cookie)
                                .putHeader("Location", targetUrl)
                                .setStatusCode(307)
                                .end();
                      });
            })
            .onFailure(ex -> {
              QueryRouter.internalError(ex, event, outputAllErrorMessages);
            });
    return false;
  }

  private static String buildTargetUrlWithAccessToken(String targetUrl, String accessToken) {
    String targetUrlBase = targetUrl;
    return targetUrlBase
            + (targetUrlBase.contains("?") ? "&" : "?")
            + "access_token=" + accessToken;
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
    cookie.setHttpOnly(config.isHttpOnly() == null ? false : config.isHttpOnly());
    cookie.setSecure(config.isSecure() == null ? wasTls : config.isSecure());
    cookie.setDomain(config.getDomain() != null ? config.getDomain() : domain);
    cookie.setPath(config.getPath() != null ? config.getPath() : "/");
    if (config.getSameSite() != null) {
      cookie.setSameSite(config.getSameSite());
    }
    cookie.setMaxAge(maxAge);
    return cookie;
  }

  private boolean handleLoginRequest(RoutingContext event) {
    String targetUrl = event.request().getParam("return");
    if (Strings.isNullOrEmpty(targetUrl)) {
      QueryRouter.internalError(new IllegalArgumentException("Target URL not specified"), event, outputAllErrorMessages);
      return true;
    }
    // /If there is a valid session cookie just use that access token
    Cookie cookie = getSessionCookie(event.request());
    if (cookie != null) {
      String token[] = new String[1];
      loginDao.getToken(cookie.getValue())
              .compose(tokenFromDb -> {
                token[0] = tokenFromDb;
                return requestContextBuilder.validateToken(event.request(), token[0]);
              })
              .onFailure(ex -> {
                logger.debug("Request has invalid session cookie, continuing with login");
                performOAuthRedirect(event, targetUrl);
              })
              .onSuccess(jwt -> {
                logger.debug("Request has valid session cookie, skipping login");
                String targetUrlWithToken = buildTargetUrlWithAccessToken(targetUrl, token[0]);

                event.response()
                        .putHeader("Location", targetUrlWithToken)
                        .setStatusCode(307)
                        .end();
              });
      return false;
    } else {
      return performOAuthRedirect(event, targetUrl);
    }
  }

  private boolean performOAuthRedirect(RoutingContext event, String targetUrl) {
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
    if (Strings.isNullOrEmpty(authEndpoint.getAuthorizationEndpoint())) {
      logger.warn("Provider {} has no authorization endpoint", provider);
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
