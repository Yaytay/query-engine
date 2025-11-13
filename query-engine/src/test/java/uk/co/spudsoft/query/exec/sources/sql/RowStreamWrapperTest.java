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
package uk.co.spudsoft.query.exec.sources.sql;

import inet.ipaddr.IPAddressString;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.DataRow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.context.RequestContext;

/**
 *
 * @author jtalbut
 */
public class RowStreamWrapperTest {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(RowStreamWrapperTest.class);
  
  @Test
  public void testPause() {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    
    @SuppressWarnings("unchecked")
    MetadataRowStreamImpl target = mock(MetadataRowStreamImpl.class);
    RowStreamWrapper instance = new RowStreamWrapper(pipelineContext, null, null, target, null);
    instance.pause();
    verify(target, times(2)).pause();
  }

  @Test
  public void testResume() {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    @SuppressWarnings("unchecked")
    MetadataRowStreamImpl target = mock(MetadataRowStreamImpl.class);
    RowStreamWrapper instance = new RowStreamWrapper(pipelineContext, null, null, target, null);
    instance.resume();
    verify(target).resume();
  }

  @Test
  public void testFetch() {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    @SuppressWarnings("unchecked")
    MetadataRowStreamImpl target = mock(MetadataRowStreamImpl.class);
    RowStreamWrapper instance = new RowStreamWrapper(pipelineContext, null, null, target, null);
    instance.fetch(12);
    verify(target).fetch(12);
  }

  @Test
  public void testHandlerWithoutExceptionHandler() {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    @SuppressWarnings("unchecked")
    MetadataRowStreamImpl target = mock(MetadataRowStreamImpl.class);
    RowStreamWrapper instance = new RowStreamWrapper(pipelineContext, null, null, target, null);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Handler<Row>> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
    instance.handler((DataRow jo) -> {
      throw new IllegalArgumentException("All good");
    });
    verify(target).handler(handlerCaptor.capture());
    Row row = mock(Row.class);
    handlerCaptor.getValue().handle(row);
  }

  @Test
  public void testHandlerWithExceptionHandler() {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    @SuppressWarnings("unchecked")
    MetadataRowStreamImpl target = mock(MetadataRowStreamImpl.class);
    RowStreamWrapper instance = new RowStreamWrapper(pipelineContext, null, null, target, null);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Handler<Row>> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
    AtomicBoolean called = new AtomicBoolean();
    instance.exceptionHandler(ex -> {
      assertEquals("All good", ex.getMessage());
      called.set(true);
    });
    instance.handler((DataRow jo) -> {
      throw new IllegalArgumentException("All good");
    });
    verify(target).handler(handlerCaptor.capture());
    Row row = mock(Row.class);
    handlerCaptor.getValue().handle(row);
    assertTrue(called.get());
  }

  @Test
  public void testHandlerWithEndHandler() {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    @SuppressWarnings("unchecked")
    MetadataRowStreamImpl target = mock(MetadataRowStreamImpl.class);    
    Transaction transaction = mock(Transaction.class);
    SqlConnection connection = mock(SqlConnection.class);
    when (connection.close()).thenReturn(Future.succeededFuture());
    RowStreamWrapper instance = new RowStreamWrapper(pipelineContext, connection, transaction, target, null);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Handler<Void>> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
    when(target.close()).thenReturn(Future.succeededFuture());
    when(transaction.commit()).thenReturn(Future.succeededFuture());
    instance.endHandler(v -> {
      logger.info("Ended");
    });
    verify(target).endHandler(handlerCaptor.capture());
    handlerCaptor.getValue().handle(null);
  }

  @Test
  public void testHandlerWithEndHandlerAndBadTransaction() {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    @SuppressWarnings("unchecked")
    MetadataRowStreamImpl target = mock(MetadataRowStreamImpl.class);    
    Transaction transaction = mock(Transaction.class);
    SqlConnection connection = mock(SqlConnection.class);
    when (connection.close()).thenReturn(Future.succeededFuture());
    RowStreamWrapper instance = new RowStreamWrapper(pipelineContext, connection, transaction, target, null);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Handler<Void>> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
    when(target.close()).thenReturn(Future.succeededFuture());
    when(transaction.commit()).thenReturn(Future.failedFuture("Bad transaction"));
    instance.endHandler(v -> {
      logger.info("Ended");
    });
    verify(target).endHandler(handlerCaptor.capture());
    handlerCaptor.getValue().handle(null);
    
    // Pipes reset handler to null after ending
    instance.handler(null);
  }
  
  @Test
  public void testClose() {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    MetadataRowStreamImpl rowStream = mock(MetadataRowStreamImpl.class);
    RowStreamWrapper wrapper = new RowStreamWrapper(pipelineContext, null, null, rowStream, null);
    wrapper.close();
    verify(rowStream, times(1)).close();
  }

  @Test
  void testRowStreamExceptionHandlerIsCalled() {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    // Arrange
    MetadataRowStreamImpl rowStream = mock(MetadataRowStreamImpl.class);

    // Capture the exception handler set by RowStreamWrapper
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Handler<Throwable>> exceptionHandlerCaptor = ArgumentCaptor.forClass(Handler.class);
    when(rowStream.coloumnDescriptorHandler(any())).thenReturn(rowStream);
    when(rowStream.exceptionHandler(exceptionHandlerCaptor.capture())).thenReturn(rowStream);
    when(rowStream.handler(any())).thenReturn(rowStream);
    when(rowStream.pause()).thenReturn(rowStream);

    RowStreamWrapper wrapper = new RowStreamWrapper(pipelineContext, null, null, rowStream, null);

    // Register an exception handler with the wrapper
    AtomicBoolean customExceptionHandlerCalled = new AtomicBoolean(false);
    wrapper.exceptionHandler(t -> customExceptionHandlerCalled.set(true));

    // Act: fire an exception through rowStream's exception handler (the lambda from the wrapper's constructor)
    Handler<Throwable> handlerSetByWrapper = exceptionHandlerCaptor.getValue();
    assertNotNull(handlerSetByWrapper);

    Throwable testException = new RuntimeException("Test exception");
    handlerSetByWrapper.handle(testException);

    // Assert: our handler should be called
    assertTrue(customExceptionHandlerCalled.get(), "Exception handler should have been called");
  }

}
