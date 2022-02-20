/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.queryengine;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.SqlConnectOptions;

/**
 *
 * @author jtalbut
 */
public interface ServerProviderInstance {
  
  String getName();
  
  Future<Void> prepareContainer(Vertx vertx);

  Future<Void> prepareContainer(Vertx vertx, Context context);

  Future<Void> prepareTestDatabase(Vertx vertx, SqlClient client);
  
  SqlConnectOptions getOptions();
  
  Pool createPool(Vertx vertx, SqlConnectOptions options, PoolOptions poolOptions);
  
  String limit(int maxRows, String sql);
  
}
