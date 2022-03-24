/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.main.defn.Argument;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author jtalbut
 */
public class ArgumentInstanceTest {
  
  @Test
  public void testGetDefinition() {
    Argument argument = Argument.builder().build();
    ArgumentInstance instance = new ArgumentInstance(argument, "value");
    assertEquals(argument, instance.getDefinition());
  }

  @Test
  public void testGetValue() {
    Argument argument = Argument.builder().build();
    ArgumentInstance instance = new ArgumentInstance(argument, "value");
    assertEquals("value", instance.getValue());
  }
  
}
