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
import io.vertx.junit5.VertxExtension;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import uk.co.spudsoft.query.testcontainers.ServerProviderMySQL;

/**
 * Note that this set of tests requires the sample data to be loaded, but relies on the "loadSampleData" flag to make it happen.
 * When running with the full set of tests this won't actually stress that flag because others tests may have already
 * loaded the sample data.
 * 
 * Furthermore this test requires that the MySQL instance is listening on port 2001, which it won't be if it is
 * run dynamically.
 * Thus, usually, this test is disabled.
 * 
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LookupIT {
  
  private static final ServerProviderMySQL mysql = new ServerProviderMySQL().init();
  
  private static final String CONFS_DIR = "target/query-engine/samples-" + MethodHandles.lookup().lookupClass().getSimpleName().toLowerCase();
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(LookupIT.class);
  
  @BeforeAll
  public void createDirs() {
    File confsDir = new File(CONFS_DIR);
    FileUtils.deleteQuietly(confsDir);
    confsDir.mkdirs();
  }
  
  @Test
  public void testQuery() throws Exception {
    Main main = new Main();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    main.testMain(new String[]{
      "--persistence.datasource.url=" + mysql.getJdbcUrl()
      , "--persistence.datasource.adminUser.username=" + mysql.getUser()
      , "--persistence.datasource.adminUser.password=" + mysql.getPassword()
      , "--persistence.datasource.user.username=" + mysql.getUser()
      , "--persistence.datasource.user.password=" + mysql.getPassword()
      , "--persistence.retryLimit=100"
      , "--baseConfigPath=" + CONFS_DIR
      , "--vertxOptions.eventLoopPoolSize=5"
      , "--vertxOptions.workerPoolSize=5"
      , "--httpServerOptions.tracingPolicy=ALWAYS"
      , "--pipelineCache.maxDuration=PT10M"
      , "--logging.jsonFormat=false"
      , "--jwt.acceptableIssuerRegexes[0]=.*"
      , "--jwt.defaultJwksCacheDuration=PT1M"
      , "--sampleDataLoads[0].url=" + mysql.getVertxUrl()
      , "--sampleDataLoads[0].user.username=" + mysql.getUser()
      , "--sampleDataLoads[0].user.password=" + mysql.getPassword()
      , "--outputCacheDir=target/temp/" + this.getClass().getSimpleName() + "/cache"
    }, stdout, System.getenv());
    
    RestAssured.port = main.getPort();
    
    String body = given()
            .queryParam("port", mysql.getPort())
            .get("/query/sub1/sub2/LookupIT.tsv")
            .then()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("\"dataId\"\t\"instant\"\t\"ref1\"\t\"ref2\"\t\"ref3\"\t\"ref4\"\t\"ref5\"\t\"ref6\"\t\"value1\"\t\"value2\"\t\"value3\"\t\"value4\"\t\"value5\"\t\"value6\""));
    String[] lines = body.split("\n");
    assertThat(lines.length, greaterThan(10000));
    printLines(lines, 10);
    assertEquals("4\t\"1971-05-10T12:00\"\t\"96eb6f116108b5e2\"\t\"60789943f14d2f37\"\t\"48e7153725d96dd4\"\t\"670076c476070a66\"\t\t\t\"four\"\t\"eight\"\t\"twelve\"\t\"sixteen\"\t\t", lines[4]);
            
    body = given()
            .queryParam("port", mysql.getPort())
            .queryParam("removeRefColumnsFromResults", "true")
            .get("/query/sub1/sub2/LookupIT.tsv")
            .then()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("\"dataId\"\t\"instant\"\t\"value1\"\t\"value2\"\t\"value3\"\t\"value4\"\t\"value5\"\t\"value6\""));
    lines = body.split("\n");
    assertThat(lines.length, greaterThan(10000));
    printLines(lines, 10);
    assertEquals("4\t\"1971-05-10T12:00\"\t\"four\"\t\"eight\"\t\"twelve\"\t\"sixteen\"\t\t", lines[4]);
            
    body = given()
            .queryParam("port", mysql.getPort())
            .queryParam("includeLookupsInQuery", "true")
            .get("/query/sub1/sub2/LookupIT.tsv")
            .then()
            .statusCode(200)
            .extract().body().asString();
    
    assertThat(body, startsWith("\"dataId\"\t\"instant\"\t\"ref1\"\t\"ref2\"\t\"ref3\"\t\"ref4\"\t\"ref5\"\t\"ref6\"\t\"value1\"\t\"value2\"\t\"value3\"\t\"value4\"\t\"value5\"\t\"value6\""));
    lines = body.split("\n");
    assertThat(lines.length, greaterThan(10000));
    printLines(lines, 10);
    assertEquals("4\t\"1971-05-10T12:00\"\t\"96eb6f116108b5e2\"\t\"60789943f14d2f37\"\t\"48e7153725d96dd4\"\t\"670076c476070a66\"\t\t\t\"four\"\t\"eight\"\t\"twelve\"\t\"sixteen\"\t\t", lines[4]);
            
    main.shutdown();

    // Audit records should all have been sorted by main.shutdown
    assertTrue(TestHelpers.getDirtyAudits(logger, mysql.getJdbcUrl(), mysql.getUser(), mysql.getPassword()).isEmpty());
  }
  
  private void printLines(String[] lines, int max) {
    for (int i = 0; i < max; ++i) {
      if (i < lines.length) {
        logger.info("{}", lines[i]);
      }
    }
  }
  
}
