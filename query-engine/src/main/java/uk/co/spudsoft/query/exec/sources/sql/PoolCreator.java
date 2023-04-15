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
package uk.co.spudsoft.query.exec.sources.sql;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnectOptions;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jtalbut
 */
public class PoolCreator {
  
  private static final Logger logger = LoggerFactory.getLogger(PoolCreator.class);
  
  private final Cache<JsonObject, Pool> poolCache = CacheBuilder.newBuilder()
          .recordStats()
          .expireAfterAccess(1, TimeUnit.MINUTES)
          .maximumSize(100)
          .removalListener(notification -> {
            logger.debug("Removing pool entry from cache");
            ((Pool) notification.getValue()).close();
          })
          .build();

  public Pool pool(Vertx vertx, SqlConnectOptions database, PoolOptions options) {
    JsonObject databaseJson = database.toJson();
    //options.setIdleTimeout(1);
    //options.setIdleTimeoutUnit(TimeUnit.MINUTES);
    //options.setPoolCleanerPeriod(60000);
    options.setMaxSize(40);

    JsonObject poolJson = options.toJson();
    JsonObject json = new JsonObject()
            .put("database", databaseJson)
            .put("pool", poolJson)
            ;
    try {
      logger.debug("Getting pool for {}", json);
      Pool pool = poolCache.get(json, () -> {
        logger.debug("Creating new database pool for {}", json);
        return Pool.pool(vertx, database, options);
      });
      logger.debug("Got pool for {}", json);
      return pool;
    } catch (ExecutionException ex) {
      logger.error("Failed to get pool ({}) from cache: ", json, ex);
      return Pool.pool(vertx, database, options);
    }
  }
  
}
