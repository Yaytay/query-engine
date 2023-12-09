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

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    dataSourceConfig.setUrl("jdbc:" + postgres.getUrl());
    dataSourceConfig.setSchema("public");
    dataSourceConfig.setAdminUser(new Credentials(postgres.getUser(), postgres.getPassword()));
    instance.prepare();
    assertThrows(IllegalStateException.class, () -> {
      instance.prepare();
    });
    
    testContext.completeNow();
  }

}
