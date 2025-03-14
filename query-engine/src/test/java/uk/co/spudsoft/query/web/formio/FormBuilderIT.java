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
package uk.co.spudsoft.query.web.formio;

import io.opentelemetry.api.GlobalOpenTelemetry;
import uk.co.spudsoft.query.main.*;
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
import static org.hamcrest.Matchers.startsWith;
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
public class FormBuilderIT {
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  private static final ServerProviderMySQL mysql = new ServerProviderMySQL().init();
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(FormBuilderIT.class);
  
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
      "--persistence.datasource.url=" + mysql.getJdbcUrl()
      , "--persistence.datasource.adminUser.username=" + mysql.getUser()
      , "--persistence.datasource.adminUser.password=" + mysql.getPassword()
      , "--persistence.datasource.user.username=" + mysql.getUser()
      , "--persistence.datasource.user.password=" + mysql.getPassword()
      , "--persistence.retryLimit=100"
      , "--baseConfigPath=" + baseConfigDir
      , "--vertxOptions.eventLoopPoolSize=5"
      , "--vertxOptions.workerPoolSize=5"
      , "--httpServerOptions.tracingPolicy=ALWAYS"
      , "--pipelineCache.maxDuration=PT10M"
      , "--logging.jsonFormat=false"
      , "--zipkin.baseUrl=http://localhost/wontwork"
      , "--jwt.acceptableIssuerRegexes[0]=.*"
      , "--jwt.defaultJwksCacheDuration=PT1M"
    }, stdout, System.getenv());
    
    RestAssured.port = main.getPort();
    
    String body = given()
            .log().all()
            .get("/api/formio/demo/FeatureRichExample?columns=2")
            .then()
            .log().all()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("{\"type\":\"form\""));
    assertThat(body, containsString("Output"));
    assertThat(body, containsString("minDate"));
    assertThat(body, containsString("maxId"));
    assertThat(body, containsString("maxLong"));
    assertThat(body, containsString("textLike"));
    assertThat(body, containsString("refValue"));
        
    body = given()
            .log().all()
            .get("/api/formio/args/Args01?columns=2")
            .then()
            .log().all()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("{\"type\":\"form\""));
    assertThat(body, containsString("Output"));
    assertThat(body, containsString("arg1"));
    assertThat(body, containsString("First"));
    assertThat(body, containsString("Second"));
    assertThat(body, containsString("Third"));
        
    body = given()
            .log().all()
            .get("/api/formio/args/Args14?columns=3")
            .then()
            .log().all()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("{\"type\":\"form\""));
    assertThat(body, containsString("Output"));
    assertThat(body, containsString("arg1"));
    assertThat(body, containsString("arg2"));
    assertThat(body, containsString("arg3"));
    assertThat(body, containsString("arg4"));
    assertThat(body, containsString("First"));
    assertThat(body, containsString("Second"));
    assertThat(body, containsString("Third"));
        
    body = given()
            .log().all()
            .get("/api/formio/args/Args15?columns=3")
            .then()
            .log().all()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("{\"type\":\"form\""));
    assertThat(body, containsString("First Group"));
    assertThat(body, containsString("Second Group"));
    assertThat(body, containsString("Third Group"));
    assertThat(body, containsString("Fourth Group"));
    assertThat(body, containsString("arg1"));
    assertThat(body, containsString("arg2"));
    assertThat(body, containsString("arg3"));
    assertThat(body, containsString("arg4"));
        
    body = given()
            .log().all()
            .get("/api/formio/")
            .then()
            .log().all()
            .statusCode(404)
            .extract().body().asString();
    
    assertThat(body, equalTo(""));
        
    body = given()
            .log().all()
            .get("/api/formio/bob")
            .then()
            .log().all()
            .statusCode(404)
            .extract().body().asString();
    
    assertThat(body, equalTo("Not found"));
        
    body = given()
            .log().all()
            .get("/api/formio//")
            .then()
            .log().all()
            .statusCode(404)
            .extract().body().asString();
    
    assertThat(body, equalTo(""));
        
    body = given()
            .log().all()
            .get("/api/formio/demo?columns=-2")
            .then()
            .log().all()
            .statusCode(404)
            .extract().body().asString();
    
    assertThat(body, equalTo("Not found"));
        
    body = given()
            .log().all()
            .get("/api/formio/demo/FeatureRichExample/file?columns=223")
            .then()
            .log().all()
            .statusCode(404)
            .extract().body().asString();
    
    assertThat(body, equalTo("Not found"));
        
    main.shutdown();
  }
  
}
