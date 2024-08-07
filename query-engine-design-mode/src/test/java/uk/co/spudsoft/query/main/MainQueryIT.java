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
import io.vertx.junit5.VertxTestContext;
import java.io.File;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.testcontainers.ServerProviderPostgreSQL;

import io.vertx.junit5.Timeout;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import uk.co.spudsoft.query.testcontainers.ServerProviderMySQL;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class MainQueryIT {
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  private static final ServerProviderMySQL mysql = new ServerProviderMySQL().init();
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(MainQueryIT.class);
  
  @BeforeAll
  public static void createDirs(Vertx vertx, VertxTestContext testContext) {
    File paramsDir = new File("target/query-engine/samples-mainqueryit");
    Main.prepareBaseConfigPath(paramsDir, null);
    postgres.prepareTestDatabase(vertx)
            .compose(v -> mysql.prepareTestDatabase(vertx))
            .onComplete(testContext.succeedingThenComplete())
            ;
    new File("target/classes/samples/sub1/sub3").mkdirs();
  }
  
  @Test
  @Timeout(value = 2400, timeUnit = TimeUnit.SECONDS)
  public void testQuery() throws Exception {
    Main main = new DesignMain();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    GlobalOpenTelemetry.resetForTest();
    main.testMain(new String[]{
        "--persistence.datasource.url=" + mysql.getJdbcUrl()
      , "--persistence.datasource.adminUser.username=" + mysql.getUser()
      , "--persistence.datasource.adminUser.password=" + mysql.getPassword()
      , "--persistence.datasource.user.username=" + mysql.getUser()
      , "--persistence.datasource.user.password=" + mysql.getPassword()
      , "--persistence.retryLimit=100"
      , "--persistence.retryIncrementMs=500"
      , "--baseConfigPath=target/query-engine/samples-mainqueryit"
      , "--vertxOptions.eventLoopPoolSize=5"
      , "--vertxOptions.workerPoolSize=5"
      , "--vertxOptions.tracingOptions.serviceName=Query-Engine"
      , "--httpServerOptions.tracingPolicy=ALWAYS"
      , "--pipelineCache.maxDuration=PT10M"
      , "--logging.jsonFormat=true"
      , "--jwt.acceptableIssuerRegexes[0]=.*"
      , "--jwt.defaultJwksCacheDuration=PT1M"
      , "--zipkin.baseUrl=http://localhost/wontwork"
    }, stdout);
    
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
            .log().all()
            .get("/openapi.json")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, containsString("\"openapi\" : \"3.1.0\","));
    assertThat(body, containsString("SpudSoft Query Engine"));
    
    body = given()
            .log().all()
            .get("/api/info/available")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("{\"name\":\"\",\"children\":[{\"name\":\"args\",\"children\":[{\"name\":\"Args00\",\"path\":\"args/Args00\",\"title\":\"No Arguments\",\"description\":\"Test pipeline that has no arguments\",\"arguments"));

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
