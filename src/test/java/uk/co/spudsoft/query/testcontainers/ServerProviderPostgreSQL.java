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
import io.vertx.mssqlclient.MSSQLConnectOptions;
import io.vertx.pgclient.PgConnectOptions;
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
import uk.co.spudsoft.query.testhelpers.RowSetHelper;

import static uk.co.spudsoft.query.testcontainers.AbstractServerProvider.ROOT_PASSWORD;

/**
 *
 * @author jtalbut
 */
public class ServerProviderPostgreSQL extends AbstractServerProvider implements ServerProvider {

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
      } catch (Throwable ex) {
        p.fail(ex);
      }
    });
  }

  @Override
  public String getUrl() {
    return "postgresql://localhost:" + port + "/test";
  }

  @Override
  public String getUser() {
    return "postgres";
  }

  @Override
  public String getPassword() {
    return AbstractServerProvider.ROOT_PASSWORD;
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
  public PgConnectOptions getConnectOptions() {
    getContainer();
    return new PgConnectOptions()
            .setPort(port)
            .setHost("localhost")
            .setUser(getUser())
            .setPassword(ROOT_PASSWORD)
            .setDatabase("test")
            ;
  }

  @Override
  public Future<Void> prepareTestDatabase(Vertx vertx) {

    SqlConnectOptions connectOptions = getConnectOptions();
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
                               insert into testManyData (dataid, sort, refid)
                               select d.id, $1, $2
                               from testData d left
                                 join testManyData m on d.id = m.dataId and m.refid = $3
                               where id % $4 >= $5 and m.dataId is null
                               order by id
                              """
                      )
                      , 0
              );
            })

            .compose(rs -> pool.preparedQuery("select count(*) from information_schema.tables where table_name='testfields'").execute())
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
            .compose(rs -> pool.preparedQuery("select count(*) from testFields").execute())
            .compose(rs -> {
              logger.info("Inserting testFields");
              return doFieldsInserts(
                      pool.preparedQuery(
                              """
                               insert into testFields (fieldId, name, type, valueField)
                               values ($1, $2, $3, $4)
                               on conflict (fieldId) do
                               update set name = $2, type = $3, valueField = $4
                              """
                      )
                      , true, false
              );
            })
            
            .compose(v -> pool.preparedQuery("select count(*) from information_schema.tables where table_name='testfieldvalues'").execute())
            .compose(rs -> {
              logger.info("Creating testFieldValues table");
              int existingTable = rs.iterator().next().getInteger(0);
              if (existingTable == 0) {
                String query = CREATE_FIELD_DATA_TABLE
                        .replaceAll(" bit ", " boolean ")
                        .replaceAll(" datetime ", " timestamp ")
                        ;
                logger.debug("SQL: {}", query);
                return pool.preparedQuery(query).execute();
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
                            , case when f."type" = 'Date' then cast(concat('1971-05-', case when (1 + p.id % 30) < 10 then '0' else '' end, (1 + p.id % 30)) as date) end as dateValue
                            , case when f."type" = 'Time' then cast(concat('16:', case when (p.id % 60) < 10 then '0' else '' end, (p.id % 60)) as time) end as timeValue
                            , case when f."type" = 'DateTime' then cast(concat('1971-05-', case when (1 + p.id % 30) < 10 then '0' else '' end, (1 + p.id % 30), ' ', '16:', case when (p.id % 60) < 10 then '0' else '' end, (p.id % 60)) as timestamp) end as dateTimeValue
                            , case when f."type" = 'Long' then p.id end as longValue
                            , case when f."type" = 'Double' then 1.0 / p.id end as doubleValue
                            , case when f."type" = 'Boolean' then (p.id % 2 <> 0) end as boolValue
                            , case when f."type" = 'String' then concat('Text', p.id) end as textValue
                          from
                            testData p
                            cross join testFields f
                          where
                            (p.id + f.fieldId) % 3 = 0
                            and not exists (select * from testFieldValues where parentId = p.id and fieldId = f.fieldId)
                        """
              ).execute();
            })
            .compose(v -> pool.preparedQuery("select count(*) from information_schema.tables where table_name='testdynamicendpoint'").execute())
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
    
}
