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

import uk.co.spudsoft.query.testhelpers.RowSetHelper;
import com.github.dockerjava.api.model.Container;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.mssqlclient.MSSQLConnectOptions;
import io.vertx.mssqlclient.MSSQLPool;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnectOptions;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MSSQLServerContainer;

/**
 *
 * @author jtalbut
 */
public class ServerProviderMsSQL extends AbstractServerProvider implements ServerProvider {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ServerProviderMsSQL.class);
  
  public static final String MSSQL_IMAGE_NAME = "mcr.microsoft.com/mssql/server:2019-latest";

  private static final Object lock = new Object();
  private static MSSQLServerContainer<?> mssqlserver;
  private static int port;

  @Override
  public String getName() {
    return "MS SQL Server";
  }

  @Override
  protected String getScript() {
    return "/MS SQL Test Structures.sql";
  }
  
  public ServerProviderMsSQL init() {
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
            })
            .compose(v -> {
              Pool pool = MSSQLPool.pool(vertx, getConnectOptions(), new PoolOptions().setMaxSize(1));
              return pool.preparedQuery("select count(*) from sys.databases where name = 'test'").execute()
                      .compose(rs -> {
                        int existingDb = rs.iterator().next().getInteger(0);
                        if (existingDb == 0) {
                          return pool.preparedQuery("create database test").execute();
                        } else {
                          return Future.succeededFuture();
                        }
                      })
                      .onSuccess(rs -> {
                        if (rs != null) {
                          logger.info("Database created: {}", RowSetHelper.toString(rs));
                        }
                      })
                      .mapEmpty()
                      ;
            });
  }

  @Override
  public MSSQLConnectOptions getConnectOptions() {
    getContainer();
    return new MSSQLConnectOptions()
            .setPort(port)
            .setHost("localhost")
            .setUser("sa")
            .setPassword(AbstractServerProvider.ROOT_PASSWORD)
            ;
  }

  @Override
  public String getUrl() {
    return "sqlserver://localhost:" + port + "/test";
  }

  @Override
  public String getUser() {
    return "sa";
  }

  @Override
  public String getPassword() {
    return AbstractServerProvider.ROOT_PASSWORD;
  }

  @Override
  public int getPort() {
    return port;
  }
  
  public MSSQLServerContainer<?> getContainer() {
    synchronized (lock) {
      long start = System.currentTimeMillis();
      
      Container createdContainer = findContainer("/query-engine-mssql-1");
      if (createdContainer != null) {
        port = Arrays.asList(createdContainer.ports).stream().filter(cp -> cp.getPrivatePort() == 1433).map(cp -> cp.getPublicPort()).findFirst().orElse(0);
        return null;
      } 
      
      if (mssqlserver == null) {
        mssqlserver = new MSSQLServerContainer<>(MSSQL_IMAGE_NAME)
                .withPassword(ROOT_PASSWORD)
                .withEnv("ACCEPT_EULA", "Y")
                .withExposedPorts(1433)
                .withUrlParam("trustServerCertificate", "true")
                ;
      }
      if (!mssqlserver.isRunning()) {
        mssqlserver.start();
        logger.info("Started test instance of Microsoft SQL Server with ports {} in {}s",
                mssqlserver.getExposedPorts().stream().map(p -> Integer.toString(p) + ":" + Integer.toString(mssqlserver.getMappedPort(p))).collect(Collectors.toList()),
                (System.currentTimeMillis() - start) / 1000.0
        );
      }
      port = mssqlserver.getMappedPort(1433);
    }
    return mssqlserver;
  }

  private Future<Void> createTestDatabase(Vertx vertx) {
    String fullUrl = getUrl();
    String shortUrl = fullUrl.substring(0, fullUrl.lastIndexOf("/"));
    SqlConnectOptions connectOptions = SqlConnectOptions.fromUri(shortUrl);
    connectOptions.setUser("sa");
    connectOptions.setPassword(ROOT_PASSWORD);
    Pool pool = Pool.pool(vertx, connectOptions, new PoolOptions().setMaxSize(3));
    
    return Future.succeededFuture()            
            .compose(rs -> pool.preparedQuery("""
                    IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'test')
                    BEGIN
                      CREATE DATABASE test;
                    END;
                  """).execute())
            .mapEmpty()
            ;
  }
  
  @Override
  public Future<Void> prepareTestDatabase(Vertx vertx) {
    SqlConnectOptions connectOptions = SqlConnectOptions.fromUri(getUrl());
    connectOptions.setUser("sa");
    connectOptions.setPassword(ROOT_PASSWORD);
    Pool pool = Pool.pool(vertx, connectOptions, new PoolOptions().setMaxSize(3));
    
    String sql;
    try (InputStream strm = getClass().getResourceAsStream(getScript())) {
      sql = new String(strm.readAllBytes(), StandardCharsets.UTF_8);
    } catch(Throwable ex) {
      return Future.failedFuture(ex);
    }
    
    return createTestDatabase(vertx)
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
