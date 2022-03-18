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
    assertEquals(DestinationType.Logger, instance.getType());
    instance = DestinationLogger.builder().type(DestinationType.Logger).build();
    assertEquals(DestinationType.Logger, instance.getType());
    try {
      DestinationLogger.builder().type(DestinationType.Nothing).build();
      fail("Should have thrown exception");
    } catch(IllegalArgumentException ex) {      
    }
  }
  
}
