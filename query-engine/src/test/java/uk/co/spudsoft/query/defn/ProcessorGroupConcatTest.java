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
  public void testValidate() {
    assertEquals("Processor of type GROUP_CONCAT configured with type MERGE", assertThrows(IllegalArgumentException.class, () -> {
      ProcessorGroupConcat.builder().type(ProcessorType.MERGE).build().validate();
    }).getMessage());
    assertEquals("ID column(s) not specified for parent stream", assertThrows(IllegalArgumentException.class, () -> {
      ProcessorGroupConcat.builder().input(SourcePipeline.builder().source(SourceTest.builder().build()).build()).build().validate();
    }).getMessage());
    assertEquals("ID column(s) not specified for child stream", assertThrows(IllegalArgumentException.class, () -> {
      ProcessorGroupConcat.builder().parentIdColumns(Arrays.asList("one")).input(SourcePipeline.builder().source(SourceTest.builder().build()).build()).build().validate();
    }).getMessage());
    assertEquals("ID column(s) specified for parent stream does not have the same number of fields as those specified for input stream", assertThrows(IllegalArgumentException.class, () -> {
      ProcessorGroupConcat.builder().parentIdColumns(Arrays.asList("one")).childIdColumns(Arrays.asList("two", "three")).input(SourcePipeline.builder().source(SourceTest.builder().build()).build()).build().validate();
    }).getMessage());
    assertEquals("The parentValueColumn name is specified, but the childValueColumn is not", assertThrows(IllegalArgumentException.class, () -> {
      ProcessorGroupConcat.builder().parentIdColumns(Arrays.asList("one")).childIdColumns(Arrays.asList("one")).parentValueColumn("parentValue").input(SourcePipeline.builder().source(SourceTest.builder().build()).build()).build().validate();
    }).getMessage());
    ProcessorGroupConcat.builder().parentIdColumns(Arrays.asList("one")).childIdColumns(Arrays.asList("two")).input(SourcePipeline.builder().source(SourceTest.builder().build()).build()).build().validate();
    ProcessorGroupConcat.builder().parentIdColumns(Arrays.asList("one")).childIdColumns(Arrays.asList("two")).childValueColumn("childValue").input(SourcePipeline.builder().source(SourceTest.builder().build()).build()).build().validate();
    ProcessorGroupConcat.builder().parentIdColumns(Arrays.asList("one")).childIdColumns(Arrays.asList("two")).childValueColumn("childValue").parentValueColumn("parentValue").input(SourcePipeline.builder().source(SourceTest.builder().build()).build()).build().validate();
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
