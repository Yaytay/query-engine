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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.healthchecks.Status;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import liquibase.Scope;
import liquibase.command.CommandScope;
import liquibase.command.core.UpdateCommandStep;
import liquibase.command.core.helpers.ChangeExecListenerCommandStep;
import liquibase.command.core.helpers.DatabaseChangelogCommandStep;
import liquibase.command.core.helpers.DbUrlConnectionArgumentsCommandStep;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.dircache.DirCacheTree;
import uk.co.spudsoft.query.defn.Pipeline;
import uk.co.spudsoft.query.defn.RateLimitRule;
import uk.co.spudsoft.query.defn.RateLimitScopeType;
import static uk.co.spudsoft.query.defn.RateLimitScopeType.host;
import static uk.co.spudsoft.query.defn.RateLimitScopeType.username;
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.main.Persistence;
import uk.co.spudsoft.query.main.Credentials;
import uk.co.spudsoft.query.main.DataSourceConfig;
import uk.co.spudsoft.query.main.ExceptionToString;
import uk.co.spudsoft.query.web.ServiceException;

/**
 * Audit implementation that is based on a database.
 * Tested with MS SQL Server; PostgreSQL and MySQL.
 * @author jtalbut
 */
public class AuditorPersistenceImpl implements Auditor {
  
  private enum OffsetLimitType {
    limitOffset
    , offsetFetch
  }

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(AuditorPersistenceImpl.class);
  private static final String CHANGESET_RESOURCE_PATH = "/db/changelog/query-engine.yaml";

  private static final String PROCESS_ID = ManagementFactory.getRuntimeMXBean().getName();
  private static final Base64.Encoder ENCODER = java.util.Base64.getUrlEncoder();
  private static final Base64.Decoder DECODER = java.util.Base64.getUrlDecoder();
  
  private static final EnumMap<AuditHistorySortOrder, String> SORT_COLUMN_NAMES = prepareSortColumnNames();
  
  private static EnumMap<AuditHistorySortOrder, String> prepareSortColumnNames() {
    EnumMap<AuditHistorySortOrder, String> result = new EnumMap<>(AuditHistorySortOrder.class);
    for (AuditHistorySortOrder value : AuditHistorySortOrder.values()) {
      result.put(value, value.name());
    }
    result.put(AuditHistorySortOrder.responseStreamStart, "responseStreamStartMillis");
    result.put(AuditHistorySortOrder.responseDuration, "responseDurationMillis");
    return result;
  }
  
  private final ObjectMapper mapper = DatabindCodec.mapper();
  
  private final Vertx vertx;
  private final MeterRegistry meterRegistry;
  private final Persistence configuration;
  private String quote;
  private OffsetLimitType offsetLimitType;
  
  private JdbcHelper jdbcHelper;
  
  private boolean prepared;

  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "MeterRegisty is intended to be mutable by any user")
  public AuditorPersistenceImpl(Vertx vertx, MeterRegistry meterRegistry, Persistence audit) {
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

    if (dataSourceConfig == null || Strings.isNullOrEmpty(dataSourceConfig.getUrl())) {
      throw new IllegalStateException("AuditorPersistenceImpl configured without datasource");
    }

    String url = dataSourceConfig.getUrl();
    
    if (Strings.isNullOrEmpty(dataSourceConfig.getSchema())) {
      recordRequest = recordRequest.replaceAll("#SCHEMA#.", "");
      recordFile = recordFile.replaceAll("#SCHEMA#.", "");
      recordException = recordException.replaceAll("#SCHEMA#.", "");
      recordResponse = recordResponse.replaceAll("#SCHEMA#.", "");
      getHistory = getHistory.replaceAll("#SCHEMA#.", "");
      getHistoryCount = getHistoryCount.replaceAll("#SCHEMA#.", "");
    } else {
      recordRequest = recordRequest.replaceAll("SCHEMA", dataSourceConfig.getSchema());
      recordFile = recordFile.replaceAll("SCHEMA", dataSourceConfig.getSchema());
      recordException = recordException.replaceAll("SCHEMA", dataSourceConfig.getSchema());
      recordResponse = recordResponse.replaceAll("SCHEMA", dataSourceConfig.getSchema());
      getHistory = getHistory.replaceAll("SCHEMA", dataSourceConfig.getSchema());
      getHistoryCount = getHistoryCount.replaceAll("SCHEMA", dataSourceConfig.getSchema());
    }

    for (int retry = 0; configuration.getRetryLimit() < 0 || retry <= configuration.getRetryLimit(); ++retry) {
      logger.debug("Running liquibase, attempt {}", retry);        
      String username = dataSourceConfig.getAdminUser() == null ? null : dataSourceConfig.getAdminUser().getUsername();
      String password = dataSourceConfig.getAdminUser() == null ? null : dataSourceConfig.getAdminUser().getPassword();
      try (Connection jdbcConnection = DriverManager.getConnection(url, username, password)) {
        DatabaseMetaData dmd = jdbcConnection.getMetaData();
        quote = dmd.getIdentifierQuoteString();
        recordRequest = recordRequest.replaceAll("#", quote);
        recordFile = recordFile.replaceAll("#", quote);
        recordException = recordException.replaceAll("#", quote);
        recordResponse = recordResponse.replaceAll("#", quote);
        getHistory = getHistory.replaceAll("#", quote);
        getHistoryCount = getHistoryCount.replaceAll("#", quote);
        
        String databaseProduct = dmd.getDatabaseProductName();
        logger.debug("Database product: {}", databaseProduct);
        switch (databaseProduct) {
          case "PostgreSQL":
            this.offsetLimitType = OffsetLimitType.limitOffset;
            break;
          case "MySQL":
            this.offsetLimitType = OffsetLimitType.limitOffset;
            break;
          case "Microsoft SQL Server":
            this.offsetLimitType = OffsetLimitType.offsetFetch;
            break;
          default:
            this.offsetLimitType = OffsetLimitType.limitOffset;
            break;
        }

        Map<String, Object> liquibaseConfig = new HashMap<>();
        Scope.child(liquibaseConfig, () -> {
          Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(jdbcConnection));
          if (!Strings.isNullOrEmpty(dataSourceConfig.getSchema())) {
            logger.trace("Setting default schema to {}", dataSourceConfig.getSchema());
            database.setDefaultSchemaName(dataSourceConfig.getSchema());
          }

          CommandScope updateCommand = new CommandScope(UpdateCommandStep.COMMAND_NAME);
          updateCommand.addArgumentValue(DbUrlConnectionArgumentsCommandStep.DATABASE_ARG, database);
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
          logger.error("Failed to update database: {}", ex);
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
    jdbcHelper = new JdbcHelper(vertx, JdbcHelper.createDataSource(dataSourceConfig, credentials, meterRegistry));
    prepared = true;
  }
  
  public void healthCheck(Promise<Status> promise) {
    if (!prepared) {
      logger.debug("Failing health check because audit is not prepared");
      promise.complete(Status.KO());
    } else {
      promise.complete(Status.OK());
    }
  }

  private String recordRequest = """
                  insert into #SCHEMA#.#request# (
                    #id#
                    , #timestamp#
                    , #processId#
                    , #url#
                    , #clientIp#
                    , #host#
                    , #path#
                    , #arguments#
                    , #headers#
                    , #openIdDetails#
                    , #issuer#
                    , #subject#
                    , #username#
                    , #name#
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
  private String getHistoryCount = """
          select count(*)
          from
            #SCHEMA#.#request#
          where
            #issuer# = ?
            and #subject# = ?
          """;
  private String getHistory = """
          select
             #timestamp#
             , #id#
             , #path#
             , #arguments#
             , #host#
             , #issuer#
             , #subject#
             , #username#
             , #name#
             , #responseCode#
             , #responseRows#
             , #responseSize#
             , #responseStreamStartMillis#
             , #responseDurationMillis#
           from
             #SCHEMA#.#request#
           where
             #issuer# = ?
             and #subject# = ?
           order by
             #ORDERBY# ASCDESC""";
  
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
    JsonArray groups = Auditor.listToJson(context.getGroups());
    String openIdDetails = context.getJwt() == null ? null : context.getJwt().getPayloadAsString();
    
    logger.info("Request: {} {} {} {} {} {} {} {} {} {} {}",
             context.getUrl(),
             context.getClientIp(),
             context.getHost(),
             context.getPath(),
             arguments,
             headers,
             context.getIssuer(),
             context.getSubject(),
             context.getUsername(),
             context.getNameFromJwt(),
             context.getGroups()
    );
    
    return jdbcHelper.runSqlUpdate(recordRequest, ps -> {
                    int param = 1; 
                    ps.setString(param++, JdbcHelper.limitLength(context.getRequestId(), 100));
                    ps.setTimestamp(param++, Timestamp.from(Instant.now()));
                    ps.setString(param++, JdbcHelper.limitLength(PROCESS_ID, 1000));
                    ps.setString(param++, JdbcHelper.limitLength(context.getUrl(), 1000));
                    ps.setString(param++, JdbcHelper.limitLength(context.getClientIp().toNormalizedString(), 40));
                    ps.setString(param++, JdbcHelper.limitLength(context.getHost(), 250));
                    ps.setString(param++, JdbcHelper.limitLength(context.getPath(), 250));
                    ps.setString(param++, JdbcHelper.toString(arguments));
                    ps.setString(param++, JdbcHelper.toString(headers));
                    ps.setString(param++, openIdDetails);
                    ps.setString(param++, JdbcHelper.limitLength(context.getIssuer(), 1000));
                    ps.setString(param++, JdbcHelper.limitLength(context.getSubject(), 1000));
                    ps.setString(param++, JdbcHelper.limitLength(context.getUsername(), 1000));
                    ps.setString(param++, JdbcHelper.limitLength(context.getNameFromJwt(), 1000));
                    ps.setString(param++, JdbcHelper.toString(groups));                    
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
    jdbcHelper.runSqlUpdate(recordFile, ps -> {
             ps.setString(1, JdbcHelper.limitLength(JdbcHelper.toString(file.getPath()), 1000));
             ps.setLong(2, file.getSize());
             if (file.getModified() != null) {
               ps.setTimestamp(3, Timestamp.from(file.getModified().toInstant(ZoneOffset.UTC)));
             } else {
               ps.setTimestamp(3, null);
             }
             ps.setString(4, JdbcHelper.limitLength(context.getRequestId(), 100));
    });
  }

  @Override
  public void recordException(RequestContext context, Throwable ex) {
    logger.info("Exception: {} {}", ex.getClass().getCanonicalName(), ex.getMessage());
    jdbcHelper.runSqlUpdate(recordException, ps -> {
             ps.setTimestamp(1, Timestamp.from(Instant.now()));
             ps.setString(2, JdbcHelper.limitLength(ex.getClass().getCanonicalName(), 1000));
             ps.setString(3, JdbcHelper.limitLength(ex.getMessage(), 1000));
             ps.setString(4, ExceptionToString.convert(ex, "; "));
             ps.setString(5, context.getRequestId());
    });
  }
  
  @Override
  public void recordResponse(RequestContext context, HttpServerResponse response) {
    JsonObject headers = multiMapToJson(context.getHeaders());
    logger.info("Request complete: {} {} bytes in {}s {}"
            , response.getStatusCode()
            , response.bytesWritten()
            , (System.currentTimeMillis() - context.getStartTime()) / 1000.0
            , headers
    );
    jdbcHelper.runSqlUpdate(recordResponse, ps -> {
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
             ps.setString(7, JdbcHelper.toString(headers));
             ps.setString(8, JdbcHelper.limitLength(context.getRequestId(), 100));
    });
  }

  @Override
  public Future<Pipeline> runRateLimitRules(RequestContext context, Pipeline pipeline) {
    List<RateLimitRule> rules = pipeline.getRateLimitRules();

    logger.debug("Performing rate limit check with {} rules", rules.size());
    if (CollectionUtils.isEmpty(rules)) {
      return Future.succeededFuture(pipeline);
    }

    Instant now = LocalDateTime.now().toInstant(ZoneOffset.UTC);

    List<Object> args = new ArrayList<>();
    StringBuilder sql = new StringBuilder();
    for (int i = 0; i < rules.size(); ++i) {
      RateLimitRule rule = rules.get(i);
      if (i > 0) {
        sql.append(" union all ");
      }
      sql.append("select ").append(i).append(" as ").append(quote).append("ruleId").append(quote);
      sql.append(", count(*) - count(").append(quote).append("responseTime").append(quote).append(") as outstanding");
      sql.append(", count(*) as count");
      sql.append(", sum(").append(quote).append("responseSize").append(quote).append(") as bytes");
      sql.append(", current_timestamp ");
      sql.append("from request where timestamp > ? ");
      args.add(Timestamp.from(now.minus(rule.getTimeLimit()))); 
      sql.append("and id <> ? ");
      args.add(context.getRequestId()); 
      for (RateLimitScopeType scope : rule.getScope()) {
        switch (scope) {
          case clientip:
            sql.append("and ").append(quote).append("clientIp").append(quote).append(" = ? ");
            args.add(JdbcHelper.limitLength(context.getClientIp().toNormalizedString(), 40));
            break;
          case host:
            sql.append("and ").append(quote).append("host").append(quote).append(" = ? ");
            args.add(context.getHost());
            break;
          case path:
            sql.append("and ").append(quote).append("path").append(quote).append(" = ? ");
            args.add(context.getPath());
            break;
          case issuer:
            sql.append("and ").append(quote).append("issuer").append(quote).append(" = ? ");
            args.add(context.getIssuer());
            break;
          case subject:
            sql.append("and ").append(quote).append("subject").append(quote).append(" = ? ");
            args.add(context.getSubject());
            break;
          case username:
            sql.append("and ").append(quote).append("username").append(quote).append(" = ? ");
            args.add(context.getUsername());
            break;
          default:
            throw new IllegalStateException("Unknown RateLimitScope type: " + scope);
        }
      }
    }
    boolean[] done = new boolean[rules.size()];
    return jdbcHelper.runSqlSelect(sql.toString(), ps -> {
        for (int i = 0; i < args.size(); ++i) {
          ps.setObject(i + 1, args.get(i));
        }
      }, rs -> {
        while (rs.next()) {
          if (logger.isTraceEnabled()) {
            logger.trace("RateLimitRow: id: {}, outstanding: {}, runs: {}, bytes: {}, time: {}"
                    , rs.getInt(1)
                    , rs.getInt(2)
                    , rs.getInt(3)
                    , rs.getLong(4)
                    , rs.getTimestamp(5).toInstant()
                    );
          }
          int index = rs.getInt(1);
          if (index >= rules.size()) {
            logger.error("Error in rate limit processing: index {} out of bounds", index);
            throw new ServiceException(500, "Internal error");
          }
          if (done[index]) {
            logger.error("Error in rate limit processing: index {} found more than once", index);
            throw new ServiceException(500, "Internal error");
          } else {
            done[index] = true;
          }
          RateLimitRule rule = rules.get(index);
          evaluateRateLimitRule(rule
                  , now
                  , index
                  , rs.getInt(2)
                  , rs.getInt(3)
                  , rs.getLong(4)
                  , rs.getTimestamp(5).toLocalDateTime()
          );
        }
        for (int i = 0; i < done.length; ++i) {
          if (!done[i]) {
            logger.error("Error in rate limit processing: index {} not processed", i);
            throw new ServiceException(500, "Internal error");
          }
        }
        return null;
      }).map(v -> pipeline);
  }
  
  static void evaluateRateLimitRule(RateLimitRule rule, Instant now, int index, int outstanding, int runs, long bytes, LocalDateTime timestamp) throws ServiceException {
    if (outstanding > rule.getConcurrencyLimit()) {
      String logmsg = outstanding > 1 
              ? "RateLimitRule {} failed: Concurrency limit failed. At {} there were {} outstanding runs since {}. Rule: {}"
              : "RateLimitRule {} failed: Concurrency limit failed. At {} there was {} outstanding run since {}. Rule: {}"
              ;
      logger.error(logmsg, index, timestamp, outstanding, now.minus(rule.getTimeLimit()), Json.encode(rule));
      throw new ServiceException(429, "Query already running, please try again later");
    }
    Long runLimit = rule.getParsedRunLimit();
    if (runLimit != null) {
      if (runs > runLimit) {
        String logmsg = runs > 1 
                ? "RateLimitRule {} failed: Run limit failed. At {} there had been {} runs since {}. Rule: {}"
                : "RateLimitRule {} failed: Run limit failed. At {} there had been {} run since {}. Rule: {}"
                ;
        logger.error(logmsg, index, timestamp, outstanding, now.minus(rule.getTimeLimit()), Json.encode(rule));
        throw new ServiceException(429, "Run too many times, please try again later");
      }
    }
    Long byteLimit = rule.getParsedByteLimit();
    if (byteLimit != null) {
      if (bytes > byteLimit) {
        String logmsg = bytes > 1 
                ? "RateLimitRule {} failed: Byte limit failed. At {} there had been {} bytes sent since {}. Rule: {}"
                : "RateLimitRule {} failed: Byte limit failed. At {} there had been {} byte sent since {}. Rule: {}"
                ;
        logger.error(logmsg, index, timestamp, outstanding, now.minus(rule.getTimeLimit()), Json.encode(rule));
        throw new ServiceException(429, "Rate limit exceeded, please try again later");
      }
    }
  }
  
  private static final String AUTH = HttpHeaders.AUTHORIZATION.toString();
  private static final String BEARER = "Bearer ";
  private static final String BASIC = "Basic ";
  
  public static JsonObject multiMapToJson(MultiMap map) {
    if (map == null) {
      return null;
    }
    JsonObject jo = new JsonObject();
    for (Map.Entry<String, String> entry : map.entries()) {
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

  
  
  @Override
  public Future<AuditHistory> getHistory(String issuerArg, String subjectArg, int skipRows, int maxRows, AuditHistorySortOrder sortOrder, boolean sortDescending) {
    logger.info("Get history for : {} {} ({}, {}, {} {})", issuerArg, subjectArg, skipRows, maxRows, sortOrder, sortDescending);
    
    Future<List<AuditHistoryRow>> rowsFuture = getRows(issuerArg, subjectArg, skipRows, maxRows, sortOrder, sortDescending);
    Future<Long> countFuture = countRows(issuerArg, subjectArg);
    
    return Future.all(rowsFuture, countFuture)
            .map(cf -> {
              return new AuditHistory(
                      skipRows
                      , countFuture.result()
                      , rowsFuture.result()
                      );
            });
  }

  private Future<Long> countRows(String issuerArg, String subjectArg) {
    return jdbcHelper.runSqlSelect(getHistoryCount, ps -> {
      int param = 1;
      ps.setString(param++, JdbcHelper.limitLength(issuerArg, 1000));
      ps.setString(param++, JdbcHelper.limitLength(subjectArg, 1000));
    }, rs -> {
      while (rs.next()) {
        return rs.getLong(1);
      }
      return 0L;
    });
  }
  
  
  
  private Future<List<AuditHistoryRow>> getRows(String issuerArg, String subjectArg
          , int skipRows, int maxRows
          , AuditHistorySortOrder sortOrder, boolean sortDescending
  ) {
    
    String sql = getHistory;
    sql = sql.replaceAll("ORDERBY", SORT_COLUMN_NAMES.get(sortOrder));
    sql = sql.replaceAll("ASCDESC", sortDescending ? "desc" : "asc");
    
    switch (offsetLimitType) {
      case limitOffset -> sql = sql + " limit " + maxRows + " offset " + skipRows + " ";
      case offsetFetch -> sql = sql + " offset " + skipRows + " rows fetch next " + maxRows + " rows only ";
      default -> logger.error("No configuration for offset limit type of {}", offsetLimitType);
    }
    
    return jdbcHelper.runSqlSelect(sql, ps -> {
      int param = 1;
      ps.setString(param++, JdbcHelper.limitLength(issuerArg, 1000));
      ps.setString(param++, JdbcHelper.limitLength(subjectArg, 1000));
    }, rs -> {
      ImmutableList.Builder<AuditHistoryRow> builder = ImmutableList.<AuditHistoryRow>builder();
      while (rs.next()) {
        LocalDateTime timestamp = LocalDateTime.ofInstant(rs.getTimestamp(1).toInstant(), ZoneOffset.UTC);
        String id = rs.getString(2);
        String path = rs.getString(3);
        ObjectNode arguments = getArguments(rs, 4, id);
        String host = rs.getString(5);
        String issuer = rs.getString(6);
        String subject = rs.getString(7);
        String username = rs.getString(8);
        String name = rs.getString(9);
        int responseCode = getInteger(rs, 10);
        long responseRows = getLong(rs, 11);
        long responseSize = getLong(rs, 12);
        long responseStreamStartMs = rs.getLong(13);
        long responseDurationMs = rs.getLong(14);
        
        AuditHistoryRow ah = new AuditHistoryRow(timestamp, id, path, arguments, host, issuer, subject, username, name, responseCode, responseRows, responseSize, responseStreamStartMs, responseDurationMs);
        builder.add(ah);
      }
      return builder.build();
    });
  }
  
  private Integer getInteger(ResultSet rs, int colIdx) throws SQLException {
    int value = rs.getInt(colIdx);
    if (rs.wasNull()) {
      return null;
    } else {
      return value;
    }
  }

  private Long getLong(ResultSet rs, int colIdx) throws SQLException {
    long value = rs.getLong(colIdx);
    if (rs.wasNull()) {
      return null;
    } else {
      return value;
    }
  }

  @SuppressFBWarnings("CRLF_INJECTION_LOGS")
  ObjectNode getArguments(ResultSet rs, int idx, String id) throws SQLException {
    ObjectNode arguments = null;
    String value = rs.getString(idx);
    try {
      if (!Strings.isNullOrEmpty(value)) {
        arguments = mapper.readValue(value, ObjectNode.class);
      }
    } catch (Throwable ex) {
      logger.warn("Arguments from database ({}) for id {} failed to parse: ", value.replaceAll("[\r\n]", ""), id, ex);
    }
    return arguments;
  }
  
}
