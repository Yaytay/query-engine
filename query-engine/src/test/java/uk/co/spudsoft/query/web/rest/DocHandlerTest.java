/*
 * Copyright (C) 2023 jtalbut
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

import io.vertx.core.json.Json;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author jtalbut
 */
public class DocHandlerTest {
  
  private static final Logger logger = LoggerFactory.getLogger(DocHandlerTest.class);
  
  /**
   * Test of getDoc method, of class DocHandler.
   */
  @Test
  public void testConfiguredFiles() throws IOException {
    List<String> docsOnDisc = findAllDocs();
    docsOnDisc.sort(null);
    logger.info("Docs on disc: {}", docsOnDisc);
    List<String> docsKnown = new ArrayList<>();
    listKnownDocs(docsKnown, DocHandler.DOCS);
    docsKnown.sort(null);
    logger.info("Known docs: {} ", Json.encodePrettily(docsKnown));
    for (String doc : docsKnown) {
      docsOnDisc.remove(doc);
    }
    assertThat(docsOnDisc, is(empty()));
  }
  
  
  static List<String> findAllDocs() throws IOException {
    Path root = new File("target/classes/docs").toPath();
    List<String> result = new ArrayList<>();
    try (Stream<Path> strm = Files.find(root, 10, (path, attr) -> attr.isRegularFile())) {
      strm.forEach(p -> {
        String docPath = root.relativize(p).toString();
        // Just to make Windows happy
        docPath = docPath.replace("\\", "/");
        // The README.html file is not included in the docs output because the table of contents is always in a separate pane
        if (!"README.html".equals(docPath)) {
          result.add(docPath);
        }
      });
    }
    return result;
  }
  
  void listKnownDocs(List<String> foundDocs, DocNodesTree.DocDir docs) {
    for (DocNodesTree.DocNode node : docs.getChildren()) {
      if (node instanceof DocNodesTree.DocFile doc) {
        foundDocs.add(doc.getPath());
      } else if (node instanceof DocNodesTree.DocDir dir) {
        listKnownDocs(foundDocs, dir);
      }
    }
  }
}
