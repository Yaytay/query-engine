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
public class ProcessorScriptTest {
  
  @Test
  public void testSetType() {
    ProcessorScript instance = ProcessorScript.builder().type(ProcessorType.SCRIPT).build();
    assertEquals(ProcessorType.SCRIPT, instance.getType());
    try {
      ProcessorScript.builder().type(ProcessorType.GROUP_CONCAT).build();
      fail("Expected IllegalArgumentException");
    } catch(IllegalArgumentException ex) {
    }
  }

  
}
