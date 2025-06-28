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
import io.restassured.response.Response;
import io.vertx.ext.web.impl.Utils;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
public class CachingForkIT {
  
  private static final ServerProviderMySQL mysql = new ServerProviderMySQL().init();
  
  private static final String CONFS_DIR = "target/query-engine/samples-" + MethodHandles.lookup().lookupClass().getSimpleName().toLowerCase();
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(CachingForkIT.class);
  
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
    String cacheDir = "target/temp/" + this.getClass().getSimpleName() + "/cache";
    
    main.testMain(new String[]{
      "--persistence.datasource.url=" + mysql.getJdbcUrl()
      , "--persistence.datasource.adminUser.username=" + mysql.getUser()
      , "--persistence.datasource.adminUser.password=" + mysql.getPassword()
      , "--persistence.datasource.user.username=" + mysql.getUser()
      , "--persistence.datasource.user.password=" + mysql.getPassword()
      , "--persistence.retryLimit=100"
      , "--persistence.retryIncrementMs=500"
      , "--baseConfigPath=" + CONFS_DIR
      , "--vertxOptions.eventLoopPoolSize=5"
      , "--vertxOptions.workerPoolSize=5"
      , "--pipelineCache.maxDuration=PT10M"
      , "--logging.jsonFormat=false"
      , "--zipkin.baseUrl=http://localhost/wontwork"
      , "--jwt.acceptableIssuerRegexes[0]=.*"
      , "--jwt.defaultJwksCacheDuration=PT1M"
      , "--outputCacheDir=" + cacheDir
    }, stdout, System.getenv());
    
    RestAssured.port = main.getPort();
    
    // 1. Prime cache, takes 37s.
    
    long start = System.currentTimeMillis();
    
    Response response1 = given()
            .queryParam("rows", "37")
            .queryParam("delay", "100")
            .log().all()
            .get("/query/sub1/sub2/TestData.tsv")
            .then()
            .log().all()
            .statusCode(200)
            .extract().response();
    
    long end = System.currentTimeMillis();
    long duration1 = end - start;
    assertThat(duration1, greaterThan(3700L));
    
    String body1 = response1.asString();
    
    String lastModified1 = response1.getHeader("Last-Modified");
    assertNotNull(lastModified1);
    
    assertThat(body1, startsWith("\"value\"\t\"name\""));
    assertThat(body1, not(containsString("\t\t\t\t\t\t\t")));
    assertThat(body1, not(containsString("37\t\"test\"")));
    assertThat(body1, containsString("36\t\"test\""));
    assertThat(body1, containsString("0\t\"test\""));
    int rows1 = body1.split("\n").length;
    assertEquals(38, rows1);
    
    // 2. Get from cache
    
    start = System.currentTimeMillis();
    
    Response response2 = given()
            .queryParam("rows", "37")
            .queryParam("delay", "100")
            .log().all()
            .get("/query/sub1/sub2/TestData.tsv")
            .then()
            .log().all()
            .statusCode(200)
            .extract().response();
    
    end = System.currentTimeMillis();
    long duration2 = end - start;
    assertThat(duration2, lessThan(2000L));

    String body2 = response2.asString();
    
    String lastModified2 = response1.getHeader("Last-Modified");
    assertEquals(lastModified1, lastModified2);

    assertEquals(body1, body2);
    
    // 3. Don't return data because it's not been modified
    
    start = System.currentTimeMillis();
    
    Response response3 = given()
            .header("If-Modified-Since", Utils.formatRFC1123DateTime(System.currentTimeMillis()))
            .queryParam("rows", "37")
            .queryParam("delay", "100")
            .log().all()
            .get("/query/sub1/sub2/TestData.tsv")
            .then()
            .log().all()
            .statusCode(304)
            .extract().response();
    
    end = System.currentTimeMillis();
    long duration3 = end - start;
    assertThat(duration3, lessThan(2000L));

    String body3 = response3.asString();
    
    String lastModified3 = response1.getHeader("Last-Modified");
    assertEquals(lastModified1, lastModified3);

    assertEquals("", body3);
    
    // 4. Find the file and delete it, then get, should work but be slow

    for (String file : main.getVertx().fileSystem().readDirBlocking(cacheDir)) {
      logger.debug("Deleting {}", file);
      main.getVertx().fileSystem().deleteBlocking(file);
    }

    start = System.currentTimeMillis();
    
    Response response4 = given()
            .queryParam("rows", "37")
            .queryParam("delay", "100")
            .log().all()
            .get("/query/sub1/sub2/TestData.tsv")
            .then()
            .log().all()
            .statusCode(200)
            .extract().response();
    
    end = System.currentTimeMillis();
    long duration4 = end - start;
    assertThat(duration4, greaterThan(3700L));

    String body4 = response4.asString();
    
    String lastModified4 = response4.getHeader("Last-Modified");
    assertThat(lastModified4, not(equalTo(lastModified1)));

    assertEquals(body1, body4);
    
    main.shutdown();
  }
  
}
