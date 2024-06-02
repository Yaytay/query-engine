/*
 * Copyright (C) 2024 jtalbut
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

import uk.co.spudsoft.query.exec.procs.ListReadStream;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ListReadStreamTest {
  
  private static final Logger logger = LoggerFactory.getLogger(ListReadStreamTest.class);
  
  @Test
  public void testSimpleRun(Vertx vertx, VertxTestContext testContext) {
    List<Integer> items = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);
    List<Integer> captured = new ArrayList<>();
    ListReadStream<Integer> lrs = new ListReadStream<>(vertx.getOrCreateContext(), items);
    lrs.endHandler(v -> {
      testContext.verify(() -> {
        assertEquals(items, captured);
      });
      testContext.completeNow();
    });
    lrs.exceptionHandler(ex -> {
      logger.error("Failed: ", ex);
      testContext.failNow(ex);
    });
    lrs.handler(item -> {
      captured.add(item);
    });
    assertThrows(IllegalArgumentException.class, () -> {
      lrs.fetch(-1);
    });
    lrs.fetch(1);
    lrs.pause();
    lrs.resume();
    lrs.fetch(Long.MAX_VALUE);
    lrs.fetch(Long.MAX_VALUE);
  }
  
  @Test
  public void testException(Vertx vertx, VertxTestContext testContext) {
    List<Integer> items = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);
    List<Integer> captured = new ArrayList<>();
    ListReadStream<Integer> lrs = new ListReadStream<>(vertx.getOrCreateContext(), items);
    AtomicBoolean exceptionHandlerCalled = new AtomicBoolean(false);
    lrs.endHandler(v -> {
      testContext.verify(() -> {
        assertEquals(items, captured);
        assertTrue(exceptionHandlerCalled.get());
      });
      testContext.completeNow();
    });
    lrs.exceptionHandler(ex -> {
      logger.error("Something bad happened: ", ex);
      testContext.verify(() -> {
        assertEquals("I don't like 5", ex.getMessage());
      });
      exceptionHandlerCalled.set(true);
    });
    lrs.handler(item -> {
      captured.add(item);      
      if (item == 5) {
        throw new IllegalStateException("I don't like 5");
      }
    });
    lrs.fetch(1);
    lrs.pause();
    lrs.resume();
  }
  
}
