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
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class DesignHandlerTest {

  @Test
  public void testDetermineAbsolutePath(Vertx vertx) throws IOException, ServiceException {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    DirCache dirCache = DirCache.cache(new java.io.File("target/test-classes/sources").toPath(), Duration.of(1, ChronoUnit.SECONDS), Pattern.compile("\\..*"));
    PipelineDefnLoader loader = new PipelineDefnLoader(meterRegistry, vertx, new CacheConfig(), dirCache);
    DesignHandler dh = new DesignHandler(vertx, loader, dirCache);
    
    assertEquals("target" + java.io.File.separator + "test-classes" + java.io.File.separator + "sources" + java.io.File.separator + "bob", dh.resolveToAbsolutePath(dh.normalizePath("Test", "item", "bob")));
    assertThrows(ServiceException.class, () -> dh.resolveToAbsolutePath(dh.normalizePath("Test", "item", "bob" + java.io.File.separator + ".." + java.io.File.separator + "fred")));
    assertEquals("target" + java.io.File.separator + "test-classes" + java.io.File.separator + "sources" + java.io.File.separator + "schön", dh.resolveToAbsolutePath(dh.normalizePath("Test", "item", "scho\u0308n")));
  }
  
}
