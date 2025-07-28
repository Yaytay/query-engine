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
package uk.co.spudsoft.query.exec;

import inet.ipaddr.IPAddressString;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.main.CacheConfig;
import uk.co.spudsoft.query.testcontainers.ServerProvider;
import uk.co.spudsoft.query.testcontainers.ServerProviderPostgreSQL;
import uk.co.spudsoft.query.pipeline.PipelineDefnLoader;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import uk.co.spudsoft.dircache.DirCache;
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.defn.Format;


/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class TemplatedYamlToPipelineIT {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(TemplatedYamlToPipelineIT.class);

  private final ServerProvider serverProvider = new ServerProviderPostgreSQL();  
  
  @Test
  @Timeout(value = 120, timeUnit = TimeUnit.SECONDS)
  public void testParsingJsonToPipelineStreaming(Vertx vertx, VertxTestContext testContext) throws Throwable {

    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    CacheConfig cacheConfig = new CacheConfig();
    cacheConfig.setMaxDuration(Duration.ZERO);
    PipelineDefnLoader loader = new PipelineDefnLoader(meterRegistry, vertx, cacheConfig, DirCache.cache(new File("target/classes/samples").toPath(), Duration.ofSeconds(2), Pattern.compile("\\..*"), null));
    PipelineExecutorImpl executor = new PipelineExecutorImpl(meterRegistry, new FilterFactory(Collections.emptyList()), null);

    MultiMap params = MultiMap.caseInsensitiveMultiMap();

    RequestContext req = new RequestContext(
            null
            , null
            , null
            , "localhost"
            , null
            , params
            , new HeadersMultiMap().add("Host", "localhost:123")
            , null
            , new IPAddressString("127.0.0.1")
            , null
    );
    
    serverProvider
            .prepareContainer(vertx)
            .compose(v -> serverProvider.prepareTestDatabase(vertx))
            .onComplete(ar -> {
              logger.debug("Data prepped");
            })
            .compose(v -> {
              try {
                params.add("key", serverProvider.getName());
                params.add("port", Integer.toString(serverProvider.getPort()));
                return loader.loadPipeline("sub1/sub2/TemplatedYamlToPipelineIT", req, null);
              } catch (Throwable ex) {
                return Future.failedFuture(ex);
              }
            })
            .compose(pipelineAndFile -> executor.validatePipeline(pipelineAndFile.pipeline()))
            .compose(pipeline -> {
              Format chosenFormat = executor.getFormat(pipeline.getFormats(), null);
              FormatInstance formatInstance = chosenFormat.createInstance(vertx, Vertx.currentContext(), new ListingWriteStream<>(new ArrayList<>()));
              SourceInstance sourceInstance = pipeline.getSource().createInstance(vertx, Vertx.currentContext(), meterRegistry, executor, "source");
              PipelineInstance instance = new PipelineInstance(
                      executor.prepareArguments(req, pipeline.getArguments(), params)
                      , pipeline.getSourceEndpointsMap()
                      , executor.createPreProcessors(vertx, Vertx.currentContext(), pipeline)
                      , sourceInstance
                      , executor.createProcessors(vertx, sourceInstance, Vertx.currentContext(), pipeline, null, null)
                      , formatInstance
              );
      
              assertNotNull(instance);

              return executor.initializePipeline(instance);
            })
            .onComplete(ar -> {
              logger.debug("Pipeline complete");
              if (ar.succeeded()) {
                vertx.setTimer(2000, l -> {
                  logger.debug("Ending test");
                  testContext.completeNow();
                });
              } else {
                testContext.failNow(ar.cause());
              }
            });
  }
}
