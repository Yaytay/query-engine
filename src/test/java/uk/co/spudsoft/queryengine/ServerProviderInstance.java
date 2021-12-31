/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.queryengine;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.SqlConnectOptions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

/**
 *
 * @author jtalbut
 * @param <T>
 */
public interface ServerProviderInstance<T extends GenericContainer<?>> {
  
  String getName();
  
  Future<T> prepareContainer(Vertx vertx);

  Network getNetwork();
  
  T getContainer();
  
  SqlConnectOptions getOptions();
  
  SqlClient createClient(Vertx vertx, SqlConnectOptions options, PoolOptions poolOptions);
  
  Future<Void> prepareTestDatabase(Vertx vertx, SqlClient client);
  
  String limit(int maxRows, String sql);
  
}
