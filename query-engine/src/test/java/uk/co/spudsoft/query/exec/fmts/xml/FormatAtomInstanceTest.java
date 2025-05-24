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

import inet.ipaddr.IPAddressString;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.streams.WriteStream;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.defn.FormatAtom;
import uk.co.spudsoft.query.exec.ColumnDefn;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.conditions.RequestContext;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import uk.co.spudsoft.query.defn.FormatXml;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class FormatAtomInstanceTest {


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
  public void testDefaultStream(Vertx vertx, VertxTestContext testContext, TestInfo testInfo) {

    String outfile = "target/temp/" + testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName() + ".xml";

    FormatAtom defn = FormatAtom.builder()
      .build();

    FileSystem fs = vertx.fileSystem();
    if (!fs.existsBlocking("target/temp")) {
      fs.mkdirBlocking("target/temp");
    }
    WriteStream<Buffer> writeStream = fs.openBlocking(outfile, new OpenOptions().setCreate(true).setSync(true));
    RequestContext req = new RequestContext(
      null
      , null
      , "localhost"
      , "/data/atom"
      , new HeadersMultiMap()
      , new HeadersMultiMap().add("Host", "localhost:123")
      , null
      , new IPAddressString("127.0.0.1")
      , null
    );
    Context context = mock(Context.class);
    when(context.getLocal("req")).thenReturn(req);

    FormatAtomInstance instance = defn.createInstance(vertx, context, writeStream);

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
              <?xml version='1.0' encoding='utf-8'?><feed xmlns="http://www.w3.org/2005/Atom" xmlns:m="http://schemas.microsoft.com/ado/2007/08/dataservices/metadata" xmlns:d="http://schemas.microsoft.com/ado/2007/08/dataservices"><id>/data/atom</id><title>Atom</title><updated>recently</updated><entry><id>/data/atom/1</id><title>Atom</title><updated>recently</updated><content type="application/xml"><m:properties><d:Boolean m:type="Edm.Boolean" m:null="true"/><d:Date m:type="Edm.Date">1971-05-01</d:Date><d:DateTime m:type="Edm.DateTime">1971-05-01T00:00</d:DateTime><d:Double m:type="Edm.Double">0.0</d:Double><d:Float m:type="Edm.Single">0.0</d:Float><d:Integer m:type="Edm.Int32">0</d:Integer><d:Long m:type="Edm.Int64">0</d:Long><d:String m:type="Edm.String">This is row 0</d:String><d:Time m:type="Edm.Time">00:00</d:Time></m:properties></content></entry><entry><id>/data/atom/2</id><title>Atom</title><updated>recently</updated><content type="application/xml"><m:properties><d:Boolean m:type="Edm.Boolean">false</d:Boolean><d:Date m:type="Edm.Date" m:null="true"/><d:DateTime m:type="Edm.DateTime">1971-05-02T01:01</d:DateTime><d:Double m:type="Edm.Double">1.1</d:Double><d:Float m:type="Edm.Single">1.1</d:Float><d:Integer m:type="Edm.Int32">1</d:Integer><d:Long m:type="Edm.Int64">10000000</d:Long><d:String m:type="Edm.String">This is row 1</d:String><d:Time m:type="Edm.Time">01:01</d:Time></m:properties></content></entry>
              """.trim()
            ));
          });
          testContext.completeNow();
        }
      });

  }

  void testEndsWith() {
    // Test when string already ends with suffix
    assertEquals("hello", FormatAtomInstance.endsWith("hello", "o"));
    assertEquals("hello", FormatAtomInstance.endsWith("hello", "lo"));

    // Test when string doesn't end with suffix
    assertEquals("hello world", FormatAtomInstance.endsWith("hello", " world"));

    // Test with slash suffix
    assertEquals("hello/", FormatAtomInstance.endsWith("hello/", "/"));
    assertEquals("hello/", FormatAtomInstance.endsWith("hello", "/"));
    assertEquals("/", FormatAtomInstance.endsWith("/", "/"));


    // Test with empty string input
    assertEquals("/", FormatAtomInstance.endsWith("", "/"));
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
  void testFormatValue() {
    FormatAtomInstance instance = new FormatAtomInstance(FormatAtom.builder().build(), "/path", null);
            
    // Test null value
    assertNull(instance.formatValue((String) null));

    // Test String value
    assertEquals("Hello World", instance.formatValue("Hello World"));

    // Test numeric values
    assertEquals("42", instance.formatValue(42));
    assertEquals("42.5", instance.formatValue(42.5));
    assertEquals("123456789", instance.formatValue(123456789L));
    assertEquals("123.456", instance.formatValue(new BigDecimal("123.456")));

    // Test boolean values
    assertEquals("true", instance.formatValue(true));
    assertEquals("false", instance.formatValue(false));

    // Test date/time values
    LocalDate date = LocalDate.of(2023, 5, 15);
    assertEquals("2023-05-15", instance.formatValue(date));

    LocalTime time = LocalTime.of(14, 30, 15);
    assertEquals("14:30:15", instance.formatValue(time));

    LocalDateTime dateTime = LocalDateTime.of(2023, 5, 15, 14, 30, 15);
    assertEquals("2023-05-15T14:30:15", instance.formatValue(dateTime));
  }

  @Test
  void testFormatTemporalValues() {
    FormatAtomInstance instance = new FormatAtomInstance(FormatAtom.builder()
            .dateFormat("yyyy")
            .dateTimeFormat("HH yyyy")
            .timeFormat("mm")
            .build(), "/", null);
            
    // Test date/time values
    LocalDate date = LocalDate.of(2023, 5, 15);
    assertEquals("2023", instance.formatValue(date));

    LocalTime time = LocalTime.of(14, 30, 15);
    assertEquals("30", instance.formatValue(time));

    LocalDateTime dateTime = LocalDateTime.of(2023, 5, 15, 14, 30, 15);
    assertEquals("14 2023", instance.formatValue(dateTime));
  }
  

  @Test
  void testGetType() {
    // Test basic data types
    assertEquals("Null", FormatAtomInstance.getType(DataType.Null));
    assertEquals("Edm.Int32", FormatAtomInstance.getType(DataType.Integer));
    assertEquals("Edm.Int64", FormatAtomInstance.getType(DataType.Long));
    assertEquals("Edm.Single", FormatAtomInstance.getType(DataType.Float));
    assertEquals("Edm.Double", FormatAtomInstance.getType(DataType.Double));
    assertEquals("Edm.Boolean", FormatAtomInstance.getType(DataType.Boolean));

    // Test date/time types
    assertEquals("Edm.Date", FormatAtomInstance.getType(DataType.Date));
    assertEquals("Edm.Time", FormatAtomInstance.getType(DataType.Time));
    assertEquals("Edm.DateTime", FormatAtomInstance.getType(DataType.DateTime));
  }

}
