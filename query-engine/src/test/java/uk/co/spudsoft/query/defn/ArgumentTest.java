/*
 * Copyright (C) 2022 jtalbut
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.co.spudsoft.query.defn;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author jtalbut
 */
public class ArgumentTest {
  
  @Test
  public void testGetTitle() {
    Argument instance = Argument.builder().build();
    assertNull(instance.getTitle());
    instance = Argument.builder().title("title").build();
    assertEquals("title", instance.getTitle());
  }
  
  @Test
  public void testGetPrompt() {
    Argument instance = Argument.builder().build();
    assertNull(instance.getPrompt());
    instance = Argument.builder().prompt("prompt").build();
    assertEquals("prompt", instance.getPrompt());
  }
  
  @Test
  public void testGetDescription() {
    Argument instance = Argument.builder().build();
    assertNull(instance.getDescription());
    instance = Argument.builder().description("description").build();
    assertEquals("description", instance.getDescription());
  }
  
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
  public void testIsIgnored() {
    Argument instance = Argument.builder().build();
    assertFalse(instance.isIgnored());
    instance = Argument.builder().ignored(true).build();
    assertTrue(instance.isIgnored());
  }

  @Test
  public void testGetDependsUpon() {
    Argument instance = Argument.builder().build();
    assertNotNull(instance.getDependsUpon());
    assertTrue(instance.getDependsUpon().isEmpty());
    instance = Argument.builder().dependsUpon(Arrays.asList("one")).build();
    assertEquals("one", instance.getDependsUpon().get(0));
  }

  @Test
  public void testGetDefaultValue() {
    Argument instance = Argument.builder().build();
    assertNull(instance.getDefaultValue());
    instance = Argument.builder().defaultValue("seven").build();
    assertEquals("seven", instance.getDefaultValue());
  }

  @Test
  public void testGetPossibleValues() {
    Argument instance = Argument.builder().build();
    assertNotNull(instance.getPossibleValues());
    assertTrue(instance.getPossibleValues().isEmpty());
    instance = Argument.builder().possibleValues(Arrays.asList(ArgumentValue.builder().value("1").build())).build();
    assertEquals("1", instance.getPossibleValues().get(0).getValue());
  }
  
  @Test
  public void testValidate() {
    Argument instance1 = Argument.builder().name("£$%^").build();
    assertThrows(IllegalArgumentException.class, () -> instance1.validate());

    Argument instance2 = Argument.builder().name("name").build();
    instance2.validate();
    
    Argument instance3 = Argument.builder().name("name").permittedValuesRegex("[").build();
    assertThrows(IllegalArgumentException.class, () -> instance3.validate());
    
    Argument instance4 = Argument.builder().name("name").permittedValuesRegex("[A-Za-z]+").build();
    instance4.validate();
    
    
  }

}
