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

import com.google.common.cache.Cache;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.exception.ExceptionUtils;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import uk.co.spudsoft.jwtvalidatorvertx.AlgorithmAndKeyPair;
import uk.co.spudsoft.jwtvalidatorvertx.JsonWebAlgorithm;
import uk.co.spudsoft.jwtvalidatorvertx.JwkBuilder;
import uk.co.spudsoft.jwtvalidatorvertx.jdk.JdkTokenBuilder;
import uk.co.spudsoft.query.main.Main;
import uk.co.spudsoft.query.testcontainers.ServerProviderPostgreSQL;

/**
 *
 * @author jtalbut
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LoginRouterWithDiscoveryIT {

  private static final Logger logger = LoggerFactory.getLogger(LoginRouterWithDiscoveryIT.class);

  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();

  private static final String CONFS_DIR = "target/query-engine/samples-" + MethodHandles.lookup().lookupClass().getSimpleName().toLowerCase();
  
  @BeforeAll
  public void createDirs() {
    File confsDir = new File(CONFS_DIR);
    FileUtils.deleteQuietly(confsDir);
    confsDir.mkdirs();
  }

  private HttpServer createServer(int port) throws Exception {
    ExecutorService exeSvc = Executors.newFixedThreadPool(2);
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 2);
    server.setExecutor(exeSvc);
    return server;
  }

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

  private void sendResponse(HttpExchange exchange, int responseCode, String body) throws IOException {
    byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(responseCode, bodyBytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bodyBytes);
    }
  }

  @Test
  public void testLoginWithDiscoveryIT() throws Exception {
    logger.debug("Running testLoginWithDiscoveryIT");

    Cache<String, AlgorithmAndKeyPair> keyCache = AlgorithmAndKeyPair.createCache(Duration.ofMinutes(1));
    JdkTokenBuilder builder = new JdkTokenBuilder(keyCache);

    int port = findUnusedPort();
    HttpServer server = createServer(port);
    server.createContext("/", new HttpHandler() {
      @Override
      public void handle(HttpExchange exchange) throws IOException {
        switch (exchange.getRequestURI().getPath()) {
          case "/.well-known/openid-configuration":
            JsonObject config = new JsonObject();
            config.put("token_endpoint", "http://localhost:" + port + "/token");
            config.put("authorization_endpoint", "http://localhost:" + port + "/authorization_endpoint");
            config.put("jwks_uri", "http://localhost:" + port + "/jwks_uri");
            sendResponse(exchange, 200, config.toString());
            break;
          case "/jwks_uri":
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
            sendResponse(exchange, 200, jwkSet.encode());
            break;
          case "/token":
            // High security, you ask for a token you get one!
            JsonObject response = new JsonObject();
            long nowSeconds = System.currentTimeMillis() / 1000;
            try {
              response.put("access_token", builder.buildToken(JsonWebAlgorithm.RS512, this.toString(), "http://localhost:" + port + "/", "sub", Arrays.asList("query-engine"), nowSeconds, nowSeconds + 100, null));
              sendResponse(exchange, 200, response.toString());
            } catch (Throwable ex) {
              logger.error("Failed to generate token: ", ex);
              sendResponse(exchange, 500, ex.getClass().toString() + "\n" + ex.getMessage() + "\n" + ExceptionUtils.getStackTrace(ex));
            }
            break;
          default:
            logger.error("Unknown request: {}", exchange.getRequestURI().getPath());
            sendResponse(exchange, 404, "Not found");
        }
      }
    });
    server.start();

    Main main = new Main();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    main.testMain(new String[]{
      "--persistence.datasource.url=" + postgres.getJdbcUrl(),
       "--persistence.datasource.adminUser.username=" + postgres.getUser(),
       "--persistence.datasource.adminUser.password=" + postgres.getPassword(),
       "--persistence.datasource.schema=public",
       "--baseConfigPath=" + CONFS_DIR,
       "--jwt.acceptableIssuerRegexes[0]=.*",
       "--jwt.defaultJwksCacheDuration=PT1M",
       "--jwt.jwksEndpoints[0]=http://localhost:" + port + "/jwks_uri",
       "--logging.jsonFormat=false",
       "--sampleDataLoads[0].url=" + postgres.getVertxUrl(),
       "--sampleDataLoads[0].adminUser.username=" + postgres.getUser(),
       "--sampleDataLoads[0].adminUser.password=" + postgres.getPassword(),
       "--managementEndpoints[0]=up",
       "--managementEndpoints[2]=prometheus",
       "--managementEndpoints[3]=threads",
       "--managementEndpointPort=8001",
       "--managementEndpointUrl=http://localhost:8001/manage",
       "--session.purgeDelay=PT1S",
       "--session.requireSession=false",
       "--session.oauth.test.logoUrl=https://upload.wikimedia.org/wikipedia/commons/c/c2/GitHub_Invertocat_Logo.svg",
       "--session.oauth.test.issuer=http://localhost:" + port + "/",
       "--session.oauth.test.credentials.id=bdab017f4732085a51f9",
       "--session.oauth.test.credentials.secret=" + System.getProperty("queryEngineGithubSecret"),
       "--session.oauth.test.pkce=true"
    }, stdout, System.getenv());
    assertEquals(0, stdoutStream.size());

    RestAssured.port = main.getPort();

    String body = given()
            .log().all()
            .get("/login")
            .then()
            .statusCode(400)
            .log().all()
            .extract().body().asString();
    assertEquals("Target URL not specified", body);

    body = given()
            .log().all()
            .get("/login?return=http://fred/")
            .then()
            .statusCode(400)
            .log().all()
            .extract().body().asString();
    assertEquals("OAuth provider not specified", body);

    body = given()
            .log().all()
            .get("/login?return=http://fred/")
            .then()
            .statusCode(400)
            .log().all()
            .extract().body().asString();
    assertEquals("OAuth provider not specified", body);

    body = given()
            .log().all()
            .get("/login/return")
            .then()
            .statusCode(400)
            .log().all()
            .extract().body().asString();
    assertEquals("Code not specified", body);

    body = given()
            .log().all()
            .get("/login/return?code=code")
            .then()
            .statusCode(400)
            .log().all()
            .extract().body().asString();
    assertEquals("State not specified", body);

    body = given()
            .log().all()
            .get("/login/return?code=code&state=state")
            .then()
            .statusCode(400)
            .log().all()
            .extract().body().asString();
    assertEquals("State does not exist", body);

    body = given()
            .log().all()
            .get("/login?return=http://fred/&provider=bob")
            .then()
            .statusCode(400)
            .log().all()
            .extract().body().asString();
    assertEquals("OAuth provider not known", body);

    // Provider is case sensistive
    body = given()
            .log().all()
            .get("/login?return=http://fred/&provider=TEST")
            .then()
            .statusCode(400)
            .log().all()
            .extract().body().asString();
    assertEquals("OAuth provider not known", body);

    String authUiUrl = given()
            .redirects().follow(false)
            .log().all()
            .get("/login?provider=test&return=http://fred")
            .then()
            .statusCode(307)
            .log().all()
            .extract().header("Location");
    assertThat(authUiUrl, startsWith("http://localhost:" + port + "/authorization_endpoint"));
    logger.debug("authUiUrl: {}", authUiUrl);
    Map<String, List<String>> authUiParams = new QueryStringDecoder(authUiUrl).parameters();
    assertEquals(Arrays.asList("code"), authUiParams.get("response_type"));
    assertEquals(Arrays.asList("bdab017f4732085a51f9"), authUiParams.get("client_id"));
    assertEquals(Arrays.asList("S256"), authUiParams.get("code_challenge_method"));
    assertTrue(authUiParams.containsKey("state"));
    assertTrue(authUiParams.containsKey("code_challenge"));
    assertTrue(authUiParams.containsKey("nonce"));
    assertThat(authUiParams.get("redirect_uri").get(0), startsWith("http://localhost:" + Integer.toString(main.getPort()) + "/login/return"));

    // The test endpint will happily give out a token
    Map<String, String> cookies = given()
            .redirects().follow(false)
            .log().all()
            .get("/login/return?code=wibble&state=" + authUiParams.get("state").get(0))
            .then()
            .log().all()
            .statusCode(307)
            .header("Location", startsWith("http://fred?access_token="))
            .extract().cookies();
    logger.debug("Cookies: {}", cookies);
    assertTrue(cookies.containsKey("qe-session"));
    assertThat(cookies.get("qe-session").length(), greaterThan(64));

    body = given()
            .queryParam("key", postgres.getName())
            .queryParam("port", postgres.getPort())
            .cookies(cookies)
            .accept("text/html, application/xhtml+xml, image/webp, image/apng, application/xml; q=0.9, application/signed-exchange; v=b3; q=0.9, */*; q=0.8")
            .log().all()
            .get("/query/sub1/sub2/TemplatedJsonToPipelineIT")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();

    body = given()
            .cookies(cookies)
            .log().all()
            .get("/api/history")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    logger.debug("History: {}", body);

    Thread.sleep(1500);

    cookies = given()
            .cookies(cookies)
            .redirects().follow(false)
            .log().all()
            .get("/login/logout")
            .then()
            .log().all()
            .statusCode(307)
            .header("Location", equalTo("/"))
            .extract().cookies();
    logger.debug("Cookies: {}", cookies);
    assertTrue(cookies.containsKey("qe-session"));
    assertThat(cookies.get("qe-session").length(), equalTo(0));

    cookies = new HashMap<>();
    cookies.put("qe-session", "bad");
    body = given()
            .cookies(cookies)
            .log().all()
            .get("/api/history")
            .then()
            .log().ifError()
            .statusCode(401)
            .extract().body().asString();
    logger.debug("History: {}", body);

    server.stop(0);
    main.shutdown();
  }

}
