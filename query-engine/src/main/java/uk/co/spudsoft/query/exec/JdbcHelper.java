/*
 * Copyright (C) 2023 jtalbut
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
package uk.co.spudsoft.query.exec;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.main.Credentials;
import uk.co.spudsoft.query.main.DataSourceConfig;

/**
 * Helper class for working with JDBC within Vertx.
 *
 * @author jtalbut
 */
public class JdbcHelper {

  private static final Logger logger = LoggerFactory.getLogger(JdbcHelper.class);

  private static final Calendar TZ_CAL = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

  private final Vertx vertx;
  private final DataSource dataSource;

  // Shutdown management
  private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
  private final Set<Future<?>> activeFutures = ConcurrentHashMap.newKeySet();
  private volatile Promise<Void> shutdownPromise;

  private static final AtomicInteger INSTANCE_COUNTER = new AtomicInteger();

  /**
   * Constructor.
   * @param vertx The Vert.x instance.
   * @param dataSource The {@link DataSource} to communicate with.
   */
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public JdbcHelper(Vertx vertx, DataSource dataSource) {
    this.vertx = vertx;
    this.dataSource = dataSource;
  }

 /**
   * Create a {@link HikariDataSource} from parameters.
   * @param config the configuration of the {@link DataSource}.
   * @param credentials credentials to use.
   * @param meterRegistry optional {@link MeterRegistry} to use for gathering metrics for the {@link DataSource}.
   * @return a newly created {@link HikariDataSource}.
   */
  public static HikariDataSource createDataSource(DataSourceConfig config, @Nullable Credentials credentials, @Nullable MeterRegistry meterRegistry) {
    HikariDataSource ds = new HikariDataSource();

    // Essential connection properties
    ds.setJdbcUrl(config.getUrl());
    if (credentials != null) {
      ds.setUsername(Auditor.localizeUsername(credentials.getUsername()));
      ds.setPassword(credentials.getPassword());
    }
    if (config.getSchema() != null) {
      ds.setSchema(config.getSchema());
    }

    // Pool sizing configuration
    ds.setMaximumPoolSize(config.getMaxPoolSize());
    if (config.getMinimumIdle() > 0) {
      ds.setMinimumIdle(config.getMinimumIdle());
    }

    // Connection timing configuration
    ds.setConnectionTimeout(config.getConnectionTimeout());
    ds.setIdleTimeout(config.getIdleTimeout());
    ds.setKeepaliveTime(config.getKeepaliveTime());
    ds.setMaxLifetime(config.getMaxLifetime());
    ds.setValidationTimeout(config.getValidationTimeout());
    ds.setInitializationFailTimeout(config.getInitializationFailTimeout());
    ds.setLeakDetectionThreshold(config.getLeakDetectionThreshold());

    // Connection behavior configuration
    ds.setAutoCommit(config.isAutoCommit());
    ds.setReadOnly(config.isReadOnly());

    if (config.getIsolationLevel() != null && !config.getIsolationLevel().trim().isEmpty()) {
      ds.setTransactionIsolation(config.getIsolationLevel());
    }

    if (config.getCatalog() != null && !config.getCatalog().trim().isEmpty()) {
      ds.setCatalog(config.getCatalog());
    }

    // Pool behavior configuration
    ds.setAllowPoolSuspension(config.isAllowPoolSuspension());
    ds.setRegisterMbeans(config.isRegisterMbeans());

    // JDBC driver configuration
    if (config.getDriverClassName() != null && !config.getDriverClassName().trim().isEmpty()) {
      ds.setDriverClassName(config.getDriverClassName());
    }

    if (config.getDataSourceClassName() != null && !config.getDataSourceClassName().trim().isEmpty()) {
      ds.setDataSourceClassName(config.getDataSourceClassName());
    }

    if (config.getConnectionTestQuery() != null && !config.getConnectionTestQuery().trim().isEmpty()) {
      ds.setConnectionTestQuery(config.getConnectionTestQuery());
    }

    if (config.getConnectionInitSql() != null && !config.getConnectionInitSql().trim().isEmpty()) {
      ds.setConnectionInitSql(config.getConnectionInitSql());
    }

    // Pool identification
    if (config.getPoolName() != null && !config.getPoolName().trim().isEmpty()) {
      ds.setPoolName(config.getPoolName());
    } else {
      ds.setPoolName("QueryEngine-" + INSTANCE_COUNTER.incrementAndGet());
    }

    // Metrics configuration
    if (meterRegistry != null) {
      ds.setMetricsTrackerFactory(new MicrometerMetricsTrackerFactory(meterRegistry));
    }

    return ds;
  }  
  
  /**
   * Check if new operations should be blocked.
   * @throws IllegalStateException if shutdown has been initiated.
   */
  private void checkNotShuttingDown() {
    if (shuttingDown.get()) {
      throw new IllegalStateException("JdbcHelper is shutting down - no new operations allowed");
    }
  }

  /**
   * Register a future to track its completion for shutdown purposes.
   * @param future the future to track
   * @return the same future for chaining
   */
  private <T> Future<T> trackFuture(Future<T> future) {
    activeFutures.add(future);
    future.onComplete(ar -> {
      activeFutures.remove(future);
      checkShutdownComplete();
    });
    return future;
  }

  /**
   * Check if shutdown can complete (all operations finished).
   */
  private void checkShutdownComplete() {
    Promise<Void> promise = shutdownPromise;
    if (promise != null && activeFutures.isEmpty()) {
      logger.info("All JDBC operations completed, closing datasource");
      closeDataSource();
      promise.complete();
    }
  }

  /**
   * Actually close the datasource.
   */
  private void closeDataSource() {
    if (dataSource instanceof HikariDataSource hikari) {
      try {
        hikari.close();
        logger.info("HikariDataSource closed successfully");
      } catch (Exception ex) {
        logger.error("Error closing HikariDataSource", ex);
      }
    }
  }

  /**
   * Initiate graceful shutdown.
   * Blocks new operations and waits for existing ones to complete.
   *
   * @return A Future that completes when all operations are finished and the datasource is closed.
   */
  public Future<Void> shutdown() {
    if (shuttingDown.compareAndSet(false, true)) {
      logger.info("Initiating JdbcHelper shutdown, {} active operations", activeFutures.size());

      shutdownPromise = Promise.promise();

      if (activeFutures.isEmpty()) {
        // No active operations, can shutdown immediately
        logger.info("No active operations, closing datasource immediately");
        closeDataSource();
        shutdownPromise.complete();
      } else {
        logger.info("Waiting for {} active operations to complete", activeFutures.size());
        // Operations will trigger checkShutdownComplete() when they finish
      }

      return shutdownPromise.future();
    } else {
      // Already shutting down, return existing promise
      Promise<Void> promise = shutdownPromise;
      return promise != null ? promise.future() : Future.succeededFuture();
    }
  }

  /**
   * Force shutdown with timeout.
   * @param timeoutMs timeout in milliseconds
   * @return Future that completes when shutdown is done or timeout is reached
   */
  public Future<Void> shutdown(long timeoutMs) {
    Future<Void> shutdownFuture = shutdown();

    if (timeoutMs <= 0) {
      return shutdownFuture;
    }

    Promise<Void> timeoutPromise = Promise.promise();

    // Set up timeout
    long timerId = vertx.setTimer(timeoutMs, id -> {
      if (!timeoutPromise.future().isComplete()) {
        logger.warn("Shutdown timeout reached, forcing closure of {} active operations", activeFutures.size());
        closeDataSource();
        timeoutPromise.complete();
      }
    });

    // Complete when either shutdown finishes or timeout occurs
    shutdownFuture.onComplete(ar -> {
      vertx.cancelTimer(timerId);
      if (!timeoutPromise.future().isComplete()) {
        timeoutPromise.handle(ar);
      }
    });

    return timeoutPromise.future();
  }

  /**
   * Run a SQL update asynchronously (in the Vertx worker thread).
   * @param name name of the action being taken for log messages.
   * @param sql statement to be run.
   * @param prepareStatement a {@link SqlConsumer} to use to set parameters on the {@link PreparedStatement}.
   * @return A Future that will be completed when the operation is complete.
   */
  public Future<Integer> runSqlUpdate(String name, String sql, SqlConsumer<PreparedStatement> prepareStatement) {
    checkNotShuttingDown();

    Future<Integer> future = vertx.executeBlocking(() -> runSqlUpdateSynchronously(name, sql, prepareStatement));
    return trackFuture(future);
  }

  /**
   * Run a SQL select asynchronously (in the Vertx worker thread).
   *
   * @param <R> The type of data being returned.
   * @param sql statement to be run.
   * @param prepareStatement a {@link SqlConsumer} to use to set parameters on the {@link PreparedStatement}.
   * @param resultSetHandler a {@link SqlFunction} called once to convert the complete {@link ResultSet} to an object of type R.
   * @return A Future that will be completed when all the results have been processed.
   */
  public <R> Future<R> runSqlSelect(String sql
          , SqlConsumer<PreparedStatement> prepareStatement
          , SqlFunction<ResultSet, R> resultSetHandler
  ) {
    checkNotShuttingDown();

    Future<R> future = vertx.executeBlocking(() -> {
      return runSqlSelectSynchronously(sql, prepareStatement, resultSetHandler);
    });
    return trackFuture(future);
  }

  /**
   * Functional interface defining a consumer that takes in one argument and can throw an exception.
   *
   * @param <T> The type of the argument.
   */
  @FunctionalInterface
  public interface SqlConsumer<T> {

    /**
     * Perform this operation on the given argument.
     *
     * @param t the input argument
     * @throws Throwable if something goes wrong
     */
    void accept(T t) throws Throwable;
  }

  /**
   * Functional interface defining a consumer that takes in one argument and can throw an exception.
   *
   * @param <T> The type of the argument.
   * @param <R> The type returned by the function.
   */
  @FunctionalInterface
  public interface SqlFunction<T, R> {

    /**
     * Perform this operation on the given argument.
     *
     * @param t the input argument
     * @throws Throwable if something goes wrong
     * @return a value of type R
     */
    R accept(T t) throws Throwable;
  }

  /**
   * Set a timestamp parameter on a {@link PreparedStatement} from a {@link LocalDateTime}.
   * @param ps the {@link PreparedStatement} having a parameter set on it.
   * @param index the (one-based) index of the parameter being set.
   * @param utc the {@link LocalDateTime} value being set, which is assumed to be UTC.
   * @throws SQLException if something goes wrong.
   */
  public static void setLocalDateTimeUTC(PreparedStatement ps, int index, LocalDateTime utc) throws SQLException {
    if (utc == null) {
      ps.setTimestamp(index, null);
    } else {
      ps.setTimestamp(index, Timestamp.from(utc.toInstant(ZoneOffset.UTC)), TZ_CAL);
    }
  }

  /**
   * Run commands that use a JDBC connection on the calling thread.
   *
   * @param <R> The return type of the consumer.
   * @param name name of the action being taken for log messages.
   * @param consumer consumer that will do something on the connection.
   * @return A value of type R.
   * @throws Exception if something goes wrong.
   */
  public <R> R runOnConnectionSynchronously(String name, SqlFunction<Connection, R> consumer) throws Exception {
    String logMessage = "Failed to get connection ({}): ";
    try {
      Connection conn = dataSource.getConnection();
      try {
        return consumer.accept(conn);
      } finally {
        closeConnection(conn);
      }
    } catch (Exception ex) {
      logger.error(logMessage, name, ex);
      throw ex;
    } catch (Throwable ex) {
      logger.error(logMessage, name, ex);
      throw new RuntimeException(logMessage.replace("{}", name), ex);
    }
  }

  /**
   * Run a SQL update synchronously.
   * @param name name of the action being taken for log messages.
   * @param sql statement to be run.
   * @param prepareStatement a {@link SqlConsumer} to use to set parameters on the {@link PreparedStatement}.
   * @return the number of rows affected.
   * @throws Exception if anything goes wrong.
   */
  @SuppressFBWarnings(value = "SQL_INJECTION_JDBC", justification = "SQL is generated from static strings")
  public int runSqlUpdateSynchronously(String name, String sql, SqlConsumer<PreparedStatement> prepareStatement) throws Exception {
    logger.trace("Executing update ({}: {}): {}", name, sql);
    String logMessage = "Failed to get connection ({}): ";
    try {
      Connection conn = dataSource.getConnection();
      try {
        logMessage = "Failed to create statement ({}): ";
        PreparedStatement statement = conn.prepareStatement(sql);
        try {
          logMessage = "Failed to prepare statement ({}): ";
          prepareStatement.accept(statement);
          logMessage = "Failed to execute query ({}): ";
          return statement.executeUpdate();
        } finally {
          closeStatement(statement);
        }
      } finally {
        closeConnection(conn);
      }
    } catch (Exception ex) {
      logger.error(logMessage, name, ex);
      throw ex;
    } catch (Throwable ex) {
      logger.error(logMessage, name, ex);
      throw new RuntimeException(logMessage.replace("{}", name), ex);
    }
  }

  /**
   * Run a SQL select synchronously.
   *
   * @param <R> The type of data being returned.
   * @param sql statement to be run.
   * @param prepareStatement a {@link SqlConsumer} to use to set parameters on the {@link PreparedStatement}.
   * @param resultSetHandler a {@link SqlFunction} called once to convert the complete {@link ResultSet} to an object of type R.
   * @return A Future that will be completed when all the results have been processed.
   * @throws Exception if anything goes wrong.
   */
  @SuppressFBWarnings(value = "SQL_INJECTION_JDBC", justification = "SQL is generated from static strings")
  public <R> R runSqlSelectSynchronously(String sql
          , SqlConsumer<PreparedStatement> prepareStatement
          , SqlFunction<ResultSet, R> resultSetHandler
  ) throws Exception {
    logger.trace("Executing select: {}", sql);
    String logMessage = null;
    try {
      logMessage = "Failed to get connection: ";
      Connection conn = dataSource.getConnection();
      try {
        logMessage = "Failed to create statement: ";
        PreparedStatement statement = conn.prepareStatement(sql);
        try {
          logMessage = "Failed to prepare statement: ";
          prepareStatement.accept(statement);

          logMessage = "Failed to execute query: ";
          ResultSet rs = statement.executeQuery();
          try {
            return resultSetHandler.accept(rs);
          } finally {
            rs.close();
          }
        } finally {
          closeStatement(statement);
        }
      } finally {
        closeConnection(conn);
      }
    } catch (Exception ex) {
      logger.error(logMessage, ex);
      throw ex;
    } catch (Throwable ex) {
      logger.error(logMessage, ex);
      throw new RuntimeException(logMessage, ex);
    }
  }

  /**
   * Return the string value truncated to at most maxLen-4 characters with "..." appended.
   * @param value the value to be truncated.
   * @param maxLen the maximum length of the string, must be at least 4.
   * @return the string value truncated to at most maxLen-4 characters with "..." appended.
   */
  public static String limitLength(String value, int maxLen) {
    if (value == null) {
      return value;
    }
    if (maxLen < 0) {
      maxLen = 0;
    }
    if (value.length() <= maxLen) {
      return value;
    } else if (maxLen < 8) {
      return value.substring(0, maxLen);
    } else {
      return value.substring(0, maxLen - 3) + "...";
    }
  }

  /**
   * Convert value to a string, returning null if value is null.
   * @param value the value to convert.
   * @return value as a string (or null).
   */
  public static String toString(Object value) {
    if (value == null) {
      return null;
    }
    return value.toString();
  }

  /**
   * Close a {@link Connection} without throwing an error (just reporting it).
   * @param conn the {@link Connection} to close.
   */
  static void closeConnection(Connection conn) {
    try {
      conn.close();
    } catch (Throwable ex) {
      logger.error("Failed to close connection: ", ex);
    }
  }

  /**
   * Close a {@link PreparedStatement} without throwing an error (just reporting it).
   * @param conn the {@link PreparedStatement} to close.
   */
  static void closeStatement(PreparedStatement statement) {
    try {
      statement.close();
    } catch (Throwable ex) {
      logger.error("Failed to close statement: ", ex);
    }
  }


}
