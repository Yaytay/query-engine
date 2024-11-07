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

import com.google.common.cache.Cache;
import com.google.common.collect.ImmutableMap;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.http.Header;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.testcontainers.ServerProviderPostgreSQL;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import uk.co.spudsoft.jwtvalidatorvertx.AlgorithmAndKeyPair;
import uk.co.spudsoft.jwtvalidatorvertx.JsonWebAlgorithm;
import uk.co.spudsoft.jwtvalidatorvertx.TokenBuilder;
import uk.co.spudsoft.jwtvalidatorvertx.jdk.JdkJwksHandler;
import uk.co.spudsoft.jwtvalidatorvertx.jdk.JdkTokenBuilder;
import uk.co.spudsoft.query.testcontainers.ServerProviderMySQL;

/**
 * Note that this set of tests requires the sample data to be loaded, but relies on the "loadSampleData" flag to make it happen.
 * When running with the full set of tests this won't actually stress that flag because others tests may have already
 * loaded the sample data.
 * 
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ConditionalArgumentIT {
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  private static final ServerProviderMySQL mysql = new ServerProviderMySQL().init();
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ConditionalArgumentIT.class);
  
  private JdkJwksHandler jwks;
  private TokenBuilder tokenBuilder;
  
  @BeforeAll
  public void createDirs(Vertx vertx) throws IOException {
    File paramsDir = new File("target/query-engine/samples-authqueryit");
    try {
      FileUtils.deleteDirectory(paramsDir);
    } catch (Throwable ex) {
    }
    paramsDir.mkdirs();

    Cache<String, AlgorithmAndKeyPair> keyCache = AlgorithmAndKeyPair.createCache(Duration.ofMinutes(1));
    tokenBuilder = new JdkTokenBuilder(keyCache);

    jwks = JdkJwksHandler.create();
    logger.debug("Starting JWKS endpoint");
    jwks.start();
    jwks.setKeyCache(keyCache);
  }
  
  @Test
  public void testQuery() throws Exception {
    GlobalOpenTelemetry.resetForTest();
    Main main = new Main();
    String baseConfigDir = "target/query-engine/samples-conditionalargumentit";
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    main.testMain(new String[]{
      "--persistence.datasource.url=" + mysql.getJdbcUrl()
      , "--persistence.datasource.adminUser.username=" + mysql.getUser()
      , "--persistence.datasource.adminUser.password=" + mysql.getPassword()
      , "--persistence.datasource.user.username=" + mysql.getUser()
      , "--persistence.datasource.user.password=" + mysql.getPassword()
      , "--persistence.retryLimit=100"
      , "--persistence.retryIncrement=PT500"
      , "--baseConfigPath=" + baseConfigDir
      , "--vertxOptions.eventLoopPoolSize=5"
      , "--vertxOptions.workerPoolSize=5"
      , "--httpServerOptions.tracingPolicy=ALWAYS"
      , "--pipelineCache.maxDuration=PT10M"
      , "--logging.jsonFormat=true"
      , "--zipkin.baseUrl=http://localhost/wontwork"
      , "--jwt.acceptableIssuerRegexes[0]=.*"
      , "--jwt.jwksEndpoints[0]=" + jwks.getBaseUrl() + "/jwks"
      , "--jwt.defaultJwksCacheDuration=PT1M"
      , "--managementEndpoints[0]=health"
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
      , "--tracing.protocol=otlphttp"
      , "--tracing.sampler=alwaysOn"
      , "--tracing.url=http://nonexistent/otlphttp"
    }, stdout);
    
    RestAssured.port = main.getPort();
    
    String body;
    
    body = given()
            .log().all()
            .get("/api/info/available")
            .then()
            .log().all()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("{\"name\":\"\",\"children\":[{\"name\":\"args\",\"children\":[{\"name\":\"Args00\",\"path\":\"args/Args00\",\"title\":\"No Arguments\",\"description\":\"Test pipeline that has no arguments\",\"arguments"));
    assertThat(body, containsString("ConditionalArgument"));
        
    body = given()
            .log().all()
            .get("/api/formio/sub1/sub2/ConditionalArgument")
            .then()
            .log().all()
            .statusCode(200)
            .extract().body().asString();
    
    logger.debug("Form definition without auth:", body);
    JsonObject formDef = new JsonObject(body);
    assertEquals(5, formDef.getJsonArray("components").size());
    assertEquals("Arguments", formDef.getJsonArray("components").getJsonObject(1).getString("legend"));
    assertEquals("name", formDef.getJsonArray("components").getJsonObject(1).getJsonArray("components").getJsonObject(0).getJsonArray("columns").getJsonObject(0).getJsonArray("components").getJsonObject(0).getString("key"));
    assertEquals("defaultName", formDef.getJsonArray("components").getJsonObject(1).getJsonArray("components").getJsonObject(0).getJsonArray("columns").getJsonObject(0).getJsonArray("components").getJsonObject(1).getString("key"));
    
    String token = tokenBuilder.buildToken(JsonWebAlgorithm.RS512
            , "ConditionalArgumentIT-" + UUID.randomUUID().toString()
            , jwks.getBaseUrl()
            , "sub" + System.currentTimeMillis()
            , Arrays.asList("query-engine")
            , System.currentTimeMillis() / 1000
            , System.currentTimeMillis() / 1000 + 3600
            , ImmutableMap.<String, Object>builder()
                    .put("name", "Name From Token")
                    .put("preferred_username", "UsernameFromToken")
                    .build()
    );
    
    body = given()
            .header(new Header("Authorization", "Bearer " + token))
            .log().all()
            .get("/api/formio/sub1/sub2/ConditionalArgument")
            .then()
            .log().all()
            .statusCode(200)
            .extract().body().asString();
    formDef = new JsonObject(body);
    assertEquals(4, formDef.getJsonArray("components").size());
    assertEquals("Filters", formDef.getJsonArray("components").getJsonObject(1).getString("legend"));
    
    logger.debug("Form definition with auth:", body);
    
    // Output with no query or token
    
    body = given()
            .log().all()
            .get("/query/sub1/sub2/ConditionalArgument")
            .then()
            .log().all()
            .statusCode(400)
            .extract().body().asString();

    logger.debug("Output with no query or token:", body);
    assertEquals("The argument \"name\" is mandatory and was not provided.", body);
    
    // Output with query but no token
    
    body = given()
            .log().all()
            .queryParam("name", "Name From Query")
            .get("/query/sub1/sub2/ConditionalArgument")
            .then()
            .log().all()
            .statusCode(200)
            .extract().body().asString();

    logger.debug("Output with query but no token:", body);
    assertEquals("""
                 "value"	"name"	"defaultName"	"iterator"
                 0	"Name From Query"	"Henry"	0
                 1	"Name From Query"	"Henry"	1
                 """, body);
    
    // Output with token but no query
    
    body = given()
            .header(new Header("Authorization", "Bearer " + token))
            .log().all()
            .get("/query/sub1/sub2/ConditionalArgument")
            .then()
            .log().all()
            .statusCode(200)
            .extract().body().asString();

    logger.debug("Output with token but no query:", body);
    assertEquals("""
                 "value"	"name"	"defaultName"	"iterator"
                 0	"Name From Token"	"Henry"	0
                 1	"Name From Token"	"Henry"	1
                 """, body);
    
    // Output with token and query
    
    body = given()
            .header(new Header("Authorization", "Bearer " + token))
            .queryParam("name", "Name From Query")
            .log().all()
            .get("/query/sub1/sub2/ConditionalArgument")
            .then()
            .log().all()
            .statusCode(200)
            .extract().body().asString();

    logger.debug("Output with token and query:", body);
    assertEquals("""
                 "value"	"name"	"defaultName"	"iterator"
                 0	"Name From Token"	"Henry"	0
                 1	"Name From Token"	"Henry"	1
                 """, body);
    
    main.shutdown();
  }
  
}
