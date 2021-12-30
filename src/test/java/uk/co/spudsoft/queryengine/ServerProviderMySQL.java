/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.queryengine;

import com.google.common.collect.Iterators;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;

/**
 *
 * @author jtalbut
 */
public class ServerProviderMySQL extends ServerProviderBase implements ServerProviderInstance<MySQLContainer<?>> {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ServerProviderMySQL.class);
  
  public static final String MYSQL_IMAGE_NAME = "mysql:8.0";

  private static MySQLContainer<?> mysqlserver;
  
  public ServerProviderMySQL init() {
    getContainer();
    return this;
  }
  
  @Override
  public MySQLContainer<?> getContainer() {
    synchronized (lock) {
      if (network == null) {
        network = Network.newNetwork();
      }
      long start = System.currentTimeMillis();
      if (mysqlserver == null) {
        mysqlserver = new MySQLContainer<>(MYSQL_IMAGE_NAME)
                .withUsername("user")
                .withPassword(ROOT_PASSWORD)
                .withExposedPorts(3306)
                .withDatabaseName("test")
                .withNetwork(network);
      }
      if (!mysqlserver.isRunning()) {
        mysqlserver.start();
        logger.info("Started test instance of MySQL with ports {} in {}s",
                mysqlserver.getExposedPorts().stream().map(p -> Integer.toString(p) + ":" + Integer.toString(mysqlserver.getMappedPort(p))).collect(Collectors.toList()),
                (System.currentTimeMillis() - start) / 1000.0
        );
      }
    }
    return mysqlserver;
  }

  @Override
  public Future<Void> prepareTestDatabase(Vertx vertx, SqlClient client) {

    return Future.succeededFuture()
            .compose(v -> client.preparedQuery("select count(*) from information_schema.tables where table_schema='public' and table_name='testRefData'").execute())
            .compose(rs -> {
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
              int existingRows = rs.iterator().next().getInteger(0);
              Iterator<Map.Entry<UUID, String>> iter = REF_DATA.entrySet().iterator();
              iter = Iterators.limit(iter, REF_ROWS);
              Iterators.advance(iter, existingRows);
              return doRefDataInserts(
                      client.preparedQuery("insert into testRefData (id, value) values (?, ?)"),
                      iter
              );
            })
            .compose(v -> client.preparedQuery("select count(*) from information_schema.tables where table_schema='public' and table_name='testData'").execute())
            .compose(rs -> {
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
              int existingRows = rs.iterator().next().getInteger(0);
              return doDataInserts(
                      client.preparedQuery("insert into testData (id, lookup, instant, value) values (?, ?, ?, ?)"),
                       existingRows,
                       DATA_ROWS
              );
            })
            .onFailure(ex -> {
              logger.error("Failed: ", ex);
            })
            .mapEmpty();

  }

  private Buffer uuidToArray(UUID uuid) {
    Buffer b = Buffer.buffer(16);
    b.appendLong(uuid.getMostSignificantBits());
    b.appendLong(uuid.getLeastSignificantBits());
    return b;
  }
  
  @Override
  protected void prepareRefDataInsertTuple(List<Tuple> args, Map.Entry<UUID, String> entry) {
    args.add(Tuple.of(uuidToArray(entry.getKey()), entry.getValue()));
  }
  
  
  @Override
  protected void prepareDataInsertTuple(List<Tuple> args, int currentRow, UUID lookup, LocalDateTime instant, int i) {
    args.add(Tuple.of(currentRow, uuidToArray(lookup), instant, Integer.toHexString(i)));
  }  
  
}
