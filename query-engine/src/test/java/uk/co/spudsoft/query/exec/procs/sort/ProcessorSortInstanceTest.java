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
package uk.co.spudsoft.query.exec.procs.sort;

import inet.ipaddr.IPAddressString;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.defn.ProcessorSort;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.exec.procs.ListReadStream;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProcessorSortInstanceTest {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ProcessorSortInstanceTest.class);
  
  @Test
  public void testInitialize(Vertx vertx, VertxTestContext testContext) {
    Types types = new Types();
    List<DataRow> rowsList = Arrays.asList(
              DataRow.create(types, "id", 1, "timestamp", LocalDateTime.of(1971, Month.MARCH, 3, 5, 1), "value", "one")
            , DataRow.create(types, "id", 2, "timestamp", LocalDateTime.of(1971, Month.MARCH, 3, 5, 2), "value", "one")
            , DataRow.create(types, "id", 3, "timestamp", LocalDateTime.of(1971, Month.MARCH, 3, 5, 3), "value", "one")
            , DataRow.create(types, "id", 4, "timestamp", LocalDateTime.of(1971, Month.MARCH, 3, 5, 4), "value", "one")
    );
    
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    
    ProcessorSortInstance instance = new ProcessorSortInstance(vertx, null, pipelineContext
            , ProcessorSort.builder().fields(Arrays.asList("timestamp")).build()
            , "P0-Sort"
    );
    instance.initialize(null, null, "source", 1, new ReadStreamWithTypes(new ListReadStream<>(vertx.getOrCreateContext(), rowsList), types))
            .andThen(testContext.succeedingThenComplete());
  }

  @Test
  public void testGetId() {
    ProcessorSort defn = ProcessorSort.builder().name("id").build();
    ProcessorSortInstance instance = new ProcessorSortInstance(null, null, null, defn, "P0-Sort");
    assertEquals("P0-Sort", instance.getName());
  }
  
  @Test
  public void testSerialize(Vertx vertx) throws IOException {
    Types types = new Types();
    DataRow row = DataRow.create(types
            , "integer", 1
            , "datetime", LocalDateTime.of(1971, Month.MARCH, 3, 5, 1)
            , "string", "one"
            , "null", null
            , "boolean", Boolean.FALSE
            , "date", LocalDate.of(1971, Month.MARCH, 3)
            , "time", LocalTime.of(5, 1)
            , "double", (Double) 3.141
            , "float", (Float) (float) 6.282
            , "long", Long.MAX_VALUE
    );

    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    
    ProcessorSortInstance instance = new ProcessorSortInstance(vertx, null, pipelineContext
            , ProcessorSort.builder().fields(Arrays.asList("timestamp")).build()
            , "P0-Sort"
    );
    // This will fail, but its only purpose here is to set the types
    instance.initialize(null, null, null, 0, new ReadStreamWithTypes(null, types));
    
    byte[] serialized = instance.dataRowSerializer(row);
    assertNotNull(serialized);
    assertThat(serialized.length, equalTo(124));
    
    DataRow deserialized = instance.dataRowDeserializer(serialized);
    assertNotNull(deserialized);
    
    assertEquals(row.size(), deserialized.size());
    assertEquals(row.get("integer"), deserialized.get("integer"));
    assertEquals(row.get("datetime"), deserialized.get("datetime"));
    assertEquals(row.get("string"), deserialized.get("string"));
    assertEquals(row.get("null"), deserialized.get("null"));
    assertEquals(row.get("boolean"), deserialized.get("boolean"));
    assertEquals(row.get("date"), deserialized.get("date"));
    assertEquals(row.get("time"), deserialized.get("time"));
    assertEquals(row.get("double"), deserialized.get("double"));
    assertEquals(row.get("float"), deserialized.get("float"));
    assertEquals(row.get("long"), deserialized.get("long"));
  }

  
  @Test
  public void testSerializeNotNull(Vertx vertx) throws IOException {
    Types types = new Types();
    DataRow row = DataRow.create(types
            , "integer", 1
            , "wasnull", null
    );
    assertEquals(DataType.Null, row.getType("wasnull"));

    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    
    ProcessorSortInstance instance = new ProcessorSortInstance(vertx, null, pipelineContext
            , ProcessorSort.builder().fields(Arrays.asList("timestamp")).build()
            , "P0-Sort"
    );
    // This will fail, but its only purpose here is to set the types
    instance.initialize(null, null, null, 0, new ReadStreamWithTypes(null, types));
    
    byte[] serialized = instance.dataRowSerializer(row);
    assertNotNull(serialized);
    assertThat(serialized.length, equalTo(12));
    
    // To change the type of wasnull
    DataRow.create(types
            , "integer", 1
            , "wasnull", "Not null any more"
    );

    DataRow deserialized = instance.dataRowDeserializer(serialized);
    assertNotNull(deserialized);
    
    assertEquals(row.size(), deserialized.size());
    assertEquals(row.get("integer"), deserialized.get("integer"));
    assertEquals(row.get("wasnull"), deserialized.get("wasnull"));
    
    assertNull(row.get("wasnull"));
    assertEquals(DataType.String, row.getType("wasnull"));
  }
  
}
