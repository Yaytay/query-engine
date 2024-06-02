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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
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
    assertEquals(DataType.String, instance.getType());
    instance = Argument.builder().type(DataType.Long).build();
    assertEquals(DataType.Long, instance.getType());
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
  public void testIsValidate() {
    Argument instance = Argument.builder().build();
    assertTrue(instance.isValidate());
    instance = Argument.builder().validate(false).build();
    assertFalse(instance.isValidate());
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
    assertNull(instance.getDefaultValueExpression());
    instance = Argument.builder().defaultValueExpression("seven").build();
    assertEquals("seven", instance.getDefaultValueExpression());
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
    assertEquals("The argument \"£$%^\" does not have a valid name."
            , assertThrows(IllegalArgumentException.class
                    , () -> Argument.builder().name("£$%^").build().validate()
            ).getMessage()
    );
    
    
    Argument instance2 = Argument.builder().name("name").build();
    instance2.validate();
    
    assertThat(
            assertThrows(IllegalArgumentException.class
                    , () -> Argument.builder().name("name").permittedValuesRegex("[").build().validate()
            ).getMessage()
            , startsWith("The argument \"name\" does not have a valid permittedValuesRegex.Unclosed character class near index 0")
    );
    Argument.builder().name("name").permittedValuesRegex("[A-Za-z]+").build().validate();
    
    assertEquals("The argument \"arg\" has a minimum value of \"fred\" but that cannot be parsed as Integer."
            , assertThrows(IllegalArgumentException.class
                    , () -> Argument.builder().name("arg").type(DataType.Integer).minimumValue("fred").build().validate()
            ).getMessage()
    );
    assertEquals("The argument \"arg\" has a maximum value of \"fred\" but that cannot be parsed as Integer."
            , assertThrows(IllegalArgumentException.class
                    , () -> Argument.builder().name("arg").type(DataType.Integer).maximumValue("fred").build().validate()
            ).getMessage()
    );
    assertEquals("The argument \"arg\" has a default value specified, but is not optional or conditional."
            , assertThrows(IllegalArgumentException.class
                    , () -> Argument.builder().name("arg").type(DataType.Integer).defaultValueExpression("7").optional(false).build().validate()
            ).getMessage()
    );
    
  }

}
