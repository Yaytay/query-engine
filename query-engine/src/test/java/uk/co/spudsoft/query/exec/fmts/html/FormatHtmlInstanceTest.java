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
package uk.co.spudsoft.query.exec.fmts.html;

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
import java.util.List;
import org.apache.commons.io.FileUtils;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.defn.FormatHtml;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.procs.ListReadStream;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class FormatHtmlInstanceTest {
  
  private Types buildTypes() {
    Types types = new Types();
    types.putIfAbsent("Boolean", DataType.Boolean);
    types.putIfAbsent("Date", DataType.Date);
    types.putIfAbsent("DateTime", DataType.DateTime);
    types.putIfAbsent("Double", DataType.Double);
    types.putIfAbsent("Float", DataType.Float);
    types.putIfAbsent("Integer", DataType.Integer);
    types.putIfAbsent("Long", DataType.Long);
    types.putIfAbsent("String", DataType.String);
    types.putIfAbsent("Time", DataType.Time);
    return types;
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

    String outfile = "target/temp/" + testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName() + ".html";

    FormatHtml defn = FormatHtml.builder()
            .dateFormat(null)
            .dateTimeFormat(null)
            .timeFormat(null)
            .decimalFormat(null)
            .booleanFormat(null)
            .build();

    FileSystem fs = vertx.fileSystem();
    if (!fs.existsBlocking("target/temp")) {
      fs.mkdirBlocking("target/temp");
    }
    WriteStream<Buffer> writeStream = fs.openBlocking(outfile, new OpenOptions().setCreate(true).setSync(true));

    RequestContext requestContext = new RequestContext(null, "requestId", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.0"), null);
    PipelineContext pipelineContext = new PipelineContext("test", requestContext);
    
    FormatHtmlInstance instance = defn.createInstance(vertx, pipelineContext, writeStream);

    Types types = buildTypes();
    List<DataRow> rowsList = new ArrayList<>();
    for (int i = 0; i < 10; ++i) {
      rowsList.add(createDataRow(types, i));
    }

    instance.initialize(null, null, new ReadStreamWithTypes(new ListReadStream<>(null, vertx.getOrCreateContext(), rowsList), types))
      .compose(v -> {
        return instance.getFinalFuture();
      })
      .onComplete(ar -> {
        if (ar.failed()) {
          testContext.failNow(ar.cause());
        } else {
          testContext.verify(() -> {
            String outString = FileUtils.readFileToString(new File(outfile), StandardCharsets.UTF_8);
            assertThat(outString, startsWith("""
                                             <table class="qetable"><thead>
                                             <tr class="header"><th class="header oddCol" >Boolean</th><th class="header evenCol" >Date</th><th class="header oddCol" >DateTime</th><th class="header evenCol" >Double</th><th class="header oddCol" >Float</th><th class="header evenCol" >Integer</th><th class="header oddCol" >Long</th><th class="header evenCol" >String</th><th class="header oddCol" >Time</th></tr>
                                             </thead><tbody>
                                             <tr class="dataRow oddRow" ><td class="oddRow oddCol"></td><td class="oddRow evenCol">1971-05-01</td><td class="oddRow oddCol">1971-05-01T00:00</td><td class="oddRow evenCol">0.0</td><td class="oddRow oddCol">0.0</td><td class="oddRow evenCol">0</td><td class="oddRow oddCol">0</td><td class="oddRow evenCol">This is row 0</td><td class="oddRow oddCol">00:00</td></tr>
                                             <tr class="dataRow evenRow" ><td class="evenRow oddCol">false</td><td class="evenRow evenCol"></td><td class="evenRow oddCol">1971-05-02T01:01</td><td class="evenRow evenCol">1.1</td><td class="evenRow oddCol">1.1</td><td class="evenRow evenCol">1</td><td class="evenRow oddCol">10000000</td><td class="evenRow evenCol">This is row 1</td><td class="evenRow oddCol">01:01</td></tr>
                                             <tr class="dataRow oddRow" ><td class="oddRow oddCol">true</td><td class="oddRow evenCol">1971-05-03</td><td class="oddRow oddCol"></td><td class="oddRow evenCol">2.2</td><td class="oddRow oddCol">2.2</td><td class="oddRow evenCol">2</td><td class="oddRow oddCol">20000000</td><td class="oddRow evenCol">This is row 2</td><td class="oddRow oddCol">02:02</td></tr>
                                             """));
            assertThat(outString, endsWith("</tbody></table>"));
          });
          testContext.completeNow();
        }
      });
  }
  
  @Test
  public void testFormattedStream(Vertx vertx, VertxTestContext testContext, TestInfo testInfo) throws IOException {

    String outfile = "target/temp/" + testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName() + ".html";

    FormatHtml defn = FormatHtml.builder()
            .dateFormat("uuuu-MM")
            .dateTimeFormat("uuuu-MM-dd'T'HH:mm:ss.SSS")
            .timeFormat("HH:mm")
            .decimalFormat("0.000")
            .booleanFormat("['\"1\"', '\"0\"']")
            .build();

    FileSystem fs = vertx.fileSystem();
    if (!fs.existsBlocking("target/temp")) {
      fs.mkdirBlocking("target/temp");
    }
    WriteStream<Buffer> writeStream = fs.openBlocking(outfile, new OpenOptions().setCreate(true).setSync(true));

    RequestContext requestContext = new RequestContext(null, "requestId", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.0"), null);
    PipelineContext pipelineContext = new PipelineContext("test", requestContext);
    
    FormatHtmlInstance instance = defn.createInstance(vertx, pipelineContext, writeStream);

    Types types = buildTypes();
    List<DataRow> rowsList = new ArrayList<>();
    for (int i = 0; i < 10; ++i) {
      rowsList.add(createDataRow(types, i));
    }

    instance.initialize(null, null, new ReadStreamWithTypes(new ListReadStream<>(null, vertx.getOrCreateContext(), rowsList), types))
      .compose(v -> {
        return instance.getFinalFuture();
      })
      .onComplete(ar -> {
        if (ar.failed()) {
          testContext.failNow(ar.cause());
        } else {
          testContext.verify(() -> {
            String outString = FileUtils.readFileToString(new File(outfile), StandardCharsets.UTF_8);
            assertThat(outString, startsWith("""
                                             <table class="qetable"><thead>
                                             <tr class="header"><th class="header oddCol" >Boolean</th><th class="header evenCol" >Date</th><th class="header oddCol" >DateTime</th><th class="header evenCol" >Double</th><th class="header oddCol" >Float</th><th class="header evenCol" >Integer</th><th class="header oddCol" >Long</th><th class="header evenCol" >String</th><th class="header oddCol" >Time</th></tr>
                                             </thead><tbody>
                                             <tr class="dataRow oddRow" ><td class="oddRow oddCol"></td><td class="oddRow evenCol">1971-05</td><td class="oddRow oddCol">1971-05-01T00:00:00.000</td><td class="oddRow evenCol">0.000</td><td class="oddRow oddCol">0.000</td><td class="oddRow evenCol">0</td><td class="oddRow oddCol">0</td><td class="oddRow evenCol">This is row 0</td><td class="oddRow oddCol">00:00</td></tr>
                                             <tr class="dataRow evenRow" ><td class="evenRow oddCol">"0"</td><td class="evenRow evenCol"></td><td class="evenRow oddCol">1971-05-02T01:01:00.000</td><td class="evenRow evenCol">1.100</td><td class="evenRow oddCol">1.100</td><td class="evenRow evenCol">1</td><td class="evenRow oddCol">10000000</td><td class="evenRow evenCol">This is row 1</td><td class="evenRow oddCol">01:01</td></tr>
                                             <tr class="dataRow oddRow" ><td class="oddRow oddCol">"1"</td><td class="oddRow evenCol">1971-05</td><td class="oddRow oddCol"></td><td class="oddRow evenCol">2.200</td><td class="oddRow oddCol">2.200</td><td class="oddRow evenCol">2</td><td class="oddRow oddCol">20000000</td><td class="oddRow evenCol">This is row 2</td><td class="oddRow oddCol">02:02</td></tr>
                                             """));
            assertThat(outString, endsWith("</tbody></table>"));
          });
          testContext.completeNow();
        }
      });
  }  
  
}
