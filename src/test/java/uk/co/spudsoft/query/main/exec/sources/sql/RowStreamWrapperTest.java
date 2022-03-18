/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.sources.sql;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowStream;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 *
 * @author jtalbut
 */
public class RowStreamWrapperTest {
  
  @Test
  public void testPause() {
    @SuppressWarnings("unchecked")
    RowStream<Row> target = mock(RowStream.class);
    RowStreamWrapper instance = new RowStreamWrapper(null, target);
    instance.pause();
    verify(target).pause();
  }

  @Test
  public void testResume() {
    @SuppressWarnings("unchecked")
    RowStream<Row> target = mock(RowStream.class);
    RowStreamWrapper instance = new RowStreamWrapper(null, target);
    instance.resume();
    verify(target).resume();
  }

  @Test
  public void testFetch() {
    @SuppressWarnings("unchecked")
    RowStream<Row> target = mock(RowStream.class);
    RowStreamWrapper instance = new RowStreamWrapper(null, target);
    instance.fetch(12);
    verify(target).fetch(12);
  }

  @Test
  public void testHandlerWithoutExceptionHandler() {
    @SuppressWarnings("unchecked")
    RowStream<Row> target = mock(RowStream.class);
    RowStreamWrapper instance = new RowStreamWrapper(null, target);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Handler<Row>> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
    instance.handler((JsonObject jo) -> {
      throw new IllegalArgumentException("All good");
    });
    verify(target).handler(handlerCaptor.capture());
    Row row = mock(Row.class);
    handlerCaptor.getValue().handle(row);
  }

  @Test
  public void testHandlerWithExceptionHandler() {
    @SuppressWarnings("unchecked")
    RowStream<Row> target = mock(RowStream.class);
    RowStreamWrapper instance = new RowStreamWrapper(null, target);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Handler<Row>> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
    AtomicBoolean called = new AtomicBoolean();
    instance.exceptionHandler(ex -> {
      assertEquals("All good", ex.getMessage());
      called.set(true);
    });
    instance.handler((JsonObject jo) -> {
      throw new IllegalArgumentException("All good");
    });
    verify(target).handler(handlerCaptor.capture());
    Row row = mock(Row.class);
    handlerCaptor.getValue().handle(row);
    assertTrue(called.get());
  }
  
}
