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

import uk.co.spudsoft.query.exec.context.RequestContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
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
import java.util.Calendar;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
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
import uk.co.spudsoft.query.main.Persistence;
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
  private static final Base64.Encoder ENCODER = java.util.Base64.getEncoder();
  private static final Base64.Decoder DECODER = java.util.Base64.getDecoder();

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
  private final long retryBaseMs;
  private final long retryIncrementMs;
  private String quote;
  private OffsetLimitType offsetLimitType;

  private final JdbcHelper jdbcHelper;

  private boolean prepared;

  /**
   * Constructor.
   * @param vertx The Vert.x instance.
   * @param meterRegistry Meter registry to record metrics.
   * @param audit Configuration.
   * @param jdbcHelper Helper object for making DB calls.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "MeterRegisty is intended to be mutable by any user")
  public AuditorPersistenceImpl(Vertx vertx, MeterRegistry meterRegistry, Persistence audit, JdbcHelper jdbcHelper) {
    this.vertx = vertx;
    this.meterRegistry = meterRegistry;
    this.configuration = audit;
    this.retryBaseMs = audit.getRetryBase() == null ? 1000 : audit.getRetryBase().toMillis();
    this.retryIncrementMs = audit.getRetryIncrement() == null ? 0 : audit.getRetryIncrement().toMillis();
    this.jdbcHelper = jdbcHelper;
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

    Boolean done = Boolean.FALSE;
    for (int retry = 0; !done && (configuration.getRetryLimit() < 0 || retry <= configuration.getRetryLimit()); ++retry) {
      logger.debug("Running liquibase, attempt {}", retry);
      String username = dataSourceConfig.getAdminUser() == null ? null : dataSourceConfig.getAdminUser().getUsername();
      String password = dataSourceConfig.getAdminUser() == null ? null : dataSourceConfig.getAdminUser().getPassword();
      try (Connection jdbcConnection = DriverManager.getConnection(url, username, password)) {
        DatabaseMetaData dmd = jdbcConnection.getMetaData();
        quote = dmd.getIdentifierQuoteString();

        SqlTemplate.initializeAll(dataSourceConfig, quote);

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
        Thread.sleep(retryBaseMs + retry * retryIncrementMs);
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

    prepared = true;
  }

  @Override
  public void healthCheck(Promise<Status> promise) {
    if (!prepared) {
      logger.debug("Failing health check because audit is not prepared");
      promise.complete(Status.KO());
    } else {
      promise.complete(Status.OK());
    }
  }

  private enum SqlTemplate {
    RECORD_REQUEST("""
                  insert into #SCHEMA#.#request# (
                    #id#
                    , #runid#
                    , #timestamp#
                    , #processId#
                    , #url#
                    , #clientIp#
                    , #host#
                    , #path#
                    , #arguments#
                    , #headers#
                    , #openIdDetails#
                    , #audience#
                    , #issuer#
                    , #subject#
                    , #username#
                    , #name#
                    , #groups#
                    , #roles#
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
                    , ?
                    , ?
                    , ?
                  )"""
    ),
    RECORD_TOKEN_DETAILS("""
                  update
                    #SCHEMA#.#request#
                  set
                    #openIdDetails# = ?
                    , #audience# = ?
                    , #issuer# = ?
                    , #subject# = ?
                    , #username# = ?
                    , #name# = ?
                    , #groups# = ?
                    , #roles# = ?
                  where
                    #id# = ?
                  )"""
    ),
    RECORD_FILE("""
           update
            #SCHEMA#.#request#
           set
            #filePath# = ?
            , #fileSize# = ?
            , #fileModified# = ?
            , #fileHash# = ?
           where
            #id# = ?"""
    ),
    RECORD_EXCEPTION("""
                                    update
                                      #SCHEMA#.#request#
                                    set
                                      #exceptionTime# = ?
                                      , #exceptionClass# = ?
                                      , #exceptionMessage# = ?
                                      , #exceptionStackTrace# = ?
                                    where
                                      #id# = ?
                                   """
    ),
    RECORD_RESPONSE("""
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
            #id# = ?"""
    ),
    GET_HISTORY_COUNT("""
          select count(*)
          from
            #SCHEMA#.#request#
          where
            #issuer# = ?
            and #subject# = ?
          """
    ),
    GET_HISTORY("""
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
             #ORDERBY# ASCDESC"""
    ),
    GET_CACHE_FILE("""
          select
            #id#
            , #cacheFile#
            , #cacheExpiry#
          from
            #SCHEMA#.#request#
          where
            #cacheKey# = ?
            and #cacheExpiry# > ?
            and #cacheDeleted# is null
            and #responseDurationMillis# is not null
            and #fileHash# = ?
          order by
            responseTime desc
          """
    ),
    RECORD_CACHE_FILE("""
          update
            #SCHEMA#.#request#
          set
            #cacheKey# = ?
            , #cacheFile# = ?
            , #cacheExpiry# = ?
            , #cacheDeleted# = null
          where
            #id# = ?
          """
    ),
    RECORD_CACHE_FILE_USED("""
          update
            #SCHEMA#.#request#
          set
            #cacheFile# = ?
          where
            #id# = ?
          """
    ),
    DELETE_CACHE_FILE("""
          update
            #SCHEMA#.#request#
          set
            #cacheDeleted# = ?
          where
            #id# = ?
          """
    ),
    COUNT_OUTSTANDING_REQUESTS("""
          select
            count(*)
          from
            #SCHEMA#.#request#
          where
            #responseTime# is null
            and #timestamp# > ?
          """
    ),
    MARK_RATE_LIMIT_RULES_PROCESSED("""
          update
            #SCHEMA#.#request#
          set
            #rulesProcessed# = ?
          where
            #id# = ?
          """
    );

    private final String template;
    private String sql;

    SqlTemplate(String template) {
        this.template = template;
    }

    public String sql() {
        return sql;
    }

    public static void initializeAll(DataSourceConfig dataSourceConfig, String quote) {
        String schemaPrefix = Strings.isNullOrEmpty(dataSourceConfig.getSchema())
            ? ""
            : dataSourceConfig.getSchema() + ".";

        for (SqlTemplate template : SqlTemplate.values()) {
            String processed = template.template.replaceAll("#SCHEMA#\\.", schemaPrefix);
            template.sql = processed.replaceAll("#", quote);
        }
    }
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

  private static void hashNullableString(Hasher hasher, String value) {
    if (value != null) {
      hasher.putString(value, StandardCharsets.UTF_8);
    }
  }

  /**
   * The cache key is built of:
   * <UL>
   * <LI>The full request URL.
   * <LI>Headers:
   * <UL>
   * <LI>Accept
   * <LI>Accept-Encoding
   * </UL>
   * <LI>Token fields:
   * <UL>
   * <LI>aud
   * <LI>iss
   * <LI>sub
   * <LI>groups
   * <LI>roles
   * </UL>
   * </UL>
   *
   * Note that the fileHash must also match, but isn't built into the key (should usually match because of the use of the inclusion of full URL).
   *
   * @param context The request context.
   * @return
   */
  static String buildCacheKey(RequestContext requestContext) {

    Hasher sha = Hashing.sha256().newHasher();
    hashNullableString(sha, requestContext.getUrl());
    hashNullableString(sha, requestContext.getHeaders().get("Accept"));
    hashNullableString(sha, requestContext.getHeaders().get("Accept-Encoding"));
    hashNullableString(sha, Objects.toString(requestContext.getAudience()));
    hashNullableString(sha, requestContext.getIssuer());
    hashNullableString(sha, requestContext.getSubject());
    hashNullableString(sha, Objects.toString(requestContext.getGroups()));
    hashNullableString(sha, Objects.toString(requestContext.getRoles()));
    return sha.hash().toString();

  }

  @Override
  public Future<CacheDetails> getCacheFile(RequestContext requestContext, Pipeline pipeline) {

    String cacheKey = buildCacheKey(requestContext);

    return jdbcHelper.runSqlSelect(SqlTemplate.GET_CACHE_FILE.sql(), ps -> {
        ps.setString(1, cacheKey);
        JdbcHelper.setLocalDateTimeUTC(ps, 2, LocalDateTime.now(ZoneOffset.UTC));
        ps.setString(3, pipeline.getSha256());
      }, rs -> {
        while (rs.next()) {
          return new CacheDetails(rs.getString(1), rs.getString(2), rs.getTimestamp(3).toLocalDateTime());
        }
        return null;
      });
  }

  @Override
  public Future<Void> recordCacheFile(RequestContext requestContext, String fileName, LocalDateTime expiry) {
    return jdbcHelper.runSqlUpdate("recordCacheFile", SqlTemplate.RECORD_CACHE_FILE.sql(), ps -> {
                    int param = 1;
                    ps.setString(param++, buildCacheKey(requestContext));
                    ps.setString(param++, fileName);
                    JdbcHelper.setLocalDateTimeUTC(ps, param++, expiry);
                    ps.setString(param++, requestContext.getRequestId());
    }).mapEmpty();
  }

  @Override
  public Future<Void> recordCacheFileUsed(RequestContext requestContext, String fileName) {
    return jdbcHelper.runSqlUpdate("recordCacheFileUsed", SqlTemplate.RECORD_CACHE_FILE_USED.sql(), ps -> {
                    int param = 1;
                    ps.setString(param++, fileName);
                    ps.setString(param++, requestContext.getRequestId());
    }).mapEmpty();
  }

  @Override
  public Future<Void> deleteCacheFile(String auditId) {
    return jdbcHelper.runSqlUpdate("deleteCacheFile", SqlTemplate.DELETE_CACHE_FILE.sql(), ps -> {
                    int param = 1;
                    ps.setString(param++, auditId);
                    JdbcHelper.setLocalDateTimeUTC(ps, param++, LocalDateTime.now(ZoneOffset.UTC));
    })
            .recover(ex -> {
              logger.error("Failed to mark cache file deleted for {}: ", auditId, ex);
              return Future.succeededFuture();
            })
            .mapEmpty();
  }

  @Override
  public Future<Void> recordRequest(RequestContext requestContext) {

    JsonObject headers = multiMapToJson(requestContext.getHeaders());
    JsonObject arguments = multiMapToJson(requestContext.getParams());

    logger.info("Request: {} {} {} {} {} {}",
             requestContext.getUrl(),
             requestContext.getClientIp(),
             requestContext.getHost(),
             requestContext.getPath(),
             arguments,
             headers
    );

    JsonArray audience = Auditor.listToJson(requestContext.getAudience());
    JsonArray groups = Auditor.listToJson(requestContext.getGroups());
    JsonArray roles = Auditor.listToJson(requestContext.getRoles());
    String openIdDetails = requestContext.getJwt() == null ? null : requestContext.getJwt().getPayloadAsString();

    return jdbcHelper.runSqlUpdate("recordRequest", SqlTemplate.RECORD_REQUEST.sql(), ps -> {
                    int param = 1;
                    ps.setString(param++, JdbcHelper.limitLength(requestContext.getRequestId(), 100));
                    ps.setString(param++, JdbcHelper.limitLength(requestContext.getRunID(), 100));
                    JdbcHelper.setLocalDateTimeUTC(ps, param++, LocalDateTime.now(ZoneOffset.UTC));
                    ps.setString(param++, JdbcHelper.limitLength(PROCESS_ID, 1000));
                    ps.setString(param++, JdbcHelper.limitLength(requestContext.getUrl(), 1000));
                    ps.setString(param++, JdbcHelper.limitLength(requestContext.getClientIp().toNormalizedString(), 40));
                    ps.setString(param++, JdbcHelper.limitLength(requestContext.getHost(), 250));
                    ps.setString(param++, JdbcHelper.limitLength(requestContext.getPath(), 250));
                    ps.setString(param++, JdbcHelper.toString(arguments));
                    ps.setString(param++, JdbcHelper.toString(headers));

                    ps.setString(param++, openIdDetails);
                    ps.setString(param++, JdbcHelper.toString(audience));
                    ps.setString(param++, JdbcHelper.limitLength(requestContext.getIssuer(), 1000));
                    ps.setString(param++, JdbcHelper.limitLength(requestContext.getSubject(), 1000));
                    ps.setString(param++, JdbcHelper.limitLength(requestContext.getUsername(), 1000));
                    ps.setString(param++, JdbcHelper.limitLength(requestContext.getName(), 1000));
                    ps.setString(param++, JdbcHelper.toString(groups));
                    ps.setString(param++, JdbcHelper.toString(roles));

    }).mapEmpty();
  }

  @Override
  public Future<Void> recordFileDetails(RequestContext requestContext, DirCacheTree.File file, Pipeline pipeline) {
    logger.info("File: {} {} {}", file.getPath(), file.getSize(), file.getModified());
    return jdbcHelper.runSqlUpdate("recordFile", SqlTemplate.RECORD_FILE.sql(), ps -> {
             ps.setString(1, JdbcHelper.limitLength(JdbcHelper.toString(file.getPath()), 1000));
             ps.setLong(2, file.getSize());
             JdbcHelper.setLocalDateTimeUTC(ps, 3, file.getModified());
             ps.setString(4, pipeline == null ? null : pipeline.getSha256());
             ps.setString(5, JdbcHelper.limitLength(requestContext.getRequestId(), 100));
    }).mapEmpty();
  }

  @Override
  public void recordException(RequestContext requestContext, Throwable ex) {
    logger.info("Exception: {} {}", ex.getClass().getCanonicalName(), ex.getMessage());
    jdbcHelper.runSqlUpdate("recordException", SqlTemplate.RECORD_EXCEPTION.sql(), ps -> {
             JdbcHelper.setLocalDateTimeUTC(ps, 1, LocalDateTime.now(ZoneOffset.UTC));
             ps.setString(2, JdbcHelper.limitLength(ex.getClass().getCanonicalName(), 1000));
             ps.setString(3, JdbcHelper.limitLength(ex.getMessage(), 1000));
             ps.setString(4, ExceptionToString.convert(ex, "; "));
             ps.setString(5, requestContext.getRequestId());
    });
  }

  @Override
  public void recordResponse(RequestContext requestContext, HttpServerResponse response) {
    JsonObject headers = multiMapToJson(response.headers());
    logger.info("Request complete: {} {} bytes in {}s {}"
            , response.getStatusCode()
            , response.bytesWritten()
            , (System.currentTimeMillis() - requestContext.getStartTime()) / 1000.0
            , headers
    );
    jdbcHelper.runSqlUpdate("recordResponse", SqlTemplate.RECORD_RESPONSE.sql(), ps -> {
             JdbcHelper.setLocalDateTimeUTC(ps, 1, LocalDateTime.now(ZoneOffset.UTC));
             if (requestContext.getHeadersSentTime() > 0) {
               ps.setLong(2, requestContext.getHeadersSentTime() - requestContext.getStartTime());
             } else {
               ps.setNull(2, Types.BIGINT);
             }
             ps.setLong(3, System.currentTimeMillis() - requestContext.getStartTime());
             ps.setInt(4, response.getStatusCode());
             ps.setLong(5, requestContext.getRowsWritten());
             ps.setLong(6, response.bytesWritten());
             ps.setString(7, JdbcHelper.toString(headers));
             ps.setString(8, JdbcHelper.limitLength(requestContext.getRequestId(), 100));
    });
  }

  @Override
  public Future<Pipeline> runRateLimitRules(RequestContext requestContext, Pipeline pipeline) {
     List<RateLimitRule> rules = pipeline.getRateLimitRules();

    logger.debug("Performing rate limit check with {} rules", rules.size());
    Instant now = LocalDateTime.now(ZoneOffset.UTC).toInstant(ZoneOffset.UTC);
    if (CollectionUtils.isEmpty(rules)) {
      return jdbcHelper.runSqlUpdate("markRateLimitRulesProcessed", SqlTemplate.MARK_RATE_LIMIT_RULES_PROCESSED.sql(), ps -> {
        Timestamp ts = Timestamp.from(now);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        ps.setTimestamp(1, ts, cal);
        ps.setString(2, requestContext.getRequestId());
      }).map(v -> pipeline);
    }

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
      args.add(LocalDateTime.ofInstant(now.minus(rule.getTimeLimit()), ZoneOffset.UTC));
      sql.append("and ").append(quote).append("rulesProcessed").append(quote).append(" is not null ");
      for (RateLimitScopeType scope : rule.getScope()) {
        switch (scope) {
          case clientip:
            sql.append("and ").append(quote).append("clientIp").append(quote).append(" = ? ");
            args.add(JdbcHelper.limitLength(requestContext.getClientIp().toNormalizedString(), 40));
            break;
          case host:
            sql.append("and ").append(quote).append("host").append(quote).append(" = ? ");
            args.add(requestContext.getHost());
            break;
          case path:
            sql.append("and ").append(quote).append("path").append(quote).append(" = ? ");
            args.add(requestContext.getPath());
            break;
          case issuer:
            sql.append("and ").append(quote).append("issuer").append(quote).append(" = ? ");
            args.add(requestContext.getIssuer());
            break;
          case subject:
            sql.append("and ").append(quote).append("subject").append(quote).append(" = ? ");
            args.add(requestContext.getSubject());
            break;
          case username:
            sql.append("and ").append(quote).append("username").append(quote).append(" = ? ");
            args.add(requestContext.getUsername());
            break;
          default:
            throw new IllegalStateException("Unknown RateLimitScope type: " + scope);
        }
      }
    }

    String sqlString = sql.toString();
    logger.info("Running rate limit rules {} with {}", sqlString, args);

    return jdbcHelper.runInTransaction("rateLimitRules", JdbcHelper.IsolationLevel.TRANSACTION_SERIALIZABLE, conn -> {
      runRateLimitRulesSqlInTransaction(conn, sqlString, args, rules, requestContext.getRequestId(), now);
      return null;
    }).map(v -> pipeline);
  }

  @SuppressFBWarnings(value = "SQL_INJECTION_JDBC", justification = "SQL is generated from known strings")
  private void runRateLimitRulesSqlInTransaction(Connection conn, String sql, List<Object> args, List<RateLimitRule> rules, String requestId, Instant now) throws Throwable {
    jdbcHelper.runSqlSelectOnConnectionSynchronously(conn,
            sql,
            ps -> {
              for (int i = 0; i < args.size(); ++i) {
                ps.setObject(i + 1, args.get(i));
              }
            }, rs -> {
              boolean[] done = new boolean[rules.size()];
              while (rs.next()) {
                if (logger.isTraceEnabled()) {
                  logger.trace("RateLimitRow: id: {}, outstanding: {}, runs: {}, bytes: {}, time: {}",
                          rs.getInt(1),
                          rs.getInt(2),
                          rs.getInt(3),
                          rs.getLong(4),
                          rs.getTimestamp(5).toInstant()
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
                evaluateRateLimitRule(rule,
                        now,
                        index,
                        rs.getInt(2),
                        rs.getInt(3),
                        rs.getLong(4),
                        rs.getTimestamp(5).toLocalDateTime()
                );
              }
              for (int i = 0; i < done.length; ++i) {
                if (!done[i]) {
                  logger.error("Error in rate limit processing: index {} not processed", i);
                  throw new ServiceException(500, "Internal error");
                }
              }
              return null;
            });
    int rowsAffected = jdbcHelper.runSqlUpdateOnConnectionSynchronously(conn, SqlTemplate.MARK_RATE_LIMIT_RULES_PROCESSED.sql(), ps -> {
      Timestamp ts = Timestamp.from(now);
      Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
      ps.setTimestamp(1, ts, cal);
      ps.setString(2, requestId);
    });
    logger.debug("Marking rate limit rules processed affected {} rows", rowsAffected);
 }

  static void evaluateRateLimitRule(RateLimitRule rule, Instant now, int index, int outstanding, int runs, long bytes, LocalDateTime timestamp) throws ServiceException {
    if (outstanding > rule.getConcurrencyLimit()) {
      String logmsg = outstanding > 1
              ? "RateLimitRule {} failed: Concurrency limit failed. At {} there were {} outstanding runs since {}. Rule: {}"
              : "RateLimitRule {} failed: Concurrency limit failed. At {} there was {} outstanding run since {}. Rule: {}"
              ;
      logger.error(logmsg, index, timestamp, outstanding, now.minus(rule.getTimeLimit()), rule);
      throw new ServiceException(429, "Query already running, please try again later");
    }
    Long runLimit = rule.getParsedRunLimit();
    if (runLimit != null) {
      if (runs > runLimit) {
        String logmsg = runs > 1
                ? "RateLimitRule {} failed: Run limit failed. At {} there had been {} runs since {}. Rule: {}"
                : "RateLimitRule {} failed: Run limit failed. At {} there had been {} run since {}. Rule: {}"
                ;
        logger.error(logmsg, index, timestamp, outstanding, now.minus(rule.getTimeLimit()), rule);
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
        logger.error(logmsg, index, timestamp, outstanding, now.minus(rule.getTimeLimit()), rule);
        throw new ServiceException(429, "Rate limit exceeded, please try again later");
      }
    }
  }

  private static final String AUTH = HttpHeaders.AUTHORIZATION.toString();
  private static final String BEARER = "Bearer ";
  private static final String BASIC = "Basic ";

  /**
   * Convert a Vert.x MultiMap to JSON.
   * <p>
   * Individual values are written directly, multiple values are written as a JsonArray.
   * @param map The input MultiMap.
   * @return A JsonObject.
   */
  public static JsonObject multiMapToJson(MultiMap map) {
    if (map == null) {
      return null;
    }
    JsonObject jo = new JsonObject();
    for (String key : map.names()) {
      List<String> values = map.getAll(key);
      if (values != null && !values.isEmpty()) {
        if (AUTH.equalsIgnoreCase(key)) {
          values = values.stream().map(v -> protectAuthHeader(v)).collect(Collectors.toList());
        }
        if (values.size() == 1) {
          jo.put(key, values.get(0));
        } else {
          JsonArray array = new JsonArray();
          for (String value : values) {
            array.add(value);
          }
          jo.put(key, array);
        }
      }
    }
    return jo;
  }

  static String protectAuthHeader(String value) {
    if  (value == null) {
      return null;
    }
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
    return jdbcHelper.runSqlSelect(SqlTemplate.GET_HISTORY_COUNT.sql(), ps -> {
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

    String sql = SqlTemplate.GET_HISTORY.sql();
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
        Integer responseCode = getInteger(rs, 10);
        Long responseRows = getLong(rs, 11);
        Long responseSize = getLong(rs, 12);
        Long responseStreamStartMs = rs.getLong(13);
        Long responseDurationMs = rs.getLong(14);

        AuditHistoryRow ah = new AuditHistoryRow(timestamp, id, path, arguments, host, issuer, subject, username, name, responseCode, responseRows, responseSize, responseStreamStartMs, responseDurationMs);
        builder.add(ah);
      }
      return builder.build();
    });
  }

  static Integer getInteger(ResultSet rs, int colIdx) throws SQLException {
    int value = rs.getInt(colIdx);
    if (rs.wasNull()) {
      return null;
    } else {
      return value;
    }
  }

  static Long getLong(ResultSet rs, int colIdx) throws SQLException {
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
    if (rs.wasNull()) {
      return null;
    }
    try {
      if (!Strings.isNullOrEmpty(value)) {
        arguments = mapper.readValue(value, ObjectNode.class);
      }
    } catch (Throwable ex) {
      logger.warn("Arguments from database ({}) for id {} failed to parse: ", value.replaceAll("[\r\n]", ""), id, ex);
    }
    return arguments;
  }

  @Override
  public Future<Void> waitForOutstandingRequests(long timeoutMs) {
    Promise<Void> promise = Promise.promise();

    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    Timestamp relevanceThreshold = Timestamp.from(now.minusDays(1).toInstant(ZoneOffset.UTC));
    waitForOutstandingRequests(promise, relevanceThreshold, System.currentTimeMillis() + timeoutMs);
    return promise.future();
  }

  private void waitForOutstandingRequests(Promise<Void> promise, Timestamp relevanceThreshold, long terminalTime) {

    jdbcHelper.runSqlSelect(SqlTemplate.COUNT_OUTSTANDING_REQUESTS.sql(), ps -> {
      Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
      ps.setTimestamp(1, relevanceThreshold, cal);
    }, rs -> {
      while (rs.next()) {
        return rs.getLong(1);
      }
      return null;
    })
            .onComplete(ar -> {
              if (ar.failed()) {
                promise.fail(ar.cause());
              } else {
                Long count = ar.result();
                if (count == null) {
                  promise.fail(new IllegalStateException("Outstanding requests query returned null"));
                } else if (count == 0) {
                  promise.complete();
                } else if (System.currentTimeMillis() > terminalTime) {
                  promise.fail(new TimeoutException("Outstanding requests (" + count + ") remain after timeout"));
                } else {
                  logger.info("There are {} outstanding requests to be resolved", count);
                  vertx.setTimer(500, l -> {
                    waitForOutstandingRequests(promise, relevanceThreshold, terminalTime);
                  });
                }
              }
            });
  }

}
