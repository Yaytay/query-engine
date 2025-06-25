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
package uk.co.spudsoft.query.main.sample;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.query.testcontainers.ServerProviderPostgreSQL;

/**
 *
 * @author njt
 */
@ExtendWith(VertxExtension.class)
public class AbstractSampleDataLoaderIT {
  
  @Test
  public void testPrepareTestDatabase(Vertx vertx, VertxTestContext testContext) {
    
    ServerProviderPostgreSQL provider = new ServerProviderPostgreSQL();
    
    Future<Void> future1 = prep(vertx, provider);
    Future<Void> future2 = prep(vertx, provider);
    Future<Void> future3 = prep(vertx, provider);
    
    Future.all(future1, future2, future3)
            .andThen(testContext.succeedingThenComplete());
    
  }
  
  private Future<Void> prep(Vertx vertx, ServerProviderPostgreSQL provider) {
    return provider.init()
            .prepareTestDatabase(vertx)
            .compose(v -> {
              return provider.prepareTestDatabase(vertx);
            });
  }
}
