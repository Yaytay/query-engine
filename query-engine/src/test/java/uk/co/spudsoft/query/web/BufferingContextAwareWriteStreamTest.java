/*
 * Copyright (C) 2025 njt
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
package uk.co.spudsoft.query.web;

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(VertxExtension.class)
public class BufferingContextAwareWriteStreamTest {

  @Test
  @SuppressWarnings("unchecked")
  void testWriteQueueFullAndDrain(Vertx vertx, VertxTestContext testContext) throws Exception {
    WriteStream<Buffer> mockDelegate = mock(WriteStream.class);
    Context producerContext = vertx.getOrCreateContext();
    Context consumerContext = vertx.getOrCreateContext();

    // Use a small threshold for testing
    BufferingContextAwareWriteStream stream = new BufferingContextAwareWriteStream(mockDelegate, consumerContext, 100);

    // Get access to internal counter to simulate backlog
    Field pendingField = BufferingContextAwareWriteStream.class.getDeclaredField("pendingFlushes");
    pendingField.setAccessible(true);
    AtomicInteger pendingFlushes = (AtomicInteger) pendingField.get(stream);

    // 1. Test writeQueueFull based on delegate
    when(mockDelegate.writeQueueFull()).thenReturn(true);
    assertTrue(stream.writeQueueFull(), "Should be full if delegate is full");

    // 2. Test writeQueueFull based on pending flushes
    when(mockDelegate.writeQueueFull()).thenReturn(false);
    pendingFlushes.set(20); // Greater than MAX_PENDING_WRITES (16)
    assertTrue(stream.writeQueueFull(), "Should be full if too many flushes pending");

    // 3. Test Drain Handler registration and execution
    producerContext.runOnContext(v -> {
      stream.drainHandler(v2 -> {
        // This is what we are waiting for
        testContext.verify(() -> {
          assertFalse(stream.writeQueueFull(), "Drain should only fire when queue is not full");
        });
        testContext.completeNow();
      });

      // Capture the handler passed to the delegate
      consumerContext.runOnContext(v3 -> {
        ArgumentCaptor<Handler<Void>> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
        verify(mockDelegate, timeout(1000)).drainHandler(handlerCaptor.capture());
        Handler<Void> capturedHandler = handlerCaptor.getValue();

        // Simulate queue becoming free
        pendingFlushes.set(0);
        when(mockDelegate.writeQueueFull()).thenReturn(false);

        // Trigger the delegate's drain signal
        capturedHandler.handle(null);
      });
    });
  }

  @Test
  @SuppressWarnings("unchecked")
  void testDrainFiredOnFlushCompletion(Vertx vertx, VertxTestContext testContext) throws Exception {
    WriteStream<Buffer> mockDelegate = mock(WriteStream.class);
    Context consumerContext = vertx.getOrCreateContext();
    
    // Setup mock to return a future we can control
    io.vertx.core.Promise<Void> writePromise = io.vertx.core.Promise.promise();
    when(mockDelegate.write(any())).thenReturn(writePromise.future());
    when(mockDelegate.writeQueueFull()).thenReturn(false);

    BufferingContextAwareWriteStream stream = new BufferingContextAwareWriteStream(mockDelegate, consumerContext, 10);

    stream.drainHandler(v -> {
      testContext.completeNow();
    });

    // Manually pump up the pending flushes to trigger "full"
    Field pendingField = BufferingContextAwareWriteStream.class.getDeclaredField("pendingFlushes");
    pendingField.setAccessible(true);
    AtomicInteger pendingFlushes = (AtomicInteger) pendingField.get(stream);
    pendingFlushes.set(17); // > 16

    assertTrue(stream.writeQueueFull());

    pendingFlushes.set(8); // 16 / 2
    
    consumerContext.runOnContext(v -> {
      // Trigger a write that will complete and decrement the counter
      stream.write(Buffer.buffer("0123456789012345")); // Triggers flush()
    });

    // Complete the delegate write
    consumerContext.runOnContext(v -> {
      writePromise.complete();
    });
    
    // If the drainHandler isn't called within 2s, the test will timeout (fail)
  }
}