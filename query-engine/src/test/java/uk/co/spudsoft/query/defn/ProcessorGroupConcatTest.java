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
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * @author jtalbut
 */
public class ProcessorGroupConcatTest {
  
  @Test
  public void testGetType() {
    ProcessorGroupConcat instance = ProcessorGroupConcat.builder().build();
    assertEquals(ProcessorType.GROUP_CONCAT, instance.getType());
  }

  @Test
  public void testSetType() {
    ProcessorGroupConcat instance = ProcessorGroupConcat.builder().type(ProcessorType.GROUP_CONCAT).build();
    assertEquals(ProcessorType.GROUP_CONCAT, instance.getType());
    try {
      ProcessorGroupConcat.builder().type(ProcessorType.LIMIT).build();
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
    }
  }

  @Test
  public void testIsInnerJoin() {
    ProcessorGroupConcat instance = ProcessorGroupConcat.builder().innerJoin(true).build();
    assertEquals(true, instance.isInnerJoin());
  }

  @Test
  public void testGetParentIdColumn() {
    ProcessorGroupConcat instance = ProcessorGroupConcat.builder().parentIdColumns(Arrays.asList("parentId")).build();
    assertEquals(Arrays.asList("parentId"), instance.getParentIdColumns());
  }

  @Test
  public void testGetChildIdColumn() {
    ProcessorGroupConcat instance = ProcessorGroupConcat.builder().childIdColumns(Arrays.asList("childId")).build();
    assertEquals(Arrays.asList("childId"), instance.getChildIdColumns());
  }

  @Test
  public void testGetChildValueColumn() {
    ProcessorGroupConcat instance = ProcessorGroupConcat.builder().childValueColumn("childValue").build();
    assertEquals("childValue", instance.getChildValueColumn());
  }

  @Test
  public void testGetParentValueColumn() {
    ProcessorGroupConcat instance = ProcessorGroupConcat.builder().parentValueColumn("parentValue").build();
    assertEquals("parentValue", instance.getParentValueColumn());
  }

  @Test
  public void testGetDelimiter() {
    ProcessorGroupConcat instance = ProcessorGroupConcat.builder().delimiter("delimiter").build();
    assertEquals("delimiter", instance.getDelimiter());
  }

}
