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

import static io.restassured.RestAssured.given;
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
    File paramsDir = new File("target/query-engine");
    paramsDir.mkdirs();
    postgres.prepareTestDatabase(vertx)
            .compose(v -> mysql.prepareTestDatabase(vertx))
            .onComplete(testContext.succeedingThenComplete())
            ;
    new File("target/test-classes/sources/sub1/sub3").mkdirs();
  }
  
  @Test
  public void testQuery() throws Exception {
    Main main = new Main();
    main.testMain(new String[]{
      "audit.datasource.url=jdbc:" + postgres.getUrl()
      , "audit.datasource.schema=public"
      , "audit.datasource.adminUser.username=" + postgres.getUser()
      , "audit.datasource.adminUser.password=" + postgres.getPassword()
      , "audit.datasource.user.username=" + postgres.getUser()
      , "audit.datasource.user.password=" + postgres.getPassword()
      , "baseConfigPath=target/test-classes/sources"
      , "vertxOptions.eventLoopPoolSize=5"
      , "vertxOptions.workerPoolSize=5"
      , "vertxOptions.tracingOptions.serviceName=Query-Engine"
      , "httpServerOptions.tracingPolicy=ALWAYS"
      , "pipelineCache.maxDurationMs=60000"
      , "logging.jsonFormat=false"
      , "zipkin.baseUrl=http://localhost/wontwork"
    });
    
    RestAssured.port = main.getPort();
    
    String body = given()
            .log().all()
            .get("/openapi.yaml")
            .then()
            .statusCode(200)
            .log().all()
            .extract().body().asString();
    
    assertThat(body, startsWith("openapi: 3.1.0"));
    assertThat(body, containsString("SpudSoft Query Engine"));
    
    body = given()
            .log().all()
            .get("/openapi.json")
            .then()
            .statusCode(200)
            .log().all()
            .extract().body().asString();
    
    assertThat(body, containsString("\"openapi\" : \"3.1.0\","));
    assertThat(body, containsString("SpudSoft Query Engine"));
    
    body = given()
            .log().all()
            .get("/api/info/available")
            .then()
            .statusCode(200)
            .log().all()
            .extract().body().asString();
    
    // assertThat(body, equalTo("{\"name\":\"\",\"children\":[{\"name\":\"demo\",\"children\":[{\"name\":\"FeatureRichExample\",\"path\":\"demo/FeatureRichExample\",\"title\":\"Feature Rich Example\",\"description\":\"A complex pipeline that tries to demonstrate as many features as I can cram into a single pipeline.\\n\",\"arguments\":{\"key\":{\"type\":\"String\",\"optional\":false,\"multiValued\":false,\"possibleValues\":[{\"id\":\"MS SQL Server\"},{\"id\":\"MySQL\"},{\"id\":\"PostgreSQL\"}]},\"port\":{\"type\":\"Integer\",\"optional\":false,\"multiValued\":false,\"possibleValues\":[]},\"maxId\":{\"type\":\"Integer\",\"optional\":false,\"multiValued\":false,\"possibleValues\":[]}},\"destinations\":[{\"type\":\"Logger\",\"type\":\"LOGGER\",\"format\":\"log\"}],\"leaf\":true},{\"name\":\"LookupValues\",\"path\":\"demo/LookupValues\",\"title\":\"Lookup Values\",\"description\":\"Extract values to use for the demo/FeatureRichExample.\",\"arguments\":{\"key\":{\"type\":\"String\",\"optional\":false,\"multiValued\":false,\"possibleValues\":[{\"id\":\"MS SQL Server\"},{\"id\":\"MySQL\"},{\"id\":\"PostgreSQL\"}]},\"port\":{\"type\":\"Integer\",\"optional\":false,\"multiValued\":false,\"possibleValues\":[]},\"maxId\":{\"type\":\"Integer\",\"optional\":false,\"multiValued\":false,\"possibleValues\":[]}},\"destinations\":[{\"type\":\"JSON\",\"type\":\"JSON\",\"format\":\"json\",\"extension\":\"json\",\"mediaType\":{}}],\"leaf\":true}],\"path\":\"demo\",\"leaf\":false},{\"name\":\"sub1\",\"children\":[{\"name\":\"sub2\",\"children\":[{\"name\":\"JsonToPipelineIT\",\"path\":\"sub1/sub2/JsonToPipelineIT\",\"title\":\"Test Pipeline in JSON\",\"arguments\":{\"key\":{\"type\":\"String\",\"optional\":false,\"multiValued\":false,\"possibleValues\":[]},\"port\":{\"type\":\"Integer\",\"optional\":false,\"multiValued\":false,\"possibleValues\":[]}},\"destinations\":[{\"type\":\"Logger\",\"type\":\"LOGGER\",\"format\":\"log\"}],\"leaf\":true},{\"name\":\"TemplatedJsonToPipelineIT\",\"path\":\"sub1/sub2/TemplatedJsonToPipelineIT\",\"arguments\":{\"key\":{\"type\":\"String\",\"optional\":false,\"multiValued\":false,\"possibleValues\":[]},\"port\":{\"type\":\"Integer\",\"optional\":false,\"multiValued\":false,\"possibleValues\":[]}},\"destinations\":[{\"type\":\"JSON\",\"type\":\"JSON\",\"format\":\"json\",\"extension\":\"json\",\"mediaType\":{}}],\"leaf\":true},{\"name\":\"TemplatedYamlToPipelineIT\",\"path\":\"sub1/sub2/TemplatedYamlToPipelineIT\",\"arguments\":{\"key\":{\"type\":\"String\",\"optional\":false,\"multiValued\":false,\"possibleValues\":[{\"id\":\"MS SQL Server\"},{\"id\":\"MySQL\"},{\"id\":\"PostgreSQL\"}]},\"port\":{\"type\":\"Integer\",\"optional\":false,\"multiValued\":false,\"possibleValues\":[]}},\"destinations\":[{\"type\":\"Logger\",\"type\":\"LOGGER\",\"format\":\"log\"},{\"type\":\"XLSX\",\"type\":\"XLSX\",\"format\":\"xlsx\",\"extension\":\"xlsx\",\"mediaType\":{}},{\"type\":\"Delimited\",\"type\":\"CSV\",\"format\":\"tab\",\"extension\":\"tsv\",\"mediaType\":{},\"delimiter\":\"\\t\",\"openQuote\":\"\\\"\",\"closeQuote\":\"\\\"\",\"newline\":\"\\n\"},{\"type\":\"Delimited\",\"type\":\"CSV\",\"format\":\"csv\",\"extension\":\"csv\",\"mediaType\":{},\"delimiter\":\",\",\"openQuote\":\"\\\"\",\"closeQuote\":\"\\\"\",\"newline\":\"\\r\\n\"},{\"type\":\"HTML\",\"type\":\"HTML\",\"format\":\"table\",\"extension\":\"html\",\"mediaType\":{}}],\"leaf\":true},{\"name\":\"YamlToPipelineIT\",\"path\":\"sub1/sub2/YamlToPipelineIT\",\"description\":\"Test pipeline written as YAML\",\"arguments\":{\"key\":{\"type\":\"String\",\"optional\":false,\"multiValued\":false,\"possibleValues\":[{\"id\":\"MS SQL Server\"},{\"id\":\"MySQL\"},{\"id\":\"PostgreSQL\"}]},\"port\":{\"type\":\"Integer\",\"optional\":false,\"multiValued\":false,\"possibleValues\":[]},\"maxId\":{\"type\":\"Integer\",\"optional\":false,\"multiValued\":false,\"possibleValues\":[]}},\"destinations\":[{\"type\":\"Logger\",\"type\":\"LOGGER\",\"format\":\"log\"}],\"leaf\":true}],\"path\":\"sub1/sub2\",\"leaf\":false}],\"path\":\"sub1\",\"leaf\":false}],\"path\":\"\",\"leaf\":false}"));
    assertThat(body, startsWith("{\"name\":\"\",\"children\":[{\"name\":\"demo\",\"children\":[{\"name\":\"FeatureRichExample\",\"path\":"));
    
    body = given()
            .queryParam("key", postgres.getName())
            .queryParam("port", postgres.getPort())
            .accept("text/html, application/xhtml+xml, image/webp, image/apng, application/xml; q=0.9, application/signed-exchange; v=b3; q=0.9, */*; q=0.8")
            .log().all()
            .get("/query/sub1/sub2/TemplatedJsonToPipelineIT")
            .then()
            .statusCode(200)
            .log().all()
            .extract().body().asString();
    
    assertThat(body, startsWith("[{\"dataId\":1,\"instant\":\"1971-05-07T03:00\",\"ref\":\"antiquewhite\",\"value\":\"first\",\"children\":\"one\",\"DateField\":\"2023-05-05\",\"TimeField\":null,\"DateTimeField\":null,\"LongField\":null,\"DoubleField\":null,\"BoolField\":null,\"TextField\":null}"));
    
    body = given()
            .queryParam("key", mysql.getName())
            .queryParam("port", mysql.getPort())
            .log().all()
            .get("/query/sub1/sub2/TemplatedJsonToPipelineIT")
            .then()
            .statusCode(200)
            .log().all()
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
            .statusCode(200)
            .log().all()
            .extract().body().asByteArray();
    
    assertThat(bodyBytes, notNullValue());
    
    body = given()
            .queryParam("key", postgres.getName())
            .queryParam("port", postgres.getPort())
            .log().all()
            .get("/query/sub1/sub2/TemplatedYamlToPipelineIT?_fmt=csv")
            .then()
            .statusCode(200)
            .log().all()
            .extract().body().asString();
    
    assertThat(body, startsWith("1,\"1971-05-07T03:00\",\"antiquewhite\",\"first\",\""));
    
    body = given()
            .queryParam("key", postgres.getName())
            .queryParam("port", postgres.getPort())
            .log().all()
            .get("/query/sub1/sub2/TemplatedYamlToPipelineIT.tsv")
            .then()
            .statusCode(200)
            .log().all()
            .extract().body().asString();
    
    assertThat(body, startsWith("\"dataId\"\t\"instant\"\t\"ref\"\t\"value\"\t\"children\"\n1\t\"1971-05-07T03:00\"\t\"antiquewhite\"\t\"first\"\t\"\""));
    
    body = given()
            .queryParam("key", postgres.getName())
            .queryParam("port", postgres.getPort())
            .log().all()
            .get("/query/sub1/sub2/TemplatedYamlToPipelineIT.html")
            .then()
            .statusCode(200)
            .log().all()
            .extract().body().asString();
    
    assertThat(body, startsWith("<table class=\"qetable\"><thead>\n<tr class=\"header\"><th class=\"header evenCol\" >dataId</th><th class=\"header oddCol\" >instant</th><th class=\"header evenCol\" >ref</th><th class=\"header oddCol\" >value</th><th class=\"header evenCol\" >children</th></tr>\n</thead><tbody>\n<tr class=\"dataRow evenRow\" ><td class=\"evenRow evenCol\">1</td><td class=\"evenRow oddCol\">1971-05-07T03:00</td><td class=\"evenRow evenCol\">antiquewhite</td><td class=\"evenRow oddCol\">first</td><td class=\"evenRow evenCol\"></td></tr>"));
    
    main.shutdown();
  }
  
}
