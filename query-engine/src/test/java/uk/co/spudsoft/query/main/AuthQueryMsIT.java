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
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.http.Header;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
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
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import uk.co.spudsoft.jwtvalidatorvertx.AlgorithmAndKeyPair;
import uk.co.spudsoft.jwtvalidatorvertx.JsonWebAlgorithm;
import uk.co.spudsoft.jwtvalidatorvertx.TokenBuilder;
import uk.co.spudsoft.jwtvalidatorvertx.jdk.JdkJwksHandler;
import uk.co.spudsoft.jwtvalidatorvertx.jdk.JdkTokenBuilder;
import uk.co.spudsoft.query.testcontainers.ServerProviderMsSQL;
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
@Timeout(60000)
public class AuthQueryMsIT {
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  private static final ServerProviderMySQL mysql = new ServerProviderMySQL().init();
  private static final ServerProviderMsSQL mssql = new ServerProviderMsSQL().init();

  private static final String CONFS_DIR = "target/query-engine/samples-" + MethodHandles.lookup().lookupClass().getSimpleName().toLowerCase();
  
  private JdkJwksHandler jwks;
  private TokenBuilder tokenBuilder;

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(AuthQueryMsIT.class);
  
  @BeforeAll
  public void createDirs() throws IOException {
    File confsDir = new File(CONFS_DIR);
    FileUtils.deleteQuietly(confsDir);
    confsDir.mkdirs();

    Cache<String, AlgorithmAndKeyPair> keyCache = AlgorithmAndKeyPair.createCache(Duration.ofMinutes(1));
    tokenBuilder = new JdkTokenBuilder(keyCache);

    jwks = JdkJwksHandler.create();
    logger.debug("Starting JWKS endpoint");
    jwks.start();
    jwks.setKeyCache(keyCache);
  }
  
  @Test
  public void testQuery() throws Exception {
    Main main = new Main();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    main.testMain(new String[]{
      "--persistence.datasource.url=" + mssql.getJdbcUrl()
      , "--persistence.datasource.adminUser.username=" + mssql.getUser()
      , "--persistence.datasource.adminUser.password=" + mssql.getPassword()
      , "--persistence.datasource.user.username=" + mssql.getUser()
      , "--persistence.datasource.user.password=" + mssql.getPassword()
      , "--persistence.retryLimit=100"
      , "--baseConfigPath=" + CONFS_DIR
      , "--vertxOptions.eventLoopPoolSize=5"
      , "--vertxOptions.workerPoolSize=5"
      , "--httpServerOptions.tracingPolicy=ALWAYS"
      , "--pipelineCache.maxDuration=PT10M"
      , "--logging.jsonFormat=false"
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
      , "--sampleDataLoads[4].url=" + mssql.getVertxUrl()
      , "--sampleDataLoads[4].user.username=" + mssql.getUser()
      , "--sampleDataLoads[4].user.password=" + mssql.getPassword()
      , "--session.requireSession=true"
      , "--session.oauth.Test.logoUrl=https://upload.wikimedia.org/wikipedia/commons/c/c2/GitHub_Invertocat_Logo.svg"
      , "--session.oauth.Test.authorizationEndpoint=https://github.com/login/oauth/authorize"
      , "--session.oauth.Test.tokenEndpoint=https://github.com/login/oauth/access_token"
      , "--session.oauth.Test.credentials.id=bdab017f4732085a51f9"
      , "--session.oauth.Test.credentials.secret=" + System.getProperty("queryEngineGithubSecret")
      , "--session.oauth.Test.pkce=false"
      , "--securityHeaders.referrerPolicy=strict-origin-when-cross-origin"
    }, stdout, System.getenv());
    
    RestAssured.port = main.getPort();
    
    String body = given()
            .get("/openapi.yaml")
            .then()
            .log().ifError()
            .statusCode(200)
            .header("X-Frame-Options", equalTo("SAMEORIGIN"))
            .header("Referrer-Policy", equalTo("strict-origin-when-cross-origin"))
            .extract().body().asString();
    
    assertThat(body, startsWith("openapi: 3.1.0"));
    assertThat(body, containsString("SpudSoft Query Engine"));
    
    body = given()
            .get("/openapi.json")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, containsString("\"openapi\" : \"3.1.0\","));
    assertThat(body, containsString("SpudSoft Query Engine"));
    
    body = given()
            .get("/api/info/available")
            .then()
            .log().ifError()
            .statusCode(401)
            .extract().body().asString();
    
    String token = tokenBuilder.buildToken(JsonWebAlgorithm.RS512
            , "AuthQueryMSIT-" + UUID.randomUUID().toString()
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
            .get("/api/info/available")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith(StandardExpectedResults.AVAILABLE_AS_JSON_START));
        
    body = given()
            .get("/api/formio/demo/FeatureRichExample")
            .then()
            .log().ifError()
            .statusCode(401)
            .extract().body().asString();
    
    body = given()
            .header(new Header("Authorization", "Bearer " + token))
            .get("/api/formio/demo/FeatureRichExample")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("{\"type\":\"form\""));
    assertThat(body, containsString("Output"));
        
    body = given()
            .header(new Header("Authorization", "Bearer " + token))
            .queryParam("key", postgres.getName())
            .queryParam("port", postgres.getPort())
            .accept("text/html, application/xhtml+xml, image/webp, image/apng, application/xml; q=0.9, application/signed-exchange; v=b3; q=0.9, */*; q=0.8")
            .get("/query/sub1/sub2/TemplatedJsonToPipelineIT")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("[\n{\"dataId\":1,\"instant\":\"1971-05-07T03:00\",\"ref\":\"antiquewhite\",\"value\":\"first\",\"children\":\"one\",\"DateField\":\"2023-05-05\",\"TimeField\":null,\"DateTimeField\":null,\"LongField\":null,\"DoubleField\":null,\"BoolField\":null,\"TextField\":null},\n{\"dataId\":2,\"instant\":\"1971-05-08T06:00\",\"ref\":\"aqua\",\"value\":\"second\",\"children\":\"two,four\",\"DateField\":\"2023-05-04\",\"TimeField\":\"23:58\",\"DateTimeField\":null,\"LongField\":null,\"DoubleField\":null,\"BoolField\":null,\"TextField\":null},"));
    
    body = given()
            .header(new Header("Authorization", "Bearer " + token))
            .queryParam("key", mysql.getName())
            .queryParam("port", mysql.getPort())
            .get("/query/sub1/sub2/TemplatedJsonToPipelineIT")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    // Note that MySQL doesn't do booleans
    assertThat(body, startsWith("[\n{\"dataId\":1,\"instant\":\"1971-05-07T03:00\",\"ref\":\"antiquewhite\",\"value\":\"first\",\"children\":\"one\",\"DateField\":\"2023-05-05\",\"TimeField\":null,\"DateTimeField\":null,\"LongField\":null,\"DoubleField\":null,\"BoolField\":null,\"TextField\":null},\n{\"dataId\":2,\"instant\":\"1971-05-08T06:00\",\"ref\":\"aqua\",\"value\":\"second\",\"children\":\"two,four\",\"DateField\":\"2023-05-04\",\"TimeField\":\"23:58\",\"DateTimeField\":null,\"LongField\":null,\"DoubleField\":null,\"BoolField\":null,\"TextField\":null},"));
    
    byte[] bodyBytes = given()
            .header(new Header("Authorization", "Bearer " + token))
            .queryParam("key", postgres.getName())
            .queryParam("port", postgres.getPort())
            .accept("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
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
            .get("/query/sub1/sub2/TemplatedYamlToPipelineIT.html")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("""
                                <table class="qetable"><thead>
                                <tr class="header"><th class="header oddCol" >dataId</th><th class="header evenCol" >instant</th><th class="header oddCol" >ref</th><th class="header evenCol" >value</th><th class="header oddCol" >children</th></tr>
                                </thead><tbody>
                                <tr class="dataRow oddRow" ><td class="oddRow oddCol">1</td><td class="oddRow evenCol">1971-05-07T03:00</td><td class="oddRow oddCol">antiquewhite</td><td class="oddRow evenCol">first</td><td class="oddRow oddCol">one</td></tr>
                                <tr class="dataRow evenRow" ><td class="evenRow oddCol">2</td><td class="evenRow evenCol">1971-05-08T06:00</td><td class="evenRow oddCol">aqua</td><td class="evenRow evenCol">second</td><td class="evenRow oddCol">two,four</td></tr>
                                <tr class="dataRow oddRow" ><td class="oddRow oddCol">3</td><td class="oddRow evenCol">1971-05-09T09:00</td><td class="oddRow oddCol">aquamarine</td><td class="oddRow evenCol">third</td><td class="oddRow oddCol">three,six,nine</td></tr>
                                """));

    body = given()
            .header(new Header("Authorization", "Bearer " + token))
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
