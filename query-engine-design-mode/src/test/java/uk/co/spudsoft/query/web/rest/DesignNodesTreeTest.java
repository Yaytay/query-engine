/*
 * Copyright (C) 2023 njt
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

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.dircache.DirCacheTree;


/**
 *
 * @author njt
 */
public class DesignNodesTreeTest {
  
  @Test
  public void testDesignFileConstructor() {
    DesignNodesTree.DesignFile file = new DesignNodesTree.DesignFile("path", LocalDateTime.MIN, "name", 0);
    assertEquals("path", file.getPath());
    assertEquals("name", file.getName());
    assertEquals(LocalDateTime.MIN, file.getModified());
    assertEquals(0L, file.getSize());
    assertNull(file.getChildren());
  }
  
  @Test
  public void testDesignDirConstructor() {
    DesignNodesTree.DesignDir dir = new DesignNodesTree.DesignDir("path", LocalDateTime.MIN, "name", Arrays.asList(
            new DesignNodesTree.DesignFile("path", LocalDateTime.MIN, "name", 0)
    ));
    assertEquals("path", dir.getPath());
    assertEquals("name", dir.getName());
    assertEquals(LocalDateTime.MIN, dir.getModified());
    assertEquals(1, dir.getChildren().size());
  }
  
  @Test
  public void testRelativize() {
    if ("\\".equals(File.separator)) {
      assertEquals("..\\..\\target", DesignNodesTree.relativize("/", Path.of("parent", "child"), new DirCacheTree.Directory(Path.of("target"), LocalDateTime.now(ZoneOffset.UTC), Collections.emptyList())));
      assertEquals("../../target", DesignNodesTree.relativize("\\", Path.of("parent", "child"), new DirCacheTree.Directory(Path.of("target"), LocalDateTime.now(ZoneOffset.UTC), Collections.emptyList())));
    } else {
      assertEquals("../../target", DesignNodesTree.relativize("/", Path.of("parent", "child"), new DirCacheTree.Directory(Path.of("target"), LocalDateTime.now(ZoneOffset.UTC), Collections.emptyList())));
      assertEquals("../../target", DesignNodesTree.relativize("\\", Path.of("parent", "child"), new DirCacheTree.Directory(Path.of("target"), LocalDateTime.now(ZoneOffset.UTC), Collections.emptyList())));
    }
  }
  
}
