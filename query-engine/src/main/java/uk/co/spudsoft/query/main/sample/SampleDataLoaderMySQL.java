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

import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnectOptions;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author jtalbut
 */
public class SampleDataLoaderMySQL implements SampleDataLoader {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(SampleDataLoaderMySQL.class);
  
  @Override
  public String getName() {
    return "MySQL";
  }
  
  @Override
  public String getIdentifierQuote() {
    return "`";
  }
  
  protected String getScript() {
    return "/sampleData/MySQL Test Structures.sql";
  }
  
  @Override
  @SuppressFBWarnings("SQL_INJECTION_VERTX")
  public Future<Void> prepareTestDatabase(Vertx vertx, String url, String username, String password) {
    SqlConnectOptions connectOptions = SqlConnectOptions.fromUri(url);
    connectOptions.setUser(username);
    connectOptions.setPassword(password);
    Pool pool = Pool.pool(vertx, connectOptions, new PoolOptions().setMaxSize(3));
    
    String contents;
    try (InputStream strm = getClass().getResourceAsStream(getScript())) {
      contents = new String(strm.readAllBytes(), StandardCharsets.UTF_8);
    } catch (Throwable ex) {
      return Future.failedFuture(ex);
    }
    
    List<String> sqlList = new ArrayList<>();
    String delimiter = ";";
    int start = 0;
    Pattern delimPat = Pattern.compile("DELIMITER (\\S+)");
    Matcher matcher = delimPat.matcher(contents);
    while (matcher.find()) {
      sqlList.addAll(Arrays.asList(contents.substring(start, matcher.start()).split(delimiter)));
      delimiter = matcher.group(1);
      start = matcher.end() + 1;
    }    
    sqlList.addAll(Arrays.asList(contents.substring(start).split(delimiter)));
        
    return executeSql(pool, sqlList.iterator())
            .mapEmpty()
            ;

  }
  
  @SuppressFBWarnings("SQL_INJECTION_VERTX")
  private Future<Void> executeSql(Pool pool, Iterator<String> iter) {
    if (iter.hasNext()) {
      String stmt = iter.next().trim();
      if (Strings.isNullOrEmpty(stmt)) {
        return executeSql(pool, iter);
      } else {
        return pool.query(stmt).execute()
                .compose(rs -> executeSql(pool, iter));
      }
    } else {
      return Future.succeededFuture();
    }
  }
}
