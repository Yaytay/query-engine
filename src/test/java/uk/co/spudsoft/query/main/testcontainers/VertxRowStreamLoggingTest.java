/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.testcontainers;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.PreparedStatement;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowStream;
import io.vertx.sqlclient.SqlConnectOptions;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class VertxRowStreamLoggingTest {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(VertxRowStreamLoggingTest.class);

  
  /**
   * Convert a RowStream into a ReadStream<JsonObject>.
   * No caching or queueing, a call to handler is simpler passed on.
   */
  private static class RowStreamLogger {

    private final Promise<Void> ended;

    public RowStreamLogger(Transaction transaction, RowStream<Row> rowStream, String name, int maxId, int fetchSize) {
      this.ended = Promise.promise();
      
      rowStream.handler(row -> {
        if (ended.future().isComplete()) {
          logger.debug("{} {},{}: Ended, but got: {}", name, maxId, fetchSize, row.toJson());
        } else {
          logger.debug("{} {},{}: Got: {}", name, maxId, fetchSize, row.toJson());
        }
      });
      rowStream.endHandler(v -> {
        logger.debug("{} {},{}: Ending ({})", name, maxId, fetchSize, ended);
        if (ended.tryComplete()) {
          transaction.commit().onComplete(ar -> {
            if (ar.succeeded()) {
              logger.debug("{} {},{}: Transaction completed", name, maxId, fetchSize);
            } else {
              logger.warn("{} {},{}: Transaction failed: ", name, maxId, fetchSize, ar.cause());
            }
          });
        }
      });
      rowStream.exceptionHandler(ex -> {
        logger.warn("{} {},{}: Broken: ", name, maxId, fetchSize, ex);
      });
    }
    
    public Future<Void> ended() {
      return ended.future();
    }
  }
  
  private SqlConnection conn;
  private PreparedStatement preparedStatement;

  @Test
  @Timeout(value = 20, timeUnit = TimeUnit.SECONDS)
  public void testPostgresFactor(Vertx vertx, VertxTestContext testContext) {

    SqlConnectOptions connectOptions = SqlConnectOptions.fromUri("postgres://localhost:2003/test");
    connectOptions.setUser("postgres");
    connectOptions.setPassword("T0p-secret");

    int fetchSize = 5;
    int maxId = 20;
    
    runTest(vertx, testContext, "postgresql", connectOptions, maxId, fetchSize);
  }

  @Test
  @Timeout(value = 20, timeUnit = TimeUnit.SECONDS)
  public void testMySqlFactor(Vertx vertx, VertxTestContext testContext) {

    SqlConnectOptions connectOptions = SqlConnectOptions.fromUri("mysql://localhost:2001/test");
    connectOptions.setUser("user");
    connectOptions.setPassword("T0p-secret");

    int fetchSize = 5;
    int maxId = 20;
    
    runTest(vertx, testContext, "mysql", connectOptions, maxId, fetchSize);
  }

  private void runTest(Vertx vertx, VertxTestContext testContext, String name, SqlConnectOptions connectOptions, int maxId, int fetchSize) {
    Pool pool = Pool.pool(vertx, connectOptions, new PoolOptions().setMaxSize(2));
    pool.getConnection()
            .compose(c -> {
              conn = c;
              return conn.prepare("select d.id from testData d where d.id < " + maxId + " order by d.id");
            }).compose(ps -> {
              preparedStatement = ps;
              return conn.begin();
            }).compose(tran -> {
              logger.debug("{} {},{}: Creating SQL stream", name, maxId, fetchSize);
              RowStream<Row> stream = preparedStatement.createStream(fetchSize);
              RowStreamLogger rsl = new RowStreamLogger(tran, stream, name, maxId, fetchSize);
              return rsl.ended();
            }).onComplete(ar -> {
              if (ar.failed()) {
                testContext.failNow(ar.cause());
              } else {
                vertx.setTimer(4000, l -> {
                  logger.debug("{} {},{}: Test completed", name, maxId, fetchSize);
                  testContext.completeNow();
                });
              }
            });
  }

}
