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
package uk.co.spudsoft.query.exec;

import io.vertx.core.Vertx;
import io.vertx.core.streams.WriteStream;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class NullWriteStreamTest {
  
  @Test
  public void testEnd(Vertx vertx, VertxTestContext testContext) {
    WriteStream<String> stream = new NullWriteStream<>();
    stream.exceptionHandler(ex -> {
      testContext.failNow(ex);
    });
    stream.drainHandler(v -> {
      testContext.failNow("Drain handler called");
    });
    stream.setWriteQueueMaxSize(0);
    stream.setWriteQueueMaxSize(-1);
    stream.setWriteQueueMaxSize(1);
    assertFalse(stream.writeQueueFull());
    assertTrue(stream.write("1").succeeded());
    assertFalse(stream.writeQueueFull());
    assertTrue(stream.write("2").succeeded());
    assertFalse(stream.writeQueueFull());
    assertTrue(stream.write("3").succeeded());
    assertFalse(stream.writeQueueFull());
    stream.write("4").andThen(ar -> {
      testContext.verify(() -> {
        assertTrue(ar.succeeded());
      });
    });
    assertFalse(stream.writeQueueFull());
    assertTrue(stream.end().succeeded());
    testContext.completeNow();
  }

}
