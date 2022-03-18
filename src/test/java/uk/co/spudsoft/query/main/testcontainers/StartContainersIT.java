package uk.co.spudsoft.query.main.testcontainers;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.Cursor;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnectOptions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import uk.co.spudsoft.query.main.testhelpers.RowSetHelper;




/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class StartContainersIT {

  private static final Logger logger = LoggerFactory.getLogger(StartContainersIT.class);
  
  private static final ServerProviderMsSQL mssql = new ServerProviderMsSQL().init();
  private static final ServerProviderMySQL mysql = new ServerProviderMySQL().init();
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();

  @BeforeAll
  public static void setup() {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }
  
  @Test
  @Timeout(value = 10, timeUnit = TimeUnit.MINUTES)
  public void startAllContainers(Vertx vertx, VertxTestContext testContext) {
       
    List<ServerProvider> instances = Arrays.asList(
            mssql
            , mysql
            , postgres
    );
    
    
    List<Future> futures = new ArrayList<>();
    
    for (ServerProvider instance : instances) {
      long startTime = System.currentTimeMillis();
      Future<?> future = instance.prepareContainer(vertx)
              .compose(container -> {
                logger.debug("{} - {}s: Container started", instance.getName(), (System.currentTimeMillis() - startTime) / 1000.0);
                try {
                  SqlConnectOptions connectOptions = SqlConnectOptions.fromUri(instance.getUrl());
                  connectOptions.setUser(instance.getUser());
                  connectOptions.setPassword(instance.getPassword());
                  Pool pool = Pool.pool(vertx, connectOptions, new PoolOptions().setMaxSize(4));
                  return Future.succeededFuture(pool);
                } catch(Throwable ex) {
                  return Future.failedFuture(ex);
                }
              })
              .compose(sqlPool -> {
                return instance.prepareTestDatabase(vertx)
                        .compose(v -> {
                          logger.debug("{} - {}s: Test data prepared", instance.getName(), (System.currentTimeMillis() - startTime) / 1000.0);
                          
                          return sqlPool.getConnection();
                        })
                        .compose(conn -> {
                          String sql = """
                                       select d.id, d.instant, l.value as ref, d.value 
                                       from testData d
                                       join testRefData l on d.lookup = l.id  
                                       order by d.id
                                       """
                                       ;
                          return conn.prepare(sql)
                                  .compose(ps -> {
                                    Cursor cursor = ps.cursor();
                                    return cursor.read(10)
                                            .onSuccess(rs -> {
                                              int count = rs.size();
                                              logger.debug("{}: Rows: {} vs {}", instance.getName(), rs.rowCount(), rs.size());
                                              logger.debug("{} - {}s: Results: {}"
                                                      , instance.getName()
                                                      , (System.currentTimeMillis() - startTime) / 1000.0
                                                      , RowSetHelper.toString(rs)
                                              );
                                              logger.debug("{}: Rows: {} vs {}", instance.getName(), rs.rowCount(), rs.size());
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
