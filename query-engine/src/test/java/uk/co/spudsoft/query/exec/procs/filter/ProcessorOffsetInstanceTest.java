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
package uk.co.spudsoft.query.exec.procs.filter;

import inet.ipaddr.IPAddressString;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.defn.ProcessorOffset;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.exec.fmts.ReadStreamToList;
import uk.co.spudsoft.query.exec.procs.ListReadStream;
import uk.co.spudsoft.query.exec.procs.filters.ProcessorOffsetInstance;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProcessorOffsetInstanceTest {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ProcessorOffsetInstanceTest.class);
  
  @Test
  public void testInitialize(Vertx vertx) {
    Types types = new Types();
    List<DataRow> rowsList = Arrays.asList(
              DataRow.create(types, "id", 1, "timestamp", LocalDateTime.of(1971, Month.MARCH, 3, 5, 1), "value", "one")
            , DataRow.create(types, "id", 2, "timestamp", LocalDateTime.of(1971, Month.MARCH, 3, 5, 2), "value", "two")
            , DataRow.create(types, "id", 3, "timestamp", LocalDateTime.of(1971, Month.MARCH, 3, 5, 3), "value", "three")
            , DataRow.create(types, "id", 4, "timestamp", LocalDateTime.of(1971, Month.MARCH, 3, 5, 4), "value", "four")
    );
    
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
        
    ProcessorOffsetInstance instance = new ProcessorOffsetInstance(vertx, null, pipelineContext, ProcessorOffset.builder().offset(17).build(), "P0-Offset");
    assertEquals("P0-Offset", instance.getName());
    assertTrue(instance.initialize(null, null, "source", 1, new ReadStreamWithTypes(new ListReadStream<>(pipelineContext, vertx.getOrCreateContext(), rowsList), types)).succeeded());
  }

  
  @Test
  public void testRun(Vertx vertx, VertxTestContext testContext) {

    Types types = new Types();
    List<DataRow> rowsList = Arrays.asList(
              DataRow.create(types, "id", 1, "timestamp", LocalDateTime.of(1971, Month.MARCH, 3, 5, 1), "value", "one")
            , DataRow.create(types, "id", 2, "timestamp", LocalDateTime.of(1971, Month.MARCH, 3, 5, 2), "value", "two")
            , DataRow.create(types, "id", 3, "timestamp", LocalDateTime.of(1971, Month.MARCH, 3, 5, 3), "value", "three")
            , DataRow.create(types, "id", 4, "timestamp", LocalDateTime.of(1971, Month.MARCH, 3, 5, 4), "value", "four")
    );
    
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    
    ProcessorOffsetInstance instance = new ProcessorOffsetInstance(vertx, null, pipelineContext
            , ProcessorOffset.builder().name("fred").offset(2).build()
            , "P0-Offset"
    );
    assertEquals("P0-Offset", instance.getName());
    instance.initialize(null, null, "source", 1, new ReadStreamWithTypes(new ListReadStream<>(pipelineContext, vertx.getOrCreateContext(), rowsList), types))
            .compose(rswt -> {
              return ReadStreamToList.capture(pipelineContext, rswt.getStream());
            })
            .onFailure(ex -> {
              testContext.failNow(ex);
            })
            .onSuccess(rows -> {
              testContext.verify(() -> {
                assertEquals(2, rows.size());
                assertEquals(3, rows.get(0).get("id"));
                assertEquals(4, rows.get(1).get("id"));
              });
              testContext.completeNow();
            });
  }
    
}
