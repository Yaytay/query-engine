/*
 * Copyright (C) 2024 njt
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
import io.vertx.core.streams.ReadStream;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author njt
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RandomIntegerReadStreamTest {
  
  private static final Logger logger = LoggerFactory.getLogger(RandomIntegerReadStreamTest.class);
  
  private static final int COUNT = 2000;
  
  @Test
  public void testSimpleRun(Vertx vertx, VertxTestContext testContext) {
    AtomicInteger received = new AtomicInteger();
    ReadStream<Integer> lrs = new RandomIntegerReadStream(vertx.getOrCreateContext(), COUNT);
    lrs.endHandler(v -> {
      testContext.verify(() -> {
        assertEquals(COUNT, received.get());
      });
      testContext.completeNow();
    });
    lrs.exceptionHandler(ex -> {
      logger.error("Failed: ", ex);
      testContext.failNow(ex);
    });
    lrs.handler(item -> {
      received.incrementAndGet();
    });
    lrs.fetch(1);
    lrs.pause();
    lrs.resume();
  }
  
}
