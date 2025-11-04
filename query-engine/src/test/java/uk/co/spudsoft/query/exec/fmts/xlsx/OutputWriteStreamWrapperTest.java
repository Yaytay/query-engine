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
package uk.co.spudsoft.query.exec.fmts.xlsx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OutputWriteStreamWrapper.
 */
public class OutputWriteStreamWrapperTest {

  @Test
  @SuppressWarnings("unchecked")
  public void testWriteSingleByte() throws IOException {
    WriteStream<Buffer> mockStream = mock(WriteStream.class);
    when(mockStream.write(any(Buffer.class))).thenReturn(Future.succeededFuture());

    OutputWriteStreamWrapper wrapper = new OutputWriteStreamWrapper(mockStream);

    wrapper.write(65); // 'A'

    ArgumentCaptor<Buffer> bufferCaptor = ArgumentCaptor.forClass(Buffer.class);
    verify(mockStream).write(bufferCaptor.capture());

    Buffer capturedBuffer = bufferCaptor.getValue();
    assertEquals(1, capturedBuffer.length());
    assertEquals(65, capturedBuffer.getByte(0));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testWriteByteArray() throws IOException {
    WriteStream<Buffer> mockStream = mock(WriteStream.class);
    when(mockStream.write(any(Buffer.class))).thenReturn(Future.succeededFuture());

    OutputWriteStreamWrapper wrapper = new OutputWriteStreamWrapper(mockStream);

    byte[] data = "Hello World".getBytes();
    wrapper.write(data);

    ArgumentCaptor<Buffer> bufferCaptor = ArgumentCaptor.forClass(Buffer.class);
    verify(mockStream).write(bufferCaptor.capture());

    Buffer capturedBuffer = bufferCaptor.getValue();
    assertEquals(data.length, capturedBuffer.length());
    assertEquals("Hello World", capturedBuffer.toString());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testWriteByteArrayWithOffsetAndLength() throws IOException {
    WriteStream<Buffer> mockStream = mock(WriteStream.class);
    when(mockStream.write(any(Buffer.class))).thenReturn(Future.succeededFuture());

    OutputWriteStreamWrapper wrapper = new OutputWriteStreamWrapper(mockStream);

    byte[] data = "Hello World".getBytes();
    wrapper.write(data, 6, 5); // "World"

    ArgumentCaptor<Buffer> bufferCaptor = ArgumentCaptor.forClass(Buffer.class);
    verify(mockStream).write(bufferCaptor.capture());

    Buffer capturedBuffer = bufferCaptor.getValue();
    assertEquals(5, capturedBuffer.length());
    assertEquals("World", capturedBuffer.toString());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testWriteBuffer() {
    WriteStream<Buffer> mockStream = mock(WriteStream.class);
    when(mockStream.write(any(Buffer.class))).thenReturn(Future.succeededFuture());

    OutputWriteStreamWrapper wrapper = new OutputWriteStreamWrapper(mockStream);

    Buffer testBuffer = Buffer.buffer("Test Data");
    Future<Void> result = wrapper.write(testBuffer);

    verify(mockStream).write(testBuffer);
    assertTrue(result.succeeded());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testWriteBufferWithHandler() {
    WriteStream<Buffer> mockStream = mock(WriteStream.class);
    when(mockStream.write(any(Buffer.class))).thenReturn(Future.succeededFuture());

    OutputWriteStreamWrapper wrapper = new OutputWriteStreamWrapper(mockStream);

    Buffer testBuffer = Buffer.buffer("Test Data");
    Handler<AsyncResult<Void>> handler = mock(Handler.class);

    wrapper.write(testBuffer).andThen(handler);

    verify(mockStream).write(eq(testBuffer));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testWriteQueueFull() {
    WriteStream<Buffer> mockStream = mock(WriteStream.class);
    when(mockStream.writeQueueFull()).thenReturn(true);

    OutputWriteStreamWrapper wrapper = new OutputWriteStreamWrapper(mockStream);

    assertTrue(wrapper.writeQueueFull());
    verify(mockStream).writeQueueFull();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testWriteQueueNotFull() {
    WriteStream<Buffer> mockStream = mock(WriteStream.class);
    when(mockStream.writeQueueFull()).thenReturn(false);

    OutputWriteStreamWrapper wrapper = new OutputWriteStreamWrapper(mockStream);

    assertFalse(wrapper.writeQueueFull());
    verify(mockStream).writeQueueFull();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testSetWriteQueueMaxSize() {
    WriteStream<Buffer> mockStream = mock(WriteStream.class);
    when(mockStream.setWriteQueueMaxSize(anyInt())).thenReturn(mockStream);

    OutputWriteStreamWrapper wrapper = new OutputWriteStreamWrapper(mockStream);

    wrapper.setWriteQueueMaxSize(500);

    verify(mockStream).setWriteQueueMaxSize(500);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testDrainHandlerWhenQueueNotFull() {
    WriteStream<Buffer> mockStream = mock(WriteStream.class);
    when(mockStream.writeQueueFull()).thenReturn(false);
    when(mockStream.drainHandler(any())).thenReturn(mockStream);

    OutputWriteStreamWrapper wrapper = new OutputWriteStreamWrapper(mockStream);

    Handler<Void> drainHandler = mock(Handler.class);
    wrapper.drainHandler(drainHandler);

    verify(mockStream).drainHandler(drainHandler);
    verify(drainHandler).handle(null); // Should be called immediately when queue not full
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testDrainHandlerWhenQueueFull() {
    WriteStream<Buffer> mockStream = mock(WriteStream.class);
    when(mockStream.writeQueueFull()).thenReturn(true);
    when(mockStream.drainHandler(any())).thenReturn(mockStream);

    OutputWriteStreamWrapper wrapper = new OutputWriteStreamWrapper(mockStream);

    Handler<Void> drainHandler = mock(Handler.class);
    wrapper.drainHandler(drainHandler);

    verify(mockStream).drainHandler(drainHandler);
    verify(drainHandler, never()).handle(null); // Should NOT be called when queue is full
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testExceptionHandler() {
    WriteStream<Buffer> mockStream = mock(WriteStream.class);
    when(mockStream.exceptionHandler(any())).thenReturn(mockStream);

    OutputWriteStreamWrapper wrapper = new OutputWriteStreamWrapper(mockStream);

    Handler<Throwable> exceptionHandler = mock(Handler.class);
    wrapper.exceptionHandler(exceptionHandler);

    // Verify that the wrapper returns itself for fluent chaining
    assertSame(wrapper, wrapper.exceptionHandler(exceptionHandler));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testExceptionHandlerPropagation() {
    WriteStream<Buffer> mockStream = mock(WriteStream.class);
    ArgumentCaptor<Handler<Throwable>> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
    when(mockStream.exceptionHandler(handlerCaptor.capture())).thenReturn(mockStream);

    OutputWriteStreamWrapper wrapper = new OutputWriteStreamWrapper(mockStream);

    Handler<Throwable> exceptionHandler = mock(Handler.class);
    wrapper.exceptionHandler(exceptionHandler);

    // Simulate an exception from the underlying stream
    RuntimeException testException = new RuntimeException("Test exception");
    handlerCaptor.getValue().handle(testException);

    verify(exceptionHandler).handle(testException);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testClose() throws IOException {
    WriteStream<Buffer> mockStream = mock(WriteStream.class);
    when(mockStream.end()).thenReturn(io.vertx.core.Future.succeededFuture());

    OutputWriteStreamWrapper wrapper = new OutputWriteStreamWrapper(mockStream);

    wrapper.close();

    verify(mockStream).end();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testEndWithHandler() {
    WriteStream<Buffer> mockStream = mock(WriteStream.class);
    // The wrapper's end() returns the Future from mockStream.end(), so stub it
    when(mockStream.end()).thenReturn(Future.succeededFuture());

    OutputWriteStreamWrapper wrapper = new OutputWriteStreamWrapper(mockStream);

    Handler<AsyncResult<Void>> endHandler = mock(Handler.class);

    // Act: call wrapper.end() to obtain the future, then attach our handler to THAT future
    Future<Void> returned = wrapper.end();
    returned.andThen(endHandler);

    // Verify we invoked end() on the underlying stream
    verify(mockStream).end();

    // Since we stubbed end() with a succeeded future, our handler should be invoked once
    verify(endHandler, times(1)).handle(any(AsyncResult.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetFinalFuture() {
    WriteStream<Buffer> mockStream = mock(WriteStream.class);
    when(mockStream.end()).thenReturn(Future.succeededFuture());

    OutputWriteStreamWrapper wrapper = new OutputWriteStreamWrapper(mockStream);

    Future<Void> finalFuture = wrapper.getFinalFuture();
    assertNotNull(finalFuture);
    assertFalse(finalFuture.isComplete());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetFinalFutureCompletesOnSuccessfulEnd() {
    WriteStream<Buffer> mockStream = mock(WriteStream.class);
    when(mockStream.end()).thenReturn(Future.succeededFuture());

    OutputWriteStreamWrapper wrapper = new OutputWriteStreamWrapper(mockStream);

    Future<Void> finalFuture = wrapper.getFinalFuture();

    // Act
    Future<Void> returned = wrapper.end();
    // Returned future already succeeded due to stubbing above

    // Assert
    assertTrue(returned.succeeded());
    assertTrue(finalFuture.isComplete());
    assertTrue(finalFuture.succeeded());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetFinalFutureFailsOnEndFailure() {
    WriteStream<Buffer> mockStream = mock(WriteStream.class);
    RuntimeException testException = new RuntimeException("End failed");
    when(mockStream.end()).thenReturn(Future.failedFuture(testException));

    OutputWriteStreamWrapper wrapper = new OutputWriteStreamWrapper(mockStream);

    Future<Void> finalFuture = wrapper.getFinalFuture();

    // Act
    Future<Void> returned = wrapper.end();

    // Assert
    assertTrue(returned.failed());
    assertEquals(testException, returned.cause());
    assertTrue(finalFuture.isComplete());
    assertTrue(finalFuture.failed());
    assertEquals(testException, finalFuture.cause());
  }
}
