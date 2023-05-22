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
package uk.co.spudsoft.query.main.sample;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnectOptions;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author jtalbut
 */
public class SampleDataLoaderPostgreSQL implements SampleDataLoader {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(SampleDataLoaderPostgreSQL.class);
  
  @Override
  public String getName() {
    return "PostgreSQL";
  }
  
  protected String getScript() {
    return "/sampleData/PostgreSQL Test Structures.sql";
  }  
  
  @Override
  public String getIdentifierQuote() {
    return "\"";
  }
  
  @Override
  @SuppressFBWarnings("SQL_INJECTION_VERTX")
  public Future<Void> prepareTestDatabase(Vertx vertx, String url, String username, String password) {
    SqlConnectOptions connectOptions = SqlConnectOptions.fromUri(url);
    connectOptions.setUser(username);
    connectOptions.setPassword(password);
    Pool pool = Pool.pool(vertx, connectOptions, new PoolOptions().setMaxSize(3));
    
    String sql;
    try (InputStream strm = getClass().getResourceAsStream(getScript())) {
      sql = new String(strm.readAllBytes(), StandardCharsets.UTF_8);
    } catch (Throwable ex) {
      return Future.failedFuture(ex);
    }
    
    return Future.succeededFuture()
            .compose(rs -> executeSql(pool, sql))
            .onSuccess(rs -> {
              if (rs != null) {
                logger.info("Script run");
              }
            })

            .onFailure(ex -> {
              logger.error("Failed: ", ex);
            })
            .mapEmpty()
            ;

  }

  @SuppressFBWarnings("SQL_INJECTION_VERTX")
  private static Future<RowSet<Row>> executeSql(Pool pool, String sql) {
    return pool.query(sql).execute();
  }

}
