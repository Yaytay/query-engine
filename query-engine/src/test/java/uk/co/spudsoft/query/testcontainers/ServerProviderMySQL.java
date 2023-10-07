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
import io.vertx.mysqlclient.MySQLConnectOptions;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;
import uk.co.spudsoft.query.main.sample.SampleDataLoader;
import uk.co.spudsoft.query.main.sample.SampleDataLoaderMySQL;
import static uk.co.spudsoft.query.testcontainers.AbstractServerProvider.ROOT_PASSWORD;


/**
 *
 * @author jtalbut
 */
public class ServerProviderMySQL extends AbstractServerProvider implements ServerProvider {

  private static final Logger logger = LoggerFactory.getLogger(ServerProviderMySQL.class);
  
  public static final String MYSQL_IMAGE_NAME = "mysql:8.1";

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
  public MySQLConnectOptions getConnectOptions() {
    getContainer();
    return new MySQLConnectOptions()
            .setPort(port)
            .setHost("localhost")
            .setUser("user")
            .setDatabase("test")
            .setPassword(AbstractServerProvider.ROOT_PASSWORD)
            .setIdleTimeout(5)
            .setIdleTimeoutUnit(TimeUnit.MINUTES)         
            ;
  }

  @Override
  public String getUrl() {
    return "mysql://localhost:" + port + "/test";
  }

  @Override
  public String getUser() {
    return "root";
  }

  @Override
  public String getPassword() {
    return AbstractServerProvider.ROOT_PASSWORD;
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public String getIdentifierQuote() {
    return "`";
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
      }
      port = mysqlserver.getMappedPort(3306);
    }
    return mysqlserver;
  }

  @Override
  public Future<Void> prepareTestDatabase(Vertx vertx) {
    SampleDataLoader loader = new SampleDataLoaderMySQL();
    return loader.prepareTestDatabase(vertx, getUrl(), getUser(), getPassword());
  }
}
