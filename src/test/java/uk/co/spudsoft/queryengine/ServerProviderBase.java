/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.queryengine;

import io.vertx.core.Future;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;


/**
 *
 * @author jtalbut
 */
public class ServerProviderBase {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ServerProviderBase.class);
  
  public static final String ROOT_PASSWORD = UUID.randomUUID().toString();
  
  public static final int REF_ROWS = 123;
  public static final int DATA_ROWS = 1000;

  public static final Map<UUID, String> REF_DATA = createRefDataMap();
  
  protected static final Object lock = new Object();
  protected static Network network;
  
  public static Network getNetwork() {
    synchronized (lock) {
      if (network == null) {
        network = Network.newNetwork();
      }
    }
    return network;
  }

  private static Map<UUID, String> createRefDataMap() {
    Map<UUID, String> result = new HashMap<>();
    for (int i = 0; i < REF_ROWS; ++i) {
      result.put(UUID.randomUUID(), OrdinalNames.nameForNumber(i));
    }
    return result;
  }

  protected static final String CREATE_REF_DATA_TABLE
          = """
          create table testRefData (
            id GUID not null
            , value varchar(100) not null
          )
          """;

  protected static final String CREATE_DATA_TABLE
          = """
          create table testData (
            id int not null primary key
            , lookup GUID not null
            , instant DATETIME not null
            , value varchar(100) not null
          )
          """;

  
  protected Future<Void> doRefDataInserts(
          PreparedQuery<RowSet<Row>> stmt,
          Iterator<Map.Entry<UUID, String>> iter
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

  protected Future<Void> doDataInserts(
          PreparedQuery<RowSet<Row>> stmt,
           int currentRow,
           int totalRows
  ) {
    List<Tuple> args = new ArrayList<>();
    LocalDateTime base = LocalDateTime.now();
    List<UUID> lookups = new ArrayList<>(REF_DATA.keySet());
    for (int i = 0; i < 100 && currentRow <= totalRows; ++i, ++currentRow) {
      UUID lookup = lookups.get(currentRow % lookups.size());
      LocalDateTime instant = base.plusSeconds(currentRow);
      args.add(Tuple.of(currentRow, lookup, instant, Integer.toHexString(i)));
    }
    if (args.isEmpty()) {
      return Future.succeededFuture();
    } else {
      logger.debug("Running insert batch with {} tuples", args.size());
      int currentRowForNextBatch = currentRow;
      return stmt.executeBatch(args)
              .compose(v -> doDataInserts(stmt, currentRowForNextBatch, totalRows));
    }

  }
  
}
