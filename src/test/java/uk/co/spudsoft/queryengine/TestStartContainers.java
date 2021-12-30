/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.queryengine;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.mssqlclient.MSSQLConnectOptions;
import io.vertx.mssqlclient.MSSQLPool;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class TestStartContainers {

  private static final Logger logger = LoggerFactory.getLogger(TestStartContainers.class);
  
  @Test
  @Timeout(value = 10, timeUnit = TimeUnit.MINUTES)
  public void startAllContainers(Vertx vertx, VertxTestContext testContext) {
        
    ServerProviderMsSQL msSqlProvider = new ServerProviderMsSQL().init();
    // Create the MS client pool
    SqlClient msTempClient = null;
    if (msSqlProvider != null) {
      MSSQLConnectOptions options = new MSSQLConnectOptions()
                    .setPort(msSqlProvider.getContainer().getMappedPort(1433))
                    .setHost("localhost")
                    .setUser("sa")
                    .setPassword(ServerProviderBase.ROOT_PASSWORD)
              ;
      msTempClient = MSSQLPool.pool(
              options 
              , new PoolOptions()
                      .setMaxSize(5)
      );
    } 
    SqlClient msClient = msTempClient;

    ServerProviderMySQL mySqlProvider = new ServerProviderMySQL().init();
    // Create the MySQL client pool
    SqlClient myTempClient = null;
    if (mySqlProvider != null) {
      MySQLConnectOptions options = new MySQLConnectOptions()
              .setPort(mySqlProvider.getContainer().getMappedPort(3306))
              .setHost("localhost")
              .setUser("user")
              .setDatabase("test")
              .setPassword(ServerProviderBase.ROOT_PASSWORD)
              ;
      myTempClient = MySQLPool.pool(
              options
              , new PoolOptions()
                      .setMaxSize(5)
      );
    }
    SqlClient myClient = myTempClient;

    ServerProviderPostgreSQL pgSqlProvider = new ServerProviderPostgreSQL().init();
    // Create the PostgreSQL client pool
    SqlClient pgTempClient = null;
    if (pgSqlProvider != null) {
      PgConnectOptions options = new PgConnectOptions()
                    .setPort(pgSqlProvider.getContainer().getMappedPort(5432))
                    .setHost("localhost")
                    .setUser("postgres")
                    .setDatabase("test")
                    .setPassword(ServerProviderBase.ROOT_PASSWORD)
              ;
      pgTempClient = PgPool.client(
              options 
              , new PoolOptions()
                      .setMaxSize(5)
      );
    }
    SqlClient pgClient = pgTempClient;

    Future.succeededFuture()
            .compose(v -> msSqlProvider == null ? Future.succeededFuture() : msSqlProvider.prepareTestDatabase(vertx, msClient))
            .compose(v -> {
              if (msClient == null) {
                return Future.succeededFuture();
              }
              logger.debug("Running MS query now");
              return msClient.preparedQuery(
                      """
                      select top 30 d.id, d.instant, l.value as ref, d.value 
                      from testData d
                      join testRefData l on d.lookup = l.id  
                      order by d.id
                      """
              ).execute();
            })
            .onSuccess(rs -> {
              if (rs != null) {
                logger.debug("MS Query results: {}", RowSetHelper.toString(rs));
                testContext.verify(() -> {
                  assertThat(rs.rowCount(), greaterThan(0));
                });
              }
            })
            .compose(v -> mySqlProvider == null ? Future.succeededFuture() : mySqlProvider.prepareTestDatabase(vertx, myClient))
            .compose(v -> {
              if (myClient == null) {
                return Future.succeededFuture();
              }
              logger.debug("Running MySQL query now");
              logger.debug("Ref data: {}", Json.encode(ServerProviderBase.REF_DATA));
              return myClient.preparedQuery(
                      """
                      select d.id, d.instant, l.value as ref, d.value 
                      from testData d
                      join testRefData l on d.lookup = l.id  
                      order by d.id
                      limit 0, 30
                      """
              ).execute();
            })
            .onSuccess(rs -> {
              if (rs != null) {
                logger.debug("MySQL Query results: {}", RowSetHelper.toString(rs));
                testContext.verify(() -> {
                  assertThat(rs.rowCount(), greaterThan(0));
                });
              }
            })
            .compose(v -> pgSqlProvider == null ? Future.succeededFuture() : pgSqlProvider.prepareTestDatabase(vertx, pgClient))
            .compose(v -> {
              if (pgClient == null) {
                return Future.succeededFuture();
              }
              logger.debug("Running PG query now");
              return pgClient.preparedQuery(
                      """
                      select d.id, d.instant, l.value as ref, d.value 
                      from testData d
                      join testRefData l on d.lookup = l.id  
                      order by d.id
                      limit 30
                      """
              ).execute();
            })
            .onSuccess(rs -> {
              if (rs != null) {
                logger.debug("PG Query results: {}", RowSetHelper.toString(rs));
                testContext.verify(() -> {
                  assertThat(rs.rowCount(), greaterThan(0));
                });
              }
            })
            .onComplete(ar -> {
              if (ar.succeeded()) {
                testContext.completeNow();
              } else {
                testContext.failNow(ar.cause());
              }
            });
  }

}
