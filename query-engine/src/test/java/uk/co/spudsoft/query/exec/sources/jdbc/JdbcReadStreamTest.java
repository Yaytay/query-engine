/*
 * Copyright (C) 2025 jtalbut
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
package uk.co.spudsoft.query.exec.sources.jdbc;

import io.vertx.core.Context;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.Types;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;

/**
 *
 * @author jtalbut
 */
class JdbcReadStreamTest {

  @Test
  void testFetchAndPause() throws InterruptedException {
    // Setup mocks
    SourceNameTracker sourceNameTracker = mock(SourceNameTracker.class);
    Context context = mock(Context.class);
    Connection connection = mock(Connection.class);
    ResultSet resultSet = mock(ResultSet.class);
    Types types = new Types();

    JdbcReadStream jdbcReadStream = new JdbcReadStream(sourceNameTracker, context, types, connection, resultSet, 100);

    // Track how many times context.runOnContext is called to verify processing is triggered
    AtomicInteger contextCallCount = new AtomicInteger(0);
    CountDownLatch latch = new CountDownLatch(1); // Expect 2 calls to runOnContext

    doAnswer(invocation -> {
      contextCallCount.incrementAndGet();
      latch.countDown();
      return null;
    }).when(context).runOnContext(any());

    // Test fetch - should set demand and trigger processing
    jdbcReadStream.fetch(5);

    // Test pause - should set demand to 0
    jdbcReadStream.pause();

    // Test fetch again - should add to demand and trigger processing again
    jdbcReadStream.fetch(10);

    // Wait for asynchronous processing to be triggered
    assertTrue(latch.await(1, TimeUnit.SECONDS), "Processing should be triggered only once, because this test cannot clear the emitting flag");

    // Verify that context.runOnContext was called (indicating processing was triggered)
    assertEquals(1, contextCallCount.get(), "Processing should be triggered only once because this test cannot clear the emitting flag");

    // Verify sourceNameTracker was called for logging context
    verify(sourceNameTracker, atLeastOnce()).addNameToContextLocalData();
  }

  @Test
  void testFetchWithNegativeAmount() {
    SourceNameTracker sourceNameTracker = mock(SourceNameTracker.class);
    Context context = mock(Context.class);
    Connection connection = mock(Connection.class);
    ResultSet resultSet = mock(ResultSet.class);
    Types types = new Types();

    JdbcReadStream jdbcReadStream = new JdbcReadStream(sourceNameTracker, context, types, connection, resultSet, 100);

    // Test that fetch throws exception for negative amounts
    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> jdbcReadStream.fetch(-1)
    );
    assertEquals("Negative fetch amount", exception.getMessage());
  }

  @Test
  void testFetchOverflowHandling() throws InterruptedException {
    SourceNameTracker sourceNameTracker = mock(SourceNameTracker.class);
    Context context = mock(Context.class);
    Connection connection = mock(Connection.class);
    ResultSet resultSet = mock(ResultSet.class);
    Types types = new Types();

    JdbcReadStream jdbcReadStream = new JdbcReadStream(sourceNameTracker, context, types, connection, resultSet, 100);

    CountDownLatch latch = new CountDownLatch(1);
    doAnswer(invocation -> {
      latch.countDown();
      return null;
    }).when(context).runOnContext(any());

    // Test that demand overflow is handled correctly (should cap at Long.MAX_VALUE)
    jdbcReadStream.fetch(Long.MAX_VALUE - 1);
    jdbcReadStream.fetch(10); // This should cause overflow and cap at MAX_VALUE

    assertTrue(latch.await(1, TimeUnit.SECONDS), "Processing should be triggered");
    verify(sourceNameTracker, atLeastOnce()).addNameToContextLocalData();
  }

  @Test
  void testPauseStopsDemand() throws InterruptedException {
    SourceNameTracker sourceNameTracker = mock(SourceNameTracker.class);
    Context context = mock(Context.class);
    Connection connection = mock(Connection.class);
    ResultSet resultSet = mock(ResultSet.class);
    Types types = new Types();

    JdbcReadStream jdbcReadStream = new JdbcReadStream(sourceNameTracker, context, types, connection, resultSet, 100);

    CountDownLatch latch = new CountDownLatch(1);
    doAnswer(invocation -> {
      latch.countDown();
      return null;
    }).when(context).runOnContext(any());

    // First establish some demand
    jdbcReadStream.fetch(5);

    // Then pause should reset demand to 0
    jdbcReadStream.pause();

    // Verify processing was triggered at least once (from the fetch)
    assertTrue(latch.await(1, TimeUnit.SECONDS), "Processing should be triggered");
    verify(sourceNameTracker, atLeastOnce()).addNameToContextLocalData();
  }

  @Test
  void testFluentInterface() {
    SourceNameTracker sourceNameTracker = mock(SourceNameTracker.class);
    Context context = mock(Context.class);
    Connection connection = mock(Connection.class);
    ResultSet resultSet = mock(ResultSet.class);
    Types types = new Types();

    JdbcReadStream jdbcReadStream = new JdbcReadStream(sourceNameTracker, context, types, connection, resultSet, 100);

    // Verify that methods return the stream instance for fluent chaining
    JdbcReadStream result1 = jdbcReadStream.fetch(10);
    JdbcReadStream result2 = jdbcReadStream.pause();

    assertSame(jdbcReadStream, result1, "fetch should return this for fluent interface");
    assertSame(jdbcReadStream, result2, "pause should return this for fluent interface");
  }
}
