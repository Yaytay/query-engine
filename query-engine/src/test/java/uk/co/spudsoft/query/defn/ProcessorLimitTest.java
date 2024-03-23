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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * @author jtalbut
 */
public class ProcessorLimitTest {
  
  @Test
  public void testGetId() {
    ProcessorLimit instance = ProcessorLimit.builder().id("id").build();
    assertEquals("id", instance.getId());
  }
  
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
    } catch (IllegalArgumentException ex) {
    }
  }

  @Test
  public void testGetLimit() {
    ProcessorLimit instance = ProcessorLimit.builder().limit(17).build();
    assertEquals(17, instance.getLimit());
  }

  @Test
  public void testValidate() {
    assertThrows(IllegalArgumentException.class, () -> {
      ProcessorLimit.builder().limit(0).build().validate();
    }, "Zero limit provided");
    assertThrows(IllegalArgumentException.class, () -> {
      ProcessorLimit.builder().limit(-1).build().validate();
    }, "Negative limit provided");
    ProcessorLimit.builder().limit(1).build().validate();
  }
  
}
