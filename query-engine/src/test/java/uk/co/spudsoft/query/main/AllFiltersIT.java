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
import io.vertx.junit5.VertxExtension;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import org.apache.commons.io.FileUtils;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.testcontainers.ServerProviderPostgreSQL;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.TestInstance;
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AllFiltersIT {
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  private static final ServerProviderMySQL mysql = new ServerProviderMySQL().init();
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(AllFiltersIT.class);

  private static final String CONFS_DIR = "target/query-engine/samples-" + MethodHandles.lookup().lookupClass().getSimpleName().toLowerCase();
  
  @BeforeAll
  public void createDirs() {
    File confsDir = new File(CONFS_DIR);
    FileUtils.deleteQuietly(confsDir);
    confsDir.mkdirs();
  }
      
  @Test
  public void testQuery() throws Exception {
    Main main = new Main();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    main.testMain(new String[]{
      "--persistence.datasource.url=" + mysql.getJdbcUrl()
      , "--persistence.datasource.adminUser.username=" + mysql.getUser()
      , "--persistence.datasource.adminUser.password=" + mysql.getPassword()
      , "--persistence.datasource.user.username=" + mysql.getUser()
      , "--persistence.datasource.user.password=" + mysql.getPassword()
      , "--persistence.retryLimit=100"
      , "--baseConfigPath=" + CONFS_DIR
      , "--vertxOptions.eventLoopPoolSize=5"
      , "--vertxOptions.workerPoolSize=5"
      , "--httpServerOptions.tracingPolicy=ALWAYS"
      , "--pipelineCache.maxDuration=PT10M"
      , "--logging.jsonFormat=false"
      // , "--logging.level.uk\\\\.co\\\\.spudsoft\\\\.query\\\\.exec\\\\.sources=TRACE"
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
      , "--secrets.AllFiltersProtectedCredentials.username=" + mysql.getUser()
      , "--secrets.AllFiltersProtectedCredentials.password=" + mysql.getPassword()
      , "--secrets.AllFiltersProtectedCredentials.condition=true"
      , "--outputCacheDir=target/temp/" + this.getClass().getSimpleName() + "/cache"
      , "--securityHeaders.xFrameOptions=SAMEORIGIN"
    }, stdout, System.getenv());
    
    RestAssured.port = main.getPort();
    
    String body = given()
            .queryParam("minDate", "1971-05-06")
            .queryParam("maxId", "20")
            .get("/query/sub1/sub2/AllDynamicIT.tsv")
            .then()
            .header("Content-Disposition", equalTo("attachment; filename=\"dynamism.txt\""))
            .header("X-Frame-Options", equalTo("SAMEORIGIN"))
            .header("Referrer-Policy", "same-origin")
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("\"dataId\"\t\"instant\""));
    assertThat(body, not(containsString("\t\t\t\t\t\t\t")));
    assertThat(body, containsString("BoolField"));
    assertThat(body, containsString("TextField"));
    assertThat(body, containsString("\"seven\""));
    int rows1 = body.split("\n").length;
    assertEquals(21, rows1);
            
    body = given()
            .queryParam("minDate", "1971-05-06")
            .queryParam("maxId", "20")
            .queryParam("_map", "BoolField: TextField:")
            .get("/query/sub1/sub2/AllDynamicIT.tsv")
            .then()
            .log().ifError()
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
            .queryParam("_limit", "12")
            .get("/query/sub1/sub2/AllDynamicIT.tsv")
            .then()
            .log().ifError()
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
            .get("/query/sub1/sub2/AllDynamicIT.tsv")
            .then()
            .log().ifError()
            .statusCode(400)
            .extract().body().asString();
    
    assertThat(body, equalTo("Invalid argument to _limit filter, should be an integer"));

    body = given()
            .queryParam("minDate", "1971-05-06")
            .queryParam("maxId", "20")
            .queryParam("_query", "dataId==4")
            .get("/query/sub1/sub2/AllDynamicIT.tsv")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("\"dataId\"\t\"instant\""));
    assertThat(body, not(containsString("\t\t\t\t\t\t\t")));
    assertThat(body, containsString("BoolField"));
    assertThat(body, containsString("TextField"));
    assertThat(body, containsString("4\t\"1971-05-10T12:00\""));
    int rows5 = body.split("\n").length;
    assertEquals(2, rows5);

    body = given()
            .queryParam("minDate", "1971-05-06")
            .queryParam("maxId", "20")
            .queryParam("_query", "colour==beige and dataId=le=40")
            .get("/query/sub1/sub2/AllDynamicIT.tsv")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    int rows6 = body.split("\n").length;
    assertThat(body, startsWith("\"dataId\"\t\"instant\""));
    assertThat(body, not(containsString("\t\t\t\t\t\t\t")));
    assertThat(body, containsString("BoolField"));
    assertThat(body, containsString("TextField"));
    assertThat(body, containsString("5\t\"1971-05-11T15:00\""));
    assertEquals(2, rows6);

    body = given()
            .queryParam("minDate", "1971-05-06")
            .queryParam("maxId", "20")
            .queryParam("_query", "colour==beige;dataId=le=40")
            .get("/query/sub1/sub2/AllDynamicIT.tsv")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    int rows7 = body.split("\n").length;
    assertThat(body, startsWith("\"dataId\"\t\"instant\""));
    assertThat(body, not(containsString("\t\t\t\t\t\t\t")));
    assertThat(body, containsString("BoolField"));
    assertThat(body, containsString("TextField"));
    assertThat(body, not(containsString("5\t\"1971-05-11T15:00\"")));
    assertEquals(1, rows7);


    body = given()
            .queryParam("minDate", "1971-05-06")
            .queryParam("maxId", "20")
            .queryParam("_query", "colour==\"beige\";dataId=le=40")
            .get("/query/sub1/sub2/AllDynamicIT.tsv")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    int rows8 = body.split("\n").length;
    assertThat(body, startsWith("\"dataId\"\t\"instant\""));
    assertThat(body, not(containsString("\t\t\t\t\t\t\t")));
    assertThat(body, containsString("BoolField"));
    assertThat(body, containsString("TextField"));
    assertThat(body, not(containsString("5\t\"1971-05-11T15:00\"")));
    assertEquals(1, rows8);
    
    
    body = given()
            .queryParam("minDate", "1971-05-06")
            .queryParam("maxId", "20")
            .queryParam("_query", "bob")
            .get("/query/sub1/sub2/AllDynamicIT.tsv")
            .then()
            .log().ifError()
            .statusCode(400)
            .extract().body().asString();
    
    assertThat(body, equalTo("Invalid argument to _query filter, should be a valid RSQL expression"));

    
    body = given()
            .queryParam("minDate", "1971-05-06")
            .queryParam("maxId", "20")
            .queryParam("_offset", "bob")
            .get("/query/sub1/sub2/AllDynamicIT.tsv")
            .then()
            .log().ifError()
            .statusCode(400)
            .extract().body().asString();
    
    assertThat(body, equalTo("Invalid argument to _offset filter, should be an integer"));
    
    body = given()
            .queryParam("minDate", "1971-05-06")
            .queryParam("maxId", "20")
            .queryParam("_offset", "0")
            .queryParam("_limit", "12")
            .get("/query/sub1/sub2/AllDynamicIT.tsv")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();  
    
    assertThat(body, startsWith("\"dataId\"\t\"instant\""));
    assertThat(body, not(containsString("\t\t\t\t\t\t\t")));
    assertThat(body, containsString("BoolField"));
    assertThat(body, containsString("TextField"));
    assertThat(body, containsString("\"first\"\t\"one\""));
    assertThat(body, containsString("\"second\"\t\"two,four\""));
    int rows9 = body.split("\n").length;
    assertEquals(13, rows9);


    body = given()
            .queryParam("minDate", "1971-05-06")
            .queryParam("maxId", "20")
            .queryParam("_offset", "1")
            .queryParam("_limit", "12")
            .get("/query/sub1/sub2/AllDynamicIT.tsv")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("\"dataId\"\t\"instant\""));
    assertThat(body, not(containsString("\t\t\t\t\t\t\t")));
    assertThat(body, containsString("BoolField"));
    assertThat(body, containsString("TextField"));
    assertThat(body, not(containsString("\"first\"\t\"one\"")));
    assertThat(body, containsString("\"second\"\t\"two,four\""));
    int rows10 = body.split("\n").length;
    assertEquals(13, rows10);
    
    long start = System.currentTimeMillis();
    
    body = given()
            .get("/query/sub1/sub2/AllDynamicIT.tsv?minDate=1971-05-06&maxId=20&_limit=12&_offset=1")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("\"dataId\"\t\"instant\""));
    assertThat(body, not(containsString("\t\t\t\t\t\t\t")));
    assertThat(body, containsString("BoolField"));
    assertThat(body, containsString("TextField"));
    assertThat(body, not(containsString("\"first\"\t\"one\"")));
    assertThat(body, containsString("\"second\"\t\"two,four\""));
    int rows11 = body.split("\n").length;
    assertEquals(12, rows11);
    
    long end = System.currentTimeMillis();
    long duration1 = end - start;
    
    start = System.currentTimeMillis();
    
    String body2 = given()
            .get("/query/sub1/sub2/AllDynamicIT.tsv?minDate=1971-05-06&maxId=20&_limit=12&_offset=1")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertEquals(body, body2);
    
    end = System.currentTimeMillis();
    long duration2 = end - start;
    assertThat(duration1, greaterThan(duration2));
    
    start = System.currentTimeMillis();
    
    body = given()
            .get("/query/sub1/sub2/AllDynamicIT.tsv?minDate=1971-05-06&maxId=21")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("\"dataId\"\t\"instant\""));
    assertThat(body, not(containsString("\t\t\t\t\t\t\t")));
    assertThat(body, containsString("BoolField"));
    assertThat(body, containsString("TextField"));
    assertThat(body, containsString("\"first\"\t\"one\""));
    assertThat(body, containsString("\"second\"\t\"two,four\""));
    rows11 = body.split("\n").length;
    assertEquals(22, rows11);
    
    end = System.currentTimeMillis();
    duration1 = end - start;
    
    start = System.currentTimeMillis();
    
    body2 = given()
            .get("/query/sub1/sub2/AllDynamicIT.tsv?minDate=1971-05-06&maxId=21")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertEquals(body, body2);
    
    end = System.currentTimeMillis();
    duration2 = end - start;
    assertThat(duration1, greaterThanOrEqualTo(duration2));
    
    body = given()
            .queryParam("minDate", "1971-05-06")
            .queryParam("maxId", "20")
            .queryParam("_map", "")
            .get("/query/sub1/sub2/AllDynamicIT.tsv")
            .then()
            .log().ifError()
            .statusCode(400)
            .extract().body().asString();
    
    assertThat(body, equalTo("Invalid argument to _map filter, should be a space delimited list of relabels, each of which should be SourceLabel:NewLabel.  The new label cannot contain a colon or a space, if the new label is blank the field will be dropped - the source label may not be blank."));

    body = given()
            .queryParam("minDate", "1971-05-06")
            .queryParam("maxId", "20")
            .queryParam("_map", "bob")
            .get("/query/sub1/sub2/AllDynamicIT.tsv")
            .then()
            .log().ifError()
            .statusCode(400)
            .extract().body().asString();
    
    assertThat(body, equalTo("Invalid argument to _map filter, should be a space delimited list of relabels, each of which should be SourceLabel:NewLabel.  The new label cannot contain a colon or a space, if the new label is blank the field will be dropped - the source label may not be blank."));
    
    body = given()
            .queryParam("minDate", "1971-05-06")
            .queryParam("maxId", "20")
            .queryParam("_map", "BoolField:YesNo TimeField:When")
            .get("/query/sub1/sub2/AllDynamicIT.tsv")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("\"dataId\"\t\"instant\"\t\"colour\"\t\"value\"\t\"children\"\t\"DateField\"\t\"When\"\t\"DateTimeField\"\t\"LongField\"\t\"DoubleField\"\t\"YesNo\"\t\"TextField\""));
    assertThat(body, not(containsString("\t\t\t\t\t\t\t")));
    assertThat(body, containsString("YesNo"));
    assertThat(body, containsString("TextField"));
    assertThat(body, containsString("\"first\"\t\"one\""));
    assertThat(body, containsString("\"second\"\t\"two,four\""));
    
    body = given()
            .queryParam("minDate", "1971-05-06")
            .queryParam("maxId", "20")
            .queryParam("_sort", "-value")
            .get("/query/sub1/sub2/AllDynamicIT.tsv")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("\"dataId\"\t\"instant\"\t\"colour\"\t\"value\"\t\"children\"\t\"DateField\"\t\"TimeField\"\t\"DateTimeField\"\t\"LongField\"\t\"DoubleField\"\t\"BoolField\"\t\"TextField\""));
    assertThat(body, not(containsString("\t\t\t\t\t\t\t")));
    logger.debug("Output: {}", body);
    
    main.shutdown();
    // Audit records should all have been sorted by main.shutdown
    assertTrue(TestHelpers.getDirtyAudits(logger, mysql.getJdbcUrl(), mysql.getUser(), mysql.getPassword()).isEmpty());
  }
  
}
