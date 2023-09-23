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
import java.io.IOException;
import java.io.PrintStream;
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
public class EmptyQueryIT {
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  private static final ServerProviderMySQL mysql = new ServerProviderMySQL().init();
  
  private static final String BASE_CONFIG_DIR = "target/query-engine/samples-emptyqueryit";
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(EmptyQueryIT.class);
  
  @BeforeAll
  public static void createDirvs() throws IOException {
    File paramsDir = new File(BASE_CONFIG_DIR);
    if (paramsDir.exists()) {
      try {
        FileUtils.deleteDirectory(paramsDir);
      } catch (Throwable ex) {
      }
    }
    paramsDir.mkdirs();    
  }
  
  @Test
  public void testQuery() throws Exception {
    Main main = new Main();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    main.testMain(new String[]{
      "--audit.datasource.url=jdbc:" + mysql.getUrl()
      , "--audit.datasource.adminUser.username=" + mysql.getUser()
      , "--audit.datasource.adminUser.password=" + mysql.getPassword()
      , "--audit.datasource.user.username=" + mysql.getUser()
      , "--audit.datasource.user.password=" + mysql.getPassword()
      , "--audit.retryLimit=100"
      , "--audit.retryIncrementMs=500"
      , "--baseConfigPath=" + BASE_CONFIG_DIR
      , "--vertxOptions.eventLoopPoolSize=5"
      , "--vertxOptions.workerPoolSize=5"
      , "--vertxOptions.tracingOptions.serviceName=Query-Engine"
      , "--httpServerOptions.tracingPolicy=ALWAYS"
      , "--pipelineCache.maxDurationMs=60000"
      , "--logging.jsonFormat=false"
      , "--zipkin.baseUrl=http://localhost/wontwork"
      , "--jwt.acceptableIssuerRegexes[0]=.*"
      , "--jwt.defaultJwksCacheDuration=PT1M"
      , "--sampleDataLoads[0].url=" + postgres.getUrl()
      , "--sampleDataLoads[0].adminUser.username=" + postgres.getUser()
      , "--sampleDataLoads[0].adminUser.password=" + postgres.getPassword()
      , "--sampleDataLoads[1].url=" + mysql.getUrl()
      , "--sampleDataLoads[1].user.username=" + mysql.getUser()
      , "--sampleDataLoads[1].user.password=" + mysql.getPassword()
    }, stdout);
    
    RestAssured.port = main.getPort();
    
    String body;

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

    assertThat(body, equalTo("\"dataId\"\t\"instant"));
    assertThat(body, not(containsString("\t\t\t\t\t\t\t")));

    body = given()
            .config(RestAssuredConfig.config().httpClient(HttpClientConfig.httpClientConfig().setParam("http.socket.timeout",10000)))
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
  }
  
}
