/*
 * Copyright (C) 2022 jtalbut
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.co.spudsoft.query.testcontainers;

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
import uk.co.spudsoft.query.defn.DataType;


/**
 *
 * @author jtalbut
 */
public abstract class AbstractServerProvider {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(AbstractServerProvider.class);

  private static final Random RANDOM = new Random(0);
  
  public static final String ROOT_PASSWORD = "T0p-secret"; 
  
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
      } catch (NumberFormatException ex) {
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
            , sort int not null
            , refId GUID not null
            , constraint PK_TestManyData primary key (dataId, refId)
          )
          """;

  protected static final String CREATE_FIELD_DEFN_TABLE
          = """
          create table testFields (
            fieldId int not null primary key
            , name varchar(100) not null
            , type varchar(100) not null
            , valueField varchar(100) not null
          )
          """;

  protected static final String CREATE_FIELD_DATA_TABLE
          = """
          create table testFieldValues (
            parentId int not null
            , fieldId int not null
            , dateValue date null
            , timeValue time null
            , dateTimeValue datetime null
            , longValue bigint null
            , doubleValue real null
            , boolValue bit null
            , textValue varchar(1000) null
            , primary key (parentId, fieldId)
          )
          """;

  protected static final String CREATE_DYNAMIC_ENDPOINT_TABLE
          = """
          create table testDynamicEndpoint (
              endpointKey varchar(50) not null primary key
              , type varchar(10)
              , url varchar(1000)
              , urlTemplate varchar(1000)
              , secret varchar(100)
              , username varchar(100)
              , password varchar(100)
              , useCondition varchar(1000)
          )
          """;
  
  protected Future<Void> doRefDataInserts(
          PreparedQuery<RowSet<Row>> stmt,
          Iterator<Map.Entry<UUID, String>> iter
  ) {
    List<Tuple> args = new ArrayList<>();
    for (int i = 0; i < 100 && iter.hasNext(); ++i) {
      Map.Entry<UUID, String> entry = iter.next();
      AbstractServerProvider.this.prepareRefDataInsertTuple(args, entry);
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
      return stmt.execute(
              Tuple.of(
                        currentIteration
                      , convertUuid(REF_DATA_KEYS.get(currentIteration))
                      , convertUuid(REF_DATA_KEYS.get(currentIteration))
                      , MANY_DATA_MAX_ROWS
                      , currentIteration
              )
        ).compose(v -> doManyInserts(stmt, currentIteration + 1));
    }    
  }
  
  protected Future<Void> doFieldsInserts(
          PreparedQuery<RowSet<Row>> stmt
          , boolean lower
          , boolean repeatParams
  ) {
    List<Tuple> args = new ArrayList<>();
    args.add(repeat(repeatParams, 1, "DateField", DataType.Date.name(), lower ? "datevalue" : "dateValue"));
    args.add(repeat(repeatParams, 2, "TimeField", DataType.Time.name(), lower ? "timevalue" : "timeValue"));
    args.add(repeat(repeatParams, 3, "DateTimeField", DataType.DateTime.name(), lower ? "datetimevalue" : "dateTimeValue"));
    args.add(repeat(repeatParams, 4, "LongField", DataType.Long.name(), lower ? "longvalue" : "longValue"));
    args.add(repeat(repeatParams, 5, "DoubleField", DataType.Double.name(), lower ? "doublevalue" : "doubleValue"));
    args.add(repeat(repeatParams, 6, "BoolField", DataType.Boolean.name(), lower ? "boolvalue" : "boolValue"));
    args.add(repeat(repeatParams, 7, "TextField", DataType.String.name(), lower ? "textvalue" : "textValue"));
    return stmt.executeBatch(args).mapEmpty();
  }
  
  private static Tuple repeat(boolean repeatParams, Object arg1, Object arg2, Object arg3, Object arg4) {
    if (repeatParams) {
      return Tuple.wrap(new Object[]{arg1, arg2, arg3, arg4, arg1, arg2, arg3, arg4});
    } else {
      return Tuple.of(arg1, arg2, arg3, arg4);
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
