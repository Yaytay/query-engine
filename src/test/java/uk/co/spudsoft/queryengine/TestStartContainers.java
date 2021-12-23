/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.queryengine;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

/**
 *
 * @author jtalbut
 */
public class TestStartContainers {
  
  private static final Logger logger = LoggerFactory.getLogger(TestStartContainers.class);
  
  @Test
  public void startAllContainers() {
    GenericContainer container;
    container = ServerProvider.getMsSqlContainer();
    container = ServerProvider.getMySqlContainer();
    container = ServerProvider.getPgSqlContainer();
  }
  
}
