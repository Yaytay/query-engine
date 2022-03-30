/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.sources.sql;

import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnectOptions;

/**
 *
 * @author jtalbut
 */
public class PoolCreator {
  
  public Pool pool(Vertx vertx, SqlConnectOptions database, PoolOptions options) {
    return Pool.pool(vertx, database, options);
  }
  
}
