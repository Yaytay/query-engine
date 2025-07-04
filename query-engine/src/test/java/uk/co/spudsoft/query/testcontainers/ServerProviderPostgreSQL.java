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
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import uk.co.spudsoft.query.main.sample.SampleDataLoader;
import uk.co.spudsoft.query.main.sample.SampleDataLoaderPostgreSQL;
import static uk.co.spudsoft.query.testcontainers.AbstractServerProvider.ROOT_PASSWORD;


/**
 *
 * @author jtalbut
 */
public class ServerProviderPostgreSQL extends AbstractServerProvider implements ServerProvider {

  private static final Logger logger = LoggerFactory.getLogger(ServerProviderPostgreSQL.class);
  
  public static final String PGSQL_IMAGE_NAME = "postgres:16.2-alpine";

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
  public String getIdentifierQuote() {
    return "\"";
  }
  
  @Override
  public Future<Void> prepareContainer(Vertx vertx) {
    return vertx.executeBlocking(() -> {
      try {
        return getContainer();
      } catch (Throwable ex) {
        logger.warn("Failed to prepare container: ", ex);
        throw ex;
      }
    }).mapEmpty();
  }

  @Override
  public String getVertxUrl() {
    return "postgresql://localhost:" + port + "/test";
  }

  @Override
  public String getJdbcUrl() {
    return "jdbc:postgresql://localhost:" + port + "/test";
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
    SampleDataLoader loader = new SampleDataLoaderPostgreSQL("target" + File.separator + "temp");
    return loader.prepareTestDatabase(vertx, getVertxUrl(), getUser(), getPassword());
  }

}
