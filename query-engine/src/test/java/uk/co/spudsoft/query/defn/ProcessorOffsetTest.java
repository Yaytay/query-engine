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
public class ProcessorOffsetTest {
  
  @Test
  public void testGetType() {
    ProcessorOffset instance = ProcessorOffset.builder().build();
    assertEquals(ProcessorType.OFFSET, instance.getType());
  }

  @Test
  public void testSetType() {
    ProcessorOffset instance = ProcessorOffset.builder().type(ProcessorType.OFFSET).build();
    assertEquals(ProcessorType.OFFSET, instance.getType());
    try {
      ProcessorOffset.builder().type(ProcessorType.SCRIPT).build();
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
    }
  }

  @Test
  public void testGetOffset() {
    ProcessorOffset instance = ProcessorOffset.builder().offset(17).build();
    assertEquals(17, instance.getOffset());
  }

  @Test
  public void testValidate() {
    assertThrows(IllegalArgumentException.class, () -> {
      ProcessorOffset.builder().offset(0).build().validate();
    }, "Zero offset provided");
    assertThrows(IllegalArgumentException.class, () -> {
      ProcessorOffset.builder().offset(-1).build().validate();
    }, "Negative offset provided");
    ProcessorOffset.builder().offset(1).build().validate();
  }
  
}
