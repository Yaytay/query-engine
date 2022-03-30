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
public class ProcessorLimitTest {
  
  @Test
  public void testGetType() {
    ProcessorLimit instance = ProcessorLimit.builder().build();
    assertEquals(ProcessorType.LIMIT, instance.getType());
  }

  @Test
  public void testSetType() {
    ProcessorLimit instance = ProcessorLimit.builder().type(ProcessorType.LIMIT).build();
    assertEquals(ProcessorType.LIMIT, instance.getType());
    try {
      ProcessorLimit.builder().type(ProcessorType.SCRIPT).build();
      fail("Expected IllegalArgumentException");
    } catch(IllegalArgumentException ex) {
    }
  }

  @Test
  public void testGetLimit() {
    ProcessorLimit instance = ProcessorLimit.builder().limit(17).build();
    assertEquals(17, instance.getLimit());
  }

  
}
