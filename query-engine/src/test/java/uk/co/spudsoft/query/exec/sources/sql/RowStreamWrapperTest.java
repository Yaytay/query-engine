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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author jtalbut
 */
public class RowStreamWrapperTest {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(RowStreamWrapperTest.class);
  
  @Test
  public void testPause() {
    @SuppressWarnings("unchecked")
    MetadataRowStreamImpl target = mock(MetadataRowStreamImpl.class);
    RowStreamWrapper instance = new RowStreamWrapper(() -> {}, null, null, target);
    instance.pause();
    verify(target, times(2)).pause();
  }

  @Test
  public void testResume() {
    @SuppressWarnings("unchecked")
    MetadataRowStreamImpl target = mock(MetadataRowStreamImpl.class);
    RowStreamWrapper instance = new RowStreamWrapper(() -> {}, null, null, target);
    instance.resume();
    verify(target).resume();
  }

  @Test
  public void testFetch() {
    @SuppressWarnings("unchecked")
    MetadataRowStreamImpl target = mock(MetadataRowStreamImpl.class);
    RowStreamWrapper instance = new RowStreamWrapper(() -> {}, null, null, target);
    instance.fetch(12);
    verify(target).fetch(12);
  }

  @Test
  public void testHandlerWithoutExceptionHandler() {
    @SuppressWarnings("unchecked")
    MetadataRowStreamImpl target = mock(MetadataRowStreamImpl.class);
    RowStreamWrapper instance = new RowStreamWrapper(() -> {}, null, null, target);
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
    @SuppressWarnings("unchecked")
    MetadataRowStreamImpl target = mock(MetadataRowStreamImpl.class);
    RowStreamWrapper instance = new RowStreamWrapper(() -> {}, null, null, target);
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
    @SuppressWarnings("unchecked")
    MetadataRowStreamImpl target = mock(MetadataRowStreamImpl.class);    
    Transaction transaction = mock(Transaction.class);
    SqlConnection connection = mock(SqlConnection.class);
    when (connection.close()).thenReturn(Future.succeededFuture());
    RowStreamWrapper instance = new RowStreamWrapper(() -> {}, connection, transaction, target);
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
    @SuppressWarnings("unchecked")
    MetadataRowStreamImpl target = mock(MetadataRowStreamImpl.class);    
    Transaction transaction = mock(Transaction.class);
    SqlConnection connection = mock(SqlConnection.class);
    when (connection.close()).thenReturn(Future.succeededFuture());
    RowStreamWrapper instance = new RowStreamWrapper(() -> {}, connection, transaction, target);
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
    MetadataRowStreamImpl rowStream = mock(MetadataRowStreamImpl.class);
    RowStreamWrapper wrapper = new RowStreamWrapper(null, null, null, rowStream);
    wrapper.close();
    verify(rowStream, times(1)).close();
  }
  
}
