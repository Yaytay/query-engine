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
package uk.co.spudsoft.query.web;

import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import liquibase.exception.LiquibaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.JdbcHelper;
import uk.co.spudsoft.query.main.Credentials;
import uk.co.spudsoft.query.main.DataSourceConfig;
import uk.co.spudsoft.query.main.Persistence;

/**
 * Implementation of {@link LoginDao} that stores data in a database.
 *
 * @author jtalbut
 */
public class LoginDaoPersistenceImpl implements LoginDao {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(LoginDaoPersistenceImpl.class);

  private final Vertx vertx;
  private final MeterRegistry meterRegistry;
  private final Persistence configuration;
  private final Duration purgeDelay;
  private DataSource dataSource;
  private String quote;
  
  private final AtomicBoolean prepared = new AtomicBoolean(false);
  
  private JdbcHelper jdbcHelper;
  
  private final AtomicLong lastPurge = new AtomicLong();
  
  private record TimestampedToken(LocalDateTime expiry, String token){};
  
  private static final int MAX_TOKEN_CACHE_SIZE = 1000;
  
  private final Map<String, TimestampedToken> tokenCache = new HashMap<>();
  
  /**
   * Constructor.
   * @param vertx the Vert.x instance.
   * @param meterRegistry {@link MeterRegistry} for reporting metrics.
   * @param configuration database configuration.
   * @param purgeDelay purge delay for expired tokens/sessions.  A scheduled job will be set up with this period to clean up expired data.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "MeterRegisty is intended to be mutable by any user")
  public LoginDaoPersistenceImpl(Vertx vertx, MeterRegistry meterRegistry, Persistence configuration, Duration purgeDelay) {
    this.vertx = vertx;
    this.meterRegistry = meterRegistry;
    this.configuration = configuration;
    this.purgeDelay = purgeDelay;
  }

  private String recordLogin = """
                  insert into #SCHEMA#.#login# (
                    #state#
                    , #provider#
                    , #timestamp#
                    , #code_verifier#
                    , #nonce#
                    , #redirect_uri#
                    , #target_url#
                  ) values (
                    ?
                    , ?
                    , ?
                    , ?
                    , ?
                    , ?
                    , ?
                  )""";
  
  private String markUsed = """
                  update
                    #SCHEMA#.#login#
                  set
                    #completed# = ?
                  where
                    #state# = ?
                  """;
  
  private String purgeLogins = """
                  delete from
                    #SCHEMA#.#login#
                  where
                    #timestamp# < ?
                  """;
  
  private String getData = """
                  select
                     #provider#
                     , #code_verifier#
                     , #nonce#
                     , #redirect_uri#
                     , #target_url#
                  from
                    #SCHEMA#.#login#
                  where
                    #state# = ?
                    and #completed# is null
                  """;
  
  private String storeToken = """
                  insert into #SCHEMA#.#session# (
                    #id#
                    , #expiry#
                    , #token#
                  ) values (
                    ?
                    , ?
                    , ?
                  )
                  """;
  
  private String getToken = """
                  select
                    #expiry#
                    , #token#
                  from
                    #SCHEMA#.#session#
                  where
                    #id# = ?
                  """;
  
  private String deleteToken = """
                  delete from
                    #SCHEMA#.#session#
                  where
                    #id# = ?
                  """;
  
  private String expireTokens = """
                  delete from
                    #SCHEMA#.#session#
                  where
                    #expiry# < ?
                  """;
    
  /**
   *
   * @throws IOException if the resource accessor fails.
   * @throws SQLException if the exception thrown by the SQL driver is a non-existent driver exception.
   * @throws LiquibaseException if Liquibase is unable to prepare the database (for a non-driver error).
   *
   */
  @Override
  @SuppressFBWarnings(value = "JLM_JSR166_UTILCONCURRENT_MONITORENTER", justification = "The prepared field is final and only accessed in this method, within those constraints this usage is fine")
  public void prepare() throws Exception {
    
    synchronized (prepared) {
      if (prepared.get()) {
        throw new IllegalStateException("Already prepared");
      }

      DataSourceConfig dataSourceConfig = configuration.getDataSource();

      if (dataSourceConfig == null || Strings.isNullOrEmpty(dataSourceConfig.getUrl())) {
        throw new IllegalStateException("No persistence URL provided, should not have reached here");
      }

      if (Strings.isNullOrEmpty(dataSourceConfig.getSchema())) {
        recordLogin = recordLogin.replaceAll("#SCHEMA#.", "");
        markUsed = markUsed.replaceAll("#SCHEMA#.", "");
        purgeLogins = purgeLogins.replaceAll("#SCHEMA#.", "");
        getData = getData.replaceAll("#SCHEMA#.", "");
        storeToken = storeToken.replaceAll("#SCHEMA#.", "");
        getToken = getToken.replaceAll("#SCHEMA#.", "");
        deleteToken = deleteToken.replaceAll("#SCHEMA#.", "");
        expireTokens = expireTokens.replaceAll("#SCHEMA#.", "");
      } else {
        recordLogin = recordLogin.replaceAll("#SCHEMA#", dataSourceConfig.getSchema());
        markUsed = markUsed.replaceAll("#SCHEMA#", dataSourceConfig.getSchema());
        purgeLogins = purgeLogins.replaceAll("#SCHEMA#", dataSourceConfig.getSchema());
        getData = getData.replaceAll("#SCHEMA#", dataSourceConfig.getSchema());
        storeToken = storeToken.replaceAll("#SCHEMA#", dataSourceConfig.getSchema());
        getToken = getToken.replaceAll("#SCHEMA#", dataSourceConfig.getSchema());
        deleteToken = deleteToken.replaceAll("#SCHEMA#", dataSourceConfig.getSchema());
        expireTokens = expireTokens.replaceAll("#SCHEMA#", dataSourceConfig.getSchema());
      }

      Credentials credentials = dataSourceConfig.getUser();
      if (credentials == null) {
        credentials = dataSourceConfig.getAdminUser();
      }
      dataSource = JdbcHelper.createDataSource(dataSourceConfig, credentials, meterRegistry);
      jdbcHelper = new JdbcHelper(vertx, dataSource);
      try (Connection connection = dataSource.getConnection()) {
        quote = connection.getMetaData().getIdentifierQuoteString();
        recordLogin = recordLogin.replaceAll("#", quote);
        markUsed = markUsed.replaceAll("#", quote);
        purgeLogins = purgeLogins.replaceAll("#", quote);
        getData = getData.replaceAll("#", quote);
        storeToken = storeToken.replaceAll("#", quote);
        getToken = getToken.replaceAll("#", quote);
        deleteToken = deleteToken.replaceAll("#", quote);
        expireTokens = expireTokens.replaceAll("#", quote);
      }
      
      timezoneHandlingTest();
      
      prepared.set(true);
    }
    
    vertx.setPeriodic(purgeDelay.toMillis(), id -> {
      logger.debug("Scheduled purge of token cache at {}", purgeDelay);
      int cacheSize;
      synchronized (tokenCache) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        tokenCache.entrySet().removeIf(entry -> entry.getValue().expiry.isBefore(now));
        cacheSize = tokenCache.size();        
        jdbcHelper.runSqlUpdate("purgeLogins", purgeLogins, ps -> {
          JdbcHelper.setLocalDateTimeUTC(ps, 1, LocalDateTime.now(ZoneOffset.UTC));
        });
      }
      logger.debug("Scheduled purge of token cache resulted in cache size of {}", cacheSize);
    });
  }

  @SuppressFBWarnings(value = "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING", justification = "Generated SQL is safe")
  void timezoneHandlingTest() throws Exception {
    
    int purgedTokens = jdbcHelper.runSqlUpdateSynchronously("purgeLogins", purgeLogins, ps -> {
      JdbcHelper.setLocalDateTimeUTC(ps, 1, LocalDateTime.now(ZoneOffset.UTC).minusMonths(1));
    });
    logger.info("Purged {} expired tokens on startup", purgedTokens);
    
    String startupTestSessionId = UUID.randomUUID().toString();
    LocalDateTime expiry = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(1);
    String token = "Startup test " + ManagementFactory.getRuntimeMXBean().getName();
    int rows;
    try {
      rows = jdbcHelper.runSqlUpdateSynchronously("storeToken", storeToken, ps -> {
        int param = 1;
        ps.setString(param++, startupTestSessionId);
        JdbcHelper.setLocalDateTimeUTC(ps, param++, expiry);
        ps.setString(param++, token);
      });
    } catch (Throwable ex) {
      throw new IllegalStateException("Startup test for timezone handling failed.  The tuple (" + startupTestSessionId + ", " + expiry + ", \"" + token + "\") could not be written to the session table: ", ex);
    }
    if (rows == 0) {
      throw new IllegalStateException("Startup test for timezone handling failed.  The tuple (" + startupTestSessionId + ", " + expiry + ", \"" + token + "\") was written to the session table but did not affect any rows");
    }
    TimestampedToken testToken;
    try {
      testToken = jdbcHelper.runSqlSelectSynchronously(getToken, ps -> {
        ps.setString(1, startupTestSessionId);
      }, rs -> {
        TimestampedToken result = null;
        while (rs.next()) {
          logger.debug("rs.getTimestamp(1): {}", rs.getTimestamp(1));
          logger.debug("rs.getTimestamp(1).toLocalDateTime(): {}", rs.getTimestamp(1).toLocalDateTime());
          result = new TimestampedToken(rs.getTimestamp(1).toLocalDateTime(), rs.getString(2));
          break ;
        }
        return result;
      });
    } catch (Throwable ex) {
      throw new IllegalStateException("Startup test for timezone handling failed.  The tuple (" + startupTestSessionId + ", " + expiry + ", \"" + token + "\"}) was written to the session table but could not be read: ", ex);
    }
    if (testToken == null) {
      throw new IllegalStateException("Startup test for timezone handling failed.  The tuple (" + startupTestSessionId + ", " + expiry + ", \"" + token + "\") was written to the session table but could not be found in it.");
    }
    long difference = expiry.until(testToken.expiry, ChronoUnit.SECONDS);
    if (Math.abs(difference) > 1) {
      throw new IllegalStateException("The tuple (" + startupTestSessionId + ", " + expiry + ", \"" + token + "\") was written to the session table but on output it was (" + startupTestSessionId + ", " + expiry + ", \"" + token + "\").  Please check your database/connection details for correct handling of timezones.");
    }
  }
  
  @Override
  public Future<Void> store(String state, String provider, String codeVerifier, String nonce, String redirectUri, String targetUrl) {
    logger.debug("store: {} {} {} {} {} {} {} {} {} {}",
             state,
             provider,
             codeVerifier,
             nonce,
             redirectUri,
             targetUrl
    );
    
    return jdbcHelper.runSqlUpdate("recordLogin", recordLogin, ps -> {
                    int param = 1; 
                    ps.setString(param++, JdbcHelper.limitLength(state, 300));
                    ps.setString(param++, JdbcHelper.limitLength(provider, 300));
                    JdbcHelper.setLocalDateTimeUTC(ps, param++, LocalDateTime.now(ZoneOffset.UTC));
                    ps.setString(param++, JdbcHelper.limitLength(codeVerifier, 300));
                    ps.setString(param++, JdbcHelper.limitLength(nonce, 300));
                    ps.setString(param++, JdbcHelper.limitLength(redirectUri, 2000));
                    ps.setString(param++, JdbcHelper.limitLength(targetUrl, 2000));
    }).mapEmpty();
  }

  @Override
  public Future<Void> markUsed(String state) {
    return jdbcHelper.runSqlUpdate("markUsed", markUsed, ps -> {
                    int param = 1; 
                    JdbcHelper.setLocalDateTimeUTC(ps, param++, LocalDateTime.now(ZoneOffset.UTC));
                    ps.setString(param++, JdbcHelper.limitLength(state, 300));
    })
            .compose(count -> {
              if (count == 0) {
                return Future.failedFuture(new IllegalArgumentException("State does not exist"));
              } else {
                return Future.succeededFuture();
              }
            })
            .mapEmpty();
  }
  
  @Override
  public Future<RequestData> getRequestData(String state) {
    return jdbcHelper.runSqlSelect(getData, ps -> {
        ps.setString(1, state);
      }, rs -> {
        RequestData result = null;
        while (rs.next()) {
          result = new RequestData(
                  rs.getString(1)
                  , rs.getString(2)
                  , rs.getString(3)
                  , rs.getString(4)
                  , rs.getString(5)
          );
        }
        return result;
      });
  }

  @Override
  public Future<Void> storeToken(String id, LocalDateTime expiry, String token) {
    cacheToken(id, new TimestampedToken(expiry, token));
    return jdbcHelper.runSqlUpdate("storeToken", storeToken, ps -> {
                    int param = 1; 
                    ps.setString(param++, id);
                    JdbcHelper.setLocalDateTimeUTC(ps, param++, expiry);
                    ps.setString(param++, token);
    })
            .mapEmpty();
  }
  
  @Override
  public Future<String> getToken(String id) {
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    synchronized (tokenCache) {
      TimestampedToken token = tokenCache.get(id);
      if (token != null) {
        logger.debug("Now: {}, Expiry: {}", now, token.expiry);
        if (token.expiry.isBefore(now)) {
          tokenCache.remove(id);
          tokenCache.entrySet().removeIf(entry -> entry.getValue().expiry.isBefore(now));
        } else {
          return Future.succeededFuture(token.token);
        }
      }
    }
    
    return jdbcHelper.runSqlSelect(getToken, ps -> {
        ps.setString(1, id);
      }, rs -> {
        while (rs.next()) {
          logger.debug("rs.getTimestamp(1): {}", rs.getTimestamp(1));
          logger.debug("rs.getTimestamp(1).toLocalDateTime(): {}", rs.getTimestamp(1).toLocalDateTime());
          LocalDateTime expiry = rs.getTimestamp(1).toLocalDateTime();
          return new TimestampedToken(expiry, rs.getString(2));
        }
        return null;
      })
      .compose(tt -> {
        if (tt != null) {
          logger.debug("Now: {}, Expiry: {}", now, tt.expiry);
          if (now.isAfter(tt.expiry)) {
            jdbcHelper.runSqlUpdate("deleteToken", deleteToken, ps -> ps.setString(1, id));
            jdbcHelper.runSqlUpdate("expireTokens", expireTokens, ps -> JdbcHelper.setLocalDateTimeUTC(ps, 1, now));
            return Future.succeededFuture(null);
          } else {
            cacheToken(id, tt);
            return Future.succeededFuture(tt.token);
          }
        }
        return Future.succeededFuture(null);
      });
            
  }

  /**
   * Store a token in the local cache.
   * @param id Token ID.
   * @param tt Token, with timestamp.
   */
  void cacheToken(String id, TimestampedToken tt) {
    Entry<String, TimestampedToken> oldest = null;
    int cacheSize;
    synchronized (tokenCache) {
      cacheSize = tokenCache.size();
      if (cacheSize >= MAX_TOKEN_CACHE_SIZE) {
        // Find the oldest cache entry (one that will expire soonest) and remove it
        for (Entry<String, TimestampedToken> entry : tokenCache.entrySet()) {
          if (oldest == null || entry.getValue().expiry.isBefore(oldest.getValue().expiry)) {
            oldest = entry;       
          }
        }
        if (oldest != null) {
          tokenCache.remove(oldest.getKey());
        }
      }
      tokenCache.put(id, tt);
    }
    if (oldest != null) {
      logger.debug("Token cache contains {} entries, removed entry for {} to accomodate {}", cacheSize, oldest.getKey(), id);
    }
  }

  @Override
  public Future<Void> removeToken(String id) {
    synchronized (tokenCache) {
      tokenCache.remove(id);
    }
    return jdbcHelper.runSqlUpdate("deleteToken", deleteToken, ps -> ps.setString(1, id)).mapEmpty();    
  }
  
  /**
   * Get the size of the token cache.
   * @return the size of the token cache.
   */
  int getTokenCacheSize() {
    synchronized (tokenCache) {
      return tokenCache.size();
    }
  }

  
}
