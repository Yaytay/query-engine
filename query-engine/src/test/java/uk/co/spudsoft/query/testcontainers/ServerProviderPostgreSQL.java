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
import com.google.common.collect.Iterators;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnectOptions;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import static uk.co.spudsoft.query.testcontainers.AbstractServerProvider.ROOT_PASSWORD;
import uk.co.spudsoft.query.testhelpers.RowSetHelper;


/**
 *
 * @author jtalbut
 */
public class ServerProviderPostgreSQL extends AbstractServerProvider implements ServerProvider {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ServerProviderPostgreSQL.class);
  
  public static final String PGSQL_IMAGE_NAME = "postgres:14.1-alpine";

  private static final Object lock = new Object();
  private static PostgreSQLContainer<?> pgsqlserver;
  private static int port;
  
  @Override
  public String getName() {
    return "PostgreSQL";
  }
  
  @Override
  protected String getScript() {
    return "/PostgreSQL Test Structures.sql";
  }  
  
  public ServerProviderPostgreSQL init() {
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
  public String getUrl() {
    return "postgresql://localhost:" + port + "/test";
  }

  @Override
  public String getUser() {
    return "postgres";
  }

  @Override
  public String getPassword() {
    return AbstractServerProvider.ROOT_PASSWORD;
  }

  @Override
  public int getPort() {
    return port;
  }
    
  public PostgreSQLContainer<?> getContainer() {
    synchronized (lock) {
      long start = System.currentTimeMillis();
      
      Container createdContainer = findContainer("/query-engine-postgresql-1");
      if (createdContainer != null) {
        port = Arrays.asList(createdContainer.ports).stream().filter(cp -> cp.getPrivatePort() == 5432).map(cp -> cp.getPublicPort()).findFirst().orElse(0);
        return null;
      } 
      
      if (pgsqlserver == null) {
        pgsqlserver = new PostgreSQLContainer<>(PGSQL_IMAGE_NAME)
                .withPassword(ROOT_PASSWORD)
                .withUsername("postgres")
                .withExposedPorts(5432)
                ;
      }
      if (!pgsqlserver.isRunning()) {
        pgsqlserver.start();
        logger.info("Started test instance of PostgreSQL with ports {} in {}s",
                pgsqlserver.getExposedPorts().stream().map(p -> Integer.toString(p) + ":" + Integer.toString(pgsqlserver.getMappedPort(p))).collect(Collectors.toList()),
                (System.currentTimeMillis() - start) / 1000.0
        );
      }
      port = pgsqlserver.getMappedPort(5432);
    }
    return pgsqlserver;
  }

  @Override
  public PgConnectOptions getConnectOptions() {
    getContainer();
    return new PgConnectOptions()
            .setPort(port)
            .setHost("localhost")
            .setUser(getUser())
            .setPassword(ROOT_PASSWORD)
            .setDatabase("test")
            ;
  }

  @Override
  public Future<Void> prepareTestDatabase(Vertx vertx) {
    SqlConnectOptions connectOptions = SqlConnectOptions.fromUri(getUrl());
    connectOptions.setUser("postgres");
    connectOptions.setPassword(ROOT_PASSWORD);
    Pool pool = Pool.pool(vertx, connectOptions, new PoolOptions().setMaxSize(3));
    
    String sql;
    try (InputStream strm = getClass().getResourceAsStream(getScript())) {
      sql = new String(strm.readAllBytes(), StandardCharsets.UTF_8);
    } catch(Throwable ex) {
      return Future.failedFuture(ex);
    }
    
    return Future.succeededFuture()
            .compose(rs -> pool.query(sql).execute())
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

}
