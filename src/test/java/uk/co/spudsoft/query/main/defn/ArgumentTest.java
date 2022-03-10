/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author jtalbut
 */
public class ArgumentTest {
  
  @Test
  public void testGetType() {
    Argument instance = Argument.builder().build();
    assertNull(instance.getType());
    instance = Argument.builder().type(ArgumentType.Long).build();
    assertEquals(ArgumentType.Long, instance.getType());
  }

  @Test
  public void testIsOptional() {
    Argument instance = Argument.builder().build();
    assertFalse(instance.isOptional());
    instance = Argument.builder().optional(true).build();
    assertTrue(instance.isOptional());
  }

  @Test
  public void testGetDefaultValue() {
    Argument instance = Argument.builder().build();
    assertNull(instance.getDefaultValue());
    instance = Argument.builder().defaultValue("seven").build();
    assertEquals("seven", instance.getDefaultValue());
  }

}
