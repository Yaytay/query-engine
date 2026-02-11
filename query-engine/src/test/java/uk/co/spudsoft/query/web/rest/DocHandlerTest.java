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
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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
    listKnownDocs(docsKnown, DocHandler.BUILT_IN_DOCS);
    docsKnown.sort(null);
    logger.info("Known docs: {} ", Json.encodePrettily(docsKnown));
    for (String doc : docsKnown) {
      docsOnDisc.remove(doc);
    }
    assertThat(docsOnDisc, is(empty()));
  }

  static List<String> findAllDocs() throws IOException {
    return findAllDocs("target/classes/docs");
  }

  static List<String> findAllDocs(String dir) throws IOException {
    Path root = new File(dir).toPath();
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

  @Test
  public void testBaseFilename() throws URISyntaxException {
    assertEquals("two", DocHandler.baseFileName(Path.of("one", "two")));
    assertEquals("two", DocHandler.baseFileName(Path.of("one", "two.xml")));
    if (System.getProperty("os.name").toLowerCase().contains("win")) {
      assertNull(DocHandler.baseFileName(Paths.get("C:\\")));
    } else {
      assertNull(DocHandler.baseFileName(Paths.get("/")));
    }
    assertEquals("two", DocHandler.baseFileName(Path.of("two.xml")));
  }

  @Test
  public void testBuildAltDocsBad() {
    DocNodesTree.DocNode root = DocHandler.buildAlternativeDocs("target/non-existant");
    assertNull(root);
  }

  @Test
  public void testDocGatherer() {
    DocHandler.DocGatherer gatherer = new DocHandler.DocGatherer("target");
    
    BasicFileAttributes bfa = mock(BasicFileAttributes.class);
    
    gatherer.preVisitDirectory(Path.of("one"), bfa);

    when(bfa.isSymbolicLink()).thenReturn(Boolean.TRUE);

    gatherer.visitFile(Path.of("one", "two"), bfa);
    assertTrue(gatherer.getCurrentNodesStack().getLast().isEmpty());
    
    FileVisitResult fvr = gatherer.preVisitDirectory(Path.of("one"), bfa);
    assertEquals(FileVisitResult.SKIP_SUBTREE, fvr);
    
    IOException ex = assertThrows(IOException.class, () -> {
      gatherer.postVisitDirectory(Path.of("one"), new IOException("test"));
    });
    assertEquals("test", ex.getMessage());
    
  }

  @Test
  public void testBuildAltDocs() throws IOException {
    Path rootPath = Paths.get("target/DocHandlerTest");

    FileUtils.deleteDirectory(rootPath.toFile());
    FileUtils.copyDirectory(new File("target/classes/samples"), rootPath.toFile());
    Files.createDirectory(rootPath.resolve("empty"));
    try {
      Files.createLink(rootPath.resolve("newArgs"), rootPath.resolve("args"));
      Files.createLink(rootPath.resolve("args/Args00-link.yaml"), rootPath.resolve("args/Args00.yaml"));
    } catch (IOException ex) {
      logger.warn("Unable to create soft links (probably running Windows: ", ex);
    }

    DocNodesTree.DocDir root = DocHandler.buildAlternativeDocs(rootPath.toString());
    assertNotNull(root);

    logger.debug("{}", "Result: " + Json.encode(root));

    assertEquals("", root.getName());
    assertEquals("", root.getPath());
    assertEquals(4, root.getChildren().size());

    DocNodesTree.DocDir argsNode = dir(root.getChildren().stream().filter(n -> "args".equals(n.getName())).findFirst().get());

    assertEquals("args", argsNode.getName());
    assertEquals("args", argsNode.getPath());
    assertEquals(17, argsNode.getChildren().size());

    DocNodesTree.DocNode args00Node = argsNode.getChildren().stream().filter(n -> n.getName().endsWith("Args00")).findFirst().get();

    assertEquals("Args00", args00Node.getName());
    assertEquals("args" + File.separator + "Args00.yaml", args00Node.getPath());
    assertEquals("Args00", ((DocNodesTree.DocFile) args00Node).getTitle());

    DocNodesTree.DocDir sub1Node = dir(root.getChildren().stream().filter(n -> "sub1".equals(n.getName())).findFirst().get());

    assertEquals("sub1", sub1Node.getName());
    assertEquals("sub1", sub1Node.getPath());

    DocNodesTree.DocDir sub2Node = dir(sub1Node.getChildren().stream().filter(n -> n.getName().endsWith("sub2")).findFirst().get());

    assertEquals("sub2", sub2Node.getName());
    assertEquals("sub1" + File.separator + "sub2", sub2Node.getPath());

    DocNodesTree.DocNode sub1PermsNode = sub1Node.getChildren().stream().filter(n -> n.getName().endsWith("permissions")).findFirst().get();

    assertEquals("permissions", sub1PermsNode.getName());
    assertEquals("sub1" + File.separator + "permissions.jexl", sub1PermsNode.getPath());

    DocNodesTree.DocNode dynNode = sub2Node.getChildren().stream().filter(n -> n.getName().endsWith("AllDynamicIT")).findFirst().get();

    assertEquals("AllDynamicIT", dynNode.getName());
    assertEquals("sub1" + File.separator + "sub2" + File.separator + "AllDynamicIT.yaml", dynNode.getPath());
    assertEquals("AllDynamicIT", ((DocNodesTree.DocFile) dynNode).getTitle());
  }

  private DocNodesTree.DocDir dir(DocNodesTree.DocNode node) {
    return (DocNodesTree.DocDir) node;
  }
}
