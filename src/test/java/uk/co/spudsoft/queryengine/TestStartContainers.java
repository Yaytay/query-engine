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
import io.vertx.sqlclient.PoolOptions;
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

    // Create the client pool
    MSSQLPool client = MSSQLPool.pool(
            new MSSQLConnectOptions()
                    .setPort(ServerProvider.getMsSqlContainer().getMappedPort(1433))
                    .setHost("localhost")
                    .setUser("sa")
                    .setPassword(ServerProvider.ROOT_PASSWORD),
             new PoolOptions()
                    .setMaxSize(5)
    );
    ServerProvider.prepareMsSqlDatabase(vertx, client)
            .compose(v -> ServerProvider.prepareMsSqlDatabase(vertx, client))
            .onComplete(ar -> {
              if (ar.succeeded()) {
                testContext.completeNow();
              } else {
                testContext.failNow(ar.cause());
              }
            });
  }

}
