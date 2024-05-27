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
package uk.co.spudsoft.query.exec;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.defn.Argument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import uk.co.spudsoft.query.defn.DataType;

/**
 *
 * @author jtalbut
 */
public class ArgumentInstanceTest {
  
  @Test
  public void testConstructor1() {
    Argument argument = Argument.builder().name("arg").type(DataType.Integer).build();
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
      new ArgumentInstance(argument, ImmutableList.of("one"));
    });
    assertEquals("Argument arg value could not be parsed as Integer", ex.getMessage());
  }
  
  @Test
  public void testConstructor2() {
    Argument argument = Argument.builder().name("arg").type(DataType.Integer).build();
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
      new ArgumentInstance(argument, ImmutableList.of("1", "two"));
    });
    assertEquals("Argument arg value number 2 could not be parsed as Integer", ex.getMessage());
  }
  
  @Test
  public void testGetDefinition() {
    Argument argument = Argument.builder().optional(true).build();
    ArgumentInstance instance = new ArgumentInstance(argument, ImmutableList.of("value"));
    assertEquals(argument, instance.getDefinition());
  }

  @Test
  public void testGetValue() {
    Argument argument = Argument.builder().optional(true).build();
    ArgumentInstance instance = new ArgumentInstance(argument, ImmutableList.of("value"));
    assertEquals("value", instance.getValues().get(0));
  }

  @Test
  public void testGetName() {
    Argument argument = Argument.builder().name("name").optional(true).build();
    ArgumentInstance instance = new ArgumentInstance(argument, ImmutableList.of("value"));
    assertEquals("name", instance.getName());
  }

  @Test
  public void testGetMultiValue() {
    Argument argument = Argument.builder().optional(true).multiValued(true).build();
    ArgumentInstance instance = new ArgumentInstance(argument, null);
    assertEquals(0, instance.getValues().size());
    argument = Argument.builder().optional(false).multiValued(true).build();
    assertThrows(IllegalArgumentException.class, () -> {
      Argument required = Argument.builder().optional(false).multiValued(true).build();
      new ArgumentInstance(required, null);
    });
    assertEquals(0, instance.getValues().size());
    instance = new ArgumentInstance(argument, ImmutableList.of("value"));
    assertEquals("value", instance.getValues().get(0));
    assertEquals(1, instance.getValues().size());
    instance = new ArgumentInstance(argument, ImmutableList.of("value", "value2"));
    assertEquals("value", instance.getValues().get(0));
    assertEquals("value2", instance.getValues().get(1));
    assertEquals(2, instance.getValues().size());
  }
  
  @Test
  public void testGetSingleValue() {
    Argument argument = Argument.builder().optional(true).multiValued(false).build();
    ArgumentInstance instance = new ArgumentInstance(argument, null);
    assertEquals(0, instance.getValues().size());
    assertThrows(IllegalArgumentException.class, () -> {
      Argument required = Argument.builder().optional(false).multiValued(false).build();
      new ArgumentInstance(required, null);
    });
    instance = new ArgumentInstance(argument, ImmutableList.of("value"));
    assertEquals("value", instance.getValues().get(0));
    assertEquals(1, instance.getValues().size());
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      new ArgumentInstance(argument, ImmutableList.of("value", "value2"));
    });
  }
  
}
