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

import uk.co.spudsoft.query.exec.context.RequestContext;
import inet.ipaddr.IPAddressString;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
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
import uk.co.spudsoft.query.defn.Format;
import uk.co.spudsoft.query.exec.context.PipelineContext;


/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class EmptyDataIT {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(EmptyDataIT.class);

  private final ServerProvider serverProvider = new ServerProviderPostgreSQL();
    
  @Test
  @Timeout(value = 120, timeUnit = TimeUnit.SECONDS)
  public void testParsingJsonToPipelineStreaming(Vertx vertx, VertxTestContext testContext) throws Throwable {

    Files.createDirectories(new File("target/temp/EmptyDataIT").toPath());
    
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    CacheConfig cacheConfig = new CacheConfig();
    cacheConfig.setMaxDuration(Duration.ZERO);
    PipelineDefnLoader loader = new PipelineDefnLoader(meterRegistry, vertx, cacheConfig, DirCache.cache(new File("target/classes/samples").toPath(), Duration.ofSeconds(2), Pattern.compile("\\..*"), null));
    PipelineExecutor executor = PipelineExecutor.create(meterRegistry, new FilterFactory(Collections.emptyList()), null);

    MultiMap args = MultiMap.caseInsensitiveMultiMap();

    FileSystem fs = vertx.fileSystem();
    
    RequestContext req = new RequestContext(
            null
            , null
            , null
            , "localhost"
            , null
            , args
            , HeadersMultiMap.httpHeaders().add("Host", "localhost:123")
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
                args.add("key", serverProvider.getName());
                args.add("port", Integer.toString(serverProvider.getPort()));
                return loader.loadPipeline("sub1/sub2/EmptyDataIT", req, null);
              } catch (Throwable ex) {
                return Future.failedFuture(ex);
              }
            })
            .compose(pipelineAndFile -> executor.validatePipeline(pipelineAndFile.pipeline()))
            .compose(pipeline -> {
              AsyncFile output = fs.openBlocking("target/temp/EmptyDataIT/output.html", new OpenOptions().setCreate(true));
              PipelineContext pipelineContext = new PipelineContext("test", req);
              Format chosenFormat = executor.getFormat(pipeline.getFormats(), null);
              FormatInstance formatInstance = chosenFormat.createInstance(vertx, pipelineContext, output);
              SourceInstance sourceInstance = pipeline.getSource().createInstance(vertx, pipelineContext, meterRegistry, executor);
              PipelineInstance instance;
              try {
                instance = new PipelineInstance(
                        pipelineContext
                        , pipeline
                        , executor.prepareArguments(req, pipeline.getArguments(), args)
                        , pipeline.getSourceEndpointsMap()
                        , executor.createPreProcessors(vertx, pipelineContext, pipeline)
                        , sourceInstance
                        , executor.createProcessors(vertx, pipelineContext, pipeline, null)
                        , formatInstance
                );
              } catch (Throwable ex) {
                return Future.failedFuture(ex);
              }
      
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
