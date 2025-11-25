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

import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.main.Main;

/**
 *
 * @author jtalbut
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DocHandlerNoneIT {
  
  private static final Logger logger = LoggerFactory.getLogger(DocHandlerNoneIT.class.getName());

  private static final String CONFS_DIR = "target/query-engine/samples-" + MethodHandles.lookup().lookupClass().getSimpleName().toLowerCase();
  
  @BeforeAll
  public void createDirs() {
    File confsDir = new File(CONFS_DIR);
    FileUtils.deleteQuietly(confsDir);
    confsDir.mkdirs();
  }
  
  @Test
  public void testDocs() throws Exception {
    Main main = new Main();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    main.testMain(new String[]{
        "--baseConfigPath=" + CONFS_DIR
      , "--jwt.acceptableIssuerRegexes[0]=.*"
      , "--jwt.defaultJwksCacheDuration=PT1M"
      , "--logging.jsonFormat=true"
      , "--alternativeDocumentation=/dev/null"
    }, stdout, System.getenv());

    RestAssured.port = main.getPort();
    
    given()
            .log().all()
            .get("/api/docs")
            .then()
            .log().ifError()
            .statusCode(404)
            ;
    
    given()
            .log().all()
            .get("/api/docs/Docs.html")
            .then()
            .log().ifError()
            .statusCode(404)
            ;
    
    given()
            .log().all()
            .get("/api/docs/query-engine-compose.yml")
            .then()
            .log().ifError()
            .statusCode(404)
            ;
    
    given()
            .log().all()
            .get("/api/docs/Samples/Test Database ERD.svg")
            .then()
            .log().ifError()
            .statusCode(404)
            ;    
    
    main.shutdown();
  }  
}
