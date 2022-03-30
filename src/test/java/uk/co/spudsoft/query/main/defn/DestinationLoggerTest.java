/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * @author jtalbut
 */
public class DestinationLoggerTest {

  @Test
  public void testGetType() {
    DestinationLogger instance = DestinationLogger.builder().build();
    assertEquals(DestinationType.LOGGER, instance.getType());
  }

  @Test
  public void testSetType() {
    DestinationLogger instance = DestinationLogger.builder().type(DestinationType.LOGGER).build();
    assertEquals(DestinationType.LOGGER, instance.getType());
    try {
      DestinationLogger.builder().type(DestinationType.NOTHING).build();
      fail("Expected IllegalArgumentException");
    } catch(IllegalArgumentException ex) {
    }
  }
  
}
