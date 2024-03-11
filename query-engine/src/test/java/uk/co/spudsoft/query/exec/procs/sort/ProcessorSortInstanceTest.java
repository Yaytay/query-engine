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
import uk.co.spudsoft.query.exec.Types;
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

    ProcessorSortInstance instance = new ProcessorSortInstance(vertx, ctx -> {}, vertx.getOrCreateContext(), null);
    instance.initialize(null, null, "source", 1, new ListReadStream<>(null, rowsList))
            .andThen(testContext.succeedingThenComplete());
            
  }
}
