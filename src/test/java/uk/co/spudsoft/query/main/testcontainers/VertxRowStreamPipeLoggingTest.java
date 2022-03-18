/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.testcontainers;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.streams.WriteStream;
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
public class VertxRowStreamPipeLoggingTest {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(VertxRowStreamPipeLoggingTest.class);

  private static class LoggingWriteStream<T> implements WriteStream<T> {

    private Handler<Throwable> exceptionHandler;

    @Override
    public WriteStream<T> exceptionHandler(Handler<Throwable> handler) {
      this.exceptionHandler = handler;
      return this;
    }

    @Override
    public Future<Void> write(T data) {
      Promise<Void> promise = Promise.promise();
      write(data, promise);
      return promise.future();
    }

    @Override
    public void write(T data, Handler<AsyncResult<Void>> handler) {
      logger.debug("Received: {}", data);
      handler.handle(Future.succeededFuture());
    }

    @Override
    public void end(Handler<AsyncResult<Void>> handler) {
      handler.handle(Future.succeededFuture());
    }

    @Override
    public WriteStream<T> setWriteQueueMaxSize(int maxSize) {
      return this;
    }

    @Override
    public boolean writeQueueFull() {
      return false;
    }

    @Override
    public WriteStream<T> drainHandler(Handler<Void> handler) {
      throw new UnsupportedOperationException("Not supported yet.");
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
    
    runTest(vertx, testContext, connectOptions, maxId, fetchSize);
  }

  @Test
  @Timeout(value = 20, timeUnit = TimeUnit.SECONDS)
  public void testMySqlFactor(Vertx vertx, VertxTestContext testContext) {

    SqlConnectOptions connectOptions = SqlConnectOptions.fromUri("mysql://localhost:2001/test");
    connectOptions.setUser("user");
    connectOptions.setPassword("T0p-secret");

    int fetchSize = 5;
    int maxId = 20;
    
    runTest(vertx, testContext, connectOptions, maxId, fetchSize);
  }

  private void runTest(Vertx vertx, VertxTestContext testContext, SqlConnectOptions connectOptions, int maxId, int fetchSize) {
    Pool pool = Pool.pool(vertx, connectOptions, new PoolOptions().setMaxSize(2));
    pool.getConnection()
            .compose(c -> {
              conn = c;
              return conn.prepare("select d.id from testData d where d.id < " + maxId + " order by d.id");
            }).compose(ps -> {
              preparedStatement = ps;
              return conn.begin();
            }).compose(tran -> {
              logger.debug("Creating SQL stream");
              RowStream<Row> stream = preparedStatement.createStream(fetchSize);
              return stream.pipeTo(new LoggingWriteStream<>());
            }).onComplete(ar -> {
              if (ar.failed()) {
                testContext.failNow(ar.cause());
              } else {
                vertx.setTimer(4000, l -> {
                  testContext.completeNow();
                });
              }
            });
  }

}
