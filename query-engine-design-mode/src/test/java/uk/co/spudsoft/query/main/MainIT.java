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
import static io.restassured.config.RedirectConfig.redirectConfig;
import io.restassured.config.RestAssuredConfig;


/**
 * A set of tests that do not actually do any querying.
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class MainIT {
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(MainIT.class);
  
  @BeforeAll
  public static void createDirs(Vertx vertx, VertxTestContext testContext) {
    File paramsDir = new File("target/query-engine");
    paramsDir.mkdirs();
    postgres.prepareTestDatabase(vertx).onComplete(testContext.succeedingThenComplete());
  }
    
  @Test
  public void testBadAudit() throws Exception {
    Main main = new DesignMain();
    main.testMain(new String[]{
      "audit.datasource.url=wibble"
      , "baseConfigPath=target/test-classes/sources"
      , "vertxOptions.tracingOptions.type=io.vertx.tracing.zipkin.ZipkinTracingOptions"
      , "vertxOptions.tracingOptions.serviceName=Query-Engine"
      , "acceptableIssuerRegexes[0]=.*"
      , "pipelineCache.maxDurationMs=0"
      , "pipelineCache.purgePeriodMs=10"
      , "logging.level.uk_co_spudsoft_query_main=TRACE" 
    });
    
    main.shutdown();
  }
  
  @Test
  public void testMainDaemon() throws Exception {
    Main main = new DesignMain();
    main.testMain(new String[]{
      "audit.datasource.url=jdbc:" + postgres.getUrl()
      , "audit.datasource.adminUser.username=" + postgres.getUser()
      , "audit.datasource.adminUser.password=" + postgres.getPassword()
      , "audit.datasource.schema=public" 
      , "baseConfigPath=target/test-classes/sources"
      , "vertxOptions.tracingOptions.serviceName=Query-Engine"
      , "acceptableIssuerRegexes[0]=.*"
      , "logging.jsonFormat=true"
      , "designMode=true"
    });
    
    RestAssured.port = main.getPort();
    
    given()
            .log().all()
            .get("/query/sub1/sub2/TemplatedJsonToPipelineIT.json.vm")
            .then()
            .statusCode(400)
            .log().all()
            ;
    
    given()
            .log().all()
            .post("/query/sub1/sub2/TemplatedJsonToPipelineIT.json.vm")
            .then()
            .statusCode(404)
            .log().all()
            ;
    
    // This isn't a short path because the router stop it reaching the QueryRouter
    given()
            .log().all()
            .post("/query")
            .then()
            .statusCode(404)
            .log().all()
            ;
    
     given()
            .log().all()
            .get("/query/bob")
            .then()
            .statusCode(404)
            .log().all()
            ;
    
     given()
            .log().all()
            .get("/api/design/file/sub1/sub2/YamlToPipelineIT.yaml")
            .then()
            .statusCode(200)
            .log().all()
            ;
    
     given()
            .log().all()
            .get("/ui/index.html")
            .then()
            .statusCode(200)
            .log().all()
            ;
    
     given()
            .log().all()
            .get("/ui")
            .then()
            .statusCode(200)
            .log().all()
            ;
    
     given()
            .config(RestAssuredConfig.config().redirect(redirectConfig().followRedirects(false)))
            .log().all()
            .get("/")
            .then()
            .log().all()
            .statusCode(302)
            .header("Location", "/ui")
            ;
    
    main.shutdown();
  }
  
}
