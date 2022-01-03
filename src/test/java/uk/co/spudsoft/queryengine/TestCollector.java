package uk.co.spudsoft.queryengine;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
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
public class TestCollector {

  private static final Logger logger = LoggerFactory.getLogger(TestCollector.class);

  @BeforeAll
  public static void setup() {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }
  
  @Test
  @Timeout(value = 30, timeUnit = TimeUnit.MINUTES)
  public void startAllContainers(Vertx vertx, VertxTestContext testContext) {
       
    List<ServerProviderInstance> instances = Arrays.asList(
            new ServerProviderMsSQL()
            , new ServerProviderMySQL()
            , new ServerProviderPostgreSQL()
    );
    
    List<Future> futures = new ArrayList<>();
    
    for (ServerProviderInstance instance : instances) {
      long startTime = System.currentTimeMillis();
      Future future = instance.prepareContainer(vertx)
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
                          String sql = instance.limit(30
                                          , 
                                          """
                                            select d.id, d.instant, l.value as ref, d.value 
                                            from testData d
                                            join testRefData l on d.lookup = l.id  
                                            order by d.id
                                          """
                                          );
                          Collector<Row, ?, Integer> collector = Collectors.summingInt(row -> {
                            logger.debug("{}: {}", instance.getName(), row.toJson());
//                            try {
//                              Thread.sleep(1000);
//                            } catch(Throwable ex) {
//                            }
                            return 1;
                          });
                          return conn.preparedQuery(sql)
                                  .collecting(collector)
                                  .execute()
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
