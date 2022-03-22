/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.testcontainers;

import uk.co.spudsoft.query.main.testhelpers.RowSetHelper;
import com.github.dockerjava.api.model.Container;
import com.google.common.collect.Iterators;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.mssqlclient.MSSQLConnectOptions;
import io.vertx.mssqlclient.MSSQLPool;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnectOptions;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MSSQLServerContainer;

/**
 *
 * @author jtalbut
 */
public class ServerProviderMsSQL extends ServerProviderBase implements ServerProvider {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ServerProviderMsSQL.class);
  
  public static final String MSSQL_IMAGE_NAME = "mcr.microsoft.com/mssql/server:2019-latest";

  private static final Object lock = new Object();
  private static MSSQLServerContainer<?> mssqlserver;
  private static int port;

  @Override
  public String getName() {
    return "MS SQL Server";
  }
  
  public ServerProviderMsSQL init() {
    getContainer();
    return this;
  }
    
  @Override
  public Future<Void> prepareContainer(Vertx vertx) {
    return vertx.executeBlocking(p -> {
              try {
                getContainer();
                p.complete();
              } catch (Throwable ex) {
                p.fail(ex);
              }
            })
            .compose(v -> {
              Pool pool = MSSQLPool.pool(vertx, getBasicOptions(), new PoolOptions().setMaxSize(1));
              return pool.preparedQuery("select count(*) from sys.databases where name = 'test'").execute()
                      .compose(rs -> {
                        int existingDb = rs.iterator().next().getInteger(0);
                        if (existingDb == 0) {
                          return pool.preparedQuery("create database test").execute();
                        } else {
                          return Future.succeededFuture();
                        }
                      })
                      .onSuccess(rs -> {
                        if (rs != null) {
                          logger.info("Database created: {}", RowSetHelper.toString(rs));
                        }
                      })
                      .mapEmpty()
                      ;
            });
  }

  private MSSQLConnectOptions getBasicOptions() {
    getContainer();
    return new MSSQLConnectOptions()
            .setPort(port)
            .setHost("localhost")
            .setUser("sa")
            .setPassword(ServerProviderBase.ROOT_PASSWORD)
            ;
  }

  @Override
  public String getUrl() {
    return "sqlserver://localhost:" + port + "/test";
  }

  @Override
  public String getUser() {
    return "sa";
  }

  @Override
  public String getPassword() {
    return ServerProviderBase.ROOT_PASSWORD;
  }

  @Override
  public int getPort() {
    return port;
  }
  
  public MSSQLServerContainer<?> getContainer() {
    synchronized (lock) {
      long start = System.currentTimeMillis();
      
      Container createdContainer = findContainer("/query-engine-mssql-1");
      if (createdContainer != null) {
        port = Arrays.asList(createdContainer.ports).stream().filter(cp -> cp.getPrivatePort() == 1433).map(cp -> cp.getPublicPort()).findFirst().orElse(0);
        return null;
      } 
      
      if (mssqlserver == null) {
        mssqlserver = new MSSQLServerContainer<>(MSSQL_IMAGE_NAME)
                .withPassword(ROOT_PASSWORD)
                .withEnv("ACCEPT_EULA", "Y")
                .withExposedPorts(1433)
                .withUrlParam("trustServerCertificate", "true")
                ;
      }
      if (!mssqlserver.isRunning()) {
        mssqlserver.start();
        logger.info("Started test instance of Microsoft SQL Server with ports {} in {}s",
                mssqlserver.getExposedPorts().stream().map(p -> Integer.toString(p) + ":" + Integer.toString(mssqlserver.getMappedPort(p))).collect(Collectors.toList()),
                (System.currentTimeMillis() - start) / 1000.0
        );
      }
      port = mssqlserver.getMappedPort(1433);
    }
    return mssqlserver;
  }

  @Override
  public Future<Void> prepareTestDatabase(Vertx vertx) {

    SqlConnectOptions connectOptions = SqlConnectOptions.fromUri(getUrl());
    connectOptions.setUser("sa");
    connectOptions.setPassword(ROOT_PASSWORD);
    Pool pool = Pool.pool(vertx, connectOptions, new PoolOptions().setMaxSize(3));
    
    return Future.succeededFuture()
            
            .compose(rs -> pool.preparedQuery("select count(*) from sysobjects where name='testRefData' and xtype='U'").execute())
            .compose(rs -> {
              logger.info("Creating testRefData table");
              int existingTable = rs.iterator().next().getInteger(0);
              if (existingTable == 0) {
                return pool
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
            .compose(rs -> pool.preparedQuery("select count(*) from testRefData").execute())
            .compose(rs -> {
              logger.info("Inserting testRefData");
              int existingRows = rs.iterator().next().getInteger(0);
              Iterator<Map.Entry<UUID, String>> iter = REF_DATA.entrySet().iterator();
              iter = Iterators.limit(iter, REF_ROWS);
              Iterators.advance(iter, existingRows);
              return doRefDataInserts(
                      pool.preparedQuery("insert into testRefData (id, value) values (@p1, @p2)"),
                      iter
              );
            })
            
            .compose(rs -> pool.preparedQuery("select count(*) from sysobjects where name='testData' and xtype='U'").execute())
            .compose(rs -> {
              logger.info("Creating testData table");
              int existingTable = rs.iterator().next().getInteger(0);
              if (existingTable == 0) {
                return pool.preparedQuery(CREATE_DATA_TABLE
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
            .compose(rs -> pool.preparedQuery("select count(*) from testData").execute())
            .compose(rs -> {
              logger.info("Inserting testData");
              int existingRows = rs.iterator().next().getInteger(0);
              return doDataInserts(
                      pool.preparedQuery("insert into testData (id, lookup, instant, value) values (@p1, @p2, @p3, @p4)"),
                       existingRows,
                       DATA_ROWS
              );
            })

            .compose(rs -> pool.preparedQuery("select count(*) from sysobjects where name='testManyData' and xtype='U'").execute())
            .compose(rs -> {
              logger.info("Creating testManyData table");
              int existingTable = rs.iterator().next().getInteger(0);
              if (existingTable == 0) {
                return pool.preparedQuery(CREATE_MANY_DATA_TABLE
                        .replaceAll("GUID", "uniqueidentifier")
                ).execute();
              } else {
                return Future.succeededFuture();
              }
            })
            .onSuccess(rs -> {
              if (rs != null) {
                logger.info("testManyData table created: {}", RowSetHelper.toString(rs));
              }
            })
            .compose(rs -> pool.preparedQuery("select count(*) from testManyData").execute())
            .compose(rs -> {
              logger.info("Inserting testManyData");
              return doManyInserts(
                      pool.preparedQuery(
                              """
                               insert into testManyData (dataId, sort, refId) 
                               select d.id, @p1, @p2
                               from testData d left join testManyData m on d.id = m.dataId and m.refId = @p3
                               where id % @p4 >= @p5 and m.dataId is null 
                               order by id 
                              """
                      )
                      , 0
              );
            })

            .onFailure(ex -> {
              logger.error("Failed: ", ex);
            })
            .mapEmpty()
            ;

  }

}
