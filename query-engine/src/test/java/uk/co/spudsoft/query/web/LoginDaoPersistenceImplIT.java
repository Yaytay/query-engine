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

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.query.exec.AuditorPersistenceImpl;
import uk.co.spudsoft.query.exec.JdbcHelper;
import uk.co.spudsoft.query.main.Credentials;
import uk.co.spudsoft.query.main.DataSourceConfig;
import uk.co.spudsoft.query.main.Persistence;
import uk.co.spudsoft.query.testcontainers.ServerProviderMsSQL;
import uk.co.spudsoft.query.testcontainers.ServerProviderMySQL;
import uk.co.spudsoft.query.testcontainers.ServerProviderPostgreSQL;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class LoginDaoPersistenceImplIT {
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  private static final ServerProviderMySQL mysql = new ServerProviderMySQL().init();
  private static final ServerProviderMsSQL mssql = new ServerProviderMsSQL().init();
  
  private static String previousTimezone;
  
  @Test
  public void testTokenExpiryPostgres(Vertx vertx, VertxTestContext testContext) throws Throwable {
    
    Persistence config = new Persistence();
    
    DataSourceConfig dataSource = new DataSourceConfig();
    dataSource.setUrl(postgres.getJdbcUrl());
    dataSource.setAdminUser(new Credentials(postgres.getUser(), postgres.getPassword()));
    dataSource.setUser(new Credentials(postgres.getUser(), postgres.getPassword()));
    config.setDataSource(dataSource);
    
    JdbcHelper jdbcHelper = new JdbcHelper(vertx, JdbcHelper.createDataSource(dataSource, dataSource.getAdminUser(), null));
    
    AuditorPersistenceImpl auditor = new AuditorPersistenceImpl(vertx, null, config, jdbcHelper);
    auditor.prepare();
    
    LoginDaoPersistenceImpl instance = new LoginDaoPersistenceImpl(vertx, null, config, Duration.ofMillis(500), jdbcHelper);
    instance.prepare();
    
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    
    String id1 = UUID.randomUUID().toString();
    String id2 = UUID.randomUUID().toString();
    
    instance.storeToken(id1, now.minusSeconds(1), "token1")
            .compose(v -> instance.storeToken(id2, now.plusSeconds(1), "token2"))
            .compose(v -> instance.getToken(id1))
            .onFailure(ex -> testContext.failNow("Failed to get token"))
            .onSuccess(s -> {
              testContext.verify(() -> {
                assertNull(s);
              });
              testContext.completeNow();
            })
            ;
    
  }

  @Test
  public void testTokenCacheOverflowPostgres(Vertx vertx, VertxTestContext testContext) throws Throwable {
    Persistence config = new Persistence();
    
    DataSourceConfig dataSource = new DataSourceConfig();
    dataSource.setUrl(postgres.getJdbcUrl());
    dataSource.setAdminUser(new Credentials(postgres.getUser(), postgres.getPassword()));
    dataSource.setUser(new Credentials(postgres.getUser(), postgres.getPassword()));
    config.setDataSource(dataSource);
    
    JdbcHelper jdbcHelper = new JdbcHelper(vertx, JdbcHelper.createDataSource(dataSource, dataSource.getAdminUser(), null));
    
    AuditorPersistenceImpl auditor = new AuditorPersistenceImpl(vertx, null, config, jdbcHelper);
    auditor.prepare();
    
    LoginDaoPersistenceImpl instance = new LoginDaoPersistenceImpl(vertx, null, config, Duration.ofMillis(500), jdbcHelper);
    instance.prepare();
    
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    
    String baseId = "testTokenCacheOverflowPostgres@" + ManagementFactory.getRuntimeMXBean().getName() + ": ";
    
    List<Future<Void>> futures = new ArrayList<>();
    for (int i = 0; i < 1500; ++i) {
      futures.add(instance.storeToken(baseId + i, now.plusMinutes(1), "token"));
    }
    
    Future.all(futures)
            .onFailure(ex -> testContext.failNow("Failed to add all tokens"))
            .onSuccess(s -> {
              testContext.verify(() -> {
                assertEquals(1000, instance.getTokenCacheSize());
              });
              testContext.completeNow();
            })
            ;
    
  }

  @Test
  public void testTokenExpiryMySQL(Vertx vertx, VertxTestContext testContext) throws Throwable {
    Persistence config = new Persistence();
    
    DataSourceConfig dataSource = new DataSourceConfig();
    dataSource.setUrl(mysql.getJdbcUrl());
    dataSource.setAdminUser(new Credentials(mysql.getUser(), mysql.getPassword()));
    dataSource.setUser(new Credentials(mysql.getUser(), mysql.getPassword()));
    config.setDataSource(dataSource);
    
    JdbcHelper jdbcHelper = new JdbcHelper(vertx, JdbcHelper.createDataSource(dataSource, dataSource.getAdminUser(), null));
    
    AuditorPersistenceImpl auditor = new AuditorPersistenceImpl(vertx, null, config, jdbcHelper);
    auditor.prepare();
    
    LoginDaoPersistenceImpl instance = new LoginDaoPersistenceImpl(vertx, null, config, Duration.ofMillis(500), jdbcHelper);
    instance.prepare();
    
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

    String id1 = UUID.randomUUID().toString();
    String id2 = UUID.randomUUID().toString();
    
    instance.storeToken(id1, now.minusSeconds(1), "token1")
            .compose(v -> instance.storeToken(id2, now.plusSeconds(1), "token2"))
            .compose(v -> instance.getToken(id1))
            .onFailure(ex -> testContext.failNow("Failed to get token"))
            .onSuccess(s -> {
              testContext.verify(() -> {
                assertNull(s);
              });
              testContext.completeNow();
            })
            ;
    
  }

  @Test
  public void testTokenCacheOverflowMySQL(Vertx vertx, VertxTestContext testContext) throws Throwable {
    Persistence config = new Persistence();
    
    DataSourceConfig dataSource = new DataSourceConfig();
    dataSource.setUrl(mysql.getJdbcUrl());
    dataSource.setAdminUser(new Credentials(mysql.getUser(), mysql.getPassword()));
    dataSource.setUser(new Credentials(mysql.getUser(), mysql.getPassword()));
    config.setDataSource(dataSource);
    
    JdbcHelper jdbcHelper = new JdbcHelper(vertx, JdbcHelper.createDataSource(dataSource, dataSource.getAdminUser(), null));
    
    AuditorPersistenceImpl auditor = new AuditorPersistenceImpl(vertx, null, config, jdbcHelper);
    auditor.prepare();
    
    LoginDaoPersistenceImpl instance = new LoginDaoPersistenceImpl(vertx, null, config, Duration.ofMillis(500), jdbcHelper);
    instance.prepare();
    
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    
    String baseId = "testTokenCacheOverflowMySQL@" + ManagementFactory.getRuntimeMXBean().getName() + ": ";
    
    List<Future<Void>> futures = new ArrayList<>();
    for (int i = 0; i < 1500; ++i) {
      futures.add(instance.storeToken(baseId + i, now.plusMinutes(1), "token"));
    }
    
    Future.all(futures)
            .onFailure(ex -> testContext.failNow("Failed to add all tokens"))
            .onSuccess(s -> {
              testContext.verify(() -> {
                assertEquals(1000, instance.getTokenCacheSize());
              });
              testContext.completeNow();
            })
            ;
    
  }
  
  @Test
  public void testTokenExpiryMsSQL(Vertx vertx, VertxTestContext testContext) throws Throwable {
    Persistence config = new Persistence();
    
    DataSourceConfig dataSource = new DataSourceConfig();
    dataSource.setUrl(mssql.getJdbcUrl());
    dataSource.setAdminUser(new Credentials(mssql.getUser(), mssql.getPassword()));
    dataSource.setUser(new Credentials(mssql.getUser(), mssql.getPassword()));
    config.setDataSource(dataSource);
    
    JdbcHelper jdbcHelper = new JdbcHelper(vertx, JdbcHelper.createDataSource(dataSource, dataSource.getAdminUser(), null));
    
    AuditorPersistenceImpl auditor = new AuditorPersistenceImpl(vertx, null, config, jdbcHelper);
    auditor.prepare();
    
    LoginDaoPersistenceImpl instance = new LoginDaoPersistenceImpl(vertx, null, config, Duration.ofMillis(500), jdbcHelper);
    instance.prepare();
    
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    
    String id1 = UUID.randomUUID().toString();
    String id2 = UUID.randomUUID().toString();
    
    instance.storeToken(id1, now.minusSeconds(1), "token1")
            .compose(v -> instance.storeToken(id2, now.plusSeconds(1), "token2"))
            .compose(v -> instance.getToken(id1))
            .onFailure(ex -> testContext.failNow("Failed to get token"))
            .onSuccess(s -> {
              testContext.verify(() -> {
                assertNull(s);
              });
              testContext.completeNow();
            })
            ;
    
  }

  @Test
  public void testTokenCacheOverflowMsSQL(Vertx vertx, VertxTestContext testContext) throws Throwable {
    Persistence config = new Persistence();
    
    DataSourceConfig dataSource = new DataSourceConfig();
    dataSource.setUrl(mssql.getJdbcUrl());
    dataSource.setAdminUser(new Credentials(mssql.getUser(), mssql.getPassword()));
    dataSource.setUser(new Credentials(mssql.getUser(), mssql.getPassword()));
    config.setDataSource(dataSource);
    
    JdbcHelper jdbcHelper = new JdbcHelper(vertx, JdbcHelper.createDataSource(dataSource, dataSource.getAdminUser(), null));
    
    AuditorPersistenceImpl auditor = new AuditorPersistenceImpl(vertx, null, config, jdbcHelper);
    auditor.prepare();
    
    LoginDaoPersistenceImpl instance = new LoginDaoPersistenceImpl(vertx, null, config, Duration.ofMillis(500), jdbcHelper);
    instance.prepare();
    
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    
    String baseId = "testTokenCacheOverflowMsSQL@" + ManagementFactory.getRuntimeMXBean().getName() + ": ";
    
    List<Future<Void>> futures = new ArrayList<>();
    for (int i = 0; i < 1500; ++i) {
      futures.add(instance.storeToken(baseId + i, now.plusMinutes(1), "token"));
    }
    
    Future.all(futures)
            .onFailure(ex -> testContext.failNow("Failed to add all tokens"))
            .onSuccess(s -> {
              testContext.verify(() -> {
                assertEquals(1000, instance.getTokenCacheSize());
              });
              testContext.completeNow();
            })
            ;
    
  }
  
}
