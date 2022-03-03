/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.queryengine;

import com.github.dockerjava.api.model.Container;
import com.google.common.collect.Iterators;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.SqlConnectOptions;
import io.vertx.sqlclient.Tuple;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;

/**
 *
 * @author jtalbut
 */
public class ServerProviderMySQL extends ServerProviderBase implements ServerProviderInstance {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ServerProviderMySQL.class);
  
  public static final String MYSQL_IMAGE_NAME = "mysql:8.0";

  private static final Object lock = new Object();
  private static MySQLContainer<?> mysqlserver;
  private static int port;
  
  @Override
  public String getName() {
    return "MySQL";
  }
  
  public ServerProviderMySQL init() {
    getContainer();
    return this;
  }
  
  @Override
  public Future<Void> prepareContainer(Vertx vertx) {
    return prepareContainer(vertx, vertx.getOrCreateContext());
  }
  
  @Override
  public Future<Void> prepareContainer(Vertx vertx, Context context) {    
    return context.executeBlocking(p -> {
      try {
        getContainer();
        p.complete();
      } catch(Throwable ex) {
        p.fail(ex);
      }
    });
  }

  @Override
  public SqlConnectOptions getOptions() {
    getContainer();
    return new MySQLConnectOptions()
            .setPort(port)
            .setHost("localhost")
            .setUser("user")
            .setDatabase("test")
            .setPassword(ServerProviderBase.ROOT_PASSWORD)
            ;
  }

  @Override
  public Pool createPool(Vertx vertx, SqlConnectOptions options, PoolOptions poolOptions) {
    return MySQLPool.pool(vertx, (MySQLConnectOptions) options, poolOptions);
  }
  
  public MySQLContainer<?> getContainer() {
    synchronized (lock) {
      long start = System.currentTimeMillis();
      
      Container createdContainer = findContainer("/query-engine-mysql-1");
      if (createdContainer != null) {
        port = Arrays.asList(createdContainer.ports).stream().filter(cp -> cp.getPrivatePort() == 3306).map(cp -> cp.getPublicPort()).findFirst().orElse(0);
        return null;
      } 
      if (mysqlserver == null) {
        mysqlserver = new MySQLContainer<>(MYSQL_IMAGE_NAME)
                .withUsername("user")
                .withPassword(ROOT_PASSWORD)
                .withExposedPorts(3306)
                .withDatabaseName("test")
                ;
      }
      if (!mysqlserver.isRunning()) {
        mysqlserver.start();
        logger.info("Started test instance of MySQL with ports {} in {}s",
                mysqlserver.getExposedPorts().stream().map(p -> Integer.toString(p) + ":" + Integer.toString(mysqlserver.getMappedPort(p))).collect(Collectors.toList()),
                (System.currentTimeMillis() - start) / 1000.0
        );
        port = mysqlserver.getMappedPort(3306);
      }
    }
    return mysqlserver;
  }

  @Override
  public Future<Void> prepareTestDatabase(Vertx vertx, SqlClient client) {

    return Future.succeededFuture()
            
            .compose(v -> client.preparedQuery("select count(*) from information_schema.tables where table_name='testRefData'").execute())
            .compose(rs -> {
              logger.info("Creating testRefData table");
              int existingTable = rs.iterator().next().getInteger(0);
              if (existingTable == 0) {
                return client
                        .preparedQuery(CREATE_REF_DATA_TABLE
                                .replaceAll("GUID", "binary(16)")
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
              logger.info("Inserting testRefData");
              int existingRows = rs.iterator().next().getInteger(0);
              Iterator<Map.Entry<UUID, String>> iter = REF_DATA.entrySet().iterator();
              iter = Iterators.limit(iter, REF_ROWS);
              Iterators.advance(iter, existingRows);
              return doRefDataInserts(
                      client.preparedQuery("insert into testRefData (id, value) values (?, ?)"),
                      iter
              );
            })
            
            .compose(v -> client.preparedQuery("select count(*) from information_schema.tables where table_name='testData'").execute())
            .compose(rs -> {
              logger.info("Creating testData table");
              int existingTable = rs.iterator().next().getInteger(0);
              if (existingTable == 0) {
                return client.preparedQuery(CREATE_DATA_TABLE
                        .replaceAll("GUID", "binary(16)")
                        .replaceAll("DATETIME", "datetime")
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
              logger.info("Inserting testData");
              int existingRows = rs.iterator().next().getInteger(0);
              return doDataInserts(
                      client.preparedQuery("insert into testData (id, lookup, instant, value) values (?, ?, ?, ?)"),
                       existingRows,
                       DATA_ROWS
              );
            })

            .compose(v -> client.preparedQuery("select count(*) from information_schema.tables where table_name='testManyData'").execute())
            .compose(rs -> {
              logger.info("Creating testManyData table");
              int existingTable = rs.iterator().next().getInteger(0);
              if (existingTable == 0) {
                return client.preparedQuery(CREATE_MANY_DATA_TABLE
                        .replaceAll("GUID", "binary(16)")
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
            .compose(rs -> {
              logger.info("Inserting testManyData");
              return doManyInserts(
                      client.preparedQuery(
                              """
                               insert into testManyData (dataId, refId) 
                               select d.id, ? 
                               from testData d left join testManyData m on d.id = m.dataId and m.refId = ? 
                               where id % ? >= ? and m.dataId is null 
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

  @Override
  protected Object convertUuid(UUID uuid) {
    Buffer b = Buffer.buffer(16);
    b.appendLong(uuid.getMostSignificantBits());
    b.appendLong(uuid.getLeastSignificantBits());
    return b;
  }
  
  @Override
  protected void prepareRefDataInsertTuple(List<Tuple> args, Map.Entry<UUID, String> entry) {
    args.add(Tuple.of(convertUuid(entry.getKey()), entry.getValue()));
  }
  
  
  @Override
  protected void prepareDataInsertTuple(List<Tuple> args, int currentRow, UUID lookup, LocalDateTime instant, int i) {
    args.add(Tuple.of(currentRow, convertUuid(lookup), instant, Integer.toHexString(i)));
  }  

  @Override
  public String limit(int maxRows, String sql) {
    return sql + " limit 0," + maxRows;
  }
  
}
