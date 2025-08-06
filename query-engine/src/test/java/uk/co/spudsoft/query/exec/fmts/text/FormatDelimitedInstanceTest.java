/*
 * Copyright (C) 2025 njt
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
package uk.co.spudsoft.query.exec.fmts.text;

import inet.ipaddr.IPAddressString;
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
import java.util.List;
import org.apache.commons.io.FileUtils;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.defn.FormatDelimited;
import uk.co.spudsoft.query.exec.ColumnDefn;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.procs.ListReadStream;

/**
 *
 * @author njt
 */
@ExtendWith(VertxExtension.class)
public class FormatDelimitedInstanceTest {
  
  @Test
  public void testEncodeCloseQuote() {
    assertEquals("bob", FormatDelimitedInstance.encodeCloseQuote(FormatDelimited.builder().build(), "bob"));
    assertEquals("\\\"bob\\\"", FormatDelimitedInstance.encodeCloseQuote(FormatDelimited.builder().escapeCloseQuote("\\").build(), "\"bob\""));
    assertEquals("&quot;bob&quot;", FormatDelimitedInstance.encodeCloseQuote(FormatDelimited.builder().replaceCloseQuote("&quot;").build(), "\"bob\""));    
  }
  
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
  public void testDefaultStream(Vertx vertx, VertxTestContext testContext, TestInfo testInfo) throws IOException {

    String outfile = "target/temp/" + testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName() + ".txt";

    FormatDelimited defn = FormatDelimited.builder()
            .bom(true)
            .dateFormat(null)
            .dateTimeFormat(null)
            .timeFormat(null)
            .quoteTemporal(false)
            .newline("\n")
            .build();

    FileSystem fs = vertx.fileSystem();
    if (!fs.existsBlocking("target/temp")) {
      fs.mkdirBlocking("target/temp");
    }
    RequestContext requestContext = new RequestContext(null, "requestId", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.0"), null);
    WriteStream<Buffer> writeStream = fs.openBlocking(outfile, new OpenOptions().setCreate(true).setSync(true));

    FormatDelimitedInstance instance = defn.createInstance(vertx, requestContext, writeStream);

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
            byte[] outbytes = FileUtils.readFileToByteArray(new File(outfile));
            assertEquals(0xEF, Byte.toUnsignedInt(outbytes[0]));
            assertEquals(0xBB, Byte.toUnsignedInt(outbytes[1]));
            assertEquals(0xBF, Byte.toUnsignedInt(outbytes[2]));
            
            outbytes = Arrays.copyOfRange(outbytes, 3, outbytes.length);
            
            String outstring = new String(outbytes, StandardCharsets.UTF_8);
            assertThat(outstring, startsWith("""
                                             "Boolean","Date","DateTime","Double","Float","Integer","Long","String","Time"
                                             ,1971-05-01,1971-05-01T00:00,0.0,0.0,0,0,"This is row 0",00:00
                                             false,,1971-05-02T01:01,1.1,1.1,1,10000000,"This is row 1",01:01
                                             true,1971-05-03,,2.2,2.2,2,20000000,"This is row 2",02:02
                                             """));
          });
          testContext.completeNow();
        }
      });
  }
  
  
}
