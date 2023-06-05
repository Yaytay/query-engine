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
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.main.Main;

/**
 *
 * @author jtalbut
 */
public class DocHandlerIT {
  
  private static final Logger logger = LoggerFactory.getLogger(DocHandlerIT.class.getName());
  
  @Test
  public void testDocs() throws Exception {
    Main main = new Main();
    String baseConfigDir = "target/query-engine/samples";
    Main.prepareBaseConfigPath(new File(baseConfigDir));
    main.testMain(new String[]{
        "baseConfigPath=" + baseConfigDir
      , "vertxOptions.tracingOptions.serviceName=Query-Engine"
      , "jwt.acceptableIssuerRegexes[0]=.*"
      , "jwt.defaultJwksCacheDuration=PT1M"
      , "logging.jsonFormat=true"
      , "designMode=true"
    });

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
