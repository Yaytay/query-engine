/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.queryengine;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.SqlClient;
import org.testcontainers.containers.GenericContainer;

/**
 *
 * @author jtalbut
 * @param <T>
 */
public interface ServerProviderInstance<T extends GenericContainer<?>> {
  
  T getContainer();
  
  Future<Void> prepareTestDatabase(Vertx vertx, SqlClient client);
  
}
