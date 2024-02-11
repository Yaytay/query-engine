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

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.streams.WriteStream;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.ProcessorSort;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.DataRowStream;
import uk.co.spudsoft.query.exec.procs.PassthroughStreamTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
  public void testInitialize(Vertx vertx) {
    ProcessorSortInstance instance = new ProcessorSortInstance(vertx, ctx -> {}, vertx.getOrCreateContext(), null);
    assertEquals(Future.succeededFuture(), instance.initialize(null, null, "source", 1));
  }
  
  
  
  @Test
  public void testStream(Vertx vertx, VertxTestContext testContext) {
    
    ProcessorSort definition = ProcessorSort.builder()
            .fields(Arrays.asList("value"))
            .build();
    ProcessorSortInstance instance = new ProcessorSortInstance(vertx, ctx -> {}, vertx.getOrCreateContext(), definition);
    WriteStream<DataRow> write = instance.getWriteStream();
    DataRowStream<DataRow> read = instance.getReadStream();
    List<DataRow> received = new ArrayList<>();
    read.fetch(12);
    assertEquals(0, read.getColumnDescriptors().size());
    read.handler(jo -> {
      received.add(jo);
      read.pause();
      vertx.setTimer(1, v -> {
        read.fetch(1);
      });
    });
    read.exceptionHandler(ex -> {
      logger.debug("Exception: ", ex);
    });
    read.endHandler(v -> {
      testContext.verify(() -> {
        logger.debug("received: {}", received);
        assertEquals(1, read.getColumnDescriptors().size());
        assertThat(received, hasSize(7));
        assertThat(received.get(0).get("value"), equalTo(0));
        assertThat(received.get(1).get("value"), equalTo(1));
        assertThat(received.get(2).get("value"), equalTo(2));
        assertThat(received.get(3).get("value"), equalTo(3));
      });
      testContext.completeNow();
    });
    PassthroughStreamTest.writeData(vertx, write, 6);
  }
}
