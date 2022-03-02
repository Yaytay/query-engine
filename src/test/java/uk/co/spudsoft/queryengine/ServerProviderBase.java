/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.queryengine;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.google.common.base.Strings;
import io.vertx.core.Future;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;


/**
 *
 * @author jtalbut
 */
public abstract class ServerProviderBase {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ServerProviderBase.class);

  private static final Random RANDOM = new Random(0);
  
  public static final String ROOT_PASSWORD = "T0p-secret"; // UUID.randomUUID().toString();
  
  public static final int REF_ROWS = 123;
  public static final int DATA_ROWS = getRowsInTestDb();
  public static final int MANY_DATA_MAX_ROWS = 7;

  public static final Map<UUID, String> REF_DATA;
  public static final List<UUID> REF_DATA_KEYS;
    
  static {
    Map<UUID, String> map = new HashMap<>();
    List<UUID> list = new ArrayList<>();
    for (int i = 0; i < REF_ROWS; ++i) {
      UUID uuid = new UUID(RANDOM.nextLong(), RANDOM.nextLong());
      list.add(uuid);
      map.put(uuid, OrdinalNames.nameForNumber(i));
    }
    REF_DATA = map;
    REF_DATA_KEYS = list;
  }
  
  private static int getRowsInTestDb() {
    String value = System.getProperty("rows.in.testdb");
    if (!Strings.isNullOrEmpty(value)) {
      try {
        int rowsInTestDb = Integer.parseInt(value);
        logger.info("Using {} rows in test db", rowsInTestDb);
        return rowsInTestDb;
      } catch(NumberFormatException ex) {
        logger.warn("System property rows.in.testdb has the value {} and is not an integer: {}", value, ex.getMessage());
      }
    }
    return 1000;
  }

  protected abstract String getName();
  
  protected static final String CREATE_REF_DATA_TABLE
          = """
          create table testRefData (
            id GUID not null primary key
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

  protected static final String CREATE_MANY_DATA_TABLE
          = """
          create table testManyData (
            dataId int not null
            , refId GUID not null
            , constraint PK_TestManyData primary key (dataId, refId)
          )
          """;

  
  protected Future<Void> doRefDataInserts(
          PreparedQuery<RowSet<Row>> stmt,
          Iterator<Map.Entry<UUID, String>> iter
  ) {
    List<Tuple> args = new ArrayList<>();
    for (int i = 0; i < 100 && iter.hasNext(); ++i) {
      Map.Entry<UUID, String> entry = iter.next();
      ServerProviderBase.this.prepareRefDataInsertTuple(args, entry);
    }
    if (args.isEmpty()) {
      return Future.succeededFuture();
    } else {
      logger.debug("{}: Running refdata insert batch with {} tuples", getName(), args.size());
      return stmt.executeBatch(args)
              .compose(v -> doRefDataInserts(stmt, iter));
    }

  }

  protected void prepareRefDataInsertTuple(List<Tuple> args, Map.Entry<UUID, String> entry) {
    args.add(Tuple.of(entry.getKey(), entry.getValue()));
  }

  protected Future<Void> doDataInserts(
          PreparedQuery<RowSet<Row>> stmt,
           int currentRows,
           int totalRows
  ) {
    List<Tuple> args = new ArrayList<>();
    LocalDateTime base = LocalDateTime.of(1971, Month.MAY, 6, 0, 0);
    for (int i = 0; i < 1000 && currentRows < totalRows; ++i, ++currentRows) {
      UUID lookup = REF_DATA_KEYS.get(currentRows % REF_DATA_KEYS.size());
      LocalDateTime instant = base.plusSeconds(currentRows);
      prepareDataInsertTuple(args, currentRows, lookup, instant, i);
    }
    if (args.isEmpty()) {
      return Future.succeededFuture();
    } else {
      logger.debug("{}: Running data insert batch with {} tuples (currentRows = {} and totalRows = {})", getName(), args.size(), currentRows, totalRows);
      int currentRowForNextBatch = currentRows;
      return stmt.executeBatch(args)
              .compose(v -> doDataInserts(stmt, currentRowForNextBatch, totalRows));
    }
  }
  
  protected Future<Void> doManyInserts(
          PreparedQuery<RowSet<Row>> stmt
          , int currentIteration
  ) {
    if (currentIteration >= MANY_DATA_MAX_ROWS) {
      return Future.succeededFuture();
    } else {
      logger.debug("{}: Running many data insert batch iteration {}", getName(), currentIteration);
      return stmt.execute(Tuple.of(convertUuid(REF_DATA_KEYS.get(currentIteration)), convertUuid(REF_DATA_KEYS.get(currentIteration)), MANY_DATA_MAX_ROWS, currentIteration))
              .compose(v -> doManyInserts(stmt, currentIteration + 1));
    }    
  }
  
  protected Object convertUuid(UUID value) {
    return value;
  }

  protected void prepareDataInsertTuple(List<Tuple> args, int currentRow, UUID lookup, LocalDateTime instant, int i) {
    args.add(Tuple.of(currentRow, lookup, instant, Integer.toHexString(i)));
  }

  protected Container findContainer(String containerName) {
    DockerClient dockerClient = DockerClientFactory.lazyClient();
    Container createdContainer = dockerClient.listContainersCmd().withShowAll(true).exec().stream().filter(container -> {
      return Arrays.asList(container.getNames()).contains(containerName);
    }).findFirst().orElse(null);
    if (createdContainer != null) {
      logger.info("Container {} has state {}", createdContainer.getNames(), createdContainer.getState());
      logger.info("Container {} has status {}", createdContainer.getNames(), createdContainer.getStatus());
      if (!"running".equals(createdContainer.getState())) {
        return null;
      }
    }
    return createdContainer;
  }
  
}
