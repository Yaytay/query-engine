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
import io.vertx.core.Vertx;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.TimeZone;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.main.Credentials;
import uk.co.spudsoft.query.main.DataSourceConfig;

/**
 * Helper class for working with JDBC within Vertx.
 * 
 * @author njt
 */
public class JdbcHelper {
  
  private static final Logger logger = LoggerFactory.getLogger(JdbcHelper.class);
  
  private static final Calendar TZ_CAL = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    
  private final Vertx vertx;
  private final DataSource dataSource;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public JdbcHelper(Vertx vertx, DataSource dataSource) {
    this.vertx = vertx;
    this.dataSource = dataSource;
  }
  
  public static HikariDataSource createDataSource(DataSourceConfig config, @Nullable Credentials credentials, @Nullable MeterRegistry meterRegistry) {
    HikariDataSource ds = new HikariDataSource();
    ds.setJdbcUrl(config.getUrl());
    if (credentials != null) {
      ds.setUsername(Auditor.localizeUsername(credentials.getUsername()));
      ds.setPassword(credentials.getPassword());
    }
    if (config.getSchema() != null) {
      ds.setSchema(config.getSchema());
    }
    ds.setMaximumPoolSize(config.getMaxPoolSize());
    ds.setMinimumIdle(config.getMinPoolSize());
    ds.setIdleTimeout(30000);
    ds.setAutoCommit(true);
    if (meterRegistry != null) {
      ds.setMetricsTrackerFactory(new MicrometerMetricsTrackerFactory(meterRegistry));
    }
    return ds;

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

  public static void setLocalDateTimeUTC(PreparedStatement ps, int index, LocalDateTime utc) throws SQLException {
    if (utc == null) {
      ps.setTimestamp(index, null);
    } else {
      ps.setTimestamp(index, Timestamp.from(utc.toInstant(ZoneOffset.UTC)), TZ_CAL);
    }
  }
  
  public Future<Integer> runSqlUpdate(String sql, SqlConsumer<PreparedStatement> prepareStatement) {
    return vertx.executeBlocking(() -> runSqlUpdateSynchronously(sql, prepareStatement));
  }

  @SuppressFBWarnings(value = "SQL_INJECTION_JDBC", justification = "SQL is generated from static strings")
  public int runSqlUpdateSynchronously(String sql, SqlConsumer<PreparedStatement> prepareStatement) throws Exception {
    logger.trace("Executing update: {}", sql);
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
          return statement.executeUpdate();
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

  public <R> Future<R> runSqlSelect(String sql
          , SqlConsumer<PreparedStatement> prepareStatement
          , SqlFunction<ResultSet, R> resultSetHandler
  ) {
    return vertx.executeBlocking(() -> {
      return runSqlSelectSynchronously(sql, prepareStatement, resultSetHandler);
    });
  }

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
  
  public static String limitLength(String value, int maxLen) {
    if (value == null) {
      return value;
    }
    if (value.length() < maxLen) {
      return value;
    } else {
      return value.substring(0, maxLen - 4) + "...";
    }
  }
  
  public static String toString(Object value) {
    if (value == null) {
      return null;
    }
    return value.toString();
  }
  
  static void closeConnection(Connection conn) {
    try {
      conn.close();
    } catch (Throwable ex) {
      logger.error("Failed to close connection: ", ex);
    }
  }

  static void closeStatement(PreparedStatement statement) {
    try {
      statement.close();
    } catch (Throwable ex) {
      logger.error("Failed to close statement: ", ex);
    }
  }
  
  
}
