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

import com.github.dockerjava.api.model.Container;
import com.google.common.collect.Iterators;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
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
import uk.co.spudsoft.query.testhelpers.RowSetHelper;

import static uk.co.spudsoft.query.testcontainers.AbstractServerProvider.ROOT_PASSWORD;

/**
 *
 * @author jtalbut
 */
public class ServerProviderMySQL extends AbstractServerProvider implements ServerProvider {

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
    return vertx.executeBlocking(p -> {
      try {
        getContainer();
        p.complete();
      } catch (Throwable ex) {
        p.fail(ex);
      }
    });
  }

  @Override
  public MySQLConnectOptions getConnectOptions() {
    getContainer();
    return new MySQLConnectOptions()
            .setPort(port)
            .setHost("localhost")
            .setUser("user")
            .setDatabase("test")
            .setPassword(AbstractServerProvider.ROOT_PASSWORD)
            ;
  }

  @Override
  public String getUrl() {
    return "mysql://localhost:" + port + "/test";
  }

  @Override
  public String getUser() {
    return "user";
  }

  @Override
  public String getPassword() {
    return AbstractServerProvider.ROOT_PASSWORD;
  }

  @Override
  public int getPort() {
    return port;
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
  public Future<Void> prepareTestDatabase(Vertx vertx) {

    SqlConnectOptions connectOptions = SqlConnectOptions.fromUri(getUrl());
    connectOptions.setUser("user");
    connectOptions.setPassword(ROOT_PASSWORD);
    Pool pool = Pool.pool(vertx, connectOptions, new PoolOptions().setMaxSize(3));
    
    return Future.succeededFuture()
            
            .compose(v -> pool.preparedQuery("select count(*) from information_schema.tables where table_name='testRefData'").execute())
            .compose(rs -> {
              logger.info("Creating testRefData table");
              int existingTable = rs.iterator().next().getInteger(0);
              if (existingTable == 0) {
                return pool
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
            .compose(rs -> pool.preparedQuery("select count(*) from testRefData").execute())
            .compose(rs -> {
              logger.info("Inserting testRefData");
              int existingRows = rs.iterator().next().getInteger(0);
              Iterator<Map.Entry<UUID, String>> iter = REF_DATA.entrySet().iterator();
              iter = Iterators.limit(iter, REF_ROWS);
              Iterators.advance(iter, existingRows);
              return doRefDataInserts(
                      pool.preparedQuery("insert into testRefData (id, value) values (?, ?)"),
                      iter
              );
            })
            
            .compose(v -> pool.preparedQuery("select count(*) from information_schema.tables where table_name='testData'").execute())
            .compose(rs -> {
              logger.info("Creating testData table");
              int existingTable = rs.iterator().next().getInteger(0);
              if (existingTable == 0) {
                return pool.preparedQuery(CREATE_DATA_TABLE
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
            .compose(rs -> pool.preparedQuery("select count(*) from testData").execute())
            .compose(rs -> {
              logger.info("Inserting testData");
              int existingRows = rs.iterator().next().getInteger(0);
              return doDataInserts(
                      pool.preparedQuery("insert into testData (id, lookup, instant, value) values (?, ?, ?, ?)"),
                       existingRows,
                       DATA_ROWS
              );
            })

            .compose(v -> pool.preparedQuery("select count(*) from information_schema.tables where table_name='testManyData'").execute())
            .compose(rs -> {
              logger.info("Creating testManyData table");
              int existingTable = rs.iterator().next().getInteger(0);
              if (existingTable == 0) {
                return pool.preparedQuery(CREATE_MANY_DATA_TABLE
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
                      pool.preparedQuery(
                              """
                               insert into testManyData (dataId, sort, refId)
                               select d.id, ?, ?
                               from testData d left join testManyData m on d.id = m.dataId and m.refId = ?
                               where id % ? >= ? and m.dataId is null
                               order by id
                              """
                      )
                      , 0
              );
            })            

            .compose(v -> pool.preparedQuery("select count(*) from information_schema.tables where table_name='testFields'").execute())
            .compose(rs -> {
              logger.info("Creating testFields table");
              int existingTable = rs.iterator().next().getInteger(0);
              if (existingTable == 0) {
                return pool.preparedQuery(CREATE_FIELD_DEFN_TABLE).execute();
              } else {
                return Future.succeededFuture();
              }
            })
            .onSuccess(rs -> {
              if (rs != null) {
                logger.info("testFields table created: {}", RowSetHelper.toString(rs));
              }
            })
            .compose(rs -> {
              logger.info("Inserting testFields");
              return doFieldsInserts(
                      pool.preparedQuery(
                              """
                               insert into testFields (fieldId, name, type, valueField)
                               values (?, ?, ?, ?)
                               on duplicate key update
                               fieldId = ?, name = ?, type = ?, valueField = ?
                              """
                      )
                      , false, true
              );
            })            

            .compose(v -> pool.preparedQuery("select count(*) from information_schema.tables where table_name='testFieldValues'").execute())
            .compose(rs -> {
              logger.info("Creating testFieldValues table");
              int existingTable = rs.iterator().next().getInteger(0);
              if (existingTable == 0) {
                return pool.preparedQuery(CREATE_FIELD_DATA_TABLE).execute();
              } else {
                return Future.succeededFuture();
              }
            })
            .onSuccess(rs -> {
              if (rs != null) {
                logger.info("testFieldValues table created: {}", RowSetHelper.toString(rs));
              }
            })
            .compose(rs -> pool.preparedQuery("select count(*) from testFieldValues").execute())
            .compose(rs -> {
              logger.info("Inserting testFieldValues");
              return pool.preparedQuery(
                      """
                        insert into testFieldValues
                          (parentId, fieldId, dateValue, timeValue, dateTimeValue, longValue, doubleValue, boolValue, textValue)
                        select
                          p.id
                          , fieldId
                          , case when f.type = 'Date' then cast(concat('1971-05-', case when (1 + p.id % 30) < 10 then '0' else '' end, (1 + p.id % 30)) as date) end as dateValue
                          , case when f.type = 'Time' then cast(concat('16:', case when (p.id % 60) < 10 then '0' else '' end, (p.id % 60)) as time) end as timeValue
                          , case when f.type = 'DateTime' then cast(concat('1971-05-', case when (1 + p.id % 30) < 10 then '0' else '' end, (1 + p.id % 30), ' ', '16:', case when (p.id % 60) < 10 then '0' else '' end, (p.id % 60)) as datetime) end as dateTimeValue
                          , case when f.type = 'Long' then p.id end as longValue
                          , case when f.type = 'Double' then 1.0 / p.id end as doubleValue
                          , case when f.type = 'Boolean' then p.id % 2 end as boolValue
                          , case when f.type = 'String' then concat('Text', p.id) end as textValue
                        from
                          testData p
                          cross join testFields f
                        where
                          (p.id + f.fieldId) % 3 = 0
                          and not exists (select * from testFieldValues where parentId = p.id and fieldId = f.fieldId)
                      """
              ).execute();
            })
            .compose(v -> pool.preparedQuery("select count(*) from information_schema.tables where table_name='testDynamicEndpoint'").execute())
            .compose(rs -> {
              logger.info("Creating testDynamicEndpoint table");
              int existingTable = rs.iterator().next().getInteger(0);
              if (existingTable == 0) {
                return pool.preparedQuery(CREATE_DYNAMIC_ENDPOINT_TABLE).execute();
              } else {
                return Future.succeededFuture();
              }
            })
            .onSuccess(rs -> {
              if (rs != null) {
                logger.info("testDynamicEndpoint table created: {}", RowSetHelper.toString(rs));
              }
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
  
}
