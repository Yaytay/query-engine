/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.testcontainers;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

/**
 *
 * @author jtalbut
 */
public interface ServerProvider {
  
  String getName();
  
  Future<Void> prepareContainer(Vertx vertx);

  Future<Void> prepareTestDatabase(Vertx vertx);
  
  String getUrl();
  
  String getUser();
  
  String getPassword();
  
  int getPort();
  
}
