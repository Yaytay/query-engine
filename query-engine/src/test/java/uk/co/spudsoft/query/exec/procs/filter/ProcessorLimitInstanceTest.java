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

import io.vertx.core.Context;
import uk.co.spudsoft.query.exec.procs.filters.ProcessorLimitInstance;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.DataRow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import uk.co.spudsoft.query.defn.ProcessorLimit;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.fmts.ReadStreamToList;
import uk.co.spudsoft.query.exec.procs.ListReadStream;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProcessorLimitInstanceTest {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ProcessorLimitInstanceTest.class);
  
  @Test
  public void testInitialize(Vertx vertx) {
    Types types = new Types();
    List<DataRow> rowsList = Arrays.asList(
              DataRow.create(types, "id", 1, "timestamp", LocalDateTime.of(1971, Month.MARCH, 3, 5, 1), "value", "one")
            , DataRow.create(types, "id", 2, "timestamp", LocalDateTime.of(1971, Month.MARCH, 3, 5, 2), "value", "two")
            , DataRow.create(types, "id", 3, "timestamp", LocalDateTime.of(1971, Month.MARCH, 3, 5, 3), "value", "three")
            , DataRow.create(types, "id", 4, "timestamp", LocalDateTime.of(1971, Month.MARCH, 3, 5, 4), "value", "four")
    );
    
    Context context = vertx.getOrCreateContext();
    
    ProcessorLimitInstance instance = new ProcessorLimitInstance(vertx, ctx -> {}, context
            , ProcessorLimit.builder().limit(17).build()
    );
    assertTrue(instance.initialize(null, null, "source", 1, new ReadStreamWithTypes(new ListReadStream<>(context, rowsList), types)).isComplete());
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
    
    Context context = vertx.getOrCreateContext();
    
    ProcessorLimitInstance instance = new ProcessorLimitInstance(vertx, ctx -> {}, context
            , ProcessorLimit.builder().limit(3).build()
    );
    instance.initialize(null, null, "source", 1, new ReadStreamWithTypes(new ListReadStream<>(context, rowsList), types))
            .compose(v -> {
              return ReadStreamToList.capture(instance.getReadStream());
            })
            .onFailure(ex -> {
              testContext.failNow(ex);
            })
            .onSuccess(rows -> {
              testContext.verify(() -> {
                assertEquals(3, rows.size());
                assertEquals(1, rows.get(0).get("id"));
                assertEquals(2, rows.get(1).get("id"));
                assertEquals(3, rows.get(2).get("id"));
              });
              testContext.completeNow();
            });
  }
  
}
