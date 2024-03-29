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

import io.netty.handler.codec.http.QueryStringDecoder;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
public class LoginRouterWithoutDiscoveryIT {
  
  private static final Logger logger = LoggerFactory.getLogger(LoginRouterWithoutDiscoveryIT.class);
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  
  @Test
  public void testLoginWithoutDiscoveryIT() throws Exception {
    logger.debug("Running testLoginWithoutDiscoveryIT");
    Main main = new Main();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    GlobalOpenTelemetry.resetForTest();
    main.testMain(new String[]{
      "--persistence.datasource.url=" + postgres.getJdbcUrl()
      , "--persistence.datasource.adminUser.username=" + postgres.getUser()
      , "--persistence.datasource.adminUser.password=" + postgres.getPassword()
      , "--persistence.datasource.schema=public" 
      , "--baseConfigPath=target/query-engine/samples-mainit"
      , "--vertxOptions.tracingOptions.serviceName=Query-Engine"
      , "--jwt.acceptableIssuerRegexes[0]=.*"
      , "--jwt.defaultJwksCacheDuration=PT1M"
      , "--logging.jsonFormat=false"
      , "--sampleDataLoads[0].url=" + postgres.getVertxUrl()
      , "--sampleDataLoads[0].adminUser.username=" + postgres.getUser()
      , "--sampleDataLoads[0].adminUser.password=" + postgres.getPassword()
      , "--managementEndpoints[0]=up"
      , "--managementEndpoints[2]=prometheus"
      , "--managementEndpoints[3]=threads"
      , "--managementEndpointPort=8001"
      , "--managementEndpointUrl=http://localhost:8001/manage"
      , "--session.requireSession=false"
      , "--session.oauth.GitHub.logoUrl=https://upload.wikimedia.org/wikipedia/commons/c/c2/GitHub_Invertocat_Logo.svg"
      , "--session.oauth.GitHub.authorizationEndpoint=https://github.com/login/oauth/authorize"
      , "--session.oauth.GitHub.tokenEndpoint=https://github.com/login/oauth/access_token"
      , "--session.oauth.GitHub.credentials.id=bdab017f4732085a51f9"
      , "--session.oauth.GitHub.credentials.secret=" + System.getProperty("queryEngineGithubSecret")
      , "--session.oauth.GitHub.pkce=false"
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
            .get("/login?return=http://fred/&provider=github")
            .then()
            .statusCode(400)
            .log().all()
            .extract().body().asString()
            ;
    assertEquals("OAuth provider not known", body);
    
    String authUiUrl = given()
            .redirects().follow(false)
            .log().all()
            .get("/login?provider=GitHub&return=http://fred")
            .then()
            .statusCode(307)
            .log().all()
            .extract().header("Location")
            ;
    assertThat(authUiUrl, startsWith("https://github.com/login/oauth/authorize"));
    Map<String, List<String>> authUiParams = new QueryStringDecoder(authUiUrl).parameters();
    assertEquals(Arrays.asList("code"), authUiParams.get("response_type"));
    assertEquals(Arrays.asList("bdab017f4732085a51f9"), authUiParams.get("client_id"));
    assertEquals(Arrays.asList("S256"), authUiParams.get("code_challenge_method"));
    assertTrue(authUiParams.containsKey("state"));
    assertTrue(authUiParams.containsKey("code_challenge"));
    assertTrue(authUiParams.containsKey("nonce"));
    assertThat(authUiParams.get("redirect_uri").get(0), startsWith("http://localhost:" + Integer.toString(main.getPort()) + "/login/return"));
    

    // Can't actually login, so this call will fail at the point of trying to get the token
    given()
            .redirects().follow(false)
            .log().all()
            .get("/login/return?code=wibble&state=" + authUiParams.get("state").get(0))
            .then()
            .log().all()
            .statusCode(500)
            ;
    
    
  }
  
}
