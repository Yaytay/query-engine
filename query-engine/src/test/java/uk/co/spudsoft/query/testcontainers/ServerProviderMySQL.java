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
package uk.co.spudsoft.query.testcontainers;

import com.github.dockerjava.api.model.Container;
import com.google.common.base.Strings;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.mysqlclient.MySQLConnectOptions;
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
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;
import static uk.co.spudsoft.query.testcontainers.AbstractServerProvider.ROOT_PASSWORD;


/**
 *
 * @author jtalbut
 */
public class ServerProviderMySQL extends AbstractServerProvider implements ServerProvider {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ServerProviderMySQL.class);
  
  public static final String MYSQL_IMAGE_NAME = "mysql:8.0";

  private static final Object lock = new Object();
  private static MySQLContainer<?> mysqlserver;
  private static int port;
  
  @Override
  public String getName() {
    return "MySQL";
  }
  
  @Override
  protected String getScript() {
    return "/MySQL Test Structures.sql";
  }
  
  public ServerProviderMySQL init() {
    getContainer();
    return this;
  }
  
  @Override
  public Future<Void> prepareContainer(Vertx vertx) {
    return vertx.executeBlocking(p -> {
      try {
        getContainer();
        p.complete();
      } catch (Throwable ex) {
        p.fail(ex);
      }
    });
  }

  @Override
  public MySQLConnectOptions getConnectOptions() {
    getContainer();
    return new MySQLConnectOptions()
            .setPort(port)
            .setHost("localhost")
            .setUser("user")
            .setDatabase("test")
            .setPassword(AbstractServerProvider.ROOT_PASSWORD)
            ;
  }

  @Override
  public String getUrl() {
    return "mysql://localhost:" + port + "/test";
  }

  @Override
  public String getUser() {
    return "user";
  }

  @Override
  public String getPassword() {
    return AbstractServerProvider.ROOT_PASSWORD;
  }

  @Override
  public int getPort() {
    return port;
  }

  public MySQLContainer<?> getContainer() {
    synchronized (lock) {
      long start = System.currentTimeMillis();
      
      Container createdContainer = findContainer("/query-engine-mysql-1");
      if (createdContainer != null) {
        port = Arrays.asList(createdContainer.ports).stream().filter(cp -> cp.getPrivatePort() == 3306).map(cp -> cp.getPublicPort()).findFirst().orElse(0);
        return null;
      } 
      if (mysqlserver == null) {
        mysqlserver = new MySQLContainer<>(MYSQL_IMAGE_NAME)
                .withUsername("user")
                .withPassword(ROOT_PASSWORD)
                .withExposedPorts(3306)
                .withDatabaseName("test")
                ;
      }
      if (!mysqlserver.isRunning()) {
        mysqlserver.start();
        logger.info("Started test instance of MySQL with ports {} in {}s",
                mysqlserver.getExposedPorts().stream().map(p -> Integer.toString(p) + ":" + Integer.toString(mysqlserver.getMappedPort(p))).collect(Collectors.toList()),
                (System.currentTimeMillis() - start) / 1000.0
        );
        port = mysqlserver.getMappedPort(3306);
      }
    }
    return mysqlserver;
  }

  @Override
  public Future<Void> prepareTestDatabase(Vertx vertx) {
    SqlConnectOptions connectOptions = SqlConnectOptions.fromUri(getUrl());
    connectOptions.setUser("root");
    connectOptions.setPassword(ROOT_PASSWORD);
    Pool pool = Pool.pool(vertx, connectOptions, new PoolOptions().setMaxSize(3));
    
    String contents;
    try (InputStream strm = getClass().getResourceAsStream(getScript())) {
      contents = new String(strm.readAllBytes(), StandardCharsets.UTF_8);
    } catch(Throwable ex) {
      return Future.failedFuture(ex);
    }
    
    List<String> sqlList = new ArrayList<>();
    String delimiter = ";";
    int start = 0;
    Pattern delimPat = Pattern.compile("DELIMITER (\\S+)");
    Matcher matcher = delimPat.matcher(contents);
    while(matcher.find()) {
      logger.info("Matches from {} to {} with delim '{}'", matcher.start(), matcher.end(), matcher.group(1));
      sqlList.addAll(Arrays.asList(contents.substring(start, matcher.start()).split(delimiter)));
      delimiter = matcher.group(1);
      start = matcher.end() + 1;
    }    
    sqlList.addAll(Arrays.asList(contents.substring(start).split(delimiter)));
        
    return executeSql(pool, sqlList.iterator())
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
  
  private Future<Void> executeSql(Pool pool, Iterator<String> iter) {
    if (iter.hasNext()) {
      String stmt = iter.next().trim();
      if (Strings.isNullOrEmpty(stmt)) {
        return executeSql(pool, iter);
      } else {
        logger.info("Executing {}", stmt);
        return pool.query(stmt).execute().compose(rs -> executeSql(pool, iter));
      }
    } else {
      return Future.succeededFuture();
    }
  }
}
