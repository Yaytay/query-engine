package uk.co.spudsoft.queryengine;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class TestStartContainers1 {

  private static final Logger logger = LoggerFactory.getLogger(TestStartContainers1.class);
  
  @Test
  @Timeout(value = 10, timeUnit = TimeUnit.MINUTES)
  public void startAllContainers(Vertx vertx, VertxTestContext testContext) {
        
    List<ServerProviderInstance<?>> instances = Arrays.asList(
            new ServerProviderMsSQL()
            , new ServerProviderMySQL()
            , new ServerProviderPostgreSQL()
    );
    
    List<Future> futures = new ArrayList<>();
    
    for (ServerProviderInstance<?> instance : instances) {
      long startTime = System.currentTimeMillis();
      Future future = instance.prepareContainer(vertx)
              .compose(container -> {
                logger.debug("{} - {}s: Container started", instance.getName(), (System.currentTimeMillis() - startTime) / 1000.0);
                try {
                  SqlClient client = instance.createClient(vertx, instance.getOptions(), new PoolOptions().setMaxSize(4));
                  return Future.succeededFuture(client);
                } catch(Throwable ex) {
                  return Future.failedFuture(ex);
                }
              })
              .compose(sqlClient -> {
                return instance.prepareTestDatabase(vertx, sqlClient)
                        .compose(v -> {
                          logger.debug("{} - {}s: Test data prepared", instance.getName(), (System.currentTimeMillis() - startTime) / 1000.0);
                          return sqlClient.preparedQuery(
                                  instance.limit(30
                                          , 
                                          """
                                            select d.id, d.instant, l.value as ref, d.value 
                                            from testData d
                                            join testRefData l on d.lookup = l.id  
                                            order by d.id
                                          """
                                          )
                                  ).execute()
                                          .onSuccess(rs -> {
                                            logger.debug("{} - {}s: Results: {}"
                                                    , instance.getName()
                                                    , (System.currentTimeMillis() - startTime) / 1000.0
                                                    , RowSetHelper.toString(rs)
                                            );
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
