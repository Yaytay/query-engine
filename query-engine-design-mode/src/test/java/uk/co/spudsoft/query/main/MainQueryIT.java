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
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.testcontainers.ServerProviderPostgreSQL;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import uk.co.spudsoft.query.testcontainers.ServerProviderMySQL;

/**
 * Note that this set of tests requires the sample data to be loaded, but relies on the "loadSampleData" flag to make it happen.
 * When running with the full set of tests this won't actually stress that flag because others tests may have already
 * loaded the sample data.
 * 
 * @author jtalbut
 */
public class MainQueryIT {
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  private static final ServerProviderMySQL mysql = new ServerProviderMySQL().init();
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(MainQueryIT.class);
  
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
      , "--logging.jsonFormat=true"
      , "--jwt.acceptableIssuerRegexes[0]=.*"
      , "--jwt.jwksEndpoints[0]=http://localhost/"
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
    
    assertThat(body, startsWith("{\"name\":\"\",\"children\":[{\"name\":\"args\",\"children\":[{\"name\":\"Args00\",\"path\":\"args/Args00\",\"title\":\"No Arguments\",\"description\":\"Test pipeline that has no arguments\",\"argumentGroups\":[],\"arguments\":[],\"destinations\":[{\"type\":\"HTML\",\"name\":\"html\",\"extension\":\"html\",\"mediaType\":\"text/html\"},{\"type\":\"XLSX\",\"name\":\"xlsx\",\"extension\":\"xlsx\",\"mediaType\":\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\",\"sheetName\":\"data\",\"gridLines\":true,\"headers\":true,\"defaultDateFormat\":\"yyyy-mm-dd\",\"defaultDateTimeFormat\":\"yyyy-mm-dd hh:mm:ss\",\"defaultTimeFormat\":\"hh:mm:ss\",\"columns\":[]},{\"type\":\"Delimited\",\"name\":\"tab\",\"extension\":\"tsv\",\"mediaType\":\"text/tsv\",\"delimiter\":\"\\t\",\"openQuote\":\"\\\"\",\"closeQuote\":\"\\\"\",\"escapeCloseQuote\":\"\\\"\",\"newline\":\"\\n\"}]},{\"name\":\"Args01\",\"path\":\"args/Args01\",\"title\":\"One Argument\",\"description\":\"Test pipeline that has 1 argument\",\"argumentGroups\":[],\"arguments\":[{\"type\":\"String\",\"name\":\"arg1\",\"title\":\"First\",\"prompt\":\"arg1\",\"description\":\"The First Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":false,\"ignored\":false,\"validate\":true,\"dependsUpon\":[],\"possibleValues\":[{\"value\":\"First\"},{\"value\":\"Second\"},{\"value\":\"Third\"}]}],\"destinations\":[{\"type\":\"HTML\",\"name\":\"html\",\"extension\":\"html\",\"mediaType\":\"text/html\"},{\"type\":\"XLSX\",\"name\":\"xlsx\",\"extension\":\"xlsx\",\"mediaType\":\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\",\"sheetName\":\"data\",\"gridLines\":true,\"headers\":true,\"defaultDateFormat\":\"yyyy-mm-dd\",\"defaultDateTimeFormat\":\"yyyy-mm-dd hh:mm:ss\",\"defaultTimeFormat\":\"hh:mm:ss\",\"columns\":[]},{\"type\":\"Delimited\",\"name\":\"tab\",\"extension\":\"tsv\",\"mediaType\":\"text/tsv\",\"delimiter\":\"\\t\",\"openQuote\":\"\\\"\",\"closeQuote\":\"\\\"\",\"escapeCloseQuote\":\"\\\"\",\"newline\":\"\\n\"}]},{\"name\":\"Args02\",\"path\":\"args/Args02\",\"title\":\"Two Arguments\",\"description\":\"Test pipeline that has 2 arguments\",\"argumentGroups\":[],\"arguments\":[{\"type\":\"String\",\"name\":\"arg1\",\"title\":\"First\",\"prompt\":\"arg1\",\"description\":\"The First Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":false,\"ignored\":false,\"validate\":true,\"dependsUpon\":[],\"possibleValues\":[{\"value\":\"First\"},{\"value\":\"Second\"},{\"value\":\"Third\"}]},{\"type\":\"String\",\"name\":\"arg2\",\"title\":\"Second\",\"prompt\":\"arg2\",\"description\":\"The Second Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":false,\"ignored\":false,\"validate\":true,\"dependsUpon\":[],\"possibleValues\":[]}],\"destinations\":[{\"type\":\"HTML\",\"name\":\"html\",\"extension\":\"html\",\"mediaType\":\"text/html\"},{\"type\":\"XLSX\",\"name\":\"xlsx\",\"extension\":\"xlsx\",\"mediaType\":\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\",\"sheetName\":\"data\",\"gridLines\":true,\"headers\":true,\"defaultDateFormat\":\"yyyy-mm-dd\",\"defaultDateTimeFormat\":\"yyyy-mm-dd hh:mm:ss\",\"defaultTimeFormat\":\"hh:mm:ss\",\"columns\":[]},{\"type\":\"Delimited\",\"name\":\"tab\",\"extension\":\"tsv\",\"mediaType\":\"text/tsv\",\"delimiter\":\"\\t\",\"openQuote\":\"\\\"\",\"closeQuote\":\"\\\"\",\"escapeCloseQuote\":\"\\\"\",\"newline\":\"\\n\"}]},{\"name\":\"Args03\",\"path\":\"args/Args03\",\"title\":\"Three Arguments\",\"description\":\"Test pipeline that has 3 arguments\",\"argumentGroups\":[],\"arguments\":[{\"type\":\"String\",\"name\":\"arg1\",\"title\":\"First\",\"prompt\":\"arg1\",\"description\":\"The First Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":false,\"ignored\":false,\"validate\":true,\"dependsUpon\":[],\"possibleValues\":[{\"value\":\"First\"},{\"value\":\"Second\"},{\"value\":\"Third\"}]},{\"type\":\"String\",\"name\":\"arg2\",\"title\":\"Second\",\"prompt\":\"arg2\",\"description\":\"The Second Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":false,\"ignored\":false,\"validate\":true,\"dependsUpon\":[],\"possibleValues\":[]},{\"type\":\"String\",\"name\":\"arg3\",\"title\":\"Third\",\"prompt\":\"arg3\",\"description\":\"The Third Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":false,\"ignored\":false,\"validate\":true,\"dependsUpon\":[],\"possibleValues\":[]}],\"destinations\":[{\"type\":\"HTML\",\"name\":\"html\",\"extension\":\"html\",\"mediaType\":\"text/html\"},{\"type\":\"XLSX\",\"name\":\"xlsx\",\"extension\":\"xlsx\",\"mediaType\":\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\",\"sheetName\":\"data\",\"gridLines\":true,\"headers\":true,\"defaultDateFormat\":\"yyyy-mm-dd\",\"defaultDateTimeFormat\":\"yyyy-mm-dd hh:mm:ss\",\"defaultTimeFormat\":\"hh:mm:ss\",\"columns\":[]},{\"type\":\"Delimited\",\"name\":\"tab\",\"extension\":\"tsv\",\"mediaType\":\"text/tsv\",\"delimiter\":\"\\t\",\"openQuote\":\"\\\"\",\"closeQuote\":\"\\\"\",\"escapeCloseQuote\":\"\\\"\",\"newline\":\"\\n\"}]},{\"name\":\"Args04\",\"path\":\"args/Args04\",\"title\":\"Four Arguments\",\"description\":\"Test pipeline that has 4 arguments\",\"argumentGroups\":[],\"arguments\":[{\"type\":\"String\",\"name\":\"arg1\",\"title\":\"First\",\"prompt\":\"arg1\",\"description\":\"The First Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":false,\"ignored\":false,\"validate\":true,\"dependsUpon\":[],\"possibleValues\":[{\"value\":\"First\"},{\"value\":\"Second\"},{\"value\":\"Third\"}]},{\"type\":\"String\",\"name\":\"arg2\",\"title\":\"Second\",\"prompt\":\"arg2\",\"description\":\"The Second Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":false,\"ignored\":false,\"validate\":true,\"dependsUpon\":[],\"possibleValues\":[]},{\"type\":\"String\",\"name\":\"arg3\",\"title\":\"Third\",\"prompt\":\"arg3\",\"description\":\"The Third Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":false,\"ignored\":false,\"validate\":true,\"dependsUpon\":[],\"possibleValues\":[]},{\"type\":\"String\",\"name\":\"arg4\",\"title\":\"Fourth\",\"prompt\":\"arg1\",\"description\":\"The Fourth Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":false,\"ignored\":false,\"validate\":true,\"dependsUpon\":[],\"possibleValues\":[]}],\"destinations\":[{\"type\":\"HTML\",\"name\":\"html\",\"extension\":\"html\",\"mediaType\":\"text/html\"},{\"type\":\"XLSX\",\"name\":\"xlsx\",\"extension\":\"xlsx\",\"mediaType\":\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\",\"sheetName\":\"data\",\"gridLines\":true,\"headers\":true,\"defaultDateFormat\":\"yyyy-mm-dd\",\"defaultDateTimeFormat\":\"yyyy-mm-dd hh:mm:ss\",\"defaultTimeFormat\":\"hh:mm:ss\",\"columns\":[]},{\"type\":\"Delimited\",\"name\":\"tab\",\"extension\":\"tsv\",\"mediaType\":\"text/tsv\",\"delimiter\":\"\\t\",\"openQuote\":\"\\\"\",\"closeQuote\":\"\\\"\",\"escapeCloseQuote\":\"\\\"\",\"newline\":\"\\n\"}]},{\"name\":\"Args05\",\"path\":\"args/Args05\",\"title\":\"Five Arguments\",\"description\":\"Test pipeline that has 5 arguments\",\"argumentGroups\":[],\"arguments\":[{\"type\":\"String\",\"name\":\"arg1\",\"title\":\"First\",\"prompt\":\"arg1\",\"description\":\"The First Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":false,\"ignored\":false,\"validate\":true,\"dependsUpon\":[],\"possibleValues\":[{\"value\":\"First\"},{\"value\":\"Second\"},{\"value\":\"Third\"}]},{\"type\":\"String\",\"name\":\"arg2\",\"title\":\"Second\",\"prompt\":\"arg2\",\"description\":\"The Second Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":false,\"ignored\":false,\"validate\":true,\"dependsUpon\":[],\"possibleValues\":[]},{\"type\":\"String\",\"name\":\"arg3\",\"title\":\"Third\",\"prompt\":\"arg3\",\"description\":\"The Third Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":false,\"ignored\":false,\"validate\":true,\"dependsUpon\":[],\"possibleValues\":[]},{\"type\":\"String\",\"name\":\"arg4\",\"title\":\"Fourth\",\"prompt\":\"arg1\",\"description\":\"The Fourth Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":false,\"ignored\":false,\"validate\":true,\"dependsUpon\":[],\"possibleValues\":[]},{\"type\":\"String\",\"name\":\"arg5\",\"title\":\"Fifth\",\"prompt\":\"arg5\",\"description\":\"The Fifth Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":false,\"ignored\":false,\"validate\":true,\"dependsUpon\":[],\"possibleValues\":[]}],\"destinations\":[{\"type\":\"HTML\",\"name\":\"html\",\"extension\":\"html\",\"mediaType\":\"text/html\"},{\"type\":\"XLSX\",\"name\":\"xlsx\",\"extension\":\"xlsx\",\"mediaType\":\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\",\"sheetName\":\"data\",\"gridLines\":true,\"headers\":true,\"defaultDateFormat\":\"yyyy-mm-dd\",\"defaultDateTimeFormat\":\"yyyy-mm-dd hh:mm:ss\",\"defaultTimeFormat\":\"hh:mm:ss\",\"columns\":[]},{\"type\":\"Delimited\",\"name\":\"tab\",\"extension\":\"tsv\",\"mediaType\":\"text/tsv\",\"delimiter\":\"\\t\",\"openQuote\":\"\\\"\",\"closeQuote\":\"\\\"\",\"escapeCloseQuote\":\"\\\"\",\"newline\":\"\\n\"}]},{\"name\":\"Args06\",\"path\":\"args/Args06\",\"title\":\"Six Arguments\",\"description\":\"Test pipeline that has 6 arguments\",\"argumentGroups\":[],\"arguments\":[{\"type\":\"String\",\"name\":\"arg1\",\"title\":\"First\",\"prompt\":\"arg1\",\"description\":\"The First Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":false,\"ignored\":false,\"validate\":true,\"dependsUpon\":[],\"possibleValues\":[{\"value\":\"First\"},{\"value\":\"Second\"},{\"value\":\"Third\"}]},{\"type\":\"String\",\"name\":\"arg2\",\"title\":\"Second\",\"prompt\":\"arg2\",\"description\":\"The Second Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":false,\"ignored\":false,\"validate\":true,\"dependsUpon\":[],\"possibleValues\":[]},{\"type\":\"String\",\"name\":\"arg3\",\"title\":\"Third\",\"prompt\":\"arg3\",\"description\":\"The Third Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":false,\"ignored\":false,\"validate\":true,\"dependsUpon\":[],\"possibleValues\":[]},{\"type\":\"String\",\"name\":\"arg4\",\"title\":\"Fourth\",\"prompt\":\"arg1\",\"description\":\"The Fourth Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":false,\"ignored\":false,\"validate\":true,\"dependsUpon\":[],\"possibleValues\":[]},{\"type\":\"String\",\"name\":\"arg5\",\"title\":\"Fifth\",\"prompt\":\"arg5\",\"description\":\"The Fifth Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":false,\"ignored\":false,\"validate\":true,\"dependsUpon\":[],\"possibleValues\":[]},{\"type\":\"String\",\"name\":\"arg6\",\"title\":\"Sixth\",\"prompt\":\"arg6\",\"description\":\"The Sixth Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":false,\"ignored\":false,\"validate\":true,\"dependsUpon\":[],\"possibleValues\":[]}],\"destinations\":[{\"type\":\"HTML\",\"name\":\"html\",\"extension\":\"html\",\"mediaType\":\"text/html\"},{\"type\":\"XLSX\",\"name\":\"xlsx\",\"extension\":\"xlsx\",\"mediaType\":\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\",\"sheetName\":\"data\",\"gridLines\":true,\"headers\":true,\"defaultDateFormat\":\"yyyy-mm-dd\",\"defaultDateTimeFormat\":\"yyyy-mm-dd hh:mm:ss\",\"defaultTimeFormat\":\"hh:mm:ss\",\"columns\":[]},{\"type\":\"Delimited\",\"name\":\"tab\",\"extension\":\"tsv\",\"mediaType\":\"text/tsv\",\"delimiter\":\"\\t\",\"openQuote\":\"\\\"\",\"closeQuote\":\"\\\"\",\"escapeCloseQuote\":\"\\\"\",\"newline\":\"\\n\"}]},{\"name\":\"Args07\",\"path\":\"args/Args07\",\"title\":\"Seven Arguments\",\"description\":\"Test pipeline that has 7 arguments\",\"argumentGroups\":[],\"arguments\":[{\"type\":\"String\",\"name\":\"arg1\",\"title\":\"First\",\"prompt\":\"arg1\",\"description\":\"The First Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":false,\"ignored\":false,\"validate\":true,\"dependsUpon\":[],\"possibleValues\":[{\"value\":\"First\"},{\"value\":\"Second\"},{\"value\":\"Third\"}]},{\"type\":\"String\",\"name\":\"arg2\",\"title\":\"Second\",\"prompt\":\"arg2\",\"description\":\"The Second Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":false,\"ignored\":false,\"validate\":true,\"dependsUpon\":[],\"possibleValues\":[]},{\"type\":\"String\",\"name\":\"arg3\",\"title\":\"Third\",\"prompt\":\"arg3\",\"description\":\"The Third Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":false,\"ignored\":false,\"validate\":true,\"dependsUpon\":[],\"possibleValues\":[]},{\"type\":\"String\",\"name\":\"arg4\",\"title\":\"Fourth\",\"prompt\":\"arg1\",\"description\":\"The Fourth Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":false,\"ignored\":false,\"validate\":true,\"dependsUpon\":[],\"possibleValues\":[]},{\"type\":\"String\",\"name\":\"arg5\",\"title\":\"Fifth\",\"prompt\":\"arg5\",\"description\":\"The Fifth Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":false,\"ignored\":false,\"validate\":true,\"dependsUpon\":[],\"possibleValues\":[]},{\"type\":\"String\",\"name\":\"arg6\",\"title\":\"Sixth\",\"prompt\":\"arg6\",\"description\":\"The Sixth Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":false,\"ignored\":false,\"validate\":true,\"dependsUpon\":[],\"possibleValues\":[]},{\"type\":\"String\",\"name\":\"arg7\",\"title\":\"Seventh\",\"prompt\":\"arg7\",\"description\":\"The Seventh Argument\",\"optional\":false,\"hidden\":false,\"multiValued\":fal"));
    assertThat(body, containsString("\"mediaType\":\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\""));
        
    body = given()
            .log().all()
            .get("/api/formio/demo/FeatureRichExample")
            .then()
            .log().all()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("{\"type\":\"form\""));
    assertThat(body, not(containsString("clientIp")));
    assertThat(body, containsString("Output"));
        
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

    body = given()
            .queryParam("key", postgres.getName())
            .queryParam("port", postgres.getPort())
            .log().all()
            .get("/api/history")
            .then()
            .log().ifError()
            .statusCode(401)
            .extract().body().asString();
    
    main.shutdown();
  }
  
}
