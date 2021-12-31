/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.queryengine;

import com.google.common.collect.Iterators;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.SqlConnectOptions;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 *
 * @author jtalbut
 */
public class ServerProviderPostgreSQL extends ServerProviderBase implements ServerProviderInstance<PostgreSQLContainer<?>> {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ServerProviderPostgreSQL.class);
  
  public static final String PGSQL_IMAGE_NAME = "postgres:14.1-alpine";

  private static final Object lock = new Object();
  private static Network network;
  private static PostgreSQLContainer<?> pgsqlserver;
  
  @Override
  public String getName() {
    return "PostgreSQL";
  }
  
  @Override
  public Network getNetwork() {
    synchronized (lock) {
      if (network == null) {
        network = Network.newNetwork();
      }
    }
    return network;
  }
  
  public ServerProviderPostgreSQL init() {
    getContainer();
    return this;
  }

  @Override
  public Future<PostgreSQLContainer<?>> prepareContainer(Vertx vertx) {
    return vertx.executeBlocking(p -> {
      try {
        p.complete(getContainer());
      } catch(Throwable ex) {
        p.fail(ex);
      }
    });
  }
    
  @Override
  public SqlConnectOptions getOptions() {
    return new PgConnectOptions()
            .setPort(getContainer().getMappedPort(5432))
            .setHost("localhost")
            .setUser("postgres")
            .setDatabase("test")
            .setPassword(ServerProviderBase.ROOT_PASSWORD)
            ;
  }

  @Override
  public SqlClient createClient(Vertx vertx, SqlConnectOptions options, PoolOptions poolOptions) {
    return PgPool.client(vertx, (PgConnectOptions) options, poolOptions);
  }
    
  @Override
  public PostgreSQLContainer<?> getContainer() {
    synchronized (lock) {
      if (network == null) {
        network = Network.newNetwork();
      }
      long start = System.currentTimeMillis();
      if (pgsqlserver == null) {
        pgsqlserver = new PostgreSQLContainer<>(PGSQL_IMAGE_NAME)
                .withPassword(ROOT_PASSWORD)
                .withUsername("postgres")
                .withExposedPorts(5432)
                .withNetwork(network);
      }
      if (!pgsqlserver.isRunning()) {
        pgsqlserver.start();
        logger.info("Started test instance of PostgreSQL with ports {} in {}s",
                pgsqlserver.getExposedPorts().stream().map(p -> Integer.toString(p) + ":" + Integer.toString(pgsqlserver.getMappedPort(p))).collect(Collectors.toList()),
                (System.currentTimeMillis() - start) / 1000.0
        );
      }
    }
    return pgsqlserver;
  }

  @Override
  public Future<Void> prepareTestDatabase(Vertx vertx, SqlClient client) {

    return Future.succeededFuture()
            .compose(v -> client.preparedQuery("select count(*) from information_schema.tables where table_name='testrefdata'").execute())
            .compose(rs -> {
              int existingTable = rs.iterator().next().getInteger(0);
              if (existingTable == 0) {
                return client
                        .preparedQuery(CREATE_REF_DATA_TABLE
                                .replaceAll("GUID", "uuid")
                        ).execute();
              } else {
                return Future.succeededFuture();
              }
            })
            .onSuccess(rs -> {
              if (rs != null) {
                logger.info("testRefData table created: {}", RowSetHelper.toString(rs));
              }
            })
            .compose(rs -> client.preparedQuery("select count(*) from testRefData").execute())
            .compose(rs -> {
              int existingRows = rs.iterator().next().getInteger(0);
              Iterator<Map.Entry<UUID, String>> iter = REF_DATA.entrySet().iterator();
              iter = Iterators.limit(iter, REF_ROWS);
              Iterators.advance(iter, existingRows);
              return doRefDataInserts(
                      client.preparedQuery("insert into testRefData (id, value) values ($1, $2)"),
                      iter
              );
            })
            .compose(rs -> client.preparedQuery("select count(*) from information_schema.tables where table_name='testdata'").execute())
            .compose(rs -> {
              int existingTable = rs.iterator().next().getInteger(0);
              if (existingTable == 0) {
                return client.preparedQuery(CREATE_DATA_TABLE
                        .replaceAll("GUID", "uuid")
                        .replaceAll("DATETIME", "timestamp")
                ).execute();
              } else {
                return Future.succeededFuture();
              }
            })
            .onSuccess(rs -> {
              if (rs != null) {
                logger.info("testData table created: {}", RowSetHelper.toString(rs));
              }
            })
            .compose(rs -> client.preparedQuery("select count(*) from testData").execute())
            .compose(rs -> {
              int existingRows = rs.iterator().next().getInteger(0);
              return doDataInserts(
                      client.preparedQuery("insert into testData (id, lookup, instant, value) values ($1, $2, $3, $4)"),
                       existingRows,
                       DATA_ROWS
              );
            })
            .onFailure(ex -> {
              logger.error("Failed: ", ex);
            })
            .mapEmpty();

  }

  @Override
  public String limit(int maxRows, String sql) {
    return sql + " limit " + maxRows;
  }
    
}
