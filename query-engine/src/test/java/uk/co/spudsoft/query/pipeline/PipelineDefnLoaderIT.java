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
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.regex.Pattern;
import net.jcip.annotations.NotThreadSafe;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.dircache.DirCache;
import uk.co.spudsoft.query.main.CacheConfig;
import uk.co.spudsoft.query.main.Main;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@NotThreadSafe
public class PipelineDefnLoaderIT {
  
  private static final Logger logger = LoggerFactory.getLogger(PipelineDefnLoaderIT.class);

  private static final String CONFS_DIR = "target/query-engine/samples-" + MethodHandles.lookup().lookupClass().getSimpleName().toLowerCase();
  
  @Test
  public void testGetAccessible(Vertx vertx, VertxTestContext testContext) throws Exception {
        
    Main.prepareBaseConfigPath(new File(CONFS_DIR), null);
    
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    CacheConfig cacheConfig = new CacheConfig();
    cacheConfig.setPurgePeriod(Duration.ofMillis(100));
    cacheConfig.setMaxItems(10);
    PipelineDefnLoader loader = new PipelineDefnLoader(meterRegistry, vertx, cacheConfig, DirCache.cache(new File("target/classes/samples").toPath(), Duration.ofSeconds(1), Pattern.compile("\\..*"), null));

    loader.readJsonFile(CONFS_DIR + "/sub1/sub2/JsonToPipelineIT.json")
            .compose(p -> {
              testContext.verify(() -> {
                assertNotNull(p);
              });
              return loader.writeJsonFile(CONFS_DIR + "/sub1/sub2/JsonToPipelineIT.json", p);
            })
            .compose(v -> {
              return loader.readYamlFile(CONFS_DIR + "/sub1/sub2/YamlToPipelineIT.yaml");
            })
            .compose(p -> {
              testContext.verify(() -> {
                assertNotNull(p);
              });
              return loader.writeYamlFile(CONFS_DIR + "/sub1/sub2/YamlToPipelineIT.yaml", p);
            })
            .compose(v -> {
              return loader.writeJsonFile("nonexistant/nonexisteant/file.json", null);
            })
            .andThen(ar -> {
              testContext.verify(() -> {
                assertTrue(ar.failed());
              });
            })
            .recover(ex -> Future.succeededFuture())
            .compose(v -> {
              return loader.readJsonFile("nonexistant/nonexisteant/file.json");
            })
            .andThen(ar -> {
              testContext.verify(() -> {
                assertTrue(ar.failed());
              });
            })
            .recover(ex -> Future.succeededFuture())
            .compose(p -> {
              return loader.writeYamlFile("nonexistant/nonexisteant/file.yaml", null);
            })
            .andThen(ar -> {
              testContext.verify(() -> {
                assertTrue(ar.failed());
              });
            })
            .recover(ex -> Future.succeededFuture())
            .compose(v -> {
              return loader.readYamlFile("nonexistant/nonexisteant/file.yaml");
            })
            .andThen(ar -> {
              testContext.verify(() -> {
                assertTrue(ar.failed());
              });
            })
            .recover(ex -> Future.succeededFuture())
            .andThen(testContext.succeedingThenComplete())
            ;
        
  }

}
