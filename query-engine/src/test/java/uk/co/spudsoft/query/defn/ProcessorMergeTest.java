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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * @author jtalbut
 */
public class ProcessorMergeTest {
  
  @Test
  public void testGetType() {
    ProcessorMerge instance = ProcessorMerge.builder().build();
    assertEquals(ProcessorType.MERGE, instance.getType());
  }

  @Test
  public void testSetType() {
    ProcessorMerge instance = ProcessorMerge.builder().type(ProcessorType.MERGE).build();
    assertEquals(ProcessorType.MERGE, instance.getType());
    try {
      ProcessorMerge.builder().type(ProcessorType.LIMIT).build();
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
    }
  }

  @Test
  public void testValidate() {
    assertEquals("Processor of type MERGE configured with type GROUP_CONCAT", assertThrows(IllegalArgumentException.class, () -> {
      ProcessorMerge.builder().type(ProcessorType.GROUP_CONCAT).build().validate();
    }).getMessage());
    assertEquals("ID column(s) not specified for parent stream", assertThrows(IllegalArgumentException.class, () -> {
      ProcessorMerge.builder().build().validate();
    }).getMessage());
    assertEquals("ID column(s) not specified for child stream", assertThrows(IllegalArgumentException.class, () -> {
      ProcessorMerge.builder().parentIdColumns(Arrays.asList("one")).build().validate();
    }).getMessage());
    assertEquals("ID column(s) specified for parent stream does not have the same number of fields as those specified for input stream", assertThrows(IllegalArgumentException.class, () -> {
      ProcessorMerge.builder().parentIdColumns(Arrays.asList("one")).childIdColumns(Arrays.asList("two", "three")).build().validate();
    }).getMessage());
    ProcessorMerge.builder().parentIdColumns(Arrays.asList("one")).childIdColumns(Arrays.asList("two")).build().validate();
  }
  
  @Test
  public void testIsInnerJoin() {
    ProcessorMerge instance = ProcessorMerge.builder().innerJoin(true).build();
    assertEquals(true, instance.isInnerJoin());
  }

  @Test
  public void testGetParentIdColumn() {
    ProcessorMerge instance = ProcessorMerge.builder().parentIdColumns(Arrays.asList("parentId")).build();
    assertEquals(Arrays.asList("parentId"), instance.getParentIdColumns());
  }

  @Test
  public void testGetChildIdColumn() {
    ProcessorMerge instance = ProcessorMerge.builder().childIdColumns(Arrays.asList("childId")).build();
    assertEquals(Arrays.asList("childId"), instance.getChildIdColumns());
  }

  @Test
  public void testGetDelimiter() {
    ProcessorMerge instance = ProcessorMerge.builder().delimiter("delimiter").build();
    assertEquals("delimiter", instance.getDelimiter());
  }

}
