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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * @author jtalbut
 */
public class ProcessorExpressionTest {
  
  @Test
  public void testSetType() {
    ProcessorExpression instance = ProcessorExpression.builder().build();
    assertEquals(ProcessorType.EXPRESSION, instance.getType());
    try {
      ProcessorExpression.builder().type(ProcessorType.GROUP_CONCAT).build();
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
    }
  }
  
  @Test
  public void testGetId() {
    ProcessorExpression instance = ProcessorExpression.builder().build();
    assertNull(instance.getId());
    instance = ProcessorExpression.builder().id("one").build();
    assertEquals("one", instance.getId());
  }

  @Test
  public void testValidate() {
    assertThrows(IllegalArgumentException.class, () -> {
      ProcessorExpression.builder().field("field").build().validate();
    }, "");
    assertThrows(IllegalArgumentException.class, () -> {
      ProcessorExpression.builder().field("field").fieldValue("2").fieldType(null).build().validate();
    }, "");
    assertThrows(IllegalArgumentException.class, () -> {
      ProcessorExpression.builder().fieldValue("7").build().validate();
    }, "");
    assertThrows(IllegalArgumentException.class, () -> {
      ProcessorExpression.builder().build().validate();
    }, "");
    ProcessorExpression.builder().predicate("true").build().validate();
  }
  
}
