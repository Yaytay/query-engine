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
package uk.co.spudsoft.query.exec.procs.sort;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.streams.ReadStream;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.TestInfo;
import static org.mockito.Mockito.*;

@ExtendWith(VertxExtension.class)
public class SortingStream_FileBufferedSourceTest {

  @SuppressWarnings("unchecked")
  private Object createSource(Vertx vertx, TestInfo testInfo, SerializeReadStream<Integer> mockReadStream) throws Exception {
    SortingStream<Integer> ss = new SortingStream<>(
        vertx.getOrCreateContext(),
        vertx.fileSystem(),
        Comparator.naturalOrder(),
        i -> SerializeWriteStream.byteArrayFromInt(i),
        b -> SerializeReadStream.intFromByteArray(b),
        "target/temp/" + testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName(),
        "test",
        1000,
        i -> 1,
        (ReadStream<Integer>) mock(ReadStream.class)
    );

    Class<?> clazz = Class.forName("uk.co.spudsoft.query.exec.procs.sort.SortingStream$FileBufferedSource");
    Constructor<?> constructor = clazz.getDeclaredConstructor(SortingStream.class, SerializeReadStream.class);
    constructor.setAccessible(true);
    return constructor.newInstance(ss, mockReadStream);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testInitializeTwice(Vertx vertx, TestInfo testInfo) throws Exception {
    SerializeReadStream<Integer> mockReadStream = mock(SerializeReadStream.class);
    Object source = createSource(vertx, testInfo, mockReadStream);
    Method init = source.getClass().getDeclaredMethod("initialize");
    init.setAccessible(true);

    // First call
    init.invoke(source);
    // Second call - should skip logic due to streamStarted flag
    init.invoke(source);

    // Verify stream handlers were only set once
    verify(mockReadStream, times(1)).handler(any());
    verify(mockReadStream, times(1)).endHandler(any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testFillBufferExhaustive(Vertx vertx, TestInfo testInfo) throws Exception {
    SerializeReadStream<Integer> mockReadStream = mock(SerializeReadStream.class);
    Object source = createSource(vertx, testInfo, mockReadStream);
    
    Method fillBuffer = source.getClass().getDeclaredMethod("fillBuffer", int.class);
    fillBuffer.setAccessible(true);

    Field streamEndedField = source.getClass().getDeclaredField("streamEnded");
    streamEndedField.setAccessible(true);

    Field bufferField = source.getClass().getSuperclass().getDeclaredField("buffer");
    bufferField.setAccessible(true);
    Queue<Integer> buffer = (Queue<Integer>) bufferField.get(source);

    Field fillPromiseField = source.getClass().getDeclaredField("fillPromise");
    fillPromiseField.setAccessible(true);

    // Line 1-3: if (streamEnded) return succeeded
    streamEndedField.set(source, true);
    Future<Void> f1 = (Future<Void>) fillBuffer.invoke(source, 100);
    assertTrue(f1.succeeded());

    // Line 6-8: if (!buffer.isEmpty()) return succeeded
    streamEndedField.set(source, false);
    buffer.add(42);
    Future<Void> f2 = (Future<Void>) fillBuffer.invoke(source, 1);
    assertTrue(f2.succeeded());

    // Line 11-13: if (fillPromise != null) return existing future
    buffer.clear();
    Promise<Void> existingPromise = Promise.promise();
    fillPromiseField.set(source, existingPromise);
    Future<Void> f3 = (Future<Void>) fillBuffer.invoke(source, 100);
    assertSame(existingPromise.future(), f3);
    
    // Cleanup for GC
    existingPromise.complete();
  }

  @Test
  @SuppressWarnings("unchecked")
  void testCheckFillCompleteExhaustive(Vertx vertx, TestInfo testInfo) throws Exception {
    SerializeReadStream<Integer> mockReadStream = mock(SerializeReadStream.class);
    Object source = createSource(vertx, testInfo, mockReadStream);
    
    Method checkFillComplete = source.getClass().getDeclaredMethod("checkFillComplete");
    checkFillComplete.setAccessible(true);

    Field fillPromiseField = source.getClass().getDeclaredField("fillPromise");
    fillPromiseField.setAccessible(true);

    Field streamEndedField = source.getClass().getDeclaredField("streamEnded");
    streamEndedField.setAccessible(true);

    Field bufferField = source.getClass().getSuperclass().getDeclaredField("buffer");
    bufferField.setAccessible(true);
    Queue<Integer> buffer = (Queue<Integer>) bufferField.get(source);

    // Exhaustive test of first line: if (fillPromise != null && (!buffer.isEmpty() || streamEnded))
    
    // Case 1: fillPromise is null -> returns immediately
    fillPromiseField.set(source, null);
    checkFillComplete.invoke(source); // Should not throw even if buffer has data

    // Case 2: fillPromise exists, but buffer empty AND stream not ended (Condition is FALSE)
    Promise<Void> p2 = Promise.promise();
    fillPromiseField.set(source, p2);
    buffer.clear();
    streamEndedField.set(source, false);
    checkFillComplete.invoke(source);
    assertFalse(p2.future().isComplete(), "Promise should remain incomplete if condition is false");

    // Case 3: fillPromise exists AND buffer NOT empty (Condition is TRUE)
    buffer.add(1);
    checkFillComplete.invoke(source);
    assertTrue(p2.future().isComplete(), "Promise should complete if buffer has data");

    // Case 4: fillPromise exists AND stream ended (Condition is TRUE)
    Promise<Void> p4 = Promise.promise();
    fillPromiseField.set(source, p4);
    buffer.clear();
    streamEndedField.set(source, true);
    checkFillComplete.invoke(source);
    assertTrue(p4.future().isComplete(), "Promise should complete if stream ended");
  }
}