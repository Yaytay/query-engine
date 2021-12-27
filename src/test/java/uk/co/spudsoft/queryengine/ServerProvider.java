/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.queryengine;

import com.google.common.collect.Iterators;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.mssqlclient.MSSQLPool;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

/**
 *
 * @author jtalbut
 */
public class ServerProvider {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ServerProvider.class);

  public static final String MSSQL_IMAGE_NAME = "mcr.microsoft.com/mssql/server:2019-latest";
  public static final String MYSQL_IMAGE_NAME = "mysql:8.0";
  public static final String PGSQL_IMAGE_NAME = "postgres:14.1-alpine";
  
  public static final int REF_ROWS = 123;
  public static final int DATA_ROWS = 1000;
  
  public static final Map<UUID, String> REF_DATA = createRefDataMap();
  
  private static final Object lock = new Object();
  private static Network network;
  private static GenericContainer mssqlserver;
  private static GenericContainer mysqlserver;
  private static GenericContainer pgsqlserver;
  
  public static final String ROOT_PASSWORD = UUID.randomUUID().toString();
 
  public static Network getNetwork() {
    synchronized(lock) {
      if (network == null) {
        network = Network.newNetwork();        
      }
    }
    return network;
  }
  
  public static GenericContainer getMsSqlContainer() {
    synchronized(lock) {
      if (network == null) {
        network = Network.newNetwork();        
      }
      long start = System.currentTimeMillis();
      if (mssqlserver == null) {        
        mssqlserver = new GenericContainer(MSSQL_IMAGE_NAME)
                .withEnv("ACCEPT_EULA", "Y")
                .withEnv("SA_PASSWORD", ROOT_PASSWORD)
                .withExposedPorts(1433)
                .withNetwork(network)
                ;
      }
      if (!mssqlserver.isRunning()) {
        mssqlserver.start();
        logger.info("Started test instance of Microsoft SQL Server with ports {} in {}s"
                , mssqlserver.getExposedPorts().stream().map(p -> Integer.toString((Integer) p) + ":" + Integer.toString(mssqlserver.getMappedPort((Integer) p))).collect(Collectors.toList())
                , (System.currentTimeMillis() - start) / 1000.0
        );
      }
    }
    return mssqlserver;
  }

  public static GenericContainer getMySqlContainer() {
    synchronized(lock) {
      if (network == null) {
        network = Network.newNetwork();        
      }
      long start = System.currentTimeMillis();
      if (mysqlserver == null) {
        mysqlserver = new GenericContainer(MYSQL_IMAGE_NAME)
                .withEnv("MYSQL_ROOT_PASSWORD", ROOT_PASSWORD)
                .withExposedPorts(3306)
                .withNetwork(network)
                ;
      }
      if (!mysqlserver.isRunning()) {
        mysqlserver.start();
        logger.info("Started test instance of MySQL with ports {} in {}s"
                , mysqlserver.getExposedPorts().stream().map(p -> Integer.toString((Integer) p) + ":" + Integer.toString(mysqlserver.getMappedPort((Integer) p))).collect(Collectors.toList())
                , (System.currentTimeMillis() - start) / 1000.0
        );
      }
    }
    return mysqlserver;
  }

  public static GenericContainer getPgSqlContainer() {
    synchronized(lock) {
      if (network == null) {
        network = Network.newNetwork();        
      }
      long start = System.currentTimeMillis();
      if (pgsqlserver == null) {
        pgsqlserver = new GenericContainer(PGSQL_IMAGE_NAME)
                .withEnv("POSTGRES_PASSWORD", ROOT_PASSWORD)
                .withExposedPorts(5432)
                .withNetwork(network)
                ;
      }
      if (!pgsqlserver.isRunning()) {
        pgsqlserver.start();
        logger.info("Started test instance of PostgreSQL with ports {} in {}s"
                , pgsqlserver.getExposedPorts().stream().map(p -> Integer.toString((Integer) p) + ":" + Integer.toString(pgsqlserver.getMappedPort((Integer) p))).collect(Collectors.toList())
                , (System.currentTimeMillis() - start) / 1000.0
        );
      }
    }
    return pgsqlserver;
  }

  private static Map<UUID, String> createRefDataMap() {
    Map<UUID, String> result = new HashMap<>();
    for (int i = 0; i < REF_ROWS; ++i) {
      result.put(UUID.randomUUID(), OrdinalNames.nameForNumber(i));
    }
    return result;
  }
  
  private static final String CREATE_REF_DATA_TABLE = """
                                                      create table testRefData (
                                                        id GUID not null
                                                        , value varchar(100) not null
                                                      )
                                                      """;
  
  public static Future<Void> prepareMsSqlDatabase(Vertx vertx, MSSQLPool client) {
    
    return client.getConnection()
            .compose(conn -> {
              return conn.preparedQuery("if not exists(select * from sys.databases where name = 'test') begin create database test end")
                      .execute()
                      .onSuccess(rs -> { logger.info("Database created: {}", toString(rs)); } )
                      .compose(rs -> conn.preparedQuery("if not exists (select * from sysobjects where name='testRefData' and xtype='U') begin "
                              + CREATE_REF_DATA_TABLE.replaceAll("GUID", "uniqueidentifier")
                              + " end").execute()
                      )
                      .onSuccess(rs -> { logger.info("testRefData table created: {}", toString(rs)); } )
                      .compose(rs -> conn.preparedQuery("select count(*) from testRefData").execute())
                      .compose(rs -> {
                        int existingRows = rs.iterator().next().getInteger(0);
                        logger.info("There are currently {} rows in testRefData", existingRows);
                        Iterator<Map.Entry<UUID, String>> iter = REF_DATA.entrySet().iterator();
                        iter = Iterators.limit(iter, REF_ROWS);
                        Iterators.advance(iter, existingRows);
                        return doRefDataInserts(
                                conn.preparedQuery("insert into testRefData (id, value) values (@p1, @p2)")
                                , iter
                        );
                      })
                      .compose(rs -> conn.preparedQuery("select count(*) from testRefData").execute())
                      .compose(rs -> {
                        int existingRows = rs.iterator().next().getInteger(0);
                        logger.info("There are currently {} rows in testRefData", existingRows);
                        return Future.succeededFuture();
                      })
                      .onFailure(ex -> {
                        logger.error("Failed: ", ex);
                      })
                      .mapEmpty();
            });
    
  }
  
  private static Future<Void> doRefDataInserts(
            PreparedQuery<RowSet<Row>> stmt
          , Iterator<Map.Entry<UUID, String>> iter
  ) {
    List<Tuple> args = new ArrayList<>();
    for (int i = 0; i < 100 && iter.hasNext(); ++i) {
      Map.Entry<UUID, String> entry = iter.next();
      args.add(Tuple.of(entry.getKey(), entry.getValue()));
    }
    if (args.isEmpty()) {
      return Future.succeededFuture();
    } else {
      logger.debug("Running insert batch with {} tuples", args.size());
      return stmt.executeBatch(args)
              .compose(v -> doRefDataInserts(stmt, iter));
    }
    
  }
  
  public static String toString(RowSet<Row> rowSet) {
    JsonArray ja = new JsonArray();
    for (Row row : rowSet) {
      ja.add(row.toJson());
    }
    return ja.encode();
    
  }
  
}
