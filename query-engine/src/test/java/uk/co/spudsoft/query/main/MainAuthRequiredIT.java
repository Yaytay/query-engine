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

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.restassured.RestAssured;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import java.io.File;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.testcontainers.ServerProviderPostgreSQL;

import static io.restassured.RestAssured.given;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import uk.co.spudsoft.query.web.LoginRouterWithDiscoveryIT;


/**
 * A set of tests that do not actually do any querying.
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class MainAuthRequiredIT {
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  
  private final int mgmtPort = LoginRouterWithDiscoveryIT.findUnusedPort();
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(MainAuthRequiredIT.class);
  
  @BeforeAll
  public static void createDirs(Vertx vertx) {
    File paramsDir = new File("target/query-engine/samples-mainit");
    paramsDir.mkdirs();
  }
    
  @Test
  public void testAuthRequired() throws Exception {
    logger.debug("Running testAuthRequired");
    GlobalOpenTelemetry.resetForTest();
    Main main = new Main();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    main.testMain(new String[]{
      "--persistence.datasource.url=" + postgres.getJdbcUrl()
      , "--persistence.datasource.adminUser.username=" + postgres.getUser()
      , "--persistence.datasource.adminUser.password=" + postgres.getPassword()
      , "--persistence.datasource.schema=public" 
      , "--baseConfigPath=target/query-engine/samples-mainit"
      , "--jwt.acceptableIssuerRegexes[0]=.*"
      , "--jwt.defaultJwksCacheDuration=PT1M"
      , "--jwt.jwksEndpoints[0]=http://localhost/jwks"
      , "--logging.jsonFormat=true"
      , "--sampleDataLoads[0].url=" + postgres.getVertxUrl()
      , "--sampleDataLoads[0].adminUser.username=" + postgres.getUser()
      , "--sampleDataLoads[0].adminUser.password=" + postgres.getPassword()
      , "--managementEndpoints[0]=up"
      , "--managementEndpoints[2]=prometheus"
      , "--managementEndpoints[3]=threads"
      , "--managementEndpointPort=" + mgmtPort            
      , "--managementEndpointUrl=http://localhost:" + mgmtPort + "/manage"
      , "--session.requireSession=true"
      , "--session.oauth.GitHub.logoUrl=https://upload.wikimedia.org/wikipedia/commons/c/c2/GitHub_Invertocat_Logo.svg"
      , "--session.oauth.GitHub.authorizationEndpoint=https://github.com/login/oauth/authorize"
      , "--session.oauth.GitHub.tokenEndpoint=https://github.com/login/oauth/access_token"
      , "--session.oauth.GitHub.credentials.id=bdab017f4732085a51f9"
      , "--session.oauth.GitHub.credentials.secret=" + System.getProperty("queryEngineGithubSecret")
      , "--session.oauth.GitHub.pkce=false"
    }, stdout, System.getenv());
    assertEquals(0, stdoutStream.size());
    
    RestAssured.port = main.getPort();
    
    // UI does not require auth
    given()
            .log().all()
            .get("/ui/index.html")
            .then()
            .statusCode(200)
            .log().all()
            ;
    
    // Manage endpoints do not require auth
    given()
            .log().all()
            .get("/manage")
            .then()
            .log().all()
            .statusCode(200)
            .body(equalTo("{\"location\":\"http://localhost:" + mgmtPort + "/manage\"}"))
            ;
    
    given()
            .log().all()
            .get("/api/session/profile")
            .then()
            .statusCode(401)
            .log().all()
            ;
           
    // Autho config does not require auth
    String authConfig = given()
            .log().all()
            .get("/api/auth-config")
            .then()
            .statusCode(200)
            .log().all()
            .extract().body().asString()
            ;
    assertEquals("[{\"name\":\"GitHub\",\"logo\":\"https://upload.wikimedia.org/wikipedia/commons/c/c2/GitHub_Invertocat_Logo.svg\"}]", authConfig);
            
    given()
            .log().all()
            .get("/api/docs/")
            .then()
            .statusCode(401)
            .log().all()
            ;
            
    given()
            .log().all()
            .get("/api/formio/doesntmatterwhatgoeshere-authfailsfirst")
            .then()
            .statusCode(401)
            .log().all()
            ;
            
    given()
            .log().all()
            .get("/api/info/available")
            .then()
            .statusCode(401)
            .log().all()
            ;
    
    main.shutdown();
  }
  
}
