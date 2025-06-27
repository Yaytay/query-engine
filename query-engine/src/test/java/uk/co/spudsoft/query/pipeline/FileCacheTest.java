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
package uk.co.spudsoft.query.pipeline;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.dircache.DirCache;
import uk.co.spudsoft.dircache.DirCacheTree;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class FileCacheTest {
  
  private static final Logger logger = LoggerFactory.getLogger(FileCacheTest.class);
  
  private File create(File parent, String name, String contents) throws IOException {
    File file = new File(parent, name);
    if (!file.exists()) {
      try (FileOutputStream fos = new FileOutputStream(file)) {
        fos.write(contents.getBytes(StandardCharsets.UTF_8));
      }
    }
    return file;
  }
  
  /**
   * Test of purge method, of class FileCache.
   */
  @Test
  public void testPurge(Vertx vertx, VertxTestContext testContext) throws Exception {
    File dir = new File("target/FileCacheTest");
    dir.mkdirs();
    File subdir = new File(dir, "sub");
    subdir.mkdirs();
    
    File file1 = create(dir, "file1", "wibble");
    File file2 = create(dir, "file2", "barn");
    File file3 = create(subdir, "file3", "lower");
    
    DirCache dirCache = DirCache.cache(dir.toPath(), Duration.ofMillis(10), Pattern.compile("\\..*"), null);
    Thread.sleep(100);
    assertEquals(3, dirCache.getRoot().getChildren().size());
    
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    
    FileCache<String> cache = new FileCache<>(vertx.fileSystem(), meterRegistry, "file-cache-test", 10, Duration.ofMillis(10000));
    
    logger.debug("Children: {}", dirCache.getRoot().getChildren().stream().map(f -> f.getPath()).collect(Collectors.toList()));
    // Child 0 is the sub directory, dirs always come before files.
    DirCacheTree.File fileNode1 = (DirCacheTree.File) dirCache.getRoot().getChildren().get(1);
    DirCacheTree.File fileNode2 = (DirCacheTree.File) dirCache.getRoot().getChildren().get(2);
    cache.get(fileNode1, buffer -> buffer.toString(StandardCharsets.UTF_8))
            .compose(contents1 -> {
              testContext.verify(() -> {
                assertEquals("file1".equals(fileNode1.getName()) ? "wibble" : "barn", contents1);
                assertEquals(1, cache.size());
              });
              return cache.get(fileNode2, buffer -> buffer.toString(StandardCharsets.UTF_8));
            })
            .compose(contents2 -> {
              testContext.verify(() -> {
                assertEquals("file1".equals(fileNode2.getName()) ? "wibble" : "barn", contents2);
                assertEquals(2, cache.size());
              });
              return cache.get((DirCacheTree.File) dirCache.getRoot().getDir("sub").getChildren().get(0), buffer -> buffer.toString(StandardCharsets.UTF_8));
            })
            .compose(contents3 -> {
              testContext.verify(() -> {
                assertEquals("lower", contents3);
                assertEquals(3, cache.size());
              });
              Set<DirCacheTree.File> files = new HashSet<>();
              PipelineDefnLoader.addFilesToSet(dirCache.getRoot(), files);
              cache.purge(files);

              testContext.verify(() -> {
                assertEquals(3, cache.size());
              });

              if (!file2.delete()) {
                logger.warn("Failed to delete file2 ({})", file2);
              }
              try {
                Thread.sleep(50);
              } catch (InterruptedException ex) {
              }

              testContext.verify(() -> {
                logger.debug("Children: {}", dirCache.getRoot().getChildren().stream().map(f -> f.getPath()).collect(Collectors.toList()));
                assertEquals(2, dirCache.getRoot().getChildren().size());
                assertEquals(3, cache.size());
              });
              
              files = new HashSet<>();
              PipelineDefnLoader.addFilesToSet(dirCache.getRoot(), files);
              cache.purge(files);

              testContext.verify(() -> {
                assertEquals(2, dirCache.getRoot().getChildren().size());
                assertEquals(2, cache.size());
              });
              
              return Future.succeededFuture();
            })
            .onComplete(testContext.succeedingThenComplete());
    
  }
  
}
