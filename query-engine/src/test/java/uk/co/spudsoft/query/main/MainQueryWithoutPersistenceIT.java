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
import static io.restassured.RestAssured.given;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.testcontainers.ServerProviderPostgreSQL;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import uk.co.spudsoft.query.testcontainers.ServerProviderMySQL;

/**
 * Note that this set of tests requires the sample data to be loaded, but relies on the "loadSampleData" flag to make it happen.
 * When running with the full set of tests this won't actually stress that flag because others tests may have already
 * loaded the sample data.
 * 
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class MainQueryWithoutPersistenceIT {
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  private static final ServerProviderMySQL mysql = new ServerProviderMySQL().init();
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(MainQueryWithoutPersistenceIT.class);
  
  @BeforeAll
  public static void createDirs(Vertx vertx) {
    File paramsDir = new File("target/query-engine/samples-mainqueryit");
    try {
      FileUtils.deleteDirectory(paramsDir);
    } catch (Throwable ex) {
    }
    paramsDir.mkdirs();
  }
  
  @Test
  public void testQuery() throws Exception {
    GlobalOpenTelemetry.resetForTest();
    Main main = new Main();
    String baseConfigDir = "target/query-engine/samples-mainqueryit";
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    main.testMain(new String[]{
      "--baseConfigPath=" + baseConfigDir
      , "--vertxOptions.eventLoopPoolSize=5"
      , "--vertxOptions.workerPoolSize=5"
      , "--pipelineCache.maxDuration=PT10M"
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
      , "--secrets.AllFiltersProtectedCredentials.username=" + mysql.getUser()
      , "--secrets.AllFiltersProtectedCredentials.password=" + mysql.getPassword()
      , "--secrets.AllFiltersProtectedCredentials.condition=true"
      , "--outputCacheDir=target/temp/" + this.getClass().getSimpleName() + "/cache"
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
    assertThat(body, not(containsString("empty")));
    
    body = given()
            .log().all()
            .get("/openapi.json")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, containsString("\"openapi\" : \"3.1.0\","));
    assertThat(body, containsString("SpudSoft Query Engine"));
    assertThat(body, not(containsString("empty")));
    
    body = given()
            .log().all()
            .get("/api/info/available")
            .then()
            .log().all()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("{\"name\":\"\",\"children\":[{\"name\":\"args\",\"children\":[{\"name\":\"Args00\",\"path\":\"args/Args00\",\"title\":\"No Arguments\",\"description\":\"Test pipeline that has no arguments\"},{\"name\":\"Args01\",\"path\":\"args/Args01\",\"title\":\"One Argument\",\"description\":\"Test pipeline that has 1 argument\"}"));
        
    body = given()
            .log().all()
            .get("/api/formio/demo/FeatureRichExample")
            .then()
            .log().all()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("{\"type\":\"form\""));
    assertThat(body, containsString("Output"));

    long start = System.currentTimeMillis();
    
    body = given()
            .log().all()
            .get("/query/sub1/sub2/AllDynamicIT.tsv?minDate=1971-05-06&maxId=20&_limit=12&_offset=1")
            .then()
            .log().all()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("\"dataId\"\t\"instant\""));
    assertThat(body, not(containsString("\t\t\t\t\t\t\t")));
    assertThat(body, containsString("BoolField"));
    assertThat(body, containsString("TextField"));
    assertThat(body, not(containsString("\"first\"\t\"one\"")));
    assertThat(body, containsString("\"second\"\t\"two,four\""));
    int rows8 = body.split("\n").length;
    assertEquals(12, rows8);
    
    long end = System.currentTimeMillis();
    long duration1 = end - start;
    
    start = System.currentTimeMillis();
    
    String body2 = given()
            .log().all()
            .get("/query/sub1/sub2/AllDynamicIT.tsv?minDate=1971-05-06&maxId=20&_limit=12&_offset=1")
            .then()
            .log().all()
            .statusCode(200)
            .extract().body().asString();
    
    assertEquals(body, body2);
    
    end = System.currentTimeMillis();
    long duration2 = end - start;
    assertThat(duration1, greaterThan(duration2));
    
    body = given()
            .queryParam("key", postgres.getName())
            .queryParam("port", postgres.getPort())
            .accept("text/html, application/xhtml+xml, image/webp, image/apng, application/xml; q=0.9, application/signed-exchange; v=b3; q=0.9, */*; q=0.8")
            .log().all()
            .get("/query/sub1/sub2/TemplatedJsonToPipelineIT")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("[{\"dataId\":1,\"instant\":\"1971-05-07T03:00\",\"ref\":\"antiquewhite\",\"value\":\"first\",\"children\":\"one\",\"DateField\":\"2023-05-05\",\"TimeField\":null,\"DateTimeField\":null,\"LongField\":null,\"DoubleField\":null,\"BoolField\":null,\"TextField\":null}"));
    
    body = given()
            .queryParam("key", mysql.getName())
            .queryParam("port", mysql.getPort())
            .log().all()
            .get("/query/sub1/sub2/TemplatedJsonToPipelineIT")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    // Note that MySQL doesn't do booleans
    assertThat(body, startsWith("[{\"dataId\":1,\"instant\":\"1971-05-07T03:00\",\"ref\":\"antiquewhite\",\"value\":\"first\",\"children\":\"one\",\"DateField\":\"2023-05-05\",\"TimeField\":null,\"DateTimeField\":null,\"LongField\":null,\"DoubleField\":null,\"BoolField\":null,\"TextField\":null},"));
    
    byte[] bodyBytes = given()
            .queryParam("key", postgres.getName())
            .queryParam("port", postgres.getPort())
            .accept("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .log().all()
            .get("/query/sub1/sub2/TemplatedYamlToPipelineIT")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asByteArray();
    
    assertThat(bodyBytes, notNullValue());
    
    body = given()
            .queryParam("key", postgres.getName())
            .queryParam("port", postgres.getPort())
            .log().all()
            .get("/query/sub1/sub2/TemplatedYamlToPipelineIT?_fmt=csv")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("1,\"1971-05-07T03:00\",\"antiquewhite\",\"first\",\""));
    
    body = given()
            .queryParam("key", postgres.getName())
            .queryParam("port", postgres.getPort())
            .log().all()
            .get("/query/sub1/sub2/TemplatedYamlToPipelineIT.tsv")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
        
    assertThat(body, startsWith("\"dataId\"\t\"instant\"\t\"ref\"\t\"value\"\t\"children\"\n1\t\"1971-05-07T03:00\"\t\"antiquewhite\"\t\"first\"\t\"one\""));
    
    body = given()
            .queryParam("key", postgres.getName())
            .queryParam("port", postgres.getPort())
            .log().all()
            .get("/query/sub1/sub2/TemplatedYamlToPipelineIT.html")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("<table class=\"qetable\"><thead>\n<tr class=\"header\"><th class=\"header evenCol\" >dataId</th><th class=\"header oddCol\" >instant</th><th class=\"header evenCol\" >ref</th><th class=\"header oddCol\" >value</th><th class=\"header evenCol\" >children</th></tr>\n</thead><tbody>\n<tr class=\"dataRow evenRow\" ><td class=\"evenRow evenCol\">1</td><td class=\"evenRow oddCol\">1971-05-07T03:00</td><td class=\"evenRow evenCol\">antiquewhite</td><td class=\"evenRow oddCol\">first</td><td class=\"evenRow evenCol\">one</td></tr>"));
    
    main.shutdown();
  }
  
}
