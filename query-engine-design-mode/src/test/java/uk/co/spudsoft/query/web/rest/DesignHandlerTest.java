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

import com.google.common.net.MediaType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.dircache.DirCache;
import uk.co.spudsoft.query.main.CacheConfig;
import uk.co.spudsoft.query.pipeline.PipelineDefnLoader;
import uk.co.spudsoft.query.web.ServiceException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import uk.co.spudsoft.query.main.Main;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class DesignHandlerTest {

  @Test
  public void testDetermineAbsolutePath(Vertx vertx) throws IOException, ServiceException {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    java.io.File rootDir = new java.io.File("target/query-engine/samples-designhandlertest");
    Main.prepareBaseConfigPath(rootDir, null);
    DirCache dirCache = DirCache.cache(rootDir.toPath(), Duration.of(1, ChronoUnit.SECONDS), Pattern.compile("\\..*"));
    PipelineDefnLoader loader = new PipelineDefnLoader(meterRegistry, vertx, new CacheConfig(), dirCache);
    DesignHandler dh = new DesignHandler(vertx, loader, dirCache);
    
    assertEquals("target" + java.io.File.separator + "query-engine" + java.io.File.separator + "samples-designhandlertest" + java.io.File.separator + "bob", dh.resolveToAbsolutePath(dh.normalizePath("Test", "item", "bob")));
    assertThrows(ServiceException.class, () -> dh.resolveToAbsolutePath(dh.normalizePath("Test", "item", "bob" + java.io.File.separator + ".." + java.io.File.separator + "fred")));
    assertEquals("target" + java.io.File.separator + "query-engine" + java.io.File.separator + "samples-designhandlertest" + java.io.File.separator + "schön", dh.resolveToAbsolutePath(dh.normalizePath("Test", "item", "scho\u0308n")));
  }
  
  @Test
  public void testGetFileTypeFromName() {
    assertEquals(DesignHandler.MEDIA_TYPE_JSON_TYPE, DesignHandler.getFileTypeFromName("bob.json"));
    assertEquals(DesignHandler.MEDIA_TYPE_VELOCITY_JSON_TYPE, DesignHandler.getFileTypeFromName("bob.json.vm"));
    assertThrows(IllegalArgumentException.class, () -> {DesignHandler.getFileTypeFromName("bob.fred.vm");});
    assertThrows(IllegalArgumentException.class, () -> {DesignHandler.getFileTypeFromName("bob.fred");});
    assertThrows(IllegalArgumentException.class, () -> {DesignHandler.getFileTypeFromName("bob");});
  }
  
  @Test
  public void testPrefers() {
    assertFalse(DesignHandler.prefers("", MediaType.PDF, MediaType.JPEG));
  }
  
}
