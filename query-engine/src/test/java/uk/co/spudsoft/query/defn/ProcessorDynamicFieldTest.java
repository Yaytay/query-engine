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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * @author jtalbut
 */
public class ProcessorDynamicFieldTest {
  
  @Test
  public void testGetId() {
    ProcessorDynamicField instance = ProcessorDynamicField.builder().id("id").build();
    assertEquals("id", instance.getId());
  }
  
  @Test
  public void testValidate() {
    ProcessorDynamicField instance = ProcessorDynamicField.builder().build();
    assertThrows(IllegalArgumentException.class, () -> {
      instance.validate();
    });
  }
    
  @Test
  public void testValidateNoFieldValues() {
    ProcessorDynamicField instance = ProcessorDynamicField.builder()
            .fieldDefns(
                    SourcePipeline.builder()
                            .source(
                                    SourceTest.builder().name("test").build()
                            )
                            .build()
            )
            .build();
    assertEquals("Field values (fieldValues) pipeline not provided"
            , assertThrows(IllegalArgumentException.class, () -> {
              instance.validate();
            }).getMessage()
    );
  }
    
  @Test
  public void testValidateNoParentIdColumns() {    
    ProcessorDynamicField instance = ProcessorDynamicField.builder()
            .fieldDefns(
                    SourcePipeline.builder()
                            .source(
                                    SourceTest.builder().name("test").build()
                            )
                            .build()
            )
            .fieldValues(
                    SourcePipeline.builder()
                            .source(
                                    SourceTest.builder().name("test").build()
                            )
                            .build()
            )
            .build();
    assertEquals("ID column(s) not specified for parent stream"
            , assertThrows(IllegalArgumentException.class, () -> {
              instance.validate();
            }).getMessage()
    );
  }
    
  @Test
  public void testValidateNoChildIdColumns() {    
    ProcessorDynamicField instance = ProcessorDynamicField.builder()
            .fieldDefns(
                    SourcePipeline.builder()
                            .source(
                                    SourceTest.builder().name("test").build()
                            )
                            .build()
            )
            .fieldValues(
                    SourcePipeline.builder()
                            .source(
                                    SourceTest.builder().name("test").build()
                            )
                            .build()
            )
            .parentIdColumns(Arrays.asList("id"))
            .build();
    assertEquals("ID column(s) not specified for values stream"
            , assertThrows(IllegalArgumentException.class, () -> {
              instance.validate();
            }).getMessage()
    );
  }
    
  @Test
  public void testValidateMismatchedIdColumns() {    
    ProcessorDynamicField instance = ProcessorDynamicField.builder()
            .fieldDefns(
                    SourcePipeline.builder()
                            .source(
                                    SourceTest.builder().name("test").build()
                            )
                            .build()
            )
            .fieldValues(
                    SourcePipeline.builder()
                            .source(
                                    SourceTest.builder().name("test").build()
                            )
                            .build()
            )
            .parentIdColumns(Arrays.asList("id"))
            .valuesParentIdColumns(Arrays.asList("id1", "id2"))
            .build();
    assertEquals("ID column(s) specified for parent stream does not have the same number of fields as those specified for values stream"
            , assertThrows(IllegalArgumentException.class, () -> {
              instance.validate();
            }).getMessage()
    );
  }
    
  @Test
  public void testValidateTypeIdColumnNotSet() {    
    ProcessorDynamicField instance = ProcessorDynamicField.builder()
            .fieldDefns(
                    SourcePipeline.builder()
                            .source(
                                    SourceTest.builder().name("test").build()
                            )
                            .build()
            )
            .fieldValues(
                    SourcePipeline.builder()
                            .source(
                                    SourceTest.builder().name("test").build()
                            )
                            .build()
            )
            .parentIdColumns(Arrays.asList("id"))
            .valuesParentIdColumns(Arrays.asList("id1"))
            .fieldTypeColumn(null)
            .build();
    assertEquals("Type column not set (fieldTypeColumn)"
            , assertThrows(IllegalArgumentException.class, () -> {
              instance.validate();
            }).getMessage()
    );
  }
    
  @Test
  public void testValidateGood() {
    ProcessorDynamicField instance = ProcessorDynamicField.builder()
            .fieldDefns(
                    SourcePipeline.builder()
                            .source(
                                    SourceTest.builder().name("test").build()
                            )
                            .build()
            )
            .fieldValues(
                    SourcePipeline.builder()
                            .source(
                                    SourceTest.builder().name("test").build()
                            )
                            .build()
            )
            .parentIdColumns(Arrays.asList("id"))
            .valuesParentIdColumns(Arrays.asList("id"))
            .build();
    instance.validate();
  }

  @Test
  public void testGetType() {
    ProcessorDynamicField instance = ProcessorDynamicField.builder().build();
    assertEquals(ProcessorType.DYNAMIC_FIELD, instance.getType());
  }

  @Test
  public void testSetType() {
    ProcessorDynamicField instance = ProcessorDynamicField.builder().type(ProcessorType.DYNAMIC_FIELD).build();
    assertEquals(ProcessorType.DYNAMIC_FIELD, instance.getType());
    try {
      ProcessorDynamicField.builder().type(ProcessorType.LIMIT).build();
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
    }
  }

  @Test
  public void testDefaults() {
    ProcessorDynamicField instance = ProcessorDynamicField.builder().build();
    assertEquals(false, instance.isInnerJoin());
    assertEquals("column", instance.getFieldColumnColumn());
    assertEquals(null, instance.getFieldDefns());
    assertEquals("id", instance.getFieldIdColumn());
    assertEquals("name", instance.getFieldNameColumn());
    assertEquals("type", instance.getFieldTypeColumn());
    assertEquals(null, instance.getFieldValues());
    assertThat(instance.getParentIdColumns(), hasSize(0));
    assertEquals(ProcessorType.DYNAMIC_FIELD, instance.getType());
    assertEquals(null, instance.getValuesFieldIdColumn());
    assertThat(instance.getValuesParentIdColumns(), hasSize(0));
  }

  @Test
  public void testIsInnerJoin() {
    ProcessorDynamicField instance = ProcessorDynamicField.builder().innerJoin(true).build();
    assertEquals(true, instance.isInnerJoin());
  }

  @Test
  public void testGetParentIdColumn() {
    ProcessorDynamicField instance = ProcessorDynamicField.builder().parentIdColumns(Arrays.asList("parentId")).build();
    assertEquals(Arrays.asList("parentId"), instance.getParentIdColumns());
  }

  @Test
  public void testGetFieldIdColumn() {
    ProcessorDynamicField instance = ProcessorDynamicField.builder().fieldIdColumn("fieldIdColumn").build();
    assertEquals("fieldIdColumn", instance.getFieldIdColumn());
  }

  @Test
  public void testGetFieldNameColumn() {
    ProcessorDynamicField instance = ProcessorDynamicField.builder().fieldNameColumn("fieldNameColumn").build();
    assertEquals("fieldNameColumn", instance.getFieldNameColumn());
  }

  @Test
  public void testGetFieldTypeColumn() {
    ProcessorDynamicField instance = ProcessorDynamicField.builder().fieldTypeColumn("fieldTypeColumn").build();
    assertEquals("fieldTypeColumn", instance.getFieldTypeColumn());
  }

  @Test
  public void testGetFieldColumnColumn() {
    ProcessorDynamicField instance = ProcessorDynamicField.builder().fieldColumnColumn("fieldColumnColumn").build();
    assertEquals("fieldColumnColumn", instance.getFieldColumnColumn());
  }

  @Test
  public void testGetValuesParentIdColumn() {
    ProcessorDynamicField instance = ProcessorDynamicField.builder().valuesParentIdColumns(Arrays.asList("valuesParentIdColumns")).build();
    assertEquals(Arrays.asList("valuesParentIdColumns"), instance.getValuesParentIdColumns());
  }

  @Test
  public void testGetValuesFieldIdColumn() {
    ProcessorDynamicField instance = ProcessorDynamicField.builder().valuesFieldIdColumn("valuesFieldIdColumn").build();
    assertEquals("valuesFieldIdColumn", instance.getValuesFieldIdColumn());
  }


}
