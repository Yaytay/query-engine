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
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.testcontainers.ServerProviderPostgreSQL;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EmptyQueryIT {
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  private static final ServerProviderMySQL mysql = new ServerProviderMySQL().init();
  
  private static final String CONFS_DIR = "target/query-engine/samples-" + MethodHandles.lookup().lookupClass().getSimpleName().toLowerCase();
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(EmptyQueryIT.class);
  
  @BeforeAll
  public void createDirs() {
    File confsDir = new File(CONFS_DIR);
    FileUtils.deleteQuietly(confsDir);
    confsDir.mkdirs();

    File outDir = new File("target/temp/EmptyDataIT");
    outDir.mkdirs();
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
    }, stdout, System.getenv());
    
    RestAssured.port = main.getPort();
    
    String body;
    byte[] bodyBytes;

    body = given()
            .config(RestAssuredConfig.config().httpClient(HttpClientConfig.httpClientConfig().setParam("http.socket.timeout",10000)))
            .queryParam("key", "PostgreSQL")
            .queryParam("port", postgres.getPort())
            .queryParam("_fmt", "tab")
            .log().all()
            .get("/query/sub1/sub2/EmptyDataIT")
            .then()
            .log().all()
            .statusCode(200)
            .extract().body().asString();

    assertThat(body, startsWith("\"colourId\"\t\"name\"\t\"hex\""));
    assertThat(body, not(containsString("\t\t\t\t\t\t\t")));

    body = given()
            .config(RestAssuredConfig.config().httpClient(HttpClientConfig.httpClientConfig().setParam("http.socket.timeout",10000)))
            .queryParam("key", "PostgreSQL")
            .queryParam("port", postgres.getPort())
            .queryParam("_fmt", "html")
            .log().all()
            .get("/query/sub1/sub2/EmptyDataIT")
            .then()
            .log().all()
            .statusCode(200)
            .extract().body().asString();

    assertThat(body, equalTo("<table class=\"qetable\"><thead>\n<tr class=\"header\"><th class=\"header oddCol\" >colourId</th><th class=\"header evenCol\" >name</th><th class=\"header oddCol\" >hex</th></tr>\n</thead><tbody>\n</tbody></table>"));
    assertThat(body, not(containsString("\t\t\t\t\t\t\t")));

    bodyBytes = given()
            .config(RestAssuredConfig.config().httpClient(HttpClientConfig.httpClientConfig().setParam("http.socket.timeout",10000)))
            .queryParam("key", "PostgreSQL")
            .queryParam("port", postgres.getPort())
            .queryParam("_fmt", "xlsx")
            .log().all()
            .get("/query/sub1/sub2/EmptyDataIT")
            .then()
            .log().all()
            .statusCode(200)
            .extract().body().asByteArray();

    try (OutputStream fos = new FileOutputStream("target/temp/EmptyDataIT/EmptyDataIT.xlsx")) {
      fos.write(bodyBytes);
    }

    body = given()
            .config(RestAssuredConfig.config().httpClient(HttpClientConfig.httpClientConfig().setParam("http.socket.timeout",10000)))
            .queryParam("minDate", "2971-05-06")
            .queryParam("maxId", "20")
            .queryParam("_fmt", "tab")
            .accept("text/html")
            .log().all()
            .get("/query/demo/FeatureRichExample")
            .then()
            .log().all()
            .statusCode(200)
            .extract().body().asString();

    assertThat(body, equalTo("\"dataId\"\t\"instant\"\t\"colour\"\t\"value\"\t\"children\"\t\"DateField\"\t\"TimeField\"\t\"DateTimeField\"\t\"LongField\"\t\"DoubleField\"\t\"BoolField\"\t\"TextField\"\t\"child2\"\t\"child3\"\t\"child4\"\t\"child5\"\t\"child6\"\n"));
    assertThat(body, not(containsString("\t\t")));
    
    
    body = given()
            // .config(RestAssuredConfig.config().httpClient(HttpClientConfig.httpClientConfig().setParam("http.socket.timeout",10000)))
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

    main.shutdown();

    // Audit records should all have been sorted by main.shutdown
    assertTrue(TestHelpers.getDirtyAudits(logger, mysql.getJdbcUrl(), mysql.getUser(), mysql.getPassword()).isEmpty());
  }
  
}
