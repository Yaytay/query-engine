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
package uk.co.spudsoft.query.web;

import io.restassured.RestAssured;
import io.vertx.junit5.VertxExtension;
import java.io.File;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import org.apache.commons.io.FileUtils;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasLength;
import uk.co.spudsoft.query.main.Main;
import static org.hamcrest.Matchers.startsWith;
import org.junit.jupiter.api.TestInstance;


/**
 * A set of tests that do not actually do any querying.
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TableauWDCRouterIT {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(TableauWDCRouterIT.class);
  
  private static final String CONFS_DIR = "target/query-engine/samples-" + MethodHandles.lookup().lookupClass().getSimpleName().toLowerCase();
  
  @BeforeAll
  public void createDirs() {
    File confsDir = new File(CONFS_DIR);
    FileUtils.deleteQuietly(confsDir);
    confsDir.mkdirs();
  }
    
  @Test
  public void testMainDaemon() throws Exception {
    Main main = new Main();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    main.testMain(new String[]{
        "--baseConfigPath=" + CONFS_DIR
      , "--jwt.acceptableIssuerRegexes[0]=.*"
      , "--jwt.defaultJwksCacheDuration=PT1M"
      , "--logging.jsonFormat=false"
      , "--loadSampleData=true"
    }, stdout, System.getenv());
    
    RestAssured.port = main.getPort();
    
    Response response = given()
            .log().all()
            .get("/tableau-wdc.html")
            .then()
            .log().ifError()
            .statusCode(200)
            .contentType(ContentType.HTML)
            .extract().response()
            ;
    
    assertThat(response.header("Content-Security-Policy"), containsString("https://ajax.googleapis.com/ajax/libs/jquery"));
    assertThat(response.header("Content-Security-Policy"), containsString("https://connectors.tableau.com/libs"));
    assertThat(response.body().asString(), startsWith("<html>\n    <head>\n        <title>SpudSoft Query Engine Web Data Connector</title>\n"));
        
    String wdc = given()
            .log().all()
            .get("/tableau/wdc.html")
            .then()
            .log().ifError()
            .statusCode(200)
            .contentType(ContentType.HTML)
            .extract().body().asString()
            ;
    
    assertThat(wdc, startsWith("<html>\n    <head>\n        <title>SpudSoft Query Engine Web Data Connector</title>\n"));
        
    given()
            .log().all()
            .head("/tableau/wdc.html")
            .then()
            .log().ifError()
            .statusCode(200)
            .contentType(ContentType.HTML)
            .body(hasLength(0))
            ;
        
    given()
            .log().all()
            .put("/tableau/wdc.html")
            .then()
            .log().ifError()
            .statusCode(404)
            ;
        
    given()
            .log().all()
            .get("/tableau/nonexistent")
            .then()
            .log().ifError()
            .statusCode(404)
            ;
    
    given()
            .log().all()
            .get("/tableau-nonexistent")
            .then()
            .log().ifError()
            .statusCode(404)
            ;
    
    main.shutdown();
  }
  
}
