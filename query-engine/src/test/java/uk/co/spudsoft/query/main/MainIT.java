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

import com.fasterxml.jackson.databind.node.ArrayNode;
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
import static io.restassured.config.RedirectConfig.redirectConfig;
import io.restassured.config.RestAssuredConfig;
import io.vertx.core.json.Json;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import uk.co.spudsoft.query.web.LoginRouterWithDiscoveryIT;


/**
 * A set of tests that do not actually do any querying.
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class MainIT {
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(MainIT.class);
  
  private final int mgmtPort = LoginRouterWithDiscoveryIT.findUnusedPort();
  
  @BeforeAll
  public static void createDirs(Vertx vertx) {
    File paramsDir = new File("target/query-engine/sammainit");
    paramsDir.mkdirs();
  }
    
  @Test
  public void testHelp() throws Exception {
    logger.debug("Running testHelp");
    Main main = new Main();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    GlobalOpenTelemetry.resetForTest();
    main.testMain(new String[]{
      "--help"
    }, stdout);
    logger.debug("Help:\n{}", stdout);
    
    main.shutdown();
  }
    
  @Test
  public void testHelpEnv() throws Exception {
    logger.debug("Running testHelpEnv");
    Main main = new Main();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    GlobalOpenTelemetry.resetForTest();
    main.testMain(new String[]{
      "--helpenv"
    }, stdout);
    logger.debug("HelpEnv:\n{}", stdout);
    
    main.shutdown();
  }
  
  @Test
  public void testBadAudit() throws Exception {
    logger.debug("Running testBadAudit");
    Main main = new Main();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    GlobalOpenTelemetry.resetForTest();
    main.testMain(new String[]{
      "--persistence.datasource.url=wibble"
      , "--baseConfigPath=target/query-engine/samples-mainit"
      , "- query-engine.tracing.serviceName=query-engine2"
      , "- query-engine.tracing.protocol=otlphttp"
      , "- query-engine.tracing.url=http://jaeger:4318/v1/traces"
      , "--jwt.acceptableIssuerRegexes[0]=.*"
      , "--jwt.defaultJwksCacheDuration=PT1M"
      , "--jwt.jwksEndpoints[0]=http://localhost/jwks"
      , "--pipelineCache.maxDurationMs=0"
      , "--pipelineCache.purgePeriodMs=10"
      , "--session.requireSession=false"
      , "--session.oauth.GitHub.logoUrl=https://upload.wikimedia.org/wikipedia/commons/c/c2/GitHub_Invertocat_Logo.svg"
      , "--session.oauth.GitHub.authorizationEndpoint=https://github.com/login/oauth/authorize"
      , "--session.oauth.GitHub.tokenEndpoint=https://github.com/login/oauth/access_token"
      , "--session.oauth.GitHub.credentials.id=bdab017f4732085a51f9"
      , "--session.oauth.GitHub.credentials.secret=" + System.getProperty("queryEngineGithubSecret")
      , "--session.oauth.GitHub.pkce=false"
      , "--tracing.protocol=zipkin"
      , "--tracing.sampler=ratio"
      , "--tracing.sampleRatio=0.5"
      , "--tracing.url=http://nonexistent/zipkin"
    }, stdout);
    assertEquals(0, stdoutStream.size());
    
    main.shutdown();
  }
  
  @Test
  public void testMainDaemon() throws Exception {
    logger.debug("Running testMainDaemon");
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
      , "--session.requireSession=false"
      , "--session.oauth.GitHub.logoUrl=https://upload.wikimedia.org/wikipedia/commons/c/c2/GitHub_Invertocat_Logo.svg"
      , "--session.oauth.GitHub.authorizationEndpoint=https://github.com/login/oauth/authorize"
      , "--session.oauth.GitHub.tokenEndpoint=https://github.com/login/oauth/access_token"
      , "--session.oauth.GitHub.credentials.id=bdab017f4732085a51f9"
      , "--session.oauth.GitHub.credentials.secret=" + System.getProperty("queryEngineGithubSecret")
      , "--session.oauth.GitHub.pkce=false"
      , "--tracing.protocol=otlphttp"
      , "--tracing.sampler=alwaysOn"
      , "--tracing.url=http://nonexistent/otlphttp"
    }, stdout);
    assertEquals(0, stdoutStream.size());
    
    RestAssured.port = main.getPort();
    
    given()
            .log().all()
            .get("/query/sub1/sub2/TemplatedJsonToPipelineIT.json.vm")
            .then()
            .statusCode(400)
            .log().all()
            ;
    
    given()
            .log().all()
            .post("/query/sub1/sub2/TemplatedJsonToPipelineIT.json.vm")
            .then()
            .statusCode(404)
            .log().all()
            ;
    
    // This isn't a short path because the router stop it reaching the QueryRouter
    given()
            .log().all()
            .post("/query")
            .then()
            .statusCode(404)
            .log().all()
            ;
    
     given()
            .log().all()
            .get("/query/bob")
            .then()
            .statusCode(404)
            .log().all()
            ;
    
     given()
            .log().all()
            .get("/ui/index.html")
            .then()
            .statusCode(200)
            .log().all()
            ;
    
     given()
            .log().all()
            .get("/ui")
            .then()
            .statusCode(200)
            .log().all()
            ;
    
     given()
            .config(RestAssuredConfig.config().redirect(redirectConfig().followRedirects(false)))
            .log().all()
            .get("/")
            .then()
            .log().all()
            .statusCode(307)
            .header("Location", "/ui/")
            ;
     
     given()
            .log().all()
            .get("/manage")
            .then()
            .log().all()
            .statusCode(200)
            .body(equalTo("{\"location\":\"http://localhost:" + mgmtPort + "/manage\"}"))
            ;
    
     String manageEndpointsString = given()
            .log().all()
            .get(URI.create("http://localhost:" + mgmtPort + "/manage"))
            .then()
            .statusCode(200)
            .log().all()
            .extract().body().asString()
            ;
    ArrayNode manageEndpointsJson = Json.decodeValue(manageEndpointsString, ArrayNode.class);
    assertEquals(3, manageEndpointsJson.size());
    
    given()
            .config(RestAssuredConfig.config().redirect(redirectConfig().followRedirects(false)))
            .log().all()
            .get("/api")
            .then()
            .statusCode(301)
            .log().all()
            .header("Location", equalTo("/openapi"))
            .extract().body().asString()
            ;

    String nonProfile = given()
            .log().all()
            .get("/api/session/profile")
            .then()
            .statusCode(200)
            .log().all()
            .extract().body().asString()
            ;
    assertEquals("{\"version\":\"" + Version.MAVEN_PROJECT_NAME + " " + Version.MAVEN_PROJECT_VERSION + "\"}", nonProfile);
            
    String authConfig = given()
            .log().all()
            .get("/api/auth-config")
            .then()
            .statusCode(200)
            .log().all()
            .extract().body().asString()
            ;
    assertEquals("[{\"name\":\"GitHub\",\"logo\":\"https://upload.wikimedia.org/wikipedia/commons/c/c2/GitHub_Invertocat_Logo.svg\"}]", authConfig);
            
    
    main.shutdown();
  }
  
  @Test
  public void testAuthRequired() throws Exception {
    logger.debug("Running testAuthRequired");
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
      , "--jwt.jwksEndpoints[0]=http://localhost/jwks"
      , "--logging.jsonFormat=true"
      , "--sampleDataLoads[0].url=" + postgres.getVertxUrl()
      , "--sampleDataLoads[0].adminUser.username=" + postgres.getUser()
      , "--sampleDataLoads[0].adminUser.password=" + postgres.getPassword()
      , "--sampleDataLoads[1].url="
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
      , "--tracing.protocol=otlphttp"
      , "--tracing.sampler=parent"
      , "--tracing.parentSampler=alwaysOn"
      , "--tracing.url=http://nonexistent/otlphttp"
    }, stdout);
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
