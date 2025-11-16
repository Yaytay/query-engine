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

import inet.ipaddr.IPAddressString;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Vertx;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import uk.co.spudsoft.query.defn.ProcessorDynamicField;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.exec.Auditor;
import uk.co.spudsoft.query.exec.ColumnDefn;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.exec.procs.subquery.ProcessorDynamicFieldInstance.FieldDefn;
import static uk.co.spudsoft.query.exec.procs.subquery.ProcessorDynamicFieldInstance.rowToFieldDefn;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;

/**
 *
 * @author jtalbut
 */
public class ProcessorDynamicFieldInstanceTest {

  @Test
  public void testGetId() {
    ProcessorDynamicField defn = ProcessorDynamicField.builder().name("id").build();
    ProcessorDynamicFieldInstance instance = new ProcessorDynamicFieldInstance(null, null, null, null, defn, "P0-DynamicField");
    assertEquals("P0-DynamicField", instance.getName());
  }

  private static class ProcessorDynamicFieldInstanceTester extends ProcessorDynamicFieldInstance {

    /**
     * Constructor allowing manual specification of fields for testing.
     *
     * @param vertx the Vert.x instance.
     * @param meterRegistry
     * @param auditor
     * @param pipelineContext
     * @param definition
     * @param name
     * @param fields 
     */
    ProcessorDynamicFieldInstanceTester(Vertx vertx, MeterRegistry meterRegistry, Auditor auditor, PipelineContext pipelineContext, ProcessorDynamicField definition, String name, List<FieldDefn> fields) {
      super(vertx, meterRegistry, auditor, pipelineContext, definition, name);
      this.fields = ImmutableCollectionTools.copy(fields);
    }

  }
  
  @Test
  public void testCaseSensitive() {
    ProcessorDynamicField defn = ProcessorDynamicField.builder()
            .useCaseInsensitiveFieldNames(false)
            .valuesFieldIdColumn("fieldId")
            .build();
    ProcessorDynamicFieldInstance instance = new ProcessorDynamicFieldInstanceTester(null, null, null, null, defn, "P0-DynamicField",
             Arrays.asList(
                    new FieldDefn(0, "field", "field", DataType.String, "stringValue"),
                     new FieldDefn(1, "Field", "Field", DataType.String, "stringValue")
            )
    );

    Types parentTypes = new Types();
    DataRow parent = DataRow.create(parentTypes).put("id", "one");

    Types childTypes = new Types();
    List<DataRow> children = Arrays.asList(
            DataRow.create(childTypes).put("fieldId", 0).put("stringValue", "first"),
             DataRow.create(childTypes).put("fieldId", 1).put("stringValue", "second")
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
    ProcessorDynamicFieldInstance instance = new ProcessorDynamicFieldInstanceTester(null, null, null, null, defn, "P0-DynamicField",
             Arrays.asList(
                    new FieldDefn(0, "field", "field", DataType.String, "stringValue"),
                     new FieldDefn(1, "field", "Field", DataType.String, "stringValue")
            )
    );

    Types parentTypes = new Types();
    DataRow parent = DataRow.create(parentTypes).put("id", "one");

    Types childTypes = new Types();
    List<DataRow> children = Arrays.asList(
            DataRow.create(childTypes).put("fieldId", 0).put("stringValue", "first"),
             DataRow.create(childTypes).put("fieldId", 1).put("stringValue", "second")
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
    ProcessorDynamicFieldInstance instance = new ProcessorDynamicFieldInstanceTester(null, null, null, null, defn, "P0-DynamicField", 
             Arrays.asList(
                    new FieldDefn(1, "field", "Field", DataType.String, "stringValue"),
                     new FieldDefn(0, "field", "field", DataType.String, "stringValue")
            )
    );

    Types parentTypes = new Types();
    DataRow parent = DataRow.create(parentTypes).put("id", "one");

    Types childTypes = new Types();
    List<DataRow> children = Arrays.asList(
            DataRow.create(childTypes).put("fieldId", 1).put("stringValue", "second"),
             DataRow.create(childTypes).put("fieldId", 0).put("stringValue", "first")
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
    ProcessorDynamicFieldInstance instance = new ProcessorDynamicFieldInstanceTester(null, null, null, null, defn, "P0-DynamicField",
             Arrays.asList(
                    new FieldDefn(0, "field", "Field", DataType.String, "stringValue"),
                     new FieldDefn(1, "field", "field", DataType.String, "stringValue")
            )
    );

    Types parentTypes = new Types();
    DataRow parent = DataRow.create(parentTypes).put("id", "one");

    Types childTypes = new Types();
    List<DataRow> children = Arrays.asList(
            DataRow.create(childTypes).put("fieldId", 1).put("stringValue", "second"),
             DataRow.create(childTypes).put("fieldId", 0).put("stringValue", "first")
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

  @Test
  public void testCaseInsensitiveOnlyFirst() {
    ProcessorDynamicField defn = ProcessorDynamicField.builder()
            .useCaseInsensitiveFieldNames(true)
            .valuesFieldIdColumn("fieldId")
            .build();
    ProcessorDynamicFieldInstance instance = new ProcessorDynamicFieldInstanceTester(null, null, null, null, defn, "P0-DynamicField",
             Arrays.asList(
                    new FieldDefn(0, "field", "Field", DataType.String, "stringValue"),
                     new FieldDefn(1, "field", "field", DataType.String, "stringValue")
            )
    );

    Types parentTypes = new Types();
    DataRow parent = DataRow.create(parentTypes).put("id", "one");

    Types childTypes = new Types();
    List<DataRow> children = Arrays.asList(
            DataRow.create(childTypes).put("fieldId", 0).put("stringValue", "first")
    );

    DataRow result = instance.processChildren(parent, children);
    assertEquals(2, result.size());
    assertEquals("one", result.get("id"));
    // Case insensitive always have lowercase key
    // it finds second because field ID 1 comes after field ID 0, regardless of the order of the child rows
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
  public void testCast() {
    RequestContext requestContext = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", requestContext);
    
    ProcessorDynamicField defn = ProcessorDynamicField.builder()
            .build();
    ProcessorDynamicFieldInstance instance = new ProcessorDynamicFieldInstanceTester(null, null, null, pipelineContext, defn, "P0-DynamicField",
             Arrays.asList(
                    new FieldDefn(0, "field", "Field", DataType.String, "stringValue"),
                     new FieldDefn(1, "field", "field", DataType.String, "stringValue")
            )
    );
    
    assertEquals(LocalDateTime.of(2022, Month.OCTOBER, 07, 0, 0, 0), instance.castValue("2022-10-07 00:00:00", new FieldDefn("id", "key", "field", DataType.DateTime, "column")));
    assertEquals(Boolean.TRUE, instance.castValue(1, new FieldDefn("id", "key", "field", DataType.Boolean, "column")));
    assertEquals(Boolean.FALSE, instance.castValue(0, new FieldDefn("id", "key", "field", DataType.Boolean, "column")));
    assertEquals(Boolean.TRUE, instance.castValue("1", new FieldDefn("id", "key", "field", DataType.Boolean, "column")));
    assertEquals(Boolean.FALSE, instance.castValue("0", new FieldDefn("id", "key", "field", DataType.Boolean, "column")));
  }

  @Test
  void testRowToFieldDefn_AllBranches() {
    String id = "ID", name = "NAME", type = "TYPE", column = "COL";
    ProcessorDynamicField def = ProcessorDynamicField.builder()
            .fieldIdColumn(id)
            .fieldNameColumn(name)
            .fieldTypeColumn(type)
            .fieldColumnColumn(column)
            .build();
    
    // 1. Empty row
    DataRow r = DataRow.EMPTY_ROW;
    assertNull(ProcessorDynamicFieldInstance.rowToFieldDefn(def, r), "Empty row should return null");

    Types types = new Types();
    
    // 2. Missing id
    r = DataRow.create(types);
    r.put(name, "n");
    r.put(type, "STRING");
    assertNull(ProcessorDynamicFieldInstance.rowToFieldDefn(def, r), "Missing id should return null");

    // 3. Missing name
    r = DataRow.create(types);
    r.put(id, 123);
    r.put(type, "STRING");
    assertNull(ProcessorDynamicFieldInstance.rowToFieldDefn(def, r), "Missing name should return null");

    // 4. Empty name
    r = DataRow.create(types);
    r.put(id, 123);
    r.put(name, "");
    r.put(type, "STRING");
    assertNull(ProcessorDynamicFieldInstance.rowToFieldDefn(def, r), "Empty name should return null");

    // 5. Missing type
    r = DataRow.create(types);
    r.put(id, 123);
    r.put(name, "abc");
    assertNull(ProcessorDynamicFieldInstance.rowToFieldDefn(def, r), "Missing type should return null");

    // 6. Invalid type string
    r = DataRow.create(types);
    r.put(id, 123);
    r.put(name, "abc");
    r.put(type, "NOPE");
    assertNull(ProcessorDynamicFieldInstance.rowToFieldDefn(def, r), "Unknown type should return null");

    // 7. Valid type: value as string
    r = DataRow.create(types);
    r.put(id, "id1");
    r.put(name, "Field1");
    r.put(type, "String");
    r.put(column, "C1");
    FieldDefn f = rowToFieldDefn(def, r);
    assertNotNull(f);
    assertEquals("id1", f.id);
    assertEquals("Field1", f.key);
    assertEquals("Field1", f.name);
    assertEquals(DataType.String, f.type);
    assertEquals("C1", f.column);

    // 8. Valid type, type as enum, column is null
    r = DataRow.create(types);
    r.put(id, "id2");
    r.put(name, "Test");
    r.put(type, DataType.Integer.name()); // type as enum
    assertNotNull(ProcessorDynamicFieldInstance.rowToFieldDefn(def, r));
    assertEquals(DataType.Integer, rowToFieldDefn(def, r).type);
    assertNull(ProcessorDynamicFieldInstance.rowToFieldDefn(def, r).column);

    // 9. Case insensitive name
    ProcessorDynamicField defCase = ProcessorDynamicField.builder()
            .fieldIdColumn(id)
            .fieldNameColumn(name)
            .fieldTypeColumn(type)
            .fieldValueColumnName(column)
            .useCaseInsensitiveFieldNames(true)
            .build();
    r = DataRow.create(types);
    r.put(id, "id3");
    r.put(name, "CamelCase");
    r.put(type, "Float");
    FieldDefn f2 = rowToFieldDefn(defCase, r);
    assertEquals("camelcase", f2.key, "Key should be lowercase when case-insensitive");

    // 10. Name is number but should still work (converted via Objects.toString)
    r = DataRow.create(types);
    r.put(id, "id4");
    r.put(name, 1234);
    r.put(type, "String");
    FieldDefn f3 = rowToFieldDefn(def, r);
    assertEquals("1234", f3.key);
    assertEquals("1234", f3.name);
  }

}
