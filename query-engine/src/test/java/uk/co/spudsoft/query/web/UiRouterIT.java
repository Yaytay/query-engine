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
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import java.io.File;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import io.restassured.http.ContentType;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import uk.co.spudsoft.query.main.Main;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * A set of tests that do not actually do any querying.
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class UiRouterIT {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(UiRouterIT.class);
  
  @BeforeAll
  public static void createDirs(Vertx vertx) {
    File paramsDir = new File("target/query-engine");
    paramsDir.mkdirs();
  }
    
  @Test
  public void testMainDaemon() throws Exception {
    Main main = new Main();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    main.testMain(new String[]{
        "--baseConfigPath=target/classes/samples"
      , "--vertxOptions.tracingOptions.serviceName=Query-Engine"
      , "--jwt.acceptableIssuerRegexes[0]=.*"
      , "--jwt.defaultJwksCacheDuration=PT1M"
      , "--logging.jsonFormat=false"
      , "--loadSampleData=true"
    }, stdout);
    
    RestAssured.port = main.getPort();
    
    given()
            .redirects().follow(false)
            .log().all()
            .get("/")
            .then()
            .log().all()
            .statusCode(302)
            .header("Location", equalTo("/ui"))
            ;
        
    // POST, returns a 404
    given()
            .redirects().follow(false)
            .log().all()
            .post("/ui/index.html")
            .then()
            .log().all()
            .statusCode(404)
            ;
        
    String root = given()
            .log().all()
            .get("/ui")
            .then()
            .log().ifError()
            .log().all()
            .statusCode(200)
            .contentType(ContentType.HTML)
            .extract().body().asString()
            ;
        
    String index2 = given()
            .log().all()
            .get("/ui/index.html")
            .then()
            .log().ifError()
            .statusCode(200)
            .contentType(ContentType.HTML)
            .extract().body().asString()
            ;
        
    assertEquals(root, index2);
        
    String head = given()
            .log().all()
            .head("/ui/index.html")
            .then()
            .log().ifError()
            .statusCode(200)
            .contentType(ContentType.HTML)
            .extract().body().asString()
            ;
        
    assertEquals("", head);
        
    String endDot = given()
            .log().all()
            .get("/ui/help.")
            .then()
            .log().ifError()
            .statusCode(200)
            .contentType(ContentType.HTML)
            .extract().body().asString()
            ;
    
    assertEquals(root, endDot);
        
    String help = given()
            .log().all()
            .get("/ui/help")
            .then()
            .log().ifError()
            .statusCode(200)
            .contentType(ContentType.HTML)
            .extract().body().asString()
            ;
    
    assertEquals(root, help);
        
    String notfound = given()
            .log().all()
            .get("/ui/nonexistent")
            .then()
            .log().ifError()
            .statusCode(200)
            .contentType(ContentType.HTML)
            .extract().body().asString()
            ;
    
    assertEquals(root, notfound);
        
    String assets = given().log().all().get("/ui/assets").then().statusCode(200).contentType(ContentType.HTML).log().all().extract().body().asString();

    assertEquals("", assets);
    
    String nonexistent = given().log().all().get("/ui/nonexistent.png").then().statusCode(200).contentType(ContentType.HTML).log().all().extract().body().asString();

    assertEquals(root, nonexistent);
    
    // Note that the following tests will all fail if the UI files have not been added
    // The UI files are added automatically in a clean build if the "ui" profile is enabled.
    when().get("/ui/android-chrome-192x192.png").then().header("Content-Type", equalTo("image/png"));
    when().get("/ui/browserconfig.xml").then().header("Content-Type", equalTo("application/xml"));
    when().get("/ui/favicon.ico").then().header("Content-Type", equalTo("image/x-icon"));
    when().get("/ui/favicon.svg").then().header("Content-Type", equalTo("image/svg+xml"));
    when().get("/ui/index.html").then().header("Content-Type", equalTo("text/html;charset=utf-8"));
    when().get("/ui/manifest.json").then().header("Content-Type", equalTo("application/json"));
    when().get("/ui/robots.txt").then().header("Content-Type", equalTo("text/plain;charset=utf-8"));
    when().get("/ui/site.webmanifest").then().header("Content-Type", equalTo("application/json"));
    when().get("/ui/assets/roboto-cyrillic-300-normal-47aa3bfa.woff2").then().header("Content-Type", equalTo("font/woff2"));
    when().get("/ui/assets/roboto-cyrillic-300-normal-c07952fe.woff").then().header("Content-Type", equalTo("application/x-font-woff"));
    
    main.shutdown();
  }
  
}
