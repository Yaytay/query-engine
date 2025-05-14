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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.main.Main;

/**
 *
 * @author jtalbut
 */
public class DocHandlerNoneIT {
  
  private static final Logger logger = LoggerFactory.getLogger(DocHandlerNoneIT.class.getName());
  
  @Test
  public void testDocs() throws Exception {
    GlobalOpenTelemetry.resetForTest();
    Main main = new Main();
    String baseConfigDir = "target/query-engine/samples-dochandlernoneit";
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    main.testMain(new String[]{
        "--baseConfigPath=" + baseConfigDir
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
