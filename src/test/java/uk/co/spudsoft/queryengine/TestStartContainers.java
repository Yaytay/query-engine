/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.queryengine;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.mssqlclient.MSSQLConnectOptions;
import io.vertx.mssqlclient.MSSQLPool;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class TestStartContainers {

  private static final Logger logger = LoggerFactory.getLogger(TestStartContainers.class);

  @Test
  public void startAllContainers(Vertx vertx, VertxTestContext testContext) {
    GenericContainer container;
    container = ServerProvider.getMsSqlContainer();
    container = ServerProvider.getMySqlContainer();
    container = ServerProvider.getPgSqlContainer();

    // Create the MS client pool
    SqlClient msClient = MSSQLPool.pool(
            new MSSQLConnectOptions()
                    .setPort(ServerProvider.getMsSqlContainer().getMappedPort(1433))
                    .setHost("localhost")
                    .setUser("sa")
                    .setPassword(ServerProvider.ROOT_PASSWORD),
             new PoolOptions()
                    .setMaxSize(5)
    );
    // Create the client pool
    SqlClient pgClient = PgPool.client(
            new PgConnectOptions()
                    .setPort(ServerProvider.getPgSqlContainer().getMappedPort(5432))
                    .setHost("localhost")
                    .setUser("postgres ")
                    .setPassword(ServerProvider.ROOT_PASSWORD),
             new PoolOptions()
                    .setMaxSize(5)
    );

    ServerProvider.prepareMsSqlDatabase(vertx, msClient)
            .compose(v -> ServerProvider.prepareMsSqlDatabase(vertx, msClient))
            .compose(v -> {
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
              logger.debug("MS Query results: {}", RowSetHelper.toString(rs));
            })
            .compose(v -> ServerProvider.preparePgSqlDatabase(vertx, pgClient))
            .compose(v -> {
              logger.debug("Running PG query now");
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
              logger.debug("PG Query results: {}", RowSetHelper.toString(rs));
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
