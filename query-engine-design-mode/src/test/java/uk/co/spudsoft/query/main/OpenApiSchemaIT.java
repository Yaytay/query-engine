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
import static io.restassured.RestAssured.given;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.File;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.testcontainers.ServerProviderPostgreSQL;

import io.vertx.junit5.Timeout;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class OpenApiSchemaIT {
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(OpenApiSchemaIT.class);
  
  @BeforeAll
  public static void createDirs(Vertx vertx, VertxTestContext testContext) {
    File paramsDir = new File("target/query-engine/samples-mainqueryit");
    Main.prepareBaseConfigPath(paramsDir, null);
    postgres.prepareTestDatabase(vertx)
            .onComplete(testContext.succeedingThenComplete())
            ;
    new File("target/classes/samples/sub1/sub3").mkdirs();
  }
  
  @Test
  @Timeout(value = 2400, timeUnit = TimeUnit.SECONDS)
  public void testQuery() throws Exception {
    Main main = new DesignMain();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    main.testMain(new String[]{
        "--persistence.datasource.url=jdbc:" + postgres.getUrl()
      , "--persistence.datasource.adminUser.username=" + postgres.getUser()
      , "--persistence.datasource.adminUser.password=" + postgres.getPassword()
      , "--persistence.datasource.user.username=" + postgres.getUser()
      , "--persistence.datasource.user.password=" + postgres.getPassword()
      , "--persistence.retryLimit=100"
      , "--persistence.retryIncrementMs=500"
      , "--baseConfigPath=target/query-engine/samples-mainqueryit"
      , "--vertxOptions.eventLoopPoolSize=5"
      , "--vertxOptions.workerPoolSize=5"
      , "--vertxOptions.tracingOptions.serviceName=Query-Engine"
      , "--httpServerOptions.tracingPolicy=ALWAYS"
      , "--pipelineCache.maxDurationMs=60000"
      , "--logging.jsonFormat=false"
      , "--jwt.acceptableIssuerRegexes[0]=.*"
      , "--jwt.defaultJwksCacheDuration=PT1M"
      , "--zipkin.baseUrl=http://localhost/wontwork"
    }, stdout);
    
    RestAssured.port = main.getPort();
    
    String body = given()
            .log().all()
            .get("/openapi.json")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, containsString("\"openapi\" : \"3.1.0\","));
    assertThat(body, containsString("SpudSoft Query Engine"));
    
    JsonObject jo = new JsonObject(body);
    logger.debug("OpenAPI: {}", jo);
    JsonObject schemas = jo.getJsonObject("components").getJsonObject("schemas");
    boolean failed = false;
    for (String type : schemas.fieldNames()) {
      logger.debug("{}: {}", type, describe(type, schemas.getJsonObject(type)));
      String validationMessage = validate(type, schemas.getJsonObject(type));
      if (validationMessage != null) {
        failed = true;
        logger.warn(validationMessage);
      }
    }
    assertFalse(failed, "At least one validation failed");
        
    main.shutdown();
  }

  private String describe(String name, JsonObject schema) {
    if (schema.containsKey("type")) {
      return schema.getString("type");
    }
    return "Unknown";
    //
  }
  
  private String validate(String name, JsonObject schema) {
    return null;
  }
}
