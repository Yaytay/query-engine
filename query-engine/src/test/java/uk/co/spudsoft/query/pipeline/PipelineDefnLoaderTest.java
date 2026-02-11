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

import inet.ipaddr.IPAddressString;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.Json;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.File;
import java.time.Duration;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.dircache.DirCache;
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.main.CacheConfig;
import uk.co.spudsoft.query.web.rest.DocNodesTree;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class PipelineDefnLoaderTest {
  
  private static final Logger logger = LoggerFactory.getLogger(PipelineDefnLoaderTest.class);
  
  @Test
  public void testGetAccessible(Vertx vertx, VertxTestContext testContext) throws Exception {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    CacheConfig cacheConfig = new CacheConfig();
    cacheConfig.setPurgePeriod(Duration.ofMillis(100));
    cacheConfig.setMaxItems(10);
    PipelineDefnLoader loader = new PipelineDefnLoader(meterRegistry, vertx, cacheConfig, DirCache.cache(new File("target/classes/samples").toPath(), Duration.ofSeconds(1), Pattern.compile("\\..*"), null));
    Thread.sleep(2000);

    RequestContext req = new RequestContext(
            null
            , null
            , null
            , "localhost"
            , null
            , null
            , HeadersMultiMap.httpHeaders().add("Host", "localhost:123")
            , null
            , new IPAddressString("127.0.0.1")
            , null
    );
    loader.getAccessible(req)
            .onComplete(ar -> {
              if (ar.failed()) {
                testContext.failNow(ar.cause());
              } else {
                testContext.verify(() -> {
                  PipelineNodesTree.PipelineDir root = ar.result();
                  logger.debug("Nodes: {}", Json.encode(root));
                  assertEquals("", root.getName());
                  assertEquals("", root.getPath());
                  assertEquals(3, root.getChildren().size());
                  assertEquals("args", root.getChildren().get(0).getPath());
                  assertEquals("args", root.getChildren().get(0).getName());
                  assertEquals("demo", root.getChildren().get(1).getPath());
                  assertEquals("demo", root.getChildren().get(1).getName());
                  assertEquals("demo/FeatureRichExample", dir(root.getChildren().get(1)).getChildren().get(0).getPath());
                  assertEquals("FeatureRichExample", dir(root.getChildren().get(1)).getChildren().get(0).getName());
                  assertEquals("demo/LookupValues", dir(root.getChildren().get(1)).getChildren().get(1).getPath());
                  assertEquals("LookupValues", dir(root.getChildren().get(1)).getChildren().get(1).getName());
                  assertEquals("sub1", root.getChildren().get(2).getPath());
                  assertEquals("sub1", root.getChildren().get(2).getName());
                  assertEquals("sub1/sub2", dir(root.getChildren().get(2)).getChildren().get(0).getPath());
                  assertEquals("sub2", dir(root.getChildren().get(2)).getChildren().get(0).getName());
                  assertEquals("sub1/sub2/AllDynamicIT", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(0).getPath());
                  assertEquals("AllDynamicIT", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(0).getName());
                  assertEquals("sub1/sub2/ConcurrentRulesIT", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(1).getPath());
                  assertEquals("ConcurrentRulesIT", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(1).getName());
                  assertEquals("sub1/sub2/ConditionalArgument", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(2).getPath());
                  assertEquals("ConditionalArgument", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(2).getName());

                  assertEquals("sub1/sub2/DemoStatic", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(3).getPath());                  
                  assertEquals("DemoStatic", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(3).getName());

                  assertEquals("sub1/sub2/DynamicEndpointPipelineJdbcIT", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(4).getPath());
                  assertEquals("DynamicEndpointPipelineJdbcIT", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(4).getName());
                  
                  assertEquals("sub1/sub2/DynamicEndpointPipelineSqlIT", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(5).getPath());
                  assertEquals("DynamicEndpointPipelineSqlIT", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(5).getName());
                  
                  assertEquals("sub1/sub2/EmptyDataIT", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(6).getPath());
                  assertEquals("EmptyDataIT", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(6).getName());
                  
                  assertEquals("sub1/sub2/JsonToPipelineIT", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(7).getPath());
                  assertEquals("JsonToPipelineIT", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(7).getName());
                  
                  assertEquals("sub1/sub2/LookupIT", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(8).getPath());
                  assertEquals("LookupIT", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(8).getName());
                  
                  assertEquals("sub1/sub2/SortableIT", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(9).getPath());
                  assertEquals("SortableIT", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(9).getName());
                  
                  assertEquals("sub1/sub2/TemplatedJsonToPipelineIT", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(10).getPath());
                  assertEquals("TemplatedJsonToPipelineIT", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(10).getName());
                  
                  assertEquals("sub1/sub2/TemplatedYamlToPipelineIT", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(11).getPath());
                  assertEquals("TemplatedYamlToPipelineIT", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(11).getName());
                  
                  assertEquals("sub1/sub2/TestData", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(12).getPath());
                  assertEquals("TestData", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(12).getName());
                  
                  assertEquals("sub1/sub2/YamlToPipelineIT", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(13).getPath());
                  assertEquals("YamlToPipelineIT", dir(dir(root.getChildren().get(2)).getChildren().get(0)).getChildren().get(13).getName());
                });
                testContext.completeNow();
              }
            });
  }

  private PipelineNodesTree.PipelineDir dir(PipelineNodesTree.PipelineNode node) {
    return (PipelineNodesTree.PipelineDir) node;
  }
  
  @Test
  public void testGetInaccessible(Vertx vertx, VertxTestContext testContext) throws Exception {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    CacheConfig cacheConfig = new CacheConfig();
    cacheConfig.setMaxItems(10);
    PipelineDefnLoader loader = new PipelineDefnLoader(meterRegistry, vertx, cacheConfig, DirCache.cache(new File("target/classes/samples").toPath(), Duration.ofSeconds(1), Pattern.compile("\\..*"), null));
    Thread.sleep(2000);

    RequestContext req = new RequestContext(
            null
            , null
            , null
            , "unknown"
            , null
            , null
            , HeadersMultiMap.httpHeaders().add("Host", "bad")
            , null
            , new IPAddressString("12.34.56.78")
            , null
    );
    loader.getAccessible(req)
            .onComplete(ar -> {
              if (ar.failed()) {
                testContext.failNow(ar.cause());
              } else {
                testContext.verify(() -> {
                  PipelineNodesTree.PipelineDir root = ar.result();
                  assertEquals("", root.getName());
                  assertEquals("", root.getPath());
                  assertEquals(2, root.getChildren().size());
                  assertEquals("args", root.getChildren().get(0).getPath());
                  assertEquals("args", root.getChildren().get(0).getName());
                  assertEquals("demo", root.getChildren().get(1).getPath());
                  assertEquals("demo", root.getChildren().get(1).getName());
                  assertEquals("demo/FeatureRichExample", dir(root.getChildren().get(1)).getChildren().get(0).getPath());
                  assertEquals("FeatureRichExample", dir(root.getChildren().get(1)).getChildren().get(0).getName());
                  assertEquals("demo/LookupValues", dir(root.getChildren().get(1)).getChildren().get(1).getPath());
                  assertEquals("LookupValues", dir(root.getChildren().get(1)).getChildren().get(1).getName());
                });
                testContext.completeNow();
              }
            });
  }

}
