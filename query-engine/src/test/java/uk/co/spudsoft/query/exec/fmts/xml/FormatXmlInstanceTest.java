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
package uk.co.spudsoft.query.exec.fmts.xml;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.WriteStream;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.defn.FormatXml;
import uk.co.spudsoft.query.exec.ColumnDefn;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.procs.ListReadStream;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class FormatXmlInstanceTest {


  private List<ColumnDefn> buildTypes() {
    return Arrays.asList(
      new ColumnDefn("Boolean", DataType.Boolean)
      , new ColumnDefn("Date", DataType.Date)
      , new ColumnDefn("DateTime", DataType.DateTime)
      , new ColumnDefn("Double", DataType.Double)
      , new ColumnDefn("Float", DataType.Float)
      , new ColumnDefn("Integer", DataType.Integer)
      , new ColumnDefn("Long", DataType.Long)
      , new ColumnDefn("String", DataType.String)
      , new ColumnDefn("Time", DataType.Time)
    );
  }

  @Test
  public void testDefaultStream(Vertx vertx, VertxTestContext testContext) {

    FormatXml defn = FormatXml.builder()
      .build();

    FileSystem fs = vertx.fileSystem();
    if (!fs.existsBlocking("target/temp")) {
      fs.mkdirBlocking("target/temp");
    }
    WriteStream<Buffer> writeStream = fs.openBlocking("target/temp/FormatXmlInstanceTest_Default.xml", new OpenOptions().setCreate(true));

    FormatXmlInstance instance = (FormatXmlInstance) defn.createInstance(vertx, null, writeStream);

    Types types = new Types(buildTypes());
    List<DataRow> rowsList = new ArrayList<>();
    for (int i = 0; i < 10; ++i) {
      rowsList.add(createDataRow(types, i));
    }

    instance.initialize(null, null, new ReadStreamWithTypes(new ListReadStream<>(vertx.getOrCreateContext(), rowsList), types))
      .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  public void testIndentedStream(Vertx vertx, VertxTestContext testContext) {

    FormatXml defn = FormatXml.builder()
      .indent(true)
      .build();

    FileSystem fs = vertx.fileSystem();
    if (!fs.existsBlocking("target/temp")) {
      fs.mkdirBlocking("target/temp");
    }
    WriteStream<Buffer> writeStream = fs.openBlocking("target/temp/FormatXmlInstanceTest_Indented.xml", new OpenOptions().setCreate(true));

    FormatXmlInstance instance = (FormatXmlInstance) defn.createInstance(vertx, null, writeStream);

    Types types = new Types(buildTypes());
    List<DataRow> rowsList = new ArrayList<>();
    for (int i = 0; i < 10; ++i) {
      rowsList.add(createDataRow(types, i));
    }

    instance.initialize(null, null, new ReadStreamWithTypes(new ListReadStream<>(vertx.getOrCreateContext(), rowsList), types))
      .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  public void testIndentedFieldsAsAttributesStream(Vertx vertx, VertxTestContext testContext) {

    FormatXml defn = FormatXml.builder()
      .indent(true)
      .fieldsAsAttributes(true)
      .build();

    FileSystem fs = vertx.fileSystem();
    if (!fs.existsBlocking("target/temp")) {
      fs.mkdirBlocking("target/temp");
    }
    WriteStream<Buffer> writeStream = fs.openBlocking("target/temp/FormatXmlInstanceTest_IndentedFieldsAsAttributes.xml", new OpenOptions().setCreate(true));

    FormatXmlInstance instance = (FormatXmlInstance) defn.createInstance(vertx, null, writeStream);

    Types types = new Types(buildTypes());
    List<DataRow> rowsList = new ArrayList<>();
    for (int i = 0; i < 10; ++i) {
      rowsList.add(createDataRow(types, i));
    }

    instance.initialize(null, null, new ReadStreamWithTypes(new ListReadStream<>(vertx.getOrCreateContext(), rowsList), types))
      .onComplete(testContext.succeedingThenComplete());
  }

  private DataRow createDataRow(Types types, int rowNum) {
    DataRow row = DataRow.create(types);
    row.put("Boolean", rowNum % 9 == 0 ? null : (rowNum % 2 == 0 ? Boolean.TRUE : Boolean.FALSE));
    row.put("Date", rowNum % 9 == 1 ? null : LocalDate.of(1971, Month.MAY, 1 + rowNum));
    row.put("DateTime", rowNum % 9 == 2 ? null : LocalDateTime.of(1971, Month.MAY, 1 + rowNum, rowNum, rowNum));
    row.put("Double", rowNum % 9 == 3 ? null : rowNum + (double) rowNum / 10);
    row.put("Float", rowNum % 9 == 4 ? null : rowNum + (float) rowNum / 10);
    row.put("Integer", rowNum % 9 == 5 ? null : rowNum);
    row.put("Long", rowNum % 9 == 6 ? null : rowNum * 10000000L);
    row.put("String", rowNum % 9 == 7 ? null : "This is row " + rowNum);
    row.put("Time", rowNum % 9 == 8 ? null : LocalTime.of(rowNum, rowNum));
    return row;
  }

  @Test
  public void testGetName() {
    Map<String, String> nameMap = new HashMap<>();
    assertEquals("default", FormatXmlInstance.getName(nameMap, "F", "_", null, "default"));
    assertEquals("F", FormatXmlInstance.getName(nameMap, "F", "_", "   ", "default"));
    assertEquals("FA_", FormatXmlInstance.getName(nameMap, "F", "_", "  A ", "default"));
  }

}
