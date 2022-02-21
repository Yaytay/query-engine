package uk.co.spudsoft.queryengine;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.EventLoopContext;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;




/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class CollectorToStreamIT {

  private static final Logger logger = LoggerFactory.getLogger(CollectorToStreamIT.class);

  @BeforeAll
  public static void setup() {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }
  
  @Test
  @Timeout(value = 60, timeUnit = TimeUnit.MINUTES)
  public void startAllContainers(Vertx vertx, VertxTestContext testContext) {
       
    List<ServerProviderInstance> instances = Arrays.asList(
            new ServerProviderMsSQL()
            , new ServerProviderMySQL()
            , new ServerProviderPostgreSQL()
    );
    
    List<Future> futures = new ArrayList<>();
    
    for (ServerProviderInstance instance : instances) {
      Context ctx = vertx.getOrCreateContext();
      EventLoopContext ctx2 = ((VertxInternal) vertx).createEventLoopContext();
      logger.debug("Context: {}, EventLoopContext: {}", ctx, ctx2);
      long startTime = System.currentTimeMillis();
      Future future = 
              instance.prepareContainer(vertx, ctx2)
              .compose(container -> {
                logger.debug("{} - {}s: Container started", instance.getName(), (System.currentTimeMillis() - startTime) / 1000.0);
                try {
                  Pool pool = instance.createPool(vertx, instance.getOptions()
                          , new PoolOptions()
                                  .setShared(false)
                                  .setMaxSize(4)
                                  .setEventLoopSize(3)
                  );
                  return Future.succeededFuture(pool);
                } catch(Throwable ex) {
                  return Future.failedFuture(ex);
                }
              })
              .compose(sqlPool -> {
                return instance.prepareTestDatabase(vertx, sqlPool)
                        .compose(v -> {
                          logger.debug("{} - {}s: Test data prepared", instance.getName(), (System.currentTimeMillis() - startTime) / 1000.0);
                          
                          return sqlPool.getConnection();
                        })
                        .compose(conn -> {
                          BlockingReadStream<JsonObject> stream = new BlockingReadStream<>(vertx.getOrCreateContext(), 10);
                          stream.handler(jo -> {
                            // logger.debug("{}: Row: {}", instance.getName(), jo);
                          });
                          stream.endHandler(v -> {logger.error("{}: Ended", instance.getName());});
                          stream.exceptionHandler(ex -> {logger.error("{}: Failed: {}", instance.getName(), ex);});
                          String sql = instance.limit(10000
                                          , 
                                          """
                                            select d.id, d.instant, l.value as ref, d.value 
                                            from testData d
                                            join testRefData l on d.lookup = l.id  
                                            order by d.id
                                          """
                                          );
                          Collector<Row, ?, Integer> collector = Collectors.summingInt(row -> {
                            JsonObject jo = row.toJson();
                            // logger.debug("{}: {}", instance.getName(), jo);
                            try {
                              stream.put(jo);
                            } catch(Throwable ex) {
                              logger.debug("{}: Failed to add to stream: ", instance.getName(), ex);                              
                            }
                            return 1;
                          });
                          PreparedQuery<RowSet<Row>> pq = conn.preparedQuery(sql);
                          PreparedQuery<SqlResult<Integer>> pq2 = pq.collecting(collector);
                          return pq2
                                  .execute()
                                  .onComplete(ar -> {
                                    stream.end();
                                  })
                                  .onSuccess(result -> {
                                    logger.debug("{} - {}: Rows: {}"
                                            , instance.getName()
                                            , (System.currentTimeMillis() - startTime) / 1000.0
                                            , result.value()
                                    );
                                  })
                                  .onFailure(ex -> {
                                    logger.debug("{} - {}: Failed: "
                                            , instance.getName()
                                            , (System.currentTimeMillis() - startTime) / 1000.0
                                            , ex
                                    );
                                  })
                                  ;
                        })
                        ;
              })
              ;
      futures.add(future);
    }
    
    CompositeFuture.all(futures)
            .onFailure(ex -> {
              logger.error("Something failed: ", ex);
              testContext.failNow(ex);
            })
            .onSuccess(cf -> {
              logger.info("Everything worked");
              testContext.completeNow();
            })
            ;
    
  }
}
