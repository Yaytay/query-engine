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

import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.testcontainers.ServerProviderPostgreSQL;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
public class AllDynamicIT {
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  private static final ServerProviderMySQL mysql = new ServerProviderMySQL().init();
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(AllDynamicIT.class);
  
  @BeforeAll
  public static void createDirs(Vertx vertx) {
    File paramsDir = new File("target/query-engine/samples-featurerichqueryit");
    paramsDir.mkdirs();
    new File("target/classes/samples/sub1/sub3").mkdirs();
  }
  
  @Test
  public void testQuery() throws Exception {
    Main main = new Main();
    String baseConfigDir = "target/query-engine/samples-alldynamicit";
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    main.testMain(new String[]{
      "--persistence.datasource.url=" + mysql.getJdbcUrl()
      , "--persistence.datasource.adminUser.username=" + mysql.getUser()
      , "--persistence.datasource.adminUser.password=" + mysql.getPassword()
      , "--persistence.datasource.user.username=" + mysql.getUser()
      , "--persistence.datasource.user.password=" + mysql.getPassword()
      , "--persistence.retryLimit=100"
      , "--persistence.retryIncrementMs=500"
      , "--baseConfigPath=" + baseConfigDir
      , "--vertxOptions.eventLoopPoolSize=5"
      , "--vertxOptions.workerPoolSize=5"
      , "--vertxOptions.tracingOptions.serviceName=Query-Engine"
      , "--httpServerOptions.tracingPolicy=ALWAYS"
      , "--pipelineCache.maxDurationMs=60000"
      , "--logging.jsonFormat=false"
      , "--zipkin.baseUrl=http://localhost/wontwork"
      , "--jwt.acceptableIssuerRegexes[0]=.*"
      , "--jwt.defaultJwksCacheDuration=PT1M"
      , "--sampleDataLoads[0].url=" + postgres.getVertxUrl()
      , "--sampleDataLoads[0].adminUser.username=" + postgres.getUser()
      , "--sampleDataLoads[0].adminUser.password=" + postgres.getPassword()
      , "--sampleDataLoads[1].url=" + mysql.getVertxUrl()
      , "--sampleDataLoads[1].user.username=" + mysql.getUser()
      , "--sampleDataLoads[1].user.password=" + mysql.getPassword()
      , "--sampleDataLoads[2].url=sqlserver://localhost:1234/test"
      , "--sampleDataLoads[2].adminUser.username=sa"
      , "--sampleDataLoads[2].adminUser.password=unknown"
      , "--sampleDataLoads[3].url=wibble"
    }, stdout);
    
    RestAssured.port = main.getPort();
    
    String body = given()
            .queryParam("minDate", "1971-05-06")
            .queryParam("maxId", "20")
            .log().all()
            .get("/query/sub1/sub2/AllDynamicIT.tsv")
            .then()
            .log().all()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("\"dataId\"\t\"instant\""));
    assertThat(body, not(containsString("\t\t\t\t\t\t\t")));
    assertThat(body, containsString("BoolField"));
    assertThat(body, containsString("TextField"));
    int rows1 = body.split("\n").length;
    assertEquals(21, rows1);
            
    body = given()
            .queryParam("minDate", "1971-05-06")
            .queryParam("maxId", "20")
            .queryParam("_without", "BoolField TextField")
            .log().all()
            .get("/query/sub1/sub2/AllDynamicIT.tsv")
            .then()
            .log().all()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("\"dataId\"\t\"instant\""));
    assertThat(body, not(containsString("\t\t\t\t\t\t\t")));
    assertThat(body, not(containsString("BoolField")));
    assertThat(body, not(containsString("TextField")));
    int rows2 = body.split("\n").length;
    assertEquals(21, rows2);

    body = given()
            .queryParam("minDate", "1971-05-06")
            .queryParam("maxId", "20")
            .queryParam("_without", "")
            .log().all()
            .get("/query/sub1/sub2/AllDynamicIT.tsv")
            .then()
            .log().all()
            .statusCode(400)
            .extract().body().asString();
    
    assertThat(body, equalTo("Invalid argument to _without filter, should be a space delimited list of fields"));

    body = given()
            .queryParam("minDate", "1971-05-06")
            .queryParam("maxId", "20")
            .queryParam("_limit", "12")
            .log().all()
            .get("/query/sub1/sub2/AllDynamicIT.tsv")
            .then()
            .log().all()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("\"dataId\"\t\"instant\""));
    assertThat(body, not(containsString("\t\t\t\t\t\t\t")));
    assertThat(body, containsString("BoolField"));
    assertThat(body, containsString("TextField"));
    int rows4 = body.split("\n").length;
    assertEquals(13, rows4);

    body = given()
            .queryParam("minDate", "1971-05-06")
            .queryParam("maxId", "20")
            .queryParam("_limit", "bob")
            .log().all()
            .get("/query/sub1/sub2/AllDynamicIT.tsv")
            .then()
            .log().all()
            .statusCode(400)
            .extract().body().asString();
    
    assertThat(body, equalTo("Invalid argument to _limit filter, should be an integer"));

    main.shutdown();
  }
  
}