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
package uk.co.spudsoft.query.exec;

import com.google.common.base.Strings;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import liquibase.Scope;
import liquibase.command.CommandScope;
import liquibase.command.core.UpdateCommandStep;
import liquibase.command.core.helpers.ChangeExecListenerCommandStep;
import liquibase.command.core.helpers.DatabaseChangelogCommandStep;
import liquibase.command.core.helpers.DbUrlConnectionCommandStep;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.dircache.DirCacheTree;
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.main.Audit;
import uk.co.spudsoft.query.main.Credentials;
import uk.co.spudsoft.query.main.DataSourceConfig;
import uk.co.spudsoft.query.main.ExceptionToString;

/**
 *
 * @author jtalbut
 */
public class AuditorImpl implements Auditor {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(AuditorImpl.class);
  private static final String CHANGESET_RESOURCE_PATH = "/db/changelog/query-engine.yaml";

  
  private static final Base64.Encoder ENCODER = java.util.Base64.getUrlEncoder();
  private static final Base64.Decoder DECODER = java.util.Base64.getUrlDecoder();
  
  private final Vertx vertx;
  private final MeterRegistry meterRegistry;
  private final Audit configuration;
  private DataSource dataSource;

  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "MeterRegisty is intended to be mutable by any user")
  public AuditorImpl(Vertx vertx, MeterRegistry meterRegistry, Audit audit) {
    this.vertx = vertx;
    this.meterRegistry = meterRegistry;
    this.configuration = audit;
  }

  /**
   *
   * @throws IOException if the resource accessor fails.
   * @throws SQLException if the exception thrown by the SQL driver is a non-existent driver exception.
   * @throws LiquibaseException if Liquibase is unable to prepare the database (for a non-driver error).
   *
   */
  @Override
  public void prepare() throws Exception {
    DataSourceConfig dataSourceConfig = configuration.getDataSource();
    SQLException lastSQLException = null;
    LiquibaseException lastLiquibaseException = null;

    if (!Strings.isNullOrEmpty(dataSourceConfig == null ? null : dataSourceConfig.getUrl())) {

      if (Strings.isNullOrEmpty(dataSourceConfig.getSchema())) {
        recordRequest = recordRequest.replaceAll("#SCHEMA#.", "");
        recordFile = recordFile.replaceAll("#SCHEMA#.", "");
        recordException = recordException.replaceAll("#SCHEMA#.", "");
        recordResponse = recordResponse.replaceAll("#SCHEMA#.", "");
      } else {
        recordRequest = recordRequest.replaceAll("SCHEMA", dataSourceConfig.getSchema());
        recordFile = recordFile.replaceAll("SCHEMA", dataSourceConfig.getSchema());
        recordException = recordException.replaceAll("SCHEMA", dataSourceConfig.getSchema());
        recordResponse = recordResponse.replaceAll("SCHEMA", dataSourceConfig.getSchema());
      }

      for (int retry = 0; configuration.getRetryLimit() < 0 || retry <= configuration.getRetryLimit(); ++retry) {
        logger.debug("Running liquibase, attempt {}", retry);        
        String url = dataSourceConfig.getUrl();
        String username = dataSourceConfig.getAdminUser() == null ? null : dataSourceConfig.getAdminUser().getUsername();
        String password = dataSourceConfig.getAdminUser() == null ? null : dataSourceConfig.getAdminUser().getPassword();
        try (Connection jdbcConnection = DriverManager.getConnection(url, username, password)) {
          String quote = jdbcConnection.getMetaData().getIdentifierQuoteString();
          recordRequest = recordRequest.replaceAll("#", quote);
          recordFile = recordFile.replaceAll("#", quote);
          recordException = recordException.replaceAll("#", quote);
          recordResponse = recordResponse.replaceAll("#", quote);
          
          Map<String, Object> liquibaseConfig = new HashMap<>();
          Scope.child(liquibaseConfig, () -> {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(jdbcConnection));
            if (!Strings.isNullOrEmpty(dataSourceConfig.getSchema())) {
              logger.trace("Setting default schema to {}", dataSourceConfig.getSchema());
              database.setDefaultSchemaName(dataSourceConfig.getSchema());
            }
            
            CommandScope updateCommand = new CommandScope(UpdateCommandStep.COMMAND_NAME);
            updateCommand.addArgumentValue(DbUrlConnectionCommandStep.DATABASE_ARG, database);
            updateCommand.addArgumentValue(UpdateCommandStep.CHANGELOG_FILE_ARG, CHANGESET_RESOURCE_PATH);
            updateCommand.addArgumentValue(UpdateCommandStep.CONTEXTS_ARG, null);
            updateCommand.addArgumentValue(UpdateCommandStep.LABEL_FILTER_ARG, null);
            updateCommand.addArgumentValue(ChangeExecListenerCommandStep.CHANGE_EXEC_LISTENER_ARG, null);
            updateCommand.addArgumentValue(DatabaseChangelogCommandStep.CHANGELOG_PARAMETERS, null);
            updateCommand.execute();
          });
          
          logger.info("Database updated");
          lastSQLException = null;
          lastLiquibaseException = null;
          break;
        } catch (Throwable ex) {
          lastLiquibaseException = null;
          lastSQLException = null;
          if (ex instanceof LiquibaseException le) {
            logger.error("Failed to config database: {}", ex.getMessage());
            lastLiquibaseException = le;
          } else if (ex instanceof SQLException se) {
            logger.error("Failed to update database: {}", ex.getMessage());
            lastSQLException = se;
          } else {
            logger.error("Error attempting to update database: {}", ex.getMessage());
          }
          if (baseSqlExceptionIsNonexistantDriver(ex)) {
            throw ex;
          }
        }
        try {
          Thread.sleep(configuration.getRetryBaseMs() + retry * configuration.getRetryIncrementMs());
        } catch (InterruptedException ex) {
          logger.warn("Liquibase retry delay interrupted");
        }
      }
      if (lastSQLException != null) {
        throw lastSQLException;
      }
      if (lastLiquibaseException != null) {
        throw lastLiquibaseException;
      }

      Credentials credentials = dataSourceConfig.getUser();
      if (credentials == null) {
        credentials = dataSourceConfig.getAdminUser();
      }
      dataSource = createDataSource(dataSourceConfig, credentials, meterRegistry);
    }
  }

  private String recordRequest = """
                  insert into #SCHEMA#.#request# (
                    #id#
                    , #timestamp#
                    , #url#
                    , #clientIp#
                    , #host#
                    , #arguments#
                    , #headers#
                    , #issuer#
                    , #subject#
                    , #nameFromJwt#
                    , #groups#
                  ) values (
                    ?
                    , ?
                    , ?
                    , ?
                    , ?
                    , ?
                    , ?
                    , ?
                    , ?
                    , ?
                    , ?
                  )""";
  private String recordFile = """
           update
            #SCHEMA#.#request#
           set
            #filePath# = ?
            , #fileSize# = ?
            , #fileModified# = ?
           where
            #id# = ?""";
  private String recordException = """
           update
            #SCHEMA#.#request#
           set
            #exceptionTime# = ?
            , #exceptionClass# = ?
            , #exceptionMessage# = ?
            , #exceptionStackTrace# = ?
           where
            #id# = ?""";
  private String recordResponse = """
           update
            #SCHEMA#.#request#
           set
            #responseTime# = ?
            , #responseStreamStartMillis# = ?
            , #responseDurationMillis# = ?
            , #responseCode# = ?
            , #responseRows# = ?
            , #responseSize# = ?
            , #responseHeaders# = ?
           where
            #id# = ?""";
  
  private ResourceAccessor getBestResourceAccessor() throws IOException {
    ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor(Thread.currentThread().getContextClassLoader());
    return resourceAccessor;
  }

  private boolean baseSqlExceptionIsNonexistantDriver(Throwable ex) {
    while (ex != null) {
      if (ex instanceof SQLException) {
        SQLException sqlex = (SQLException) ex;
        String message = sqlex.getMessage();
        if (message != null) {
          if (message.startsWith("No suitable driver")) {
            return true;
          }
        }
      }
      ex = ex.getCause();
    }
    return false;
  }

  @Override
  public Future<Void> recordRequest(RequestContext context) {
    
    JsonObject headers = multiMapToJson(context.getHeaders());
    JsonObject arguments = multiMapToJson(context.getArguments());
    JsonArray groups = listToJson(context.getGroups());
    
    logger.info("Request: {} {} {} {} {} {} {} {} {}",
             context.getUrl(),
             context.getClientIp(),
             context.getHost(),
             arguments,
             headers,
             context.getIssuer(),
             context.getSubject(),
             context.getNameFromJwt(),
             context.getGroups()
    );
    
    return runSql(recordRequest, ps -> {
                    ps.setString(1, limitLength(context.getRequestId(), 100));
                    ps.setTimestamp(2, Timestamp.from(Instant.now()));
                    ps.setString(3, limitLength(context.getUrl(), 1000));
                    ps.setString(4, limitLength(context.getClientIp().toNormalizedString(), 40));
                    ps.setString(5, limitLength(context.getHost(), 100));
                    ps.setString(6, toString(arguments));
                    ps.setString(7, toString(headers));
                    ps.setString(8, limitLength(context.getIssuer(), 1000));
                    ps.setString(9, limitLength(context.getSubject(), 1000));
                    ps.setString(10, limitLength(context.getNameFromJwt(), 1000));
                    ps.setString(11, toString(groups));                    
    })
            .recover(ex -> {
              logger.error("Audit record failed: ", ex);
              return Future.succeededFuture();
            })
            .mapEmpty();
  }

  @Override
  public void recordFileDetails(RequestContext context, DirCacheTree.File file) {
    logger.info("File: {} {} {}", file.getPath(), file.getSize(), file.getModified());
    runSql(recordFile, ps -> {
             ps.setString(1, limitLength(toString(file.getPath()), 1000));
             ps.setLong(2, file.getSize());
             if (file.getModified() != null) {
               ps.setTimestamp(3, Timestamp.from(file.getModified().toInstant(ZoneOffset.UTC)));
             } else {
               ps.setTimestamp(3, null);
             }
             ps.setString(4, limitLength(context.getRequestId(), 100));
    });
  }

  @Override
  public void recordException(RequestContext context, Throwable ex) {
    logger.info("Exception: {} {}", ex.getClass().getCanonicalName(), ex.getMessage());

    runSql(recordException, ps -> {
             ps.setTimestamp(1, Timestamp.from(Instant.now()));
             ps.setString(2, limitLength(ex.getClass().getCanonicalName(), 1000));
             ps.setString(3, limitLength(ex.getMessage(), 1000));
             ps.setString(4, ExceptionToString.convert(ex, "; "));
             ps.setString(5, context.getRequestId());
    });
  }
  
  @Override
  public void recordResponse(RequestContext context, HttpServerResponse response) {
    JsonObject headers = multiMapToJson(context.getHeaders());
    
    logger.info("Request complete: {} {} bytes {}", response.getStatusCode(), response.bytesWritten(), headers);
    
    
    runSql(recordResponse, ps -> {
             ps.setTimestamp(1, Timestamp.from(Instant.now()));
             if (context.getHeadersSentTime() > 0) {
               ps.setLong(2, context.getHeadersSentTime() - context.getStartTime());
             } else {
               ps.setNull(2, Types.BIGINT);
             }
             ps.setLong(3, System.currentTimeMillis() - context.getStartTime());
             ps.setInt(4, response.getStatusCode());
             ps.setLong(5, context.getRowsWritten());
             ps.setLong(6, response.bytesWritten());
             ps.setString(7, toString(headers));
             ps.setString(8, limitLength(context.getRequestId(), 100));
    });
  }

  static String localizeUsername(String username) {
    if (username == null) {
      return username;
    }
    String parts[] = username.split("@");
    return parts[0];
  }

  static HikariDataSource createDataSource(DataSourceConfig config, @Nullable Credentials credentials, @Nullable MeterRegistry meterRegistry) {
    HikariDataSource ds = new HikariDataSource();
    ds.setJdbcUrl(config.getUrl());
    if (credentials != null) {
      ds.setUsername(localizeUsername(credentials.getUsername()));
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
  interface SqlConsumer<T> {

    /**
     * Perform this operation on the given argument.
     *
     * @param t the input argument
     * @throws Throwable if something goes wrong
     */
    void accept(T t) throws Throwable;
  }

  private Future<Integer> runSql(String sql, SqlConsumer<PreparedStatement> prepareStatement) {
    return vertx.executeBlocking(promise -> runSqlSynchronously(sql, prepareStatement, promise));
  }

  @SuppressFBWarnings(value = "SQL_INJECTION_JDBC", justification = "SQL is generated from static strings")
  private void runSqlSynchronously(String sql, SqlConsumer<PreparedStatement> prepareStatement, Promise<Integer> promise) {
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
          int result = statement.executeUpdate();
          promise.complete(result);
        } finally {
          closeStatement(statement);
        }
      } finally {
        closeConnection(conn);
      }
    } catch (Throwable ex) {
      logger.error(logMessage, ex);
      promise.fail(ex);
    }
  }
  
  static String limitLength(String value, int maxLen) {
    if (value == null) {
      return value;
    }
    if (value.length() < maxLen) {
      return value;
    } else {
      return value.substring(0, maxLen - 4) + "...";
    }
  }
  
  static String toString(Object value) {
    if (value == null) {
      return null;
    }
    return value.toString();
  }
  
  static JsonArray listToJson(List<String> items) {
    if (items == null) {
      return null;
    }
    return new JsonArray(items);
  }

  private static final String AUTH = HttpHeaders.AUTHORIZATION.toString();
  private static final String BEARER = "Bearer ";
  private static final String BASIC = "Basic ";
  
  static JsonObject multiMapToJson(MultiMap map) {
    if (map == null) {
      return null;
    }
    JsonObject jo = new JsonObject();
    for (Entry<String, String> entry : map.entries()) {
      String key = entry.getKey();
      String value = entry.getValue();
      if (AUTH.equalsIgnoreCase(key)) {
        value = protectAuthHeader(value);
      }
      Object curValue = jo.getValue(key);
      if (curValue == null) {
        jo.put(key, value);
      } else if (curValue instanceof JsonArray array) {
        array.add(value);
      } else if (curValue instanceof String string) {
        JsonArray array = new JsonArray();
        array.add(string);
        array.add(value);
        jo.put(key, array);
      }
    }
    return jo;
  }

  static String protectAuthHeader(String value) {
    if (value.startsWith(BASIC)) {
      try {
        String base64Part = value.substring(BASIC.length());
        String creds = new String(DECODER.decode(base64Part), StandardCharsets.UTF_8);
        int colonPos = creds.indexOf(":");
        if (colonPos > 0) {
          value = BASIC + new String(ENCODER.encode(creds.substring(0, colonPos).getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        }
      } catch (Throwable ex) {
        logger.warn("Failed to protect credentials in Authorization header: ", ex);
      }
    } else if (value.startsWith(BEARER)) {
      try {
        int dotPos = value.lastIndexOf(".");
        if (dotPos > 0) {
          value = value.substring(0, dotPos);
        }
      } catch (Throwable ex) {
        logger.warn("Failed to protect token in Authorization header: ", ex);
      }
    }
    return value;
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
