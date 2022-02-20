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
import io.vertx.sqlclient.RowStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
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
@Disabled
public class TestStream {

  private static final Logger logger = LoggerFactory.getLogger(TestStream.class);

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
      String sql = instance.limit(30
                      , 
                      """
                        select d.id, d.instant, l.value as ref, d.value 
                        from testData d
                        join testRefData l on d.lookup = l.id  
                        order by d.id
                      """
                      );
      
      Future future = instance.prepareContainer(vertx)
              .compose(container -> {
                logger.debug("{} - {}s: Container started", instance.getName(), (System.currentTimeMillis() - startTime) / 1000.0);
                try {
                  Pool pool = instance.createPool(vertx, instance.getOptions(), new PoolOptions().setMaxSize(4));
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
                          return conn.prepare(sql)
                                  .onSuccess(ps -> {
                                    conn.begin().onSuccess(tran -> {
                                      RowStream<Row> stream = ps.createStream(10);
                                      stream.handler(row -> {
                                        logger.debug("{}: Row: {}"
                                                , instance.getName()
                                                , row.toJson()
                                        );
                                      });
                                      stream.endHandler(v -> {
                                        logger.error("{} - {}s: Finished stream"
                                                , instance.getName()
                                                , (System.currentTimeMillis() - startTime) / 1000.0
                                        );
                                      });
                                      stream.exceptionHandler(ex -> {
                                        logger.error("{} - {}s: Failed to stream: "
                                                , instance.getName()
                                                , (System.currentTimeMillis() - startTime) / 1000.0
                                                , ex
                                        );

                                      });                                      
                                    });
                                  });
                        })
                        ;
              })
              ;
      futures.add(future);
    }
    
    CompositeFuture.all(futures)
            .onFailure(ex -> testContext.failNow(ex))
            .onSuccess(cf -> testContext.completeNow())
            ;
    
  }
}
