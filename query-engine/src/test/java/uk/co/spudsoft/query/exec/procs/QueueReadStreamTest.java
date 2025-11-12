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
package uk.co.spudsoft.query.exec.procs;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;
import uk.co.spudsoft.query.exec.fmts.ReadStreamToList;

/**
 *
 * @author njt
 */
@ExtendWith(VertxExtension.class)
public class QueueReadStreamTest {
  
  private static final Logger logger = LoggerFactory.getLogger(QueueReadStreamTest.class);
  
  @Test
  public void testStream(Vertx vertx, VertxTestContext testContext) {
    
    QueueReadStream<Integer> qrs = new QueueReadStream<>(vertx.getOrCreateContext());
    AtomicInteger value = new AtomicInteger();
    ReadStreamToList.capture(qrs)
            .andThen(ar -> {
              logger.debug("Got {}", ar);
              if (ar.failed()) {
                testContext.failNow(ar.cause());
              } else {
                testContext.verify(() -> {
                  assertThat(ar.result(), hasSize(50));
                });
              }
              testContext.completeNow();
            });
    
    vertx.setPeriodic(50, handle -> {
      int next = value.addAndGet(1);
      qrs.add(next);
      if (next >= 50) {
        vertx.cancelTimer(handle);
        logger.debug("Finished");
        qrs.complete();
      }      
    });
    
  }
  
  @Test
  public void testManual(Vertx vertx) throws InterruptedException {
    
    QueueReadStream<Integer> qrs = new QueueReadStream<>(vertx.getOrCreateContext());
    for (int i = 0; i < 30; ++i) {
      qrs.add(i);
    }
    qrs.complete();
    
    IllegalStateException ex1 = assertThrows(IllegalStateException.class, () -> {
      qrs.add(31);
    });
    assertEquals("Last item has already been sent", ex1.getMessage());
    
    IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> {
      qrs.fetch(-1);
    });
    assertEquals("Negative fetch amount", ex2.getMessage());
    
    List<Integer> received = new ArrayList<>();
    AtomicBoolean ended = new AtomicBoolean();
    qrs.handler(value -> {
      received.add(value);
    });
    qrs.endHandler(v -> {
      ended.set(true);
    });
    
    qrs.fetch(5);
    await().atMost(1, TimeUnit.MINUTES).until(() -> {
      return received.size() >= 5;
    });
    Thread.sleep(100);
    assertThat(received, hasSize(5));
    assertFalse(ended.get());

    qrs.fetch(10);
    await().atMost(1, TimeUnit.MINUTES).until(() -> {
      return received.size() >= 15;
    });
    Thread.sleep(100);
    assertThat(received, hasSize(15));
    assertFalse(ended.get());

    qrs.fetch(10);
    await().atMost(1, TimeUnit.MINUTES).until(() -> {
      return received.size() >= 25;
    });
    Thread.sleep(100);
    assertThat(received, hasSize(25));
    assertFalse(ended.get());
    

    qrs.fetch(10);
    await().atMost(1, TimeUnit.MINUTES).until(() -> {
      return received.size() >= 30;
    });
    Thread.sleep(100);
    assertThat(received, hasSize(30));
    assertTrue(ended.get());
  }
  
}
