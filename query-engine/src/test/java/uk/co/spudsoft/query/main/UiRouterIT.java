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
import java.io.File;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import io.restassured.http.ContentType;
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
    main.testMain(new String[]{
        "baseConfigPath=target/classes/samples"
      , "vertxOptions.tracingOptions.serviceName=Query-Engine"
      , "jwt.acceptableIssuerRegexes[0]=.*"
      , "jwt.defaultJwksCacheDuration=PT1M"
      , "logging.jsonFormat=false"
      , "loadSampleData=true"
    });
    
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
        
    String index = given()
            .log().all()
            .get("/ui")
            .then()
            .log().ifError()
            .log().all()
            .statusCode(200)
            .contentType(ContentType.HTML)
            .extract().body().asString()
            ;
        
    String nonexistent = given()
            .log().all()
            .get("/ui/nonexistent")
            .then()
            .log().ifError()
            .statusCode(200)
            .contentType(ContentType.HTML)
            .extract().body().asString()
            ;
        
    assertEquals(index, nonexistent);
        
    String help = given()
            .log().all()
            .get("/ui/help")
            .then()
            .log().ifError()
            .statusCode(200)
            .contentType(ContentType.HTML)
            .extract().body().asString()
            ;
    
    assertEquals(index, help);
        
    given().log().all().get("/ui/assets").then().log().all();
    
    when().get("/ui/android-chrome-192x192.png").then().header("Content-Type", equalTo("image/png"));
    when().get("/ui/browserconfig.xml").then().header("Content-Type", equalTo("application/xml"));
    when().get("/ui/favicon.ico").then().header("Content-Type", equalTo("image/x-icon"));
    when().get("/ui/favicon.svg").then().header("Content-Type", equalTo("image/svg+xml"));
    when().get("/ui/index.html").then().header("Content-Type", equalTo("text/html;charset=utf-8"));
    when().get("/ui/manifest.json").then().header("Content-Type", equalTo("application/json"));
    when().get("/ui/robots.txt").then().header("Content-Type", equalTo("text/plain;charset=utf-8"));
    when().get("/ui/site.webmanifest").then().header("Content-Type", equalTo("application/json"));
    when().get("/ui/assets/index-501da79e.css").then().header("Content-Type", equalTo("text/css;charset=utf-8"));
    when().get("/ui/assets/index-d6805c40.js").then().header("Content-Type", equalTo("text/javascript;charset=utf-8"));
    when().get("/ui/assets/roboto-cyrillic-300-normal-47aa3bfa.woff2").then().header("Content-Type", equalTo("font/woff2"));
    when().get("/ui/assets/roboto-cyrillic-300-normal-c07952fe.woff").then().header("Content-Type", equalTo("application/x-font-woff"));
    
    main.shutdown();
  }
  
}
