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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.testcontainers.ServerProviderPostgreSQL;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
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
@TestInstance(Lifecycle.PER_CLASS)
public class AuthQueryIT {
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  private static final ServerProviderMySQL mysql = new ServerProviderMySQL().init();

  private JdkJwksHandler jwks;
  private TokenBuilder tokenBuilder;

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(AuthQueryIT.class);
  
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
    String baseConfigDir = "target/query-engine/samples-authqueryit";
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
      , "--pipelineCache.maxDurationMs=PT10M"
      , "--logging.jsonFormat=false"
      , "--zipkin.baseUrl=http://localhost/wontwork"
      , "--jwt.acceptableIssuerRegexes[0]=.*"
      , "--jwt.jwksEndpoints[0]=" + jwks.getBaseUrl() + "/jwks"
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
      , "--session.requireSession=true"
      , "--session.oauth.Test.logoUrl=https://upload.wikimedia.org/wikipedia/commons/c/c2/GitHub_Invertocat_Logo.svg"
      , "--session.oauth.Test.authorizationEndpoint=https://github.com/login/oauth/authorize"
      , "--session.oauth.Test.tokenEndpoint=https://github.com/login/oauth/access_token"
      , "--session.oauth.Test.credentials.id=bdab017f4732085a51f9"
      , "--session.oauth.Test.credentials.secret=" + System.getProperty("queryEngineGithubSecret")
      , "--session.oauth.Test.pkce=false"
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
//    assertThat(body, not(containsString("empty")));
    
    body = given()
            .log().all()
            .get("/openapi.json")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, containsString("\"openapi\" : \"3.1.0\","));
    assertThat(body, containsString("SpudSoft Query Engine"));
//    assertThat(body, not(containsString("empty")));
    
    body = given()
            .log().all()
            .get("/api/info/available")
            .then()
            .log().all()
            .statusCode(401)
            .extract().body().asString();
    
    String token = tokenBuilder.buildToken(JsonWebAlgorithm.RS512
            , "AuthQueryIT-" + UUID.randomUUID().toString()
            , jwks.getBaseUrl()
            , "sub" + System.currentTimeMillis()
            , Arrays.asList("query-engine")
            , System.currentTimeMillis() / 1000
            , System.currentTimeMillis() / 1000 + 3600
            , ImmutableMap.<String, Object>builder()
                    .put("name", "Full Name")
                    .put("preferred_username", "username")
                    .build()
    );
    
    body = given()
            .header(new Header("Authorization", "Bearer " + token))
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
            .statusCode(401)
            .extract().body().asString();
    
    body = given()
            .header(new Header("Authorization", "Bearer " + token))
            .log().all()
            .get("/api/formio/demo/FeatureRichExample")
            .then()
            .log().all()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("{\"type\":\"form\""));
    assertThat(body, containsString("Output"));
        
    body = given()
            .header(new Header("Authorization", "Bearer " + token))
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
            .header(new Header("Authorization", "Bearer " + token))
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
            .header(new Header("Authorization", "Bearer " + token))
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
            .header(new Header("Authorization", "Bearer " + token))
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
            .header(new Header("Authorization", "Bearer " + token))
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
            .header(new Header("Authorization", "Bearer " + token))
            .queryParam("key", postgres.getName())
            .queryParam("port", postgres.getPort())
            .log().all()
            .get("/query/sub1/sub2/TemplatedYamlToPipelineIT.html")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("<table class=\"qetable\"><thead>\n<tr class=\"header\"><th class=\"header evenCol\" >dataId</th><th class=\"header oddCol\" >instant</th><th class=\"header evenCol\" >ref</th><th class=\"header oddCol\" >value</th><th class=\"header evenCol\" >children</th></tr>\n</thead><tbody>\n<tr class=\"dataRow evenRow\" ><td class=\"evenRow evenCol\">1</td><td class=\"evenRow oddCol\">1971-05-07T03:00</td><td class=\"evenRow evenCol\">antiquewhite</td><td class=\"evenRow oddCol\">first</td><td class=\"evenRow evenCol\">one</td></tr>"));

    body = given()
            .header(new Header("Authorization", "Bearer " + token))
            .log().all()
            .get("/api/history")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    logger.debug("History: {}", body);
    
    JsonObject history1 = new JsonObject(body);
    assertEquals(6, history1.getJsonArray("rows").size());    
    assertEquals(6, history1.getInteger("totalRows"));    
    assertEquals(0, history1.getInteger("firstRow"));
    assertThat(history1.getJsonArray("rows").getJsonObject(0).getString("subject"), startsWith("sub"));
    assertEquals("Full Name", history1.getJsonArray("rows").getJsonObject(0).getString("name"));
    assertEquals("username", history1.getJsonArray("rows").getJsonObject(0).getString("username"));
    
    body = given()
            .header(new Header("Authorization", "Bearer " + token))
            .log().all()
            .get("/api/history?skipRows=2&maxRows=3")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    logger.debug("History: {}", body);
    
    JsonObject history2 = new JsonObject(body);
    assertEquals(3, history2.getJsonArray("rows").size());    
    assertEquals(6, history2.getInteger("totalRows"));    
    assertEquals(2, history2.getInteger("firstRow"));
    assertThat(history2.getJsonArray("rows").getJsonObject(0).getString("subject"), startsWith("sub"));
    assertEquals("Full Name", history2.getJsonArray("rows").getJsonObject(0).getString("name"));
    assertEquals("username", history2.getJsonArray("rows").getJsonObject(0).getString("username"));
    
    assertEquals(history2.getJsonArray("rows").getJsonObject(0).getString("id"), history1.getJsonArray("rows").getJsonObject(2).getString("id"));
    assertEquals(history2.getJsonArray("rows").getJsonObject(1).getString("id"), history1.getJsonArray("rows").getJsonObject(3).getString("id"));
    assertEquals(history2.getJsonArray("rows").getJsonObject(2).getString("id"), history1.getJsonArray("rows").getJsonObject(4).getString("id"));

    main.shutdown();
  }
  
}
