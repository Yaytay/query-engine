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
package uk.co.spudsoft.query.web.rest;

import uk.co.spudsoft.query.main.*;
import com.google.common.collect.ImmutableMap;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.restassured.RestAssured;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import java.io.File;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.testcontainers.ServerProviderPostgreSQL;

import static io.restassured.RestAssured.given;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import uk.co.spudsoft.query.web.LoginRouterWithDiscoveryIT;


/**
 * A set of tests that do not actually do any querying.
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class InfoHandlerIT {
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(InfoHandlerIT.class);
  
  private final int mgmtPort = LoginRouterWithDiscoveryIT.findUnusedPort();
  
  private static final String CONFS_DIR = "target/query-engine/samples-mainit";
  
  @BeforeAll
  public static void createDirs(Vertx vertx) {
    File confsDir = new File(CONFS_DIR);
    try {
      FileUtils.deleteDirectory(confsDir);
    } catch (Throwable ex) {
    }
    confsDir.mkdirs();
  }
    
  @Test
  public void testMainDaemon() throws Exception {
    logger.debug("Running testMainDaemon");
    GlobalOpenTelemetry.resetForTest();
    Main main = new Main();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    main.testMain(new String[]{
      "--persistence.datasource.url=" + postgres.getJdbcUrl()
      , "--persistence.datasource.adminUser.username=" + postgres.getUser()
      , "--persistence.datasource.adminUser.password=" + postgres.getPassword()
      , "--persistence.datasource.schema=public" 
      , "--baseConfigPath=" + CONFS_DIR
      , "--jwt.acceptableIssuerRegexes[0]=.*"
      , "--jwt.defaultJwksCacheDuration=PT1M"
      , "--jwt.jwksEndpoints[0]=http://localhost/jwks"
      , "--logging.jsonFormat=true"
      , "--enableBearerAuth=false"
      , "--sampleDataLoads[0].url=" + postgres.getVertxUrl()
      , "--sampleDataLoads[0].adminUser.username=" + postgres.getUser()
      , "--sampleDataLoads[0].adminUser.password=" + postgres.getPassword()
      , "--managementEndpoints[0]=up"
      , "--managementEndpoints[2]=prometheus"
      , "--managementEndpoints[3]=threads"
      , "--managementEndpointPort=" + mgmtPort
      , "--managementEndpointUrl=http://localhost:" + mgmtPort + "/manage"
      , "--tracing.protocol=otlphttp"
      , "--tracing.sampler=alwaysOn"
      , "--tracing.url=http://nonexistent/otlphttp"
    }
            , stdout
            , ImmutableMap.<String, String>builder()
                    .put("LOGGING_AS_JSON", "tRue")
                    .put("LOGGING_LEVEL_UK_co_sPuDsoft_query_logging", "trace")
                    .build());
    assertEquals(0, stdoutStream.size());
    
    RestAssured.port = main.getPort();
    
    given()
            .log().all()
            .get("/api/info/available")
            .then()
            .statusCode(200)
            .log().all()
            ;

    String body = given()
            .log().all()
            .get("/api/info/details/demo/FeatureRichExample")
            .then()
            .statusCode(200)
            .log().all()
            .extract().body().asString()
            ;
    
    assertThat(body, containsString("\"defaultValue\":\"1971-05-06\""));
    assertThat(body, containsString("\"defaultValue\":20"));
    assertThat(body, not(containsString("defaultValueExpression")));
    assertThat(body, not(containsString("clientIp")));
    assertThat(body, not(containsString("hidden")));
    assertThat(body, not(containsString("condition")));
    assertThat(body, not(containsString("37-17")));
    assertThat(body, not(containsString("\"20\"")));
    
    main.shutdown();
  }
    
}
