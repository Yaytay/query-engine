/*
 * Copyright (C) 2025 njt
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
package uk.co.spudsoft.query.exec.sources.sql;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.mssqlclient.MSSQLBuilder;
import io.vertx.mssqlclient.MSSQLConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.PreparedStatement;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;
import io.vertx.sqlclient.Tuple;
import java.io.File;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.main.sample.AbstractSampleDataLoader;
import uk.co.spudsoft.query.main.sample.SampleDataLoaderMsSQL;
import uk.co.spudsoft.query.testcontainers.ServerProviderMsSQL;
import uk.co.spudsoft.query.web.ServiceException;

/**
 *
 * @author njt
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StreamingDataMsForkIT {

  private static final Logger logger = LoggerFactory.getLogger(StreamingDataMsForkIT.class);

  private static final ServerProviderMsSQL mssql = new ServerProviderMsSQL().init();

  @BeforeAll
  public void prep(Vertx vertx, VertxTestContext testContext) {
    String tempDir = "target/temp";
    String basePath = (tempDir.endsWith("/") ? tempDir + "database-locks" : tempDir + File.separator + "database-locks");
    AbstractSampleDataLoader dataLoader = new SampleDataLoaderMsSQL(basePath);
    Future<Void> future = dataLoader.prepareTestDatabase(vertx, mssql.getVertxUrl(), mssql.getUser(), mssql.getPassword());
    future.andThen(ar -> {
      testContext.completeNow();
    });
  }

  SqlConnection connection;
  PreparedStatement preparedStatement;
  Transaction transaction;
  RowStreamWrapper rowStreamWrapper;
  
  SourceNameTracker sourceNameTracker = new SourceNameTracker() {
    @Override
    public void addNameToContextLocalData() {
    }
  };
  
  @Test
  @Timeout(Integer.MAX_VALUE)
  public void test(Vertx vertx, VertxTestContext testContext) {
    logger.debug("Starting");

    MSSQLConnectOptions connectOptions = new MSSQLConnectOptions()
            .setPort(mssql.getPort())
            .setHost("localhost")
            .setDatabase("test")
            .setUser(mssql.getUser())
            .setPassword(mssql.getPassword());

    // Pool options
    PoolOptions poolOptions = new PoolOptions()
            .setMaxSize(5);

    // Create the client pool
    Pool client = MSSQLBuilder.pool()
            .with(poolOptions)
            .connectingTo(connectOptions)
            .build();
    
    String sql = """
                 select
                 	*
                 from
                 	ManyData md1
                 	left join ManyData md2 on md1.dataId = md2.sort
                 	left join ManyData md3 on md2.dataId = md3.sort
                 	left join ManyData md4 on md3.dataId = md4.sort
                 """;
    
    long start = System.currentTimeMillis();

    // A simple query
    client.getConnection()
            .recover(ex -> {
              logger.warn("Failed to connect to data source: ", ex);
              return Future.failedFuture(new ServiceException(500, "Failed to connect to data source", ex));
            })
            .compose(conn -> {
              connection = conn;
              logger.info("Preparing SQL: {}", sql);
              return conn.prepare(sql);
            }).compose(ps -> {
              preparedStatement = ps;
              return connection.begin();
            }).compose(tran -> {
              transaction = tran;
              if (logger.isDebugEnabled()) {
                logger.debug("Executing SQL stream on {}", connection);
              }
              MetadataRowStreamImpl rowStream = new MetadataRowStreamImpl(preparedStatement, vertx.getOrCreateContext(), 100, Tuple.tuple());
              rowStream.exceptionHandler(ex -> {
                logger.error("Exception occured in stream: ", ex);
              });
              rowStreamWrapper = new RowStreamWrapper(sourceNameTracker, connection, transaction, rowStream, null);
              rowStreamWrapper.handler(row -> {
                logger.debug("Row: {}", row);
              });
              return rowStreamWrapper.ready();
            })
            .onFailure(ex -> {
              long end = System.currentTimeMillis();
              logger.warn("SQL source failed (after {}s): ", (end - start) / 1000.0, ex);
              if (connection != null) {
                connection.close();
              }
            })
            .onComplete(ar -> {
              testContext.completeNow();
            });
    
  }

}
