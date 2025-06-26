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
import java.io.File;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import uk.co.spudsoft.query.main.Main;

/**
 *
 * @author jtalbut
 */
public class DocHandlerIT {
  
  private static final Logger logger = LoggerFactory.getLogger(DocHandlerIT.class.getName());
  
  private static final String CONFS_DIR = "target/query-engine/samples-" + MethodHandles.lookup().lookupClass().getSimpleName().toLowerCase();
  
  @BeforeAll
  public void createDirs() {
    File confsDir = new File(CONFS_DIR);
    FileUtils.deleteQuietly(confsDir);
    confsDir.mkdirs();
  }
  
  @Test
  public void testDocs() throws Exception {
    GlobalOpenTelemetry.resetForTest();
    Main main = new Main();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    main.testMain(new String[]{
        "--baseConfigPath=" + CONFS_DIR
      , "--jwt.acceptableIssuerRegexes[0]=.*"
      , "--jwt.defaultJwksCacheDuration=PT1M"
      , "--logging.jsonFormat=true"
    }, stdout, System.getenv());

    RestAssured.port = main.getPort();
    
    ObjectNode json = given()
            .log().all()
            .get("/api/docs")
            .then()
            .log().ifError()
            .statusCode(200)
            .extract()
            .body().as(ObjectNode.class)
            ;
    DocNodesTree.DocDir docsDir = toDocsDir(json);
        
    Set<String> foundDocs = new HashSet<>();
    getAllDocs(foundDocs, docsDir);
    logger.debug("Found docs: {}", foundDocs);
    
    List<String> allDocs = DocHandlerTest.findAllDocs();
    logger.debug("All docs: {}", allDocs);
    for (String realDoc : allDocs) {
      if (!foundDocs.contains(realDoc)) {
        fail("Document " + realDoc + " not configured");
      }
    }
    
    given()
            .log().all()
            .get("/api/docs/Introduction.html")
            .then()
            .log().ifError()
            .statusCode(200)
            .contentType(ContentType.HTML)
            ;
    
    given()
            .log().all()
            .get("/api/docs/query-engine-compose.yml")
            .then()
            .log().ifError()
            .statusCode(200)
            .contentType("application/yaml")
            ;
    
    given()
            .log().all()
            .get("/api/docs/Samples/Test Database ERD.svg")
            .then()
            .log().ifError()
            .statusCode(200)
            .contentType("image/svg+xml")
            ;    
    
    main.shutdown();
  }
  
  private DocNodesTree.DocDir toDocsDir(ObjectNode json) {
  
    List<DocNodesTree.DocNode> children = new ArrayList<>();
    for (JsonNode child : ((ArrayNode) json.get("children"))) {
      if (child.has("children")) {
        children.add(toDocsDir((ObjectNode) child));
      } else {
        children.add(new DocNodesTree.DocFile(child.get("path").textValue(), child.get("name").textValue()));
      }
    }
    return new DocNodesTree.DocDir(json.get("path").textValue(), children);
    
  }
  
  void getAllDocs(Set<String> foundDocs, DocNodesTree.DocDir docs) {
    for (DocNodesTree.DocNode node : docs.getChildren()) {
      if (node instanceof DocNodesTree.DocFile doc) {
        foundDocs.add(doc.getPath());
        
        given()
                .log().all()
                .get("/api/docs/" + doc.getPath())
                .then()
                .log().ifError()
                .statusCode(200)
                ;
        
      } else if (node instanceof DocNodesTree.DocDir dir) {
        getAllDocs(foundDocs, dir);
      }
    }
  }
}
