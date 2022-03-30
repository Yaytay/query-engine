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
public class SourceTestTest {
  
  @Test
  public void testGetType() {
    SourceTest instance = SourceTest.builder().build();
    assertEquals(SourceType.TEST, instance.getType());
  }

  @Test
  public void testSetType() {
    SourceTest instance = SourceTest.builder().type(SourceType.TEST).build();
    assertEquals(SourceType.TEST, instance.getType());
    try {
      SourceTest.builder().type(SourceType.HTTP).build();
      fail("Expected IllegalArgumentException");
    } catch(IllegalArgumentException ex) {
    }
  }

  @Test
  public void testGetRowCount() {
    SourceTest instance = SourceTest.builder().rowCount(17).build();
    assertEquals(17, instance.getRowCount());
  }

  @Test
  public void testGetName() {
    SourceTest instance = SourceTest.builder().name("name").build();
    assertEquals("name", instance.getName());
  }
  
}
