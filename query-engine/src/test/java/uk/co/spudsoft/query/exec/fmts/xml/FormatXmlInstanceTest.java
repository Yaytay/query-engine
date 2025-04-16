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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.defn.FormatXml;
import uk.co.spudsoft.query.exec.ColumnDefn;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.procs.ListReadStream;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
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
  public void testDefaultStream(Vertx vertx, VertxTestContext testContext, TestInfo testInfo) throws IOException {

    String outfile = "target/temp/" + testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName() + ".xml";

    FormatXml defn = FormatXml.builder()
      .build();

    FileSystem fs = vertx.fileSystem();
    if (!fs.existsBlocking("target/temp")) {
      fs.mkdirBlocking("target/temp");
    }
    WriteStream<Buffer> writeStream = fs.openBlocking(outfile, new OpenOptions().setCreate(true).setSync(true));

    FormatXmlInstance instance = (FormatXmlInstance) defn.createInstance(vertx, null, writeStream);

    Types types = new Types(buildTypes());
    List<DataRow> rowsList = new ArrayList<>();
    for (int i = 0; i < 10; ++i) {
      rowsList.add(createDataRow(types, i));
    }

    instance.initialize(null, null, new ReadStreamWithTypes(new ListReadStream<>(vertx.getOrCreateContext(), rowsList), types))
      .onComplete(ar -> {
        if (ar.failed()) {
          testContext.failNow(ar.cause());
        } else {
          testContext.verify(() -> {
            String outstring = FileUtils.readFileToString(new File(outfile), StandardCharsets.UTF_8);
            assertThat(outstring, startsWith(
              """
              <?xml version='1.0' encoding='utf-8'?><data><row><Date>1971-05-01</Date><DateTime>1971-05-01T00:00</DateTime><Double>0.0</Double><Float>0.0</Float><Integer>0</Integer><Long>0</Long><String>This is row 0</String><Time>00:00</Time></row><row><Boolean>false</Boolean><DateTime>1971-05-02T01:01</DateTime><Double>1.1</Double><Float>1.1</Float><Integer>1</Integer><Long>10000000</Long><String>This is row 1</String><Time>01:01</Time></row><row><Boolean>true</Boolean><Date>1971-05-03</Date><Double>2.2</Double><Float>2.2</Float><Integer>2</Integer><Long>20000000</Long><String>This is row 2</String><Time>02:02</Time></row><row><Boolean>false</Boolean><Date>1971-05-04</Date><DateTime>1971-05-04T03:03</DateTime><Float>3.3</Float><Integer>3</Integer><Long>30000000</Long><String>This is row 3</String><Time>03:03</Time></row><row><Boolean>true</Boolean><Date>1971-05-05</Date><DateTime>1971-05-05T04:04</DateTime><Double>4.4</Double><Integer>4</Integer><Long>40000000</Long><String>This is row 4</String><Time>04:04</Time></row><row><Boolean>false</Boolean><Date>1971-05-06</Date><DateTime>1971-05-06T05:05</DateTime><Double>5.5</Double><Float>5.5</Float><Long>50000000</Long><String>This is row 5</String><Time>05:05</Time></row><row><Boolean>true</Boolean><Date>1971-05-07</Date><DateTime>1971-05-07T06:06</DateTime><Double>6.6</Double><Float>6.6</Float><Integer>6</Integer><String>This is row 6</String><Time>06:06</Time></row><row><Boolean>false</Boolean><Date>1971-05-08</Date><DateTime>1971-05-08T07:07</DateTime><Double>7.7</Double><Float>7.7</Float><Integer>7</Integer><Long>70000000</Long><Time>07:07</Time></row><row><Boolean>true</Boolean><Date>1971-05-09</Date><DateTime>1971-05-09T08:08</DateTime><Double>8.8</Double><Float>8.8</Float><Integer>8</Integer><Long>80000000</Long><String>This is row 8</String></row><row><Date>1971-05-10</Date><DateTime>1971-05-10T09:09</DateTime><Double>9.9</Double><Float>9.9</Float><Integer>9</Integer><Long>90000000</Long><String>This is row 9</String><Time>09:09</Time></row></data>
              """.trim()
            ));
          });
          testContext.completeNow();
        }
      });
  }

  @Test
  public void testIndentedStream(Vertx vertx, VertxTestContext testContext, TestInfo testInfo) throws IOException {

    String outfile = "target/temp/" + testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName() + ".xml";

    FormatXml defn = FormatXml.builder()
      .indent(true)
      .build();

    FileSystem fs = vertx.fileSystem();
    if (!fs.existsBlocking("target/temp")) {
      fs.mkdirBlocking("target/temp");
    }
    WriteStream<Buffer> writeStream = fs.openBlocking(outfile, new OpenOptions().setCreate(true));

    FormatXmlInstance instance = (FormatXmlInstance) defn.createInstance(vertx, null, writeStream);

    Types types = new Types(buildTypes());
    List<DataRow> rowsList = new ArrayList<>();
    for (int i = 0; i < 10; ++i) {
      rowsList.add(createDataRow(types, i));
    }

    instance.initialize(null, null, new ReadStreamWithTypes(new ListReadStream<>(vertx.getOrCreateContext(), rowsList), types))
      .onComplete(ar -> {
        if (ar.failed()) {
          testContext.failNow(ar.cause());
        } else {
          testContext.verify(() -> {
            String outstring = FileUtils.readFileToString(new File(outfile), StandardCharsets.UTF_8);
            assertThat(outstring, startsWith(
              """
              <?xml version='1.0' encoding='utf-8'?>
              <data>
                <row>
                  <Date>1971-05-01</Date>
                  <DateTime>1971-05-01T00:00</DateTime>
                  <Double>0.0</Double>
                  <Float>0.0</Float>
                  <Integer>0</Integer>
                  <Long>0</Long>
                  <String>This is row 0</String>
                  <Time>00:00</Time>
                </row>
                <row>
                  <Boolean>false</Boolean>
                  <DateTime>1971-05-02T01:01</DateTime>
                  <Double>1.1</Double>
                  <Float>1.1</Float>
                  <Integer>1</Integer>
                  <Long>10000000</Long>
                  <String>This is row 1</String>
                  <Time>01:01</Time>
                </row>
              """.trim()
            ));
          });
          testContext.completeNow();
        }
      });
  }

  @Test
  public void testIndentedFieldsAsAttributesStream(Vertx vertx, VertxTestContext testContext, TestInfo testInfo) throws IOException{

    String outfile = "target/temp/" + testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName() + ".xml";

    FormatXml defn = FormatXml.builder()
      .indent(true)
      .fieldsAsAttributes(true)
      .build();

    FileSystem fs = vertx.fileSystem();
    if (!fs.existsBlocking("target/temp")) {
      fs.mkdirBlocking("target/temp");
    }
    WriteStream<Buffer> writeStream = fs.openBlocking(outfile, new OpenOptions().setCreate(true));

    FormatXmlInstance instance = (FormatXmlInstance) defn.createInstance(vertx, null, writeStream);

    Types types = new Types(buildTypes());
    List<DataRow> rowsList = new ArrayList<>();
    for (int i = 0; i < 10; ++i) {
      rowsList.add(createDataRow(types, i));
    }

    instance.initialize(null, null, new ReadStreamWithTypes(new ListReadStream<>(vertx.getOrCreateContext(), rowsList), types))
      .onComplete(ar -> {
        if (ar.failed()) {
          testContext.failNow(ar.cause());
        } else {
          testContext.verify(() -> {
            String outstring = FileUtils.readFileToString(new File(outfile), StandardCharsets.UTF_8);
            assertThat(outstring, startsWith(
                  """
                  <?xml version='1.0' encoding='utf-8'?>
                  <data>
                    <row Date="1971-05-01" DateTime="1971-05-01T00:00" Double="0.0" Float="0.0" Integer="0" Long="0" String="This is row 0" Time="00:00">
                    </row>
                    <row Boolean="false" DateTime="1971-05-02T01:01" Double="1.1" Float="1.1" Integer="1" Long="10000000" String="This is row 1" Time="01:01">
                    </row>
                    <row Boolean="true" Date="1971-05-03" Double="2.2" Float="2.2" Integer="2" Long="20000000" String="This is row 2" Time="02:02">
                    </row>
                    <row Boolean="false" Date="1971-05-04" DateTime="1971-05-04T03:03" Float="3.3" Integer="3" Long="30000000" String="This is row 3" Time="03:03">
                    </row>
                    <row Boolean="true" Date="1971-05-05" DateTime="1971-05-05T04:04" Double="4.4" Integer="4" Long="40000000" String="This is row 4" Time="04:04">
                    </row>
                  """.trim()
            ));
          });
          testContext.completeNow();
        }
      });
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
