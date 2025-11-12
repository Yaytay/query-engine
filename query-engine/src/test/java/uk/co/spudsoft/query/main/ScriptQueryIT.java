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

import com.google.common.cache.Cache;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.vertx.junit5.VertxExtension;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import uk.co.spudsoft.jwtvalidatorvertx.AlgorithmAndKeyPair;
import uk.co.spudsoft.jwtvalidatorvertx.jdk.JdkJwksHandler;
import uk.co.spudsoft.query.testcontainers.ServerProviderMySQL;

/**
 * Note that this set of tests requires the sample data to be loaded, but relies on the "loadSampleData" flag to make it happen.
 * When running with the full set of tests this won't actually stress that flag because others tests may have already
 * loaded the sample data.
 * 
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class ScriptQueryIT {
  
  private static final ServerProviderMySQL mysql = new ServerProviderMySQL().init();

  private static final String CONFS_DIR = "target/query-engine/samples-" + MethodHandles.lookup().lookupClass().getSimpleName().toLowerCase();
  
  private JdkJwksHandler jwks;

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ScriptQueryIT.class);
  
  @BeforeAll
  public void createDirs() throws IOException {
    File confsDir = new File(CONFS_DIR);
    FileUtils.deleteQuietly(confsDir);
    confsDir.mkdirs();

    Cache<String, AlgorithmAndKeyPair> keyCache = AlgorithmAndKeyPair.createCache(Duration.ofMinutes(1));

    jwks = JdkJwksHandler.create();
    logger.debug("Starting JWKS endpoint");
    jwks.start();
    jwks.setKeyCache(keyCache);
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
      , "--jwt.jwksEndpoints[0]=" + jwks.getBaseUrl() + "/jwks"
      , "--jwt.defaultJwksCacheDuration=PT1M"
    }, stdout, System.getenv());
    
    RestAssured.port = main.getPort();
    
    String body = given()
            .queryParam("arg1", "First")
            .queryParam("arg2", "Second")
            .accept("text/tsv")
            .log().all()
            .get("/query/args/Args02")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract().body().asString();
    
    logger.debug("Result: {}", body);
    assertThat(body, startsWith("\"value\"\t\"name\"\t\"arg1\"\t\"arg2\"\n0\t\"Source\"\t\"First\"\t\"Second\"\n"));
    
    main.shutdown();

    // Audit records should all have been sorted by main.shutdown
    assertTrue(TestHelpers.getDirtyAudits(logger, mysql.getJdbcUrl(), mysql.getUser(), mysql.getPassword()).isEmpty());
  }
  
}
