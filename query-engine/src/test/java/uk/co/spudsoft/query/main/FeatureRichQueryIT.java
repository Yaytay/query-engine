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
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.testcontainers.ServerProviderPostgreSQL;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
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
public class FeatureRichQueryIT {
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  private static final ServerProviderMySQL mysql = new ServerProviderMySQL().init();
  
  private static final String CONFS_DIR = "target/query-engine/samples-" + MethodHandles.lookup().lookupClass().getSimpleName().toLowerCase();
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(FeatureRichQueryIT.class);
  
  @BeforeAll
  public void createDirs() {
    File confsDir = new File(CONFS_DIR);
    FileUtils.deleteQuietly(confsDir);
    confsDir.mkdirs();
  }
  
  @Test
  public void testQuery() throws Exception {

    // Audit records should all have been sorted by main.shutdown
    assertTrue(TestHelpers.getDirtyAudits(logger, mysql.getJdbcUrl(), mysql.getUser(), mysql.getPassword()).isEmpty());
    
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
    }, stdout, System.getenv());
    
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
            .queryParam("maxId", "20")
            .queryParam("_fmt", "tab")
            .accept("text/html")
            .log().all()
            .get("/query/demo/FeatureRichExample")
            .then()
            .log().all()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("\"dataId\"\t\"instant\""));
    
    body = given()
            .queryParam("minDate", "1971-05-06")
            .queryParam("maxId", "20")
            .queryParam("_fmt", "metajson")
            .accept("text/html")
            .log().all()
            .get("/query/demo/FeatureRichExample")
            .then()
            .log().all()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("{\"meta\":{\"name\":\"Feature Rich Example\",\"description\":\"A complex pipeline that tries to demonstrate as many features as I can cram into a single pipeline.\",\"fields\":{\"dataId\":\"int\",\"instant\":\"datetime\",\"colour\":\"string\",\"value\":\"string\",\"children\":\"string\",\"DateField\":\"date\",\"TimeField\":\"time\",\"DateTimeField\":\"datetime\",\"LongField\":\"int\",\"DoubleField\":\"double\",\"BoolField\":\"bool\",\"TextField\":\"string\",\"child2\":\"string\",\"child3\":\"string\",\"child4\":\"string\",\"child5\":\"string\",\"child6\":\"string\"}},\"data\":[{\"dataId\":1,\"instant\":\"1971-05-07T03:00\",\"colour\":\"antiquewhite\",\"value\":\"first\",\"children\":\"one\",\"DateField\":\"2023-05-05\",\"TimeField\":null,\"DateTimeField\":null,\"LongField\":null,\"DoubleField\":null,\"BoolField\":null,\"TextField\":null,\"child2\":\"one\",\"child3\":\"one\",\"child4\":\"b0f56ab83929aeda\",\"child5\":\"one\",\"child6\":\"b0f56ab83929aeda\"},"));
    
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
            .queryParam("minDate", "2971-05-06")
            .queryParam("_fmt", "json")
            .queryParam("clientIp", "192.168.1.1")
            .accept("text/html")
            .log().all()
            .get("/query/demo/FeatureRichExample")
            .then()
            .log().ifError()
            .statusCode(400)
            .extract().body().asString();
    
    assertThat(body, equalTo("The argument \"clientIp\" is not permitted."));
    
    byte[] bodyBytes = given()
            .queryParam("minDate", "1971-05-06")
            .queryParam("_fmt", "xlsx")
            .accept("text/html")
            .log().all()
            .get("/query/demo/FeatureRichExample")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asByteArray();
    
    assertThat(bodyBytes[0], equalTo((byte) 80));
    assertThat(bodyBytes[1], equalTo((byte) 75));
        
    try (FileOutputStream fos = new FileOutputStream("target/temp/FeatureRichQueryIT.xlsx")) {
      fos.write(bodyBytes);
    }
    
    main.shutdown();

    // Audit records should all have been sorted by main.shutdown
    assertTrue(TestHelpers.getDirtyAudits(logger, mysql.getJdbcUrl(), mysql.getUser(), mysql.getPassword()).isEmpty());
  }
  
}
