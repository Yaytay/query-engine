/*
 * Copyright (C) 2023 njt
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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.vertx.core.json.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.main.Main;
import uk.co.spudsoft.query.testcontainers.ServerProviderPostgreSQL;


/**
 *
 * @author njt
 */
public class LoginRouterWithDiscoveryIT {
  
  private static final Logger logger = LoggerFactory.getLogger(LoginRouterWithDiscoveryIT.class);
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  
  private HttpServer createServer(int port) throws Exception {
    ExecutorService exeSvc = Executors.newFixedThreadPool(2);
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 2);
    server.setExecutor(exeSvc);
    return server;
  }

  public static int findUnusedPort() {
    try (ServerSocket s = new ServerSocket(0)) {
      return s.getLocalPort();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
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
            sendResponse(exchange, 200, config.toString());
            break;            
          case "/token":
            // High security, you ask for a token you get one!
            JsonObject response = new JsonObject();
            response.put("access_token", "toucan");
            sendResponse(exchange, 200, response.toString());
            break;                        
          default:
            sendResponse(exchange, 404, "Not found");
        }
      }
    });    
    server.start();
    
    Main main = new Main();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    main.testMain(new String[]{
      "--persistence.datasource.url=jdbc:" + postgres.getUrl()
      , "--persistence.datasource.adminUser.username=" + postgres.getUser()
      , "--persistence.datasource.adminUser.password=" + postgres.getPassword()
      , "--persistence.datasource.schema=public" 
      , "--baseConfigPath=target/query-engine/samples-mainit"
      , "--vertxOptions.tracingOptions.serviceName=Query-Engine"
      , "--jwt.acceptableIssuerRegexes[0]=.*"
      , "--jwt.defaultJwksCacheDuration=PT1M"
      , "--logging.jsonFormat=false"
      , "--sampleDataLoads[0].url=" + postgres.getUrl()
      , "--sampleDataLoads[0].adminUser.username=" + postgres.getUser()
      , "--sampleDataLoads[0].adminUser.password=" + postgres.getPassword()
      , "--managementEndpoints[0]=up"
      , "--managementEndpoints[2]=prometheus"
      , "--managementEndpoints[3]=threads"
      , "--managementEndpointPort=8001"
      , "--managementEndpointUrl=http://localhost:8001/manage"
      , "--session.requireSession=false"
      , "--session.oauth.test.logoUrl=https://upload.wikimedia.org/wikipedia/commons/c/c2/GitHub_Invertocat_Logo.svg"
      , "--session.oauth.test.issuer=http://localhost:" + port + "/"
      , "--session.oauth.test.credentials.id=bdab017f4732085a51f9"
      , "--session.oauth.test.credentials.secret=" + System.getProperty("queryEngineGithubSecret")
      , "--session.oauth.test.pkce=true"
    }, stdout);
    assertEquals(0, stdoutStream.size());
    
    RestAssured.port = main.getPort();
    
    String body = given()
            .log().all()
            .get("/login")
            .then()
            .statusCode(400)
            .log().all()
            .extract().body().asString()
            ;
    assertEquals("Target URL not specified", body);
    
    body = given()
            .log().all()
            .get("/login?return=http://fred/")
            .then()
            .statusCode(400)
            .log().all()
            .extract().body().asString()
            ;
    assertEquals("OAuth provider not specified", body);
    
    body = given()
            .log().all()
            .get("/login?return=http://fred/")
            .then()
            .statusCode(400)
            .log().all()
            .extract().body().asString()
            ;
    assertEquals("OAuth provider not specified", body);
    
    body = given()
            .log().all()
            .get("/login/return")
            .then()
            .statusCode(400)
            .log().all()
            .extract().body().asString()
            ;
    assertEquals("Code not specified", body);
    
    body = given()
            .log().all()
            .get("/login/return?code=code")
            .then()
            .statusCode(400)
            .log().all()
            .extract().body().asString()
            ;
    assertEquals("State not specified", body);
    
    body = given()
            .log().all()
            .get("/login/return?code=code&state=state")
            .then()
            .statusCode(400)
            .log().all()
            .extract().body().asString()
            ;
    assertEquals("State does not exist", body);
    
    body = given()
            .log().all()
            .get("/login?return=http://fred/&provider=bob")
            .then()
            .statusCode(400)
            .log().all()
            .extract().body().asString()
            ;
    assertEquals("OAuth provider not known", body);
    
    // Provider is case sensistive
    body = given()
            .log().all()
            .get("/login?return=http://fred/&provider=TEST")
            .then()
            .statusCode(400)
            .log().all()
            .extract().body().asString()
            ;
    assertEquals("OAuth provider not known", body);
    
    String authUiUrl = given()
            .redirects().follow(false)
            .log().all()
            .get("/login?provider=test&return=http://fred")
            .then()
            .statusCode(307)
            .log().all()
            .extract().header("Location")
            ;
    assertThat(authUiUrl, startsWith("http://localhost:" + port + "/authorization_endpoint"));
    Map<String, List<String>> authUiParams = new QueryStringDecoder(authUiUrl).parameters();
    assertEquals(Arrays.asList("code"), authUiParams.get("response_type"));
    assertEquals(Arrays.asList("bdab017f4732085a51f9"), authUiParams.get("client_id"));
    assertEquals(Arrays.asList("S256"), authUiParams.get("code_challenge_method"));
    assertTrue(authUiParams.containsKey("state"));
    assertTrue(authUiParams.containsKey("code_challenge"));
    assertTrue(authUiParams.containsKey("nonce"));
    assertThat(authUiParams.get("redirect_uri").get(0), startsWith("http://localhost:" + Integer.toString(main.getPort()) + "/login/return"));
    

    // The test endpint will happiler give out an invalid token
    given()
            .redirects().follow(false)
            .log().all()
            .get("/login/return?code=wibble&state=" + authUiParams.get("state").get(0))
            .then()
            .log().all()
            .statusCode(307)
            .header("Location", "http://fred?access_token=toucan")
            ;
    
    
  }
  
}
