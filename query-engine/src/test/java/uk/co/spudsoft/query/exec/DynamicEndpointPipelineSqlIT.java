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
import com.google.common.collect.ImmutableMap;
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
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnectOptions;
import io.vertx.sqlclient.Tuple;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import uk.co.spudsoft.dircache.DirCache;
import uk.co.spudsoft.query.main.ProtectedCredentials;
import uk.co.spudsoft.query.testcontainers.ServerProviderMsSQL;
import uk.co.spudsoft.query.testcontainers.ServerProviderMySQL;
import uk.co.spudsoft.query.web.ServiceException;
import uk.co.spudsoft.query.defn.Format;
import uk.co.spudsoft.query.exec.context.PipelineContext;


/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class DynamicEndpointPipelineSqlIT {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(DynamicEndpointPipelineSqlIT.class);

  private final ServerProvider serverProviderMs = new ServerProviderMsSQL();
  private final ServerProvider serverProviderMy = new ServerProviderMySQL();
  private final ServerProvider serverProviderPg = new ServerProviderPostgreSQL();
  
  private Future<Void> databasesPrepped = null;
  
  public Future<Void> prepareDatabase(Vertx vertx) {
    if (databasesPrepped == null) {
      databasesPrepped = Future.all(
              serverProviderMy.prepareContainer(vertx)
              , serverProviderMs.prepareContainer(vertx)
              , serverProviderPg.prepareContainer(vertx)
      )
              .compose(v -> {
                  return Future.all(
                          serverProviderMy.prepareTestDatabase(vertx)
                          , serverProviderMs.prepareTestDatabase(vertx)
                          , serverProviderPg.prepareTestDatabase(vertx)
                  );
              })
              .mapEmpty()
              ;
    }
    return databasesPrepped;
  }
    
  @Test
  @Timeout(value = 600, timeUnit = TimeUnit.SECONDS)
  public void testHandlingWithDynamicEndpoint(Vertx vertx, VertxTestContext testContext) throws Throwable {
    logger.info("testHandlingWithDynamicEndpoint");

    ch.qos.logback.classic.Logger lg = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(PipelineDefnLoader.class);
    ch.qos.logback.classic.Level origLvl = lg.getLevel();
    lg.setLevel(ch.qos.logback.classic.Level.DEBUG);
        
    Future<Void> prepFuture = prepareDatabase(vertx);
    Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> prepFuture.isComplete());
    assertTrue(prepFuture.succeeded());
    
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    Auditor auditor = new AuditorMemoryImpl(vertx);
    CacheConfig cacheConfig = new CacheConfig();
    cacheConfig.setMaxDuration(Duration.ZERO);
    PipelineDefnLoader loader = new PipelineDefnLoader(meterRegistry, vertx, cacheConfig, DirCache.cache(new File("target/classes/samples").toPath(), Duration.ofSeconds(2), Pattern.compile("\\..*"), null));
    PipelineExecutor executor = PipelineExecutor.create(meterRegistry, auditor, new FilterFactory(Collections.emptyList())
            , ImmutableMap.<String, ProtectedCredentials>builder().put("cred", new ProtectedCredentials(serverProviderMy.getUser(), serverProviderMy.getPassword(), null)).build()
    );

    MultiMap args = MultiMap.caseInsensitiveMultiMap();
    args.set("maxId", "14");
    
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
    
    logger.info("Preparing dynamic endpoints");
    SqlConnectOptions connectOptions = SqlConnectOptions.fromUri(serverProviderMy.getVertxUrl());
    connectOptions.setUser(serverProviderMy.getUser());
    connectOptions.setPassword(serverProviderMy.getPassword());
    Pool pool = Pool.pool(vertx, connectOptions, new PoolOptions().setMaxSize(8));
    pool.preparedQuery("delete from DynamicEndpoint")
            .execute()
            .compose(rs -> {
              return pool.preparedQuery("insert into DynamicEndpoint (endpointKey, type, url, username, password, useCondition) values (?, ?, ?, ?, ?, ?)")
                  .executeBatch(
                          Arrays.asList(
                                  Tuple.of("my", "SQL", serverProviderMy.getVertxUrl(), serverProviderMy.getUser(), serverProviderMy.getPassword(), null)
                                  , Tuple.of("ms", "SQL", serverProviderMs.getVertxUrl(), serverProviderMs.getUser(), serverProviderMs.getPassword(), null)
                                  , Tuple.of("pg", "SQL", serverProviderPg.getVertxUrl(), serverProviderPg.getUser(), serverProviderPg.getPassword(), null)
                          )
                  );
            })
            .onComplete(ar -> {
              logger.debug("Data prepped");
              
              vertx.getOrCreateContext().put("req", req);
              
              args.set("port", Integer.toString(serverProviderMy.getPort()));
            })
            .compose(v -> {
              try {
                return loader.loadPipeline("sub1/sub2/DynamicEndpointPipelineSqlIT", req, null);
              } catch (Throwable ex) {
                return Future.failedFuture(ex);
              }
            })
            .compose(pipelineAndFile -> executor.validatePipeline(req, pipelineAndFile.pipeline()))
            .compose(pipeline -> {
              PipelineContext pipelineContext = new PipelineContext("test", req);
              Format chosenFormat = executor.getFormat(pipelineContext, pipeline.getFormats(), null);
              FormatInstance formatInstance = chosenFormat.createInstance(vertx, pipelineContext, new ListingWriteStream<>(new ArrayList<>()));
              SourceInstance sourceInstance = pipeline.getSource().createInstance(vertx, meterRegistry, auditor, pipelineContext, executor);
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
              assertEquals(1, instance.getPreProcessors().size());
              assertEquals("dynamicEndpoints[0]", instance.getPreProcessors().get(0).getName());
              assertEquals(2, instance.getProcessors().size());
              assertEquals("test.processors[0]", instance.getProcessors().get(0).getName());
              assertEquals("test.processors[1]", instance.getProcessors().get(1).getName());

              return executor.initializePipeline(pipelineContext, instance);
            })
            .onComplete(ar -> {
              logger.debug("Pipeline complete");
              if (ar.succeeded()) {
                vertx.setTimer(2000, l -> {
                  logger.debug("Ending test");
                  lg.setLevel(origLvl);
                  testContext.completeNow();
                });
              } else {
                lg.setLevel(origLvl);
                testContext.failNow(ar.cause());
              }
            });
  }

  @Test
  @Timeout(value = 600, timeUnit = TimeUnit.SECONDS)
  public void testHandlingDynamicEndpointFailureBadPort(Vertx vertx, VertxTestContext testContext) throws Throwable {
    logger.info("testHandlingDynamicEndpointFailureBadPort");

    Future<Void> prepFuture = prepareDatabase(vertx);
    Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> prepFuture.isComplete());
    assertTrue(prepFuture.succeeded());
        
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    Auditor auditor = new AuditorMemoryImpl(vertx);
    CacheConfig cacheConfig = new CacheConfig();
    cacheConfig.setMaxDuration(Duration.ZERO);
    PipelineDefnLoader loader = new PipelineDefnLoader(meterRegistry, vertx, cacheConfig, DirCache.cache(new File("target/classes/samples").toPath(), Duration.ofSeconds(2), Pattern.compile("\\..*"), null));
    PipelineExecutor executor = PipelineExecutor.create(meterRegistry, auditor, new FilterFactory(Collections.emptyList())
            , ImmutableMap.<String, ProtectedCredentials>builder().put("cred", new ProtectedCredentials(serverProviderMy.getUser(), serverProviderMy.getPassword(), null)).build()
    );

    MultiMap args = MultiMap.caseInsensitiveMultiMap();
    args.set("maxId", "14");
    
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
    
    SqlConnectOptions connectOptions = SqlConnectOptions.fromUri(serverProviderMy.getVertxUrl());
    connectOptions.setUser(serverProviderMy.getUser());
    connectOptions.setPassword(serverProviderMy.getPassword());
    Pool pool = Pool.pool(vertx, connectOptions, new PoolOptions().setMaxSize(8));
    pool.preparedQuery("delete from DynamicEndpoint")
            .execute()
            .compose(rs -> {
              return pool.preparedQuery("insert into DynamicEndpoint (endpointKey, type, url, username, password, useCondition) values (?, ?, ?, ?, ?, ?)")
                  .executeBatch(
                          Arrays.asList(
                                  Tuple.of("my", "SQL", serverProviderMy.getVertxUrl(), serverProviderMy.getUser(), serverProviderMy.getPassword(), null)
                                  , Tuple.of("ms", "SQL", serverProviderMs.getVertxUrl(), serverProviderMs.getUser(), serverProviderMs.getPassword(), null)
                                  , Tuple.of("pg", "SQL", serverProviderPg.getVertxUrl(), serverProviderPg.getUser(), serverProviderPg.getPassword(), null)
                          )
                  );
            })
            .onComplete(ar -> {
              logger.debug("Data prepped");
              // Setting bad port for error
              args.set("port", "0");
            })
            .compose(v -> {
              try {
                return loader.loadPipeline("sub1/sub2/DynamicEndpointPipelineSqlIT", req, null);
              } catch (Throwable ex) {
                return Future.failedFuture(ex);
              }
            })
            .compose(pipelineAndFile -> executor.validatePipeline(req, pipelineAndFile.pipeline()))
            .compose(pipeline -> {
              PipelineContext pipelineContext = new PipelineContext("test", req);
              Format chosenFormat = executor.getFormat(pipelineContext, pipeline.getFormats(), null);
              FormatInstance formatInstance = chosenFormat.createInstance(vertx, pipelineContext, new ListingWriteStream<>(new ArrayList<>()));
              SourceInstance sourceInstance = pipeline.getSource().createInstance(vertx, meterRegistry, auditor, pipelineContext, executor);
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

              return executor.initializePipeline(pipelineContext, instance);
            })
            .onComplete(ar -> {
              logger.debug("Pipeline complete");
              if (ar.succeeded()) {
                vertx.setTimer(2000, l -> {
                  logger.debug("Ending test");
                  testContext.failNow("Expected to fail due to bad port specified for dynamic endpoint source endpoint");
                });
              } else {
                testContext.verify(() -> {
                  assertThat(ar.cause(), instanceOf(RuntimeException.class));
                  assertThat(ar.cause().getCause(), instanceOf(java.lang.IllegalArgumentException.class));
                  logger.debug("Cause: {}", ar.cause().getMessage());
                });
                testContext.completeNow();
              }
            });
  }

  @Test
  @Timeout(value = 600, timeUnit = TimeUnit.SECONDS)
  public void testHandlingDynamicEndpointFailureNoKey(Vertx vertx, VertxTestContext testContext) throws Throwable {
    logger.info("testHandlingDynamicEndpointFailureNoKey");

    Future<Void> prepFuture = prepareDatabase(vertx);
    Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> prepFuture.isComplete());
    assertTrue(prepFuture.succeeded());
    
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    Auditor auditor = new AuditorMemoryImpl(vertx);
    CacheConfig cacheConfig = new CacheConfig();
    cacheConfig.setMaxDuration(Duration.ZERO);
    PipelineDefnLoader loader = new PipelineDefnLoader(meterRegistry, vertx, cacheConfig, DirCache.cache(new File("target/classes/samples").toPath(), Duration.ofSeconds(2), Pattern.compile("\\..*"), null));
    PipelineExecutor executor = PipelineExecutor.create(meterRegistry, auditor, new FilterFactory(Collections.emptyList())
            , ImmutableMap.<String, ProtectedCredentials>builder().put("cred", new ProtectedCredentials(serverProviderMy.getUser(), serverProviderMy.getPassword(), null)).build()
    );

    MultiMap args = MultiMap.caseInsensitiveMultiMap();
    args.set("maxId", "7");
    
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
    
    SqlConnectOptions connectOptions = SqlConnectOptions.fromUri(serverProviderMy.getVertxUrl());
    connectOptions.setUser(serverProviderMy.getUser());
    connectOptions.setPassword(serverProviderMy.getPassword());
    Pool pool = Pool.pool(vertx, connectOptions, new PoolOptions().setMaxSize(8));
    pool.preparedQuery("delete from DynamicEndpoint")
            .execute()
            .compose(rs -> {
              return pool.preparedQuery("insert into DynamicEndpoint (endpointKey, type, url, username, password, useCondition) values (?, ?, ?, ?, ?, ?)")
                  .executeBatch(
                          Arrays.asList(
                                  Tuple.of("my", "SQL", serverProviderMy.getVertxUrl(), serverProviderMy.getUser(), serverProviderMy.getPassword(), null)
                                  , Tuple.of("ms", "SQL", serverProviderMs.getVertxUrl(), serverProviderMs.getUser(), serverProviderMs.getPassword(), null)
                                  , Tuple.of("pg", "SQL", serverProviderPg.getVertxUrl(), serverProviderPg.getUser(), serverProviderPg.getPassword(), null)
                          )
                  );
            })
            .onComplete(ar -> {
              logger.debug("Data prepped");
              args.set("port", Integer.toString(serverProviderMy.getPort()));
            })
            .compose(v -> {
              try {
                return loader.loadPipeline("sub1/sub2/DynamicEndpointPipelineSqlIT", req, null);
              } catch (Throwable ex) {
                return Future.failedFuture(ex);
              }
            })
            .compose(pipelineAndFile -> executor.validatePipeline(req, pipelineAndFile.pipeline()))
            .compose(pipeline -> {
              PipelineContext pipelineContext = new PipelineContext("test", req);
              Format chosenFormat = executor.getFormat(pipelineContext, pipeline.getFormats(), null);
              FormatInstance formatInstance = chosenFormat.createInstance(vertx, pipelineContext, new ListingWriteStream<>(new ArrayList<>()));
              SourceInstance sourceInstance = pipeline.getSource().createInstance(vertx, meterRegistry, auditor, pipelineContext, executor);
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

              return executor.initializePipeline(pipelineContext, instance);
            })
            .onComplete(ar -> {
              logger.debug("Pipeline complete");
              if (ar.succeeded()) {
                vertx.setTimer(2000, l -> {
                  logger.debug("Ending test");
                  testContext.failNow("Expected to fail due to lack of key on the endpoint (triggered by maxId = 7)");
                });
              } else {
                testContext.verify(() -> {
                  assertThat(ar.cause(), instanceOf(ServiceException.class));
                  logger.debug("Cause: {}", ar.cause().getMessage());
                });
                testContext.completeNow();
              }
            });
  }

  @Test
  @Timeout(value = 600, timeUnit = TimeUnit.SECONDS)
  public void testHandlingWithDynamicEndpointMissingSecret(Vertx vertx, VertxTestContext testContext) throws Throwable {
    logger.info("testHandlingWithDynamicEndpoint");

    Future<Void> prepFuture = prepareDatabase(vertx);
    Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> prepFuture.isComplete());
    assertTrue(prepFuture.succeeded());
    
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    Auditor auditor = new AuditorMemoryImpl(vertx);
    CacheConfig cacheConfig = new CacheConfig();
    cacheConfig.setMaxDuration(Duration.ZERO);
    PipelineDefnLoader loader = new PipelineDefnLoader(meterRegistry, vertx, cacheConfig, DirCache.cache(new File("target/classes/samples").toPath(), Duration.ofSeconds(2), Pattern.compile("\\..*"), null));
    PipelineExecutor executor = PipelineExecutor.create(meterRegistry, auditor, new FilterFactory(Collections.emptyList()), null);

    MultiMap args = MultiMap.caseInsensitiveMultiMap();
    args.set("maxId", "14");
    
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
    
    logger.info("Preparing dynamic endpoints");
    SqlConnectOptions connectOptions = SqlConnectOptions.fromUri(serverProviderMy.getVertxUrl());
    connectOptions.setUser(serverProviderMy.getUser());
    connectOptions.setPassword(serverProviderMy.getPassword());
    Pool pool = Pool.pool(vertx, connectOptions, new PoolOptions().setMaxSize(8));
    pool.preparedQuery("delete from DynamicEndpoint")
            .execute()
            .compose(rs -> {
              return pool.preparedQuery("insert into DynamicEndpoint (endpointKey, type, url, username, password, useCondition) values (?, ?, ?, ?, ?, ?)")
                  .executeBatch(
                          Arrays.asList(
                                  Tuple.of("my", "SQL", serverProviderMy.getVertxUrl(), serverProviderMy.getUser(), serverProviderMy.getPassword(), null)
                                  , Tuple.of("ms", "SQL", serverProviderMs.getVertxUrl(), serverProviderMs.getUser(), serverProviderMs.getPassword(), null)
                                  , Tuple.of("pg", "SQL", serverProviderPg.getVertxUrl(), serverProviderPg.getUser(), serverProviderPg.getPassword(), null)
                          )
                  );
            })
            .onComplete(ar -> {
              logger.debug("Data prepped");
              args.set("port", Integer.toString(serverProviderMy.getPort()));
            })
            .compose(v -> {
              try {
                return loader.loadPipeline("sub1/sub2/DynamicEndpointPipelineSqlIT", req, null);
              } catch (Throwable ex) {
                return Future.failedFuture(ex);
              }
            })
            .compose(pipelineAndFile -> executor.validatePipeline(req, pipelineAndFile.pipeline()))
            .compose(pipeline -> {
              PipelineContext pipelineContext = new PipelineContext("test", req);
              Format chosenFormat = executor.getFormat(pipelineContext, pipeline.getFormats(), null);
              FormatInstance formatInstance = chosenFormat.createInstance(vertx, pipelineContext, new ListingWriteStream<>(new ArrayList<>()));
              SourceInstance sourceInstance = pipeline.getSource().createInstance(vertx, meterRegistry, auditor, pipelineContext, executor);
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

              return executor.initializePipeline(pipelineContext, instance);
            })
            .onComplete(ar -> {
              logger.debug("Pipeline complete");
              if (ar.succeeded()) {
                vertx.setTimer(2000, l -> {
                  logger.debug("Ending test");
                  testContext.failNow("Expected to fail due to lack of secret needed for accessing DynamicEndpoint");
                });
              } else {
                testContext.verify(() -> {
                  assertThat(ar.cause(), instanceOf(ServiceException.class));
                  logger.debug("Cause: {}", ar.cause().getMessage());
                });
                testContext.completeNow();
              }
            });
  }

}
