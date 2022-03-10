/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.testcontainers;

import com.github.dockerjava.api.model.Container;
import com.google.common.collect.Iterators;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
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
import org.testcontainers.containers.PostgreSQLContainer;
import uk.co.spudsoft.queryengine.RowSetHelper;

import static uk.co.spudsoft.query.main.testcontainers.ServerProviderBase.ROOT_PASSWORD;

/**
 *
 * @author jtalbut
 */
public class ServerProviderPostgreSQL extends ServerProviderBase implements ServerProviderInstance {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ServerProviderPostgreSQL.class);
  
  public static final String PGSQL_IMAGE_NAME = "postgres:14.1-alpine";

  private static final Object lock = new Object();
  private static PostgreSQLContainer<?> pgsqlserver;
  private static int port;
  
  @Override
  public String getName() {
    return "PostgreSQL";
  }
  
  public ServerProviderPostgreSQL init() {
    getContainer();
    return this;
  }

  @Override
  public Future<Void> prepareContainer(Vertx vertx) {
    return vertx.executeBlocking(p -> {
      try {
        getContainer();
        p.complete();
      } catch(Throwable ex) {
        p.fail(ex);
      }
    });
  }

  @Override
  public String getUrl() {
    return "postgresql://localhost:" + port + "/test";
  }

  @Override
  public int getPort() {
    return port;
  }
    
  public PostgreSQLContainer<?> getContainer() {
    synchronized (lock) {
      long start = System.currentTimeMillis();
      
      Container createdContainer = findContainer("/query-engine-postgresql-1");
      if (createdContainer != null) {
        port = Arrays.asList(createdContainer.ports).stream().filter(cp -> cp.getPrivatePort() == 5432).map(cp -> cp.getPublicPort()).findFirst().orElse(0);
        return null;
      } 
      
      if (pgsqlserver == null) {
        pgsqlserver = new PostgreSQLContainer<>(PGSQL_IMAGE_NAME)
                .withPassword(ROOT_PASSWORD)
                .withUsername("postgres")
                .withExposedPorts(5432)
                ;
      }
      if (!pgsqlserver.isRunning()) {
        pgsqlserver.start();
        logger.info("Started test instance of PostgreSQL with ports {} in {}s",
                pgsqlserver.getExposedPorts().stream().map(p -> Integer.toString(p) + ":" + Integer.toString(pgsqlserver.getMappedPort(p))).collect(Collectors.toList()),
                (System.currentTimeMillis() - start) / 1000.0
        );
      }
      port = pgsqlserver.getMappedPort(5432);
    }
    return pgsqlserver;
  }

  @Override
  public Future<Void> prepareTestDatabase(Vertx vertx) {

    SqlConnectOptions connectOptions = SqlConnectOptions.fromUri(getUrl());
    connectOptions.setUser("postgres");
    connectOptions.setPassword(ROOT_PASSWORD);
    Pool pool = Pool.pool(vertx, connectOptions, new PoolOptions().setMaxSize(3));
    
    return Future.succeededFuture()
            
            .compose(v -> pool.preparedQuery("select count(*) from information_schema.tables where table_name='testrefdata'").execute())
            .compose(rs -> {
              logger.info("Creating testRefData table");
              int existingTable = rs.iterator().next().getInteger(0);
              if (existingTable == 0) {
                return pool.preparedQuery(CREATE_REF_DATA_TABLE
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
            .compose(rs -> pool.preparedQuery("select count(*) from testRefData").execute())
            .compose(rs -> {
              logger.info("Inserting testRefData");
              int existingRows = rs.iterator().next().getInteger(0);
              Iterator<Map.Entry<UUID, String>> iter = REF_DATA.entrySet().iterator();
              iter = Iterators.limit(iter, REF_ROWS);
              Iterators.advance(iter, existingRows);
              return doRefDataInserts(
                      pool.preparedQuery("insert into testRefData (id, value) values ($1, $2)"),
                      iter
              );
            })
            
            .compose(rs -> pool.preparedQuery("select count(*) from information_schema.tables where table_name='testdata'").execute())
            .compose(rs -> {
              logger.info("Creating testData table");
              int existingTable = rs.iterator().next().getInteger(0);
              if (existingTable == 0) {
                return pool.preparedQuery(CREATE_DATA_TABLE
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
            .compose(rs -> pool.preparedQuery("select count(*) from testData").execute())
            .compose(rs -> {
              logger.info("Inserting testData");
              int existingRows = rs.iterator().next().getInteger(0);
              return doDataInserts(
                      pool.preparedQuery("insert into testData (id, lookup, instant, value) values ($1, $2, $3, $4)"),
                       existingRows,
                       DATA_ROWS
              );
            })

            .compose(rs -> pool.preparedQuery("select count(*) from information_schema.tables where table_name='testmanydata'").execute())
            .compose(rs -> {
              logger.info("Creating testManyData table");
              int existingTable = rs.iterator().next().getInteger(0);
              if (existingTable == 0) {
                return pool.preparedQuery(CREATE_MANY_DATA_TABLE
                        .replaceAll("GUID", "uuid")
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
                               insert into testManyData (dataid, refid) 
                               select d.id, $1 
                               from testData d left join testManyData m on d.id = m.dataId and m.refid = $2  
                               where id % $3 >= $4 and m.dataId is null 
                               order by id  
                              """
                      )
                      , 0
              );
            })

            .onFailure(ex -> {
              logger.error("Failed: ", ex);
            })
            .mapEmpty();

  }
    
}
