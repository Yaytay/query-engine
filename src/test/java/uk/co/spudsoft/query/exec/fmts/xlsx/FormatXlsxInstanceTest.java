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
package uk.co.spudsoft.query.exec.fmts.xlsx;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.FileSystemException;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.WriteStream;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.defn.FormatXlsx;
import uk.co.spudsoft.query.defn.FormatXlsxColours;
import uk.co.spudsoft.query.exec.DataRow;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class FormatXlsxInstanceTest {
  
  
  private LinkedHashMap<String, DataType> buildTypes() {
    LinkedHashMap<String, DataType> types = new LinkedHashMap<>();
    types.put("Boolean", DataType.Boolean);
    types.put("Date", DataType.Date);
    types.put("DateTime", DataType.DateTime);
    types.put("Double", DataType.Double);
    types.put("Float", DataType.Float);
    types.put("Integer", DataType.Integer);
    types.put("Long", DataType.Long);
    types.put("String", DataType.String);
    types.put("Time", DataType.Time);
    return types;
  }
  
  @Test
  public void testStream(Vertx vertx, VertxTestContext testContext) {    
    
    FormatXlsx defn = FormatXlsx.builder()
            .headerColours(FormatXlsxColours.builder().fgColour("000000").bgColour("FFFFFF").build())
            .evenColours(FormatXlsxColours.builder().fgColour("000001").bgColour("FFFFFE").build())
            .oddColours(FormatXlsxColours.builder().fgColour("111111").bgColour("EEEEEE").build())
            .build();
    
    FileSystem fs = vertx.fileSystem();
    if (!fs.existsBlocking("target/temp")) {
      fs.mkdirBlocking("target/temp");
    }
    WriteStream<Buffer> writeStream = fs.openBlocking("target/temp/FormatXlsxInstanceTest.xlsx", new OpenOptions().setCreate(true));
    
    FormatXlsxInstance instance = (FormatXlsxInstance) defn.createInstance(vertx, null, writeStream);
    
    LinkedHashMap<String, DataType> types = buildTypes();
    instance.initialize(null, null)
            .compose(v -> addRow(types, 0, instance.getWriteStream()))
            .compose(v -> instance.getWriteStream().end())
            .onComplete(testContext.succeedingThenComplete());
  }
  
  private Future<Void> addRow(LinkedHashMap<String, DataType> types, int rowNum, WriteStream<DataRow> stream) {
    if (rowNum > 10) {
      return Future.succeededFuture();
    } else {
      DataRow row = createDataRow(types, rowNum);
      return stream.write(row)
              .compose(v -> addRow(types, rowNum + 1, stream));
    }
  }

  private DataRow createDataRow(LinkedHashMap<String, DataType> types, int rowNum) {
    DataRow row = new DataRow(types);
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
  
}
