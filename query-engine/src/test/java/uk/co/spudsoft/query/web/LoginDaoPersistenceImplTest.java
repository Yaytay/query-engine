/*
 * Copyright (C) 2023 njt
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.query.exec.AuditorPersistenceImpl;
import uk.co.spudsoft.query.main.Credentials;
import uk.co.spudsoft.query.main.DataSourceConfig;
import uk.co.spudsoft.query.main.Persistence;
import uk.co.spudsoft.query.testcontainers.ServerProviderPostgreSQL;

/**
 *
 * @author njt
 */
@ExtendWith(VertxExtension.class)
public class LoginDaoPersistenceImplTest {
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  
  @Test
  public void testPrepare(Vertx vertx, VertxTestContext testContext) throws Exception {
    
    Persistence config = new Persistence();    
    LoginDaoPersistenceImpl instance = new LoginDaoPersistenceImpl(vertx, null, config, Duration.ofHours(1));
    assertThrows(IllegalStateException.class, () -> {
      instance.prepare();
    });
    DataSourceConfig dataSourceConfig = new DataSourceConfig();
    config.setDataSource(dataSourceConfig);
    assertThrows(IllegalStateException.class, () -> {
      instance.prepare();
    });
    dataSourceConfig.setUrl(postgres.getJdbcUrl());
    dataSourceConfig.setSchema("public");
    dataSourceConfig.setAdminUser(new Credentials(postgres.getUser(), postgres.getPassword()));
    instance.prepare();
    assertThrows(IllegalStateException.class, () -> {
      instance.prepare();
    });
    
    testContext.completeNow();
  }

  @Test
  public void testTokenExpiry(Vertx vertx, VertxTestContext testContext) throws Throwable {
    Persistence config = new Persistence();
    
    DataSourceConfig dataSource = new DataSourceConfig();
    dataSource.setUrl("jdbc:h2:mem:LoginDaoPersistenceImplTest_testTokenExpiry;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=10");
    config.setDataSource(dataSource);
    
    AuditorPersistenceImpl auditor = new AuditorPersistenceImpl(vertx, null, config);
    auditor.prepare();
    
    LoginDaoPersistenceImpl instance = new LoginDaoPersistenceImpl(vertx, null, config, Duration.ofMillis(500));
    instance.prepare();
    
    LocalDateTime now = LocalDateTime.now();
    
    instance.storeToken("id", now.minusSeconds(1), "token")
            .compose(v -> instance.storeToken("id2", now.plusSeconds(1), "token"))
            .compose(v -> instance.getToken("id"))
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
  public void testTokenCacheOverflow(Vertx vertx, VertxTestContext testContext) throws Throwable {
    Persistence config = new Persistence();
    
    DataSourceConfig dataSource = new DataSourceConfig();
    dataSource.setUrl("jdbc:h2:mem:LoginDaoPersistenceImplTest_testTokenCacheOverflow;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=10");
    config.setDataSource(dataSource);
    
    AuditorPersistenceImpl auditor = new AuditorPersistenceImpl(vertx, null, config);
    auditor.prepare();
    
    LoginDaoPersistenceImpl instance = new LoginDaoPersistenceImpl(vertx, null, config, Duration.ofMillis(500));
    instance.prepare();
    
    LocalDateTime now = LocalDateTime.now();
    
    List<Future<Void>> futures = new ArrayList<>();
    for (int i = 0; i < 1500; ++i) {
      futures.add(instance.storeToken(Integer.toHexString(i), now.plusMinutes(1), "token"));
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
