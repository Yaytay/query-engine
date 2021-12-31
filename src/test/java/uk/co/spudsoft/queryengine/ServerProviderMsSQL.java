/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.queryengine;

import com.google.common.collect.Iterators;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.mssqlclient.MSSQLConnectOptions;
import io.vertx.mssqlclient.MSSQLPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.SqlConnectOptions;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.Network;

/**
 *
 * @author jtalbut
 */
public class ServerProviderMsSQL extends ServerProviderBase implements ServerProviderInstance<MSSQLServerContainer<?>> {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ServerProviderMsSQL.class);
  
  public static final String MSSQL_IMAGE_NAME = "mcr.microsoft.com/mssql/server:2019-latest";

  private static final Object lock = new Object();
  private static Network network;
  private static MSSQLServerContainer<?> mssqlserver;

  @Override
  public String getName() {
    return "MS SQL Server";
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
  
  public ServerProviderMsSQL init() {
    getContainer();
    return this;
  }
  
  @Override
  public Future<MSSQLServerContainer<?>> prepareContainer(Vertx vertx) {
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
    return new MSSQLConnectOptions()
            .setPort(getContainer().getMappedPort(1433))
            .setHost("localhost")
            .setUser("sa")
            .setPassword(ServerProviderBase.ROOT_PASSWORD)
            ;
  }

  @Override
  public SqlClient createClient(Vertx vertx, SqlConnectOptions options, PoolOptions poolOptions) {
    return MSSQLPool.pool(vertx, (MSSQLConnectOptions) options, poolOptions);
  }  
  
  @Override
  public MSSQLServerContainer<?> getContainer() {
    synchronized (lock) {
      if (network == null) {
        network = Network.newNetwork();
      }
      long start = System.currentTimeMillis();
      if (mssqlserver == null) {
        mssqlserver = new MSSQLServerContainer<>(MSSQL_IMAGE_NAME)
                .withPassword(ROOT_PASSWORD)
                .withEnv("ACCEPT_EULA", "Y")
                .withExposedPorts(1433)
                .withNetwork(network);
      }
      if (!mssqlserver.isRunning()) {
        mssqlserver.start();
        logger.info("Started test instance of Microsoft SQL Server with ports {} in {}s",
                mssqlserver.getExposedPorts().stream().map(p -> Integer.toString(p) + ":" + Integer.toString(mssqlserver.getMappedPort(p))).collect(Collectors.toList()),
                (System.currentTimeMillis() - start) / 1000.0
        );
      }
    }
    return mssqlserver;
  }

  @Override
  public Future<Void> prepareTestDatabase(Vertx vertx, SqlClient client) {

    return Future.succeededFuture()
            .compose(v -> client.preparedQuery("select count(*) from sys.databases where name = 'test'").execute())
            .compose(rs -> {
              int existingTable = rs.iterator().next().getInteger(0);
              if (existingTable == 0) {
                return client.preparedQuery("create database test").execute();
              } else {
                return Future.succeededFuture();
              }
            })
            .onSuccess(rs -> {
              if (rs != null) {
                logger.info("Database created: {}", RowSetHelper.toString(rs));
              }
            })
            .compose(rs -> client.preparedQuery("select count(*) from sysobjects where name='testRefData' and xtype='U'").execute())
            .compose(rs -> {
              int existingTable = rs.iterator().next().getInteger(0);
              if (existingTable == 0) {
                return client
                        .preparedQuery(CREATE_REF_DATA_TABLE
                                .replaceAll("GUID", "uniqueidentifier")
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
                      client.preparedQuery("insert into testRefData (id, value) values (@p1, @p2)"),
                      iter
              );
            })
            .compose(rs -> client.preparedQuery("select count(*) from sysobjects where name='testData' and xtype='U'").execute())
            .compose(rs -> {
              int existingTable = rs.iterator().next().getInteger(0);
              if (existingTable == 0) {
                return client.preparedQuery(CREATE_DATA_TABLE
                        .replaceAll("GUID", "uniqueidentifier")
                        .replaceAll("DATETIME", "datetime2")
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
                      client.preparedQuery("insert into testData (id, lookup, instant, value) values (@p1, @p2, @p3, @p4)"),
                       existingRows,
                       DATA_ROWS
              );
            })
            .onFailure(ex -> {
              logger.error("Failed: ", ex);
            })
            .mapEmpty()
            ;

  }

  @Override
  public String limit(int maxRows, String sql) {
    return sql.replaceFirst("select ", "select top " + maxRows + " ");
  }
  
}
