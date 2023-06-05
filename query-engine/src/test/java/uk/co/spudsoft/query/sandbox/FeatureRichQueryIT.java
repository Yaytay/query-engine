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
package uk.co.spudsoft.query.sandbox;

import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import java.io.File;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.main.Main;
import uk.co.spudsoft.query.testcontainers.ServerProviderPostgreSQL;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import uk.co.spudsoft.query.testcontainers.ServerProviderMySQL;

/**
 * Note that this set of tests requires the sample data to be loaded, but relies on the "loadSampleData" flag to make it happen.
 * When running with the full set of tests this won't actually stress that flag because others tests may have already
 * loaded the sample data.
 * 
 * Furthermore this test requires that the MySQL instance is listening on port 2001, which it won't be if it is
 * run dynamically.
 * Thus, usually, this test is disabled.
 * 
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class FeatureRichQueryIT {
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  private static final ServerProviderMySQL mysql = new ServerProviderMySQL().init();
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(FeatureRichQueryIT.class);
  
  @BeforeAll
  public static void createDirs(Vertx vertx) {
    File paramsDir = new File("target/query-engine");
    paramsDir.mkdirs();
    new File("target/classes/samples/sub1/sub3").mkdirs();
  }
  
  @Test
  public void testQuery() throws Exception {
    Main main = new Main();
    String baseConfigDir = "target/query-engine/samples";
    Main.prepareBaseConfigPath(new File(baseConfigDir));
    main.testMain(new String[]{
      "audit.datasource.url=jdbc:" + mysql.getUrl()
      , "audit.datasource.adminUser.username=" + mysql.getUser()
      , "audit.datasource.adminUser.password=" + mysql.getPassword()
      , "audit.datasource.user.username=" + mysql.getUser()
      , "audit.datasource.user.password=" + mysql.getPassword()
      , "audit.retryLimit=100"
      , "audit.retryIncrementMs=500"
      , "baseConfigPath=" + baseConfigDir
      , "vertxOptions.eventLoopPoolSize=5"
      , "vertxOptions.workerPoolSize=5"
      , "vertxOptions.tracingOptions.serviceName=Query-Engine"
      , "httpServerOptions.tracingPolicy=ALWAYS"
      , "pipelineCache.maxDurationMs=60000"
      , "logging.jsonFormat=false"
      , "zipkin.baseUrl=http://localhost/wontwork"
      , "jwt.acceptableIssuerRegexes[0]=.*"
      , "jwt.defaultJwksCacheDuration=PT1M"
      , "sampleDataLoads[0].url=" + postgres.getUrl()
      , "sampleDataLoads[0].adminUser.username=" + postgres.getUser()
      , "sampleDataLoads[0].adminUser.password=" + postgres.getPassword()
      , "sampleDataLoads[1].url=" + mysql.getUrl()
      , "sampleDataLoads[1].user.username=" + mysql.getUser()
      , "sampleDataLoads[1].user.password=" + mysql.getPassword()
      , "sampleDataLoads[2].url=sqlserver://localhost:1234/test"
      , "sampleDataLoads[2].adminUser.username=sa"
      , "sampleDataLoads[2].adminUser.password=unknown"
      , "sampleDataLoads[3].url=wibble"
    });
    
    RestAssured.port = main.getPort();
    
    String body = given()
            .log().all()
            .get("/openapi.yaml")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("openapi: 3.1.0"));
    assertThat(body, containsString("SpudSoft Query Engine"));
    
    body = given()
            .queryParam("minDate", "1971-05-06")
            .queryParam("_fmt", "tab")
            .accept("text/html")
            .log().all()
            .get("/query/demo/FeatureRichExample")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("\"dataId\"\t\"instant\""));
    
    body = given()
            .queryParam("minDate", "2971-05-06")
            .queryParam("_fmt", "json")
            .accept("text/html")
            .log().all()
            .get("/query/demo/FeatureRichExample")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, equalTo("[]"));
    
    body = given()
            .queryParam("minDate", "1971-05-06")
            .queryParam("_fmt", "xlsx")
            .accept("text/html")
            .log().all()
            .get("/query/demo/FeatureRichExample")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("PK"));
        
    main.shutdown();
  }
  
}