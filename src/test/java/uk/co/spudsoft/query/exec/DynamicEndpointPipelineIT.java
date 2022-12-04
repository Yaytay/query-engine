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
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
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
import uk.co.spudsoft.query.main.ProtectedCredentials;
import uk.co.spudsoft.query.testcontainers.ServerProviderMsSQL;
import uk.co.spudsoft.query.testcontainers.ServerProviderMySQL;
import uk.co.spudsoft.query.web.ServiceException;
import uk.co.spudsoft.query.defn.Format;


/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public class DynamicEndpointPipelineIT {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(DynamicEndpointPipelineIT.class);

  private final ServerProvider serverProviderMs = new ServerProviderMsSQL();
  private final ServerProvider serverProviderMy = new ServerProviderMySQL();
  private final ServerProvider serverProviderPg = new ServerProviderPostgreSQL();
    
  @Test
  @Timeout(value = 120, timeUnit = TimeUnit.SECONDS)
  public void testHandlingWithDynamicEndpoint(Vertx vertx, VertxTestContext testContext) throws Throwable {

    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    CacheConfig cacheConfig = new CacheConfig().setMaxItems(1).setMaxDurationMs(0).setPurgePeriodMs(0);
    PipelineDefnLoader loader = new PipelineDefnLoader(meterRegistry, vertx, cacheConfig, DirCache.cache(new File("target/test-classes/sources").toPath(), Duration.ofSeconds(2), Pattern.compile("\\..*")));
    PipelineExecutorImpl executor = new PipelineExecutorImpl(ImmutableMap.<String, ProtectedCredentials>builder().put("cred", new ProtectedCredentials(serverProviderMy.getUser(), serverProviderMy.getPassword(), null)).build());

    MultiMap args = MultiMap.caseInsensitiveMultiMap();
    args.set("maxId", "14");
    
    serverProviderMy
            .prepareContainer(vertx)
            .compose(v -> serverProviderMs.prepareContainer(vertx))
            .compose(v -> serverProviderPg.prepareContainer(vertx))
            .compose(v -> serverProviderMs.prepareTestDatabase(vertx))
            .compose(v -> serverProviderMy.prepareTestDatabase(vertx))
            .compose(v -> serverProviderPg.prepareTestDatabase(vertx))
            .compose(v -> {
              SqlConnectOptions connectOptions = SqlConnectOptions.fromUri(serverProviderMy.getUrl());
              connectOptions.setUser(serverProviderMy.getUser());
              connectOptions.setPassword(serverProviderMy.getPassword());
              Pool pool = Pool.pool(vertx, connectOptions, new PoolOptions().setMaxSize(1));
              return pool.preparedQuery("delete from testDynamicEndpoint")
                      .execute()
                      .compose(rs -> {
                        return pool.preparedQuery("insert into testDynamicEndpoint (endpointKey, type, url, username, password, useCondition) values (?, ?, ?, ?, ?, ?)")
                            .executeBatch(
                                    Arrays.asList(
                                            Tuple.of("my", "SQL", serverProviderMy.getUrl(), serverProviderMy.getUser(), serverProviderMy.getPassword(), null)
                                            , Tuple.of("ms", "SQL", serverProviderMs.getUrl(), serverProviderMs.getUser(), serverProviderMs.getPassword(), null)
                                            , Tuple.of("pg", "SQL", serverProviderPg.getUrl(), serverProviderPg.getUser(), serverProviderPg.getPassword(), null)
                                    )
                            );
                      });
            })
            .onComplete(ar -> {
              logger.debug("Data prepped");
              args.set("port", Integer.toString(serverProviderMy.getPort()));
            })
            .compose(v -> {
              try {
                RequestContext req = new RequestContext(
                        null
                        , null
                        , "localhost"
                        , null
                        , new HeadersMultiMap().add("Host", "localhost:123")
                        , null
                        , new IPAddressString("127.0.0.1")
                        , null
                );
                return loader.loadPipeline("sub1/sub2/DynamicEndpointPipelineIT", req, null);
              } catch (Throwable ex) {
                return Future.failedFuture(ex);
              }
            })
            .compose(pipeline -> executor.validatePipeline(pipeline))
            .compose(pipeline -> {
              Format chosenFormat = executor.getFormat(pipeline.getFormats(), null);
              FormatInstance formatInstance = chosenFormat.createInstance(vertx, Vertx.currentContext(), null);
              SourceInstance sourceInstance = pipeline.getSource().createInstance(vertx, Vertx.currentContext(), executor, "source");
              PipelineInstance instance = new PipelineInstance(
                      executor.prepareArguments(pipeline.getArguments(), args)
                      , pipeline.getSourceEndpoints()
                      , executor.createPreProcessors(vertx, Vertx.currentContext(), pipeline)
                      , sourceInstance
                      , executor.createProcessors(vertx, sourceInstance, Vertx.currentContext(), pipeline)
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

  @Test
  @Timeout(value = 120, timeUnit = TimeUnit.SECONDS)
  public void testHandlingDynamicEndpointFailureBadPort(Vertx vertx, VertxTestContext testContext) throws Throwable {

    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    CacheConfig cacheConfig = new CacheConfig().setMaxItems(1).setMaxDurationMs(0).setPurgePeriodMs(0);
    PipelineDefnLoader loader = new PipelineDefnLoader(meterRegistry, vertx, cacheConfig, DirCache.cache(new File("target/test-classes/sources").toPath(), Duration.ofSeconds(2), Pattern.compile("\\..*")));
    PipelineExecutorImpl executor = new PipelineExecutorImpl(null);

    MultiMap args = MultiMap.caseInsensitiveMultiMap();
    args.set("maxId", "14");
    
    serverProviderMy
            .prepareContainer(vertx)
            .compose(v -> serverProviderMs.prepareContainer(vertx))
            .compose(v -> serverProviderPg.prepareContainer(vertx))
            .compose(v -> serverProviderMs.prepareTestDatabase(vertx))
            .compose(v -> serverProviderMy.prepareTestDatabase(vertx))
            .compose(v -> serverProviderPg.prepareTestDatabase(vertx))
            .compose(v -> {
              SqlConnectOptions connectOptions = SqlConnectOptions.fromUri(serverProviderMy.getUrl());
              connectOptions.setUser(serverProviderMy.getUser());
              connectOptions.setPassword(serverProviderMy.getPassword());
              Pool pool = Pool.pool(vertx, connectOptions, new PoolOptions().setMaxSize(1));
              return pool.preparedQuery("delete from testDynamicEndpoint")
                      .execute()
                      .compose(rs -> {
                        return pool.preparedQuery("insert into testDynamicEndpoint (endpointKey, type, url, username, password, useCondition) values (?, ?, ?, ?, ?, ?)")
                            .executeBatch(
                                    Arrays.asList(
                                            Tuple.of("my", "SQL", serverProviderMy.getUrl(), serverProviderMy.getUser(), serverProviderMy.getPassword(), null)
                                            , Tuple.of("ms", "SQL", serverProviderMs.getUrl(), serverProviderMs.getUser(), serverProviderMs.getPassword(), null)
                                            , Tuple.of("pg", "SQL", serverProviderPg.getUrl(), serverProviderPg.getUser(), serverProviderPg.getPassword(), null)
                                    )
                            );
                      });
            })
            .onComplete(ar -> {
              logger.debug("Data prepped");
              // Setting bad port for error
              args.set("port", "0");
            })
            .compose(v -> {
              try {
                RequestContext req = new RequestContext(
                        null
                        , null
                        , "localhost"
                        , null
                        , new HeadersMultiMap().add("Host", "localhost:123")
                        , null
                        , new IPAddressString("127.0.0.1")
                        , null
                );
                return loader.loadPipeline("sub1/sub2/DynamicEndpointPipelineIT", req, null);
              } catch (Throwable ex) {
                return Future.failedFuture(ex);
              }
            })
            .compose(pipeline -> executor.validatePipeline(pipeline))
            .compose(pipeline -> {
              Format chosenFormat = executor.getFormat(pipeline.getFormats(), null);
              FormatInstance formatInstance = chosenFormat.createInstance(vertx, Vertx.currentContext(), null);
              SourceInstance sourceInstance = pipeline.getSource().createInstance(vertx, Vertx.currentContext(), executor, "source");
              PipelineInstance instance = new PipelineInstance(
                      executor.prepareArguments(pipeline.getArguments(), args)
                      , pipeline.getSourceEndpoints()
                      , executor.createPreProcessors(vertx, Vertx.currentContext(), pipeline)
                      , sourceInstance
                      , executor.createProcessors(vertx, sourceInstance, Vertx.currentContext(), pipeline)
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
                  testContext.failNow("Expected to fail due to bad port specified for dynamic endpoint source endpoint");
                });
              } else {
                testContext.verify(() -> {
                  assertThat(ar.cause(), instanceOf(IllegalArgumentException.class));
                  logger.debug("Cause: {}", ar.cause().getMessage());
                });
                testContext.completeNow();
              }
            });
  }


  @Test
  @Timeout(value = 120, timeUnit = TimeUnit.SECONDS)
  public void testHandlingDynamicEndpointFailureNoKey(Vertx vertx, VertxTestContext testContext) throws Throwable {

    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    CacheConfig cacheConfig = new CacheConfig().setMaxItems(1).setMaxDurationMs(0).setPurgePeriodMs(0);
    PipelineDefnLoader loader = new PipelineDefnLoader(meterRegistry, vertx, cacheConfig, DirCache.cache(new File("target/test-classes/sources").toPath(), Duration.ofSeconds(2), Pattern.compile("\\..*")));
    PipelineExecutorImpl executor = new PipelineExecutorImpl(null);

    MultiMap args = MultiMap.caseInsensitiveMultiMap();
    args.set("maxId", "7");
    
    serverProviderMy
            .prepareContainer(vertx)
            .compose(v -> serverProviderMs.prepareContainer(vertx))
            .compose(v -> serverProviderPg.prepareContainer(vertx))
            .compose(v -> serverProviderMs.prepareTestDatabase(vertx))
            .compose(v -> serverProviderMy.prepareTestDatabase(vertx))
            .compose(v -> serverProviderPg.prepareTestDatabase(vertx))
            .compose(v -> {
              SqlConnectOptions connectOptions = SqlConnectOptions.fromUri(serverProviderMy.getUrl());
              connectOptions.setUser(serverProviderMy.getUser());
              connectOptions.setPassword(serverProviderMy.getPassword());
              Pool pool = Pool.pool(vertx, connectOptions, new PoolOptions().setMaxSize(1));
              return pool.preparedQuery("delete from testDynamicEndpoint")
                      .execute()
                      .compose(rs -> {
                        return pool.preparedQuery("insert into testDynamicEndpoint (endpointKey, type, url, username, password, useCondition) values (?, ?, ?, ?, ?, ?)")
                            .executeBatch(
                                    Arrays.asList(
                                            Tuple.of("my", "SQL", serverProviderMy.getUrl(), serverProviderMy.getUser(), serverProviderMy.getPassword(), null)
                                            , Tuple.of("ms", "SQL", serverProviderMs.getUrl(), serverProviderMs.getUser(), serverProviderMs.getPassword(), null)
                                            , Tuple.of("pg", "SQL", serverProviderPg.getUrl(), serverProviderPg.getUser(), serverProviderPg.getPassword(), null)
                                    )
                            );
                      });
            })
            .onComplete(ar -> {
              logger.debug("Data prepped");
              args.set("port", Integer.toString(serverProviderMy.getPort()));
            })
            .compose(v -> {
              try {
                RequestContext req = new RequestContext(
                        null
                        , null
                        , "localhost"
                        , null
                        , new HeadersMultiMap().add("Host", "localhost:123")
                        , null
                        , new IPAddressString("127.0.0.1")
                        , null
                );
                return loader.loadPipeline("sub1/sub2/DynamicEndpointPipelineIT", req, null);
              } catch (Throwable ex) {
                return Future.failedFuture(ex);
              }
            })
            .compose(pipeline -> executor.validatePipeline(pipeline))
            .compose(pipeline -> {
              Format chosenFormat = executor.getFormat(pipeline.getFormats(), null);
              FormatInstance formatInstance = chosenFormat.createInstance(vertx, Vertx.currentContext(), null);
              SourceInstance sourceInstance = pipeline.getSource().createInstance(vertx, Vertx.currentContext(), executor, "source");
              PipelineInstance instance = new PipelineInstance(
                      executor.prepareArguments(pipeline.getArguments(), args)
                      , pipeline.getSourceEndpoints()
                      , executor.createPreProcessors(vertx, Vertx.currentContext(), pipeline)
                      , sourceInstance
                      , executor.createProcessors(vertx, sourceInstance, Vertx.currentContext(), pipeline)
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

}
