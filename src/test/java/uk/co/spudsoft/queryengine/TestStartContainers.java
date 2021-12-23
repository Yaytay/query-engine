/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.queryengine;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

/**
 *
 * @author jtalbut
 */
public class TestStartContainers {
  
  @Test
  public void startAllContainers() {
    GenericContainer container;
    container = ServerProvider.getMsSqlContainer();
    container = ServerProvider.getMySqlContainer();
    container = ServerProvider.getPgSqlContainer();
  }
  
}
