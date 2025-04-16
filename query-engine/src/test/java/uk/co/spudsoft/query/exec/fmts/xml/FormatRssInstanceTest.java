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
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.defn.FormatRss;
import uk.co.spudsoft.query.exec.ColumnDefn;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.procs.ListReadStream;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class FormatRssInstanceTest {


  private List<ColumnDefn> buildTypes() {
    return Arrays.asList(
      new ColumnDefn("title", DataType.String)
      , new ColumnDefn("description", DataType.String)
      , new ColumnDefn("Boolean", DataType.Boolean)
      , new ColumnDefn("Date", DataType.Date)
      , new ColumnDefn("DateTime", DataType.DateTime)
      , new ColumnDefn("Double", DataType.Double)
      , new ColumnDefn("Float", DataType.Float)
      , new ColumnDefn("Integer", DataType.Integer)
      , new ColumnDefn("Long", DataType.Long)
      , new ColumnDefn("Time", DataType.Time)
    );
  }

  @Test
  public void testDefaultStream(Vertx vertx, VertxTestContext testContext, TestInfo testInfo) {

    String outfile = "target/temp/" + testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName() + ".xml";

    FormatRss defn = FormatRss.builder()
      .build();

    FileSystem fs = vertx.fileSystem();
    if (!fs.existsBlocking("target/temp")) {
      fs.mkdirBlocking("target/temp");
    }
    WriteStream<Buffer> writeStream = fs.openBlocking(outfile, new OpenOptions().setCreate(true).setSync(true));
    FormatRssInstance instance = (FormatRssInstance) defn.createInstance(vertx, null, writeStream);

    Types types = new Types(buildTypes());
    List<DataRow> rowsList = new ArrayList<>();
    for (int i = 0; i < 10; ++i) {
      rowsList.add(createDataRow(types, i));
    }

    instance.initialize(null, null, new ReadStreamWithTypes(new ListReadStream<>(vertx.getOrCreateContext(), rowsList), types))
      .compose(v -> {
        return instance.getFinalFuture();
      })
      .onComplete(ar -> {
        if (ar.failed()) {
          testContext.failNow(ar.cause());
        } else {
          testContext.verify(() -> {
            String outstring = FileUtils.readFileToString(new File(outfile), StandardCharsets.UTF_8);
            outstring = outstring.replaceAll("<updated>[-0-9T:.Z]+</updated>", "<updated>recently</updated>");
            assertThat(outstring, startsWith(
              """
              <rss xmlns:custom="https://yaytay.github.io/query-engine/rss" version="2.0">
                <channel>
                  <item>
                    <title>This is row 0</title>
                    <description>This is the description of row 0</description>
                    <custom:Boolean/>
                    <custom:Date>1971-05-01</custom:Date>
                    <custom:DateTime>1971-05-01T00:00</custom:DateTime>
                    <custom:Double>0.0</custom:Double>
                    <custom:Float>0.0</custom:Float>
                    <custom:Integer>0</custom:Integer>
                    <custom:Long>0</custom:Long>
                    <custom:Time>00:00</custom:Time>
                  </item>
                  <item>
                    <title>This is row 1</title>
                    <description>This is the description of row 1</description>
                    <custom:Boolean>false</custom:Boolean>
                    <custom:Date/>
                    <custom:DateTime>1971-05-02T01:01</custom:DateTime>
                    <custom:Double>1.1</custom:Double>
                    <custom:Float>1.1</custom:Float>
                    <custom:Integer>1</custom:Integer>
                    <custom:Long>10000000</custom:Long>
                    <custom:Time>01:01</custom:Time>
                  </item>
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
    row.put("title", rowNum % 9 == 7 ? null : "This is row " + rowNum);
    row.put("description", rowNum % 9 == 7 ? null : "This is the description of row " + rowNum);
    row.put("Time", rowNum % 9 == 8 ? null : LocalTime.of(rowNum, rowNum));
    return row;
  }

  @Test
  void testFormatValue() {
    // Test null value
    assertNull(FormatRssInstance.formatValue((String) null));

    // Test String value
    assertEquals("Hello World", FormatRssInstance.formatValue("Hello World"));

    // Test numeric values
    assertEquals("42", FormatRssInstance.formatValue(42));
    assertEquals("42.5", FormatRssInstance.formatValue(42.5));
    assertEquals("123456789", FormatRssInstance.formatValue(123456789L));
    assertEquals("123.456", FormatRssInstance.formatValue(new BigDecimal("123.456")));

    // Test boolean values
    assertEquals("true", FormatRssInstance.formatValue(true));
    assertEquals("false", FormatRssInstance.formatValue(false));

    // Test date/time values
    LocalDate date = LocalDate.of(2023, 5, 15);
    assertEquals("2023-05-15", FormatRssInstance.formatValue(date));

    LocalTime time = LocalTime.of(14, 30, 15);
    assertEquals("14:30:15", FormatRssInstance.formatValue(time));

    LocalDateTime dateTime = LocalDateTime.of(2023, 5, 15, 14, 30, 15);
    assertEquals("2023-05-15T14:30:15", FormatRssInstance.formatValue(dateTime));
  }

}
