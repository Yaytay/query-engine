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

import com.google.common.cache.Cache;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.security.PublicKey;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.jwtvalidatorvertx.AlgorithmAndKeyPair;
import uk.co.spudsoft.jwtvalidatorvertx.IssuerAcceptabilityHandler;
import uk.co.spudsoft.jwtvalidatorvertx.JsonWebAlgorithm;
import uk.co.spudsoft.jwtvalidatorvertx.JwkBuilder;
import uk.co.spudsoft.jwtvalidatorvertx.JwtValidator;
import uk.co.spudsoft.jwtvalidatorvertx.OpenIdDiscoveryHandler;
import uk.co.spudsoft.jwtvalidatorvertx.impl.JWKSOpenIdDiscoveryHandlerImpl;
import uk.co.spudsoft.jwtvalidatorvertx.jdk.JdkTokenBuilder;
import uk.co.spudsoft.query.web.LoginDaoMemoryImpl;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RequestContextBuilderBearerAuthTest {

  private static final Logger logger = LoggerFactory.getLogger(RequestContextBuilderBearerAuthTest.class);

  private OpenIdDiscoveryHandler discoverer;
  private JwtValidator validator;
  private Cache<String, AlgorithmAndKeyPair> cache;
  private JdkTokenBuilder tokenBuilder;

  private HttpServer destServer;
  private int destPort;

  @BeforeAll
  public void init(Vertx vertx) throws IOException {
    cache = AlgorithmAndKeyPair.createCache(Duration.ofHours(1));
    tokenBuilder = new JdkTokenBuilder(cache);

    IssuerAcceptabilityHandler iah = IssuerAcceptabilityHandler.create(Arrays.asList(".*"), null, Duration.ZERO);
    discoverer = new JWKSOpenIdDiscoveryHandlerImpl(WebClient.create(vertx), iah, Duration.ofSeconds(60));
    validator = JwtValidator.create((JWKSOpenIdDiscoveryHandlerImpl) discoverer, iah);
  }

  @Test
  public void testBuildRequestContext(Vertx vertx, VertxTestContext testContext) throws Exception {
    RequestContextBuilder rcb = new RequestContextBuilder(WebClient.create(vertx), validator, discoverer, new LoginDaoMemoryImpl(Duration.ZERO), null, true, null, true, null, Collections.singletonList("aud"), null, null);

    destServer = vertx.createHttpServer();
    Router router = Router.router(vertx);

    WebClient webClient = WebClient.create(vertx);

    // Set up the test routes as a mock version of the real setup
    router.route(HttpMethod.GET, "/.well-known/openid-configuration").handler(ctx -> {
      logger.info("Got request to {}", ctx.request().uri());
      JsonObject result = new JsonObject();
      result.put("jwks_uri", ctx.request().scheme() + "://" + ctx.request().authority().toString() + "/jwks");
      HttpServerResponse response = ctx.response();
      response.setStatusCode(200);
      response.end(result.toBuffer());
    });

    router.route(HttpMethod.GET, "/jwks").handler(ctx -> {
      logger.info("Got request to {}", ctx.request().uri());

      Map<String, AlgorithmAndKeyPair> keyMap = cache.asMap();

      JsonObject jwkSet = new JsonObject();
      JsonArray jwks = new JsonArray();
      jwkSet.put("keys", jwks);
      for (Map.Entry<String, AlgorithmAndKeyPair> akEntry : keyMap.entrySet()) {
        try {
          String kid = akEntry.getKey();
          AlgorithmAndKeyPair akp = akEntry.getValue();
          PublicKey key = akp.getKeyPair().getPublic();
          JsonObject json = JwkBuilder.get(key).toJson(kid, akp.getAlgorithm().getName(), key);
          jwks.add(json);
        } catch(Throwable ex) {
          logger.error("Failed to create JWK from key: ", ex);
        }
      }

      HttpServerResponse response = ctx.response();
      response.setStatusCode(200);
      response.putHeader("cache-control", "max-age=100");
      response.end(jwkSet.toBuffer());
    });

    router.route(HttpMethod.GET, "/dest")
            .handler(ctx -> {
              logger.info("Got request to {}", ctx.request().uri());
              rcb.buildRequestContext(ctx.request())
                      .onFailure(ex -> {
                        logger.error("Failed to build request context: ", ex);
                        HttpServerResponse response = ctx.response();
                        response.setStatusCode(500);
                        response.end(ex.getMessage());
                      })
                      .onSuccess(requestContext -> {
                        logger.info("Request context: {}", requestContext);
                        HttpServerResponse response = ctx.response();
                        response.setStatusCode(200);
                        response.end("Hello " + requestContext.getName());
                      });
            });

    // Start the test server and then make request
    destServer.requestHandler(router).listen(0)
            .onSuccess(srv -> {
              destPort = srv.actualPort();
            })
            .compose(srv -> {
              long now = System.currentTimeMillis() / 1000;
              String token;
              try {
                token = tokenBuilder.buildToken(JsonWebAlgorithm.RS256, "kid", "http://localhost:" + destPort, "username", Arrays.asList("aud"), now - 1, now + 60, null);
              } catch(Throwable ex) {
                return Future.failedFuture(ex);
              }
              String url = "http://localhost:" + destPort + "/dest";
              logger.debug("Making request to {}", url);
              return webClient.getAbs(url)
                      .putHeader("Authorization", "Bearer " + token)
                      .as(BodyCodec.string())
                      .send();
            })
            .compose(response -> {
              if (response.statusCode() == 200) {
                testContext.verify(() -> {
                  assertEquals("Hello username", response.body());
                });
                testContext.completeNow();
              } else {
                logger.error("Failed with {}: {}", response.statusCode(), response.bodyAsString());
                testContext.failNow("Failed with " + response.statusCode());
              }
              return Future.succeededFuture();
            })
            .onFailure(ex -> {
              logger.error("Failed: ", ex);
              testContext.failNow(ex);
            });

  }

}
