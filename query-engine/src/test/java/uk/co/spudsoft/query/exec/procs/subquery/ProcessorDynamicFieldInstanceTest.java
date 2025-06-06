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
package uk.co.spudsoft.query.exec.procs.subquery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import uk.co.spudsoft.query.defn.ProcessorDynamicField;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.exec.ColumnDefn;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.procs.subquery.ProcessorDynamicFieldInstance.FieldDefn;

/**
 *
 * @author jtalbut
 */
public class ProcessorDynamicFieldInstanceTest {
  
  @Test
  public void testGetId() {
    ProcessorDynamicField defn = ProcessorDynamicField.builder().name("id").build();
    ProcessorDynamicFieldInstance instance = new ProcessorDynamicFieldInstance(null, null, null, defn, "P0-DynamicField");
    assertEquals("P0-DynamicField", instance.getName());
  }

  @Test
  public void testCaseSensitive() {
    ProcessorDynamicField defn = ProcessorDynamicField.builder()
            .useCaseInsensitiveFieldNames(false)
            .valuesFieldIdColumn("fieldId")
            .build();
    ProcessorDynamicFieldInstance instance = new ProcessorDynamicFieldInstance(null, null, null, defn, "P0-DynamicField"
            , Arrays.asList(
                    new FieldDefn(0, "field", "field", DataType.String, "stringValue")
                    , new FieldDefn(1, "Field", "Field", DataType.String, "stringValue")
            )
    );
    
    Types parentTypes = new Types();
    DataRow parent = DataRow.create(parentTypes).put("id", "one");

    Types childTypes = new Types();
    List<DataRow> children = Arrays.asList(
            DataRow.create(childTypes).put("fieldId", 0).put("stringValue", "first")
            , DataRow.create(childTypes).put("fieldId", 1).put("stringValue", "second")
    );
    
    DataRow result = instance.processChildren(parent, children);
    assertEquals(3, result.size());
    assertEquals("one", result.get("id"));
    assertEquals("first", result.get("field"));
    assertEquals("second", result.get("Field"));
    
    List<ColumnDefn> cds = new ArrayList<>();
    List<Object> values = new ArrayList<>();
    result.forEach((cd, value) -> {
      cds.add(cd);
      values.add(value);
    });
    assertEquals(Arrays.asList("id", "field", "Field"), cds.stream().map(cd -> cd.name()).collect(Collectors.toList()));
    assertEquals(Arrays.asList("one", "first", "second"), values);
  }
  
  @Test
  public void testCaseInsensitive() {
    ProcessorDynamicField defn = ProcessorDynamicField.builder()
            .useCaseInsensitiveFieldNames(true)
            .valuesFieldIdColumn("fieldId")
            .build();
    ProcessorDynamicFieldInstance instance = new ProcessorDynamicFieldInstance(null, null, null, defn, "P0-DynamicField"
            , Arrays.asList(
                    new FieldDefn(0, "field", "field", DataType.String, "stringValue")
                    , new FieldDefn(1, "field", "Field", DataType.String, "stringValue")
            )
    );
    
    Types parentTypes = new Types();
    DataRow parent = DataRow.create(parentTypes).put("id", "one");

    Types childTypes = new Types();
    List<DataRow> children = Arrays.asList(
            DataRow.create(childTypes).put("fieldId", 0).put("stringValue", "first")
            , DataRow.create(childTypes).put("fieldId", 1).put("stringValue", "second")
    );
    
    DataRow result = instance.processChildren(parent, children);
    assertEquals(2, result.size());
    assertEquals("one", result.get("id"));
    assertEquals("second", result.get("field"));

    
    List<ColumnDefn> cds = new ArrayList<>();
    List<Object> values = new ArrayList<>();
    result.forEach((cd, value) -> {
      cds.add(cd);
      values.add(value);
    });
    assertEquals(Arrays.asList("id", "field"), cds.stream().map(cd -> cd.name()).collect(Collectors.toList()));
    assertEquals(Arrays.asList("one", "second"), values);    
  }
  
  @Test
  public void testCaseInsensitiveBackwards() {
    ProcessorDynamicField defn = ProcessorDynamicField.builder()
            .useCaseInsensitiveFieldNames(true)
            .valuesFieldIdColumn("fieldId")
            .build();
    ProcessorDynamicFieldInstance instance = new ProcessorDynamicFieldInstance(null, null, null, defn, "P0-DynamicField"
            , Arrays.asList(
                    new FieldDefn(1, "field", "Field", DataType.String, "stringValue")
                    , new FieldDefn(0, "field", "field", DataType.String, "stringValue")
            )
    );
    
    Types parentTypes = new Types();
    DataRow parent = DataRow.create(parentTypes).put("id", "one");

    Types childTypes = new Types();
    List<DataRow> children = Arrays.asList(
            DataRow.create(childTypes).put("fieldId", 1).put("stringValue", "second")
            , DataRow.create(childTypes).put("fieldId", 0).put("stringValue", "first")
    );
    
    DataRow result = instance.processChildren(parent, children);
    assertEquals(2, result.size());
    assertEquals("one", result.get("id"));
    assertEquals("first", result.get("field"));

    
    List<ColumnDefn> cds = new ArrayList<>();
    List<Object> values = new ArrayList<>();
    result.forEach((cd, value) -> {
      cds.add(cd);
      values.add(value);
    });
    assertEquals(Arrays.asList("id", "Field"), cds.stream().map(cd -> cd.name()).collect(Collectors.toList()));
    assertEquals(Arrays.asList("one", "first"), values);    
  }
  
  @Test
  public void testCaseInsensitiveReturningUppercase() {
    ProcessorDynamicField defn = ProcessorDynamicField.builder()
            .useCaseInsensitiveFieldNames(true)
            .valuesFieldIdColumn("fieldId")
            .build();
    ProcessorDynamicFieldInstance instance = new ProcessorDynamicFieldInstance(null, null, null, defn, "P0-DynamicField"
            , Arrays.asList(
                    new FieldDefn(0, "field", "Field", DataType.String, "stringValue")
                    , new FieldDefn(1, "field", "field", DataType.String, "stringValue")
            )
    );
    
    Types parentTypes = new Types();
    DataRow parent = DataRow.create(parentTypes).put("id", "one");

    Types childTypes = new Types();
    List<DataRow> children = Arrays.asList(
            DataRow.create(childTypes).put("fieldId", 1).put("stringValue", "second")
            , DataRow.create(childTypes).put("fieldId", 0).put("stringValue", "first")
    );
    
    DataRow result = instance.processChildren(parent, children);
    assertEquals(2, result.size());
    assertEquals("one", result.get("id"));
    // Case insensitive always have lowercase key
    // it finds second because field ID 1 comes after field ID 0, regardless of the order of the child rows
    assertEquals("second", result.get("field"));

    
    List<ColumnDefn> cds = new ArrayList<>();
    List<Object> values = new ArrayList<>();
    result.forEach((cd, value) -> {
      cds.add(cd);
      values.add(value);
    });
    assertEquals(Arrays.asList("id", "Field"), cds.stream().map(cd -> cd.name()).collect(Collectors.toList()));
    assertEquals(Arrays.asList("one", "second"), values);    
  }
}
