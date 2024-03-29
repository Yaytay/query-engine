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
import static io.restassured.config.RedirectConfig.redirectConfig;
import io.restassured.config.RestAssuredConfig;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * A set of tests that do not actually do any querying.
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class MainIT {
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(MainIT.class);
  
  @BeforeAll
  public static void createDirs(Vertx vertx) {
    File paramsDir = new File("target/query-engine/samples-mainit");
    paramsDir.mkdirs();
  }
    
  @Test
  public void testBadAudit() throws Exception {
    logger.debug("Running testBadAudit");
    Main main = new DesignMain();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    GlobalOpenTelemetry.resetForTest();
    main.testMain(new String[]{
      "--persistence.datasource.url=wibble"
      , "--baseConfigPath=target/query-engine/samples-mainit"
      , "--vertxOptions.tracingOptions.type=io.vertx.tracing.zipkin.ZipkinTracingOptions"
      , "--vertxOptions.tracingOptions.serviceName=Query-Engine"
      , "--jwt.acceptableIssuerRegexes[0]=.*"
      , "--jwt.defaultJwksCacheDuration=PT1M"
      , "--pipelineCache.maxDurationMs=0"
      , "--pipelineCache.purgePeriodMs=10"
      , "--managementEndpoints[0]=up"
      , "--managementEndpoints[2]=prometheus"
      , "--managementEndpoints[3]=threads"
      , "--session.requireSession=false"
      , "--session.oauth.GitHub.logoUrl=https://upload.wikimedia.org/wikipedia/commons/c/c2/GitHub_Invertocat_Logo.svg"
      , "--session.oauth.GitHub.authorizationEndpoint=https://github.com/login/oauth/authorize"
      , "--session.oauth.GitHub.tokenEndpoint=https://github.com/login/oauth/access_token"
      , "--session.oauth.GitHub.credentials.id=bdab017f4732085a51f9"
      , "--session.oauth.GitHub.credentials.secret=" + System.getProperty("queryEngineGithubSecret")
      , "--session.oauth.GitHub.pkce=false"
    }, stdout);
    assertEquals(0, stdoutStream.size());
    
    main.shutdown();
  }
  
  @Test
  public void testMainDaemon() throws Exception {
    logger.debug("Running testMainDaemon");
    Main main = new DesignMain();
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
      , "--logging.jsonFormat=true"
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
            .get("/api/design/file/sub1/sub2/YamlToPipelineIT.yaml")
            .then()
            .statusCode(200)
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
    
    main.shutdown();
  }
  
}
