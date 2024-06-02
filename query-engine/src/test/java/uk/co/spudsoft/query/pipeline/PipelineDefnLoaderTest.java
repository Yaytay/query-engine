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
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.main.CacheConfig;

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
    cacheConfig.setMaxItems(10);
    PipelineDefnLoader loader = new PipelineDefnLoader(meterRegistry, vertx, cacheConfig, DirCache.cache(new File("target/classes/samples").toPath(), Duration.ofSeconds(1), Pattern.compile("\\..*")));
    Thread.sleep(2000);

    RequestContext req = new RequestContext(
            null
            , null
            , "localhost"
            , null
            , null
            , new HeadersMultiMap().add("Host", "localhost:123")
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
                  assertEquals("demo/FeatureRichExample", root.getChildren().get(1).getChildren().get(0).getPath());
                  assertEquals("FeatureRichExample", root.getChildren().get(1).getChildren().get(0).getName());
                  assertEquals("demo/LookupValues", root.getChildren().get(1).getChildren().get(1).getPath());
                  assertEquals("LookupValues", root.getChildren().get(1).getChildren().get(1).getName());
                  assertEquals("sub1", root.getChildren().get(2).getPath());
                  assertEquals("sub1", root.getChildren().get(2).getName());
                  assertEquals("sub1/sub2", root.getChildren().get(2).getChildren().get(0).getPath());
                  assertEquals("sub2", root.getChildren().get(2).getChildren().get(0).getName());
                  assertEquals("sub1/sub2/AllDynamicIT", root.getChildren().get(2).getChildren().get(0).getChildren().get(0).getPath());
                  assertEquals("AllDynamicIT", root.getChildren().get(2).getChildren().get(0).getChildren().get(0).getName());
                  assertEquals("sub1/sub2/ConcurrentRulesIT", root.getChildren().get(2).getChildren().get(0).getChildren().get(1).getPath());
                  assertEquals("ConcurrentRulesIT", root.getChildren().get(2).getChildren().get(0).getChildren().get(1).getName());
                  assertEquals("sub1/sub2/ConditionalArgument", root.getChildren().get(2).getChildren().get(0).getChildren().get(2).getPath());                  
                  assertEquals("ConditionalArgument", root.getChildren().get(2).getChildren().get(0).getChildren().get(2).getName());
                  assertEquals("sub1/sub2/DynamicEndpointPipelineIT", root.getChildren().get(2).getChildren().get(0).getChildren().get(3).getPath());                  
                  assertEquals("DynamicEndpointPipelineIT", root.getChildren().get(2).getChildren().get(0).getChildren().get(3).getName());
                  assertEquals("sub1/sub2/EmptyDataIT", root.getChildren().get(2).getChildren().get(0).getChildren().get(4).getPath());
                  assertEquals("EmptyDataIT", root.getChildren().get(2).getChildren().get(0).getChildren().get(4).getName());
                  assertEquals("sub1/sub2/JsonToPipelineIT", root.getChildren().get(2).getChildren().get(0).getChildren().get(5).getPath());
                  assertEquals("JsonToPipelineIT", root.getChildren().get(2).getChildren().get(0).getChildren().get(5).getName());
                  assertEquals("sub1/sub2/LookupIT", root.getChildren().get(2).getChildren().get(0).getChildren().get(6).getPath());
                  assertEquals("LookupIT", root.getChildren().get(2).getChildren().get(0).getChildren().get(6).getName());
                  assertEquals("sub1/sub2/SortableIT", root.getChildren().get(2).getChildren().get(0).getChildren().get(7).getPath());
                  assertEquals("SortableIT", root.getChildren().get(2).getChildren().get(0).getChildren().get(7).getName());
                  assertEquals("sub1/sub2/TemplatedJsonToPipelineIT", root.getChildren().get(2).getChildren().get(0).getChildren().get(8).getPath());
                  assertEquals("TemplatedJsonToPipelineIT", root.getChildren().get(2).getChildren().get(0).getChildren().get(8).getName());
                  assertEquals("sub1/sub2/TemplatedYamlToPipelineIT", root.getChildren().get(2).getChildren().get(0).getChildren().get(9).getPath());
                  assertEquals("TemplatedYamlToPipelineIT", root.getChildren().get(2).getChildren().get(0).getChildren().get(9).getName());
                  assertEquals("sub1/sub2/TestData", root.getChildren().get(2).getChildren().get(0).getChildren().get(10).getPath());
                  assertEquals("TestData", root.getChildren().get(2).getChildren().get(0).getChildren().get(10).getName());
                  assertEquals("sub1/sub2/YamlToPipelineIT", root.getChildren().get(2).getChildren().get(0).getChildren().get(11).getPath());
                  assertEquals("YamlToPipelineIT", root.getChildren().get(2).getChildren().get(0).getChildren().get(11).getName());
                });
                testContext.completeNow();
              }
            });
  }

  
  @Test
  public void testGetInaccessible(Vertx vertx, VertxTestContext testContext) throws Exception {
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    CacheConfig cacheConfig = new CacheConfig();
    cacheConfig.setMaxItems(10);
    PipelineDefnLoader loader = new PipelineDefnLoader(meterRegistry, vertx, cacheConfig, DirCache.cache(new File("target/classes/samples").toPath(), Duration.ofSeconds(1), Pattern.compile("\\..*")));
    Thread.sleep(2000);

    RequestContext req = new RequestContext(
            null
            , null
            , "unknown"
            , null
            , null
            , new HeadersMultiMap().add("Host", "bad")
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
                  assertEquals("demo/FeatureRichExample", root.getChildren().get(1).getChildren().get(0).getPath());
                  assertEquals("FeatureRichExample", root.getChildren().get(1).getChildren().get(0).getName());
                  assertEquals("demo/LookupValues", root.getChildren().get(1).getChildren().get(1).getPath());
                  assertEquals("LookupValues", root.getChildren().get(1).getChildren().get(1).getName());
                });
                testContext.completeNow();
              }
            });
  }

}
