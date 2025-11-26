/*
 * Copyright (C) 2024 jtalbut
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
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

/**
 *
 * @author jtalbut
 */
public class ProcessorSortTest {
  
  @Test
  public void testGetType() {
    ProcessorSort instance = ProcessorSort.builder().build();
    assertEquals(ProcessorType.SORT, instance.getType());
  }

  @Test
  public void testSetType() {
    ProcessorSort instance = ProcessorSort.builder().type(ProcessorType.SORT).build();
    assertEquals(ProcessorType.SORT, instance.getType());
    try {
      ProcessorSort.builder().type(ProcessorType.SCRIPT).build();
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
    }
  }

  @Test
  public void testGetSort() {
    ProcessorSort instance = ProcessorSort.builder().fields(Arrays.asList("x", "y")).build();
    assertEquals(Arrays.asList("x", "y"), instance.getFields());
  }

  @Test
  public void testValidate() {
    assertThrows(IllegalArgumentException.class, () -> {
      ProcessorSort.builder().fields(Collections.emptyList()).build().validate(null);
    }, "No fields provided for sorting");
    assertThrows(IllegalArgumentException.class, () -> {
      ProcessorSort.builder().fields(null).build().validate(null);
    }, "No fields provided for sorting");
    assertThrows(IllegalArgumentException.class, () -> {
      ProcessorSort.builder().build().validate(null);
    }, "Negative limit provided");
    ProcessorSort.builder().fields(Arrays.asList("xz")).build().validate(null);
  }
  
}
