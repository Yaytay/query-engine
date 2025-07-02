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
package uk.co.spudsoft.query.exec;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.concurrent.Callable;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.mockito.MockedStatic;
import uk.co.spudsoft.query.main.Credentials;
import uk.co.spudsoft.query.main.DataSourceConfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Exhaustive unit tests for JdbcHelper.createDataSource method.
 */
public class JdbcHelperTest {

  private DataSourceConfig config;
  private Credentials credentials;
  private MeterRegistry meterRegistry;
  private MockedStatic<Auditor> auditorMock;

  @BeforeEach
  void setUp() {
    config = new DataSourceConfig();
    credentials = new Credentials();
    meterRegistry = new SimpleMeterRegistry();

    // Mock Auditor.localizeUsername to return the input unchanged
    auditorMock = mockStatic(Auditor.class);
    auditorMock.when(() -> Auditor.localizeUsername(anyString()))
            .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @AfterEach
  void tearDown() {
    if (auditorMock != null) {
      auditorMock.close();
    }
  }

  @Test
  void testMinimalConfiguration() {
    config.setUrl("jdbc:h2:mem:test");

    HikariDataSource ds = JdbcHelper.createDataSource(config, null, null);

    assertThat(ds.getJdbcUrl(), equalTo("jdbc:h2:mem:test"));
    assertThat(ds.getUsername(), nullValue());
    assertThat(ds.getPassword(), nullValue());
    assertThat(ds.getSchema(), nullValue());
    assertThat(ds.getMaximumPoolSize(), equalTo(10)); // default
    assertThat(ds.getPoolName(), startsWith("QueryEngine-"));
    assertThat(ds.getMetricsTrackerFactory(), nullValue());

    ds.close();
  }

  @Test
  void testFullConfiguration() {
    config.setUrl("jdbc:postgresql://localhost:5432/testdb")
            .setSchema("test_schema")
            .setMaxPoolSize(20)
            .setMinimumIdle(5)
            .setConnectionTimeout(45000L)
            .setIdleTimeout(900000L)
            .setKeepaliveTime(60000L)
            .setMaxLifetime(3600000L)
            .setValidationTimeout(10000L)
            .setInitializationFailTimeout(5L)
            .setLeakDetectionThreshold(300000L)
            .setAutoCommit(false)
            .setReadOnly(true)
            .setIsolationLevel("TRANSACTION_READ_COMMITTED")
            .setCatalog("test_catalog")
            .setAllowPoolSuspension(true)
            .setRegisterMbeans(true)
            .setDriverClassName("org.postgresql.Driver")
            .setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource")
            .setConnectionTestQuery("SELECT 1")
            .setConnectionInitSql("SET TIME ZONE 'UTC'")
            .setPoolName("TestPool");

    credentials.setUsername("testuser").setPassword("testpass");

    HikariDataSource ds = JdbcHelper.createDataSource(config, credentials, meterRegistry);

    // Essential connection properties
    assertThat(ds.getJdbcUrl(), equalTo("jdbc:postgresql://localhost:5432/testdb"));
    assertThat(ds.getUsername(), equalTo("testuser"));
    assertThat(ds.getPassword(), equalTo("testpass"));
    assertThat(ds.getSchema(), equalTo("test_schema"));

    // Pool sizing
    assertThat(ds.getMaximumPoolSize(), equalTo(20));
    assertThat(ds.getMinimumIdle(), equalTo(5));

    // Connection timing
    assertThat(ds.getConnectionTimeout(), equalTo(45000L));
    assertThat(ds.getIdleTimeout(), equalTo(900000L));
    assertThat(ds.getKeepaliveTime(), equalTo(60000L));
    assertThat(ds.getMaxLifetime(), equalTo(3600000L));
    assertThat(ds.getValidationTimeout(), equalTo(10000L));
    assertThat(ds.getInitializationFailTimeout(), equalTo(5L));
    assertThat(ds.getLeakDetectionThreshold(), equalTo(300000L));

    // Connection behavior
    assertThat(ds.isAutoCommit(), equalTo(false));
    assertThat(ds.isReadOnly(), equalTo(true));
    assertThat(ds.getTransactionIsolation(), equalTo("TRANSACTION_READ_COMMITTED"));
    assertThat(ds.getCatalog(), equalTo("test_catalog"));

    // Pool behavior
    assertThat(ds.isAllowPoolSuspension(), equalTo(true));
    assertThat(ds.isRegisterMbeans(), equalTo(true));

    // JDBC driver configuration
    assertThat(ds.getDriverClassName(), equalTo("org.postgresql.Driver"));
    assertThat(ds.getDataSourceClassName(), equalTo("org.postgresql.ds.PGSimpleDataSource"));
    assertThat(ds.getConnectionTestQuery(), equalTo("SELECT 1"));
    assertThat(ds.getConnectionInitSql(), equalTo("SET TIME ZONE 'UTC'"));

    // Pool identification
    assertThat(ds.getPoolName(), equalTo("TestPool"));

    // Metrics
    assertThat(ds.getMetricsTrackerFactory(), instanceOf(MicrometerMetricsTrackerFactory.class));

    ds.close();
  }

  @Test
  void testNullCredentials() {
    config.setUrl("jdbc:h2:mem:test");

    HikariDataSource ds = JdbcHelper.createDataSource(config, null, null);

    assertThat(ds.getUsername(), nullValue());
    assertThat(ds.getPassword(), nullValue());

    ds.close();
  }

  @Test
  void testCredentialsWithNullValues() {
    config.setUrl("jdbc:h2:mem:test");
    credentials.setUsername(null).setPassword(null);

    HikariDataSource ds = JdbcHelper.createDataSource(config, credentials, null);

    assertThat(ds.getUsername(), nullValue());
    assertThat(ds.getPassword(), nullValue());

    ds.close();
  }

  @Test
  void testCredentialsWithEmptyValues() {
    config.setUrl("jdbc:h2:mem:test");
    credentials.setUsername("").setPassword("");

    HikariDataSource ds = JdbcHelper.createDataSource(config, credentials, null);

    assertThat(ds.getUsername(), equalTo(""));
    assertThat(ds.getPassword(), equalTo(""));

    ds.close();
  }

  @Test
  void testNullSchema() {
    config.setUrl("jdbc:h2:mem:test").setSchema(null);

    HikariDataSource ds = JdbcHelper.createDataSource(config, null, null);

    assertThat(ds.getSchema(), nullValue());

    ds.close();
  }

  @Test
  void testEmptySchema() {
    config.setUrl("jdbc:h2:mem:test").setSchema("");

    HikariDataSource ds = JdbcHelper.createDataSource(config, null, null);

    assertThat(ds.getSchema(), equalTo(""));

    ds.close();
  }

  @Test
  void testMinimumIdleZero() {
    config.setUrl("jdbc:h2:mem:test").setMinimumIdle(0);

    HikariDataSource ds = JdbcHelper.createDataSource(config, null, null);

    // MinimumIdle should not be set when <= 0
    assertThat(ds.getMinimumIdle(), not(equalTo(0)));

    ds.close();
  }

  @Test
  void testMinimumIdleNegative() {
    config.setUrl("jdbc:h2:mem:test").setMinimumIdle(-5);

    HikariDataSource ds = JdbcHelper.createDataSource(config, null, null);

    // MinimumIdle should not be set when <= 0
    assertThat(ds.getMinimumIdle(), not(equalTo(-5)));

    ds.close();
  }

  @Test
  void testMinimumIdlePositive() {
    config.setUrl("jdbc:h2:mem:test").setMinimumIdle(3);

    HikariDataSource ds = JdbcHelper.createDataSource(config, null, null);

    assertThat(ds.getMinimumIdle(), equalTo(3));

    ds.close();
  }

  @Test
  void testNullIsolationLevel() {
    config.setUrl("jdbc:h2:mem:test").setIsolationLevel(null);

    HikariDataSource ds = JdbcHelper.createDataSource(config, null, null);

    assertThat(ds.getTransactionIsolation(), nullValue());

    ds.close();
  }

  @Test
  void testEmptyIsolationLevel() {
    config.setUrl("jdbc:h2:mem:test").setIsolationLevel("");

    HikariDataSource ds = JdbcHelper.createDataSource(config, null, null);

    assertThat(ds.getTransactionIsolation(), nullValue());

    ds.close();
  }

  @Test
  void testWhitespaceOnlyIsolationLevel() {
    config.setUrl("jdbc:h2:mem:test").setIsolationLevel("   ");

    HikariDataSource ds = JdbcHelper.createDataSource(config, null, null);

    assertThat(ds.getTransactionIsolation(), nullValue());

    ds.close();
  }

  @Test
  void testValidIsolationLevel() {
    config.setUrl("jdbc:h2:mem:test").setIsolationLevel("TRANSACTION_SERIALIZABLE");

    HikariDataSource ds = JdbcHelper.createDataSource(config, null, null);

    assertThat(ds.getTransactionIsolation(), equalTo("TRANSACTION_SERIALIZABLE"));

    ds.close();
  }

  @Test
  void testNullCatalog() {
    config.setUrl("jdbc:h2:mem:test").setCatalog(null);

    HikariDataSource ds = JdbcHelper.createDataSource(config, null, null);

    assertThat(ds.getCatalog(), nullValue());

    ds.close();
  }

  @Test
  void testEmptyCatalog() {
    config.setUrl("jdbc:h2:mem:test").setCatalog("");

    HikariDataSource ds = JdbcHelper.createDataSource(config, null, null);

    assertThat(ds.getCatalog(), nullValue());

    ds.close();
  }

  @Test
  void testSetLocalDateTimeUTC_NullValue() throws SQLException {
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    LocalDateTime utc = null;
    int index = 1;

    JdbcHelper.setLocalDateTimeUTC(preparedStatement, index, utc);

    verify(preparedStatement).setTimestamp(index, null);
    verifyNoMoreInteractions(preparedStatement);
  }

  @Test
  void testSetLocalDateTimeUTC_ValidValue() throws SQLException {
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    LocalDateTime utc = LocalDateTime.of(2023, 6, 15, 14, 30, 45);
    int index = 2;
    Timestamp expectedTimestamp = Timestamp.from(utc.toInstant(ZoneOffset.UTC));

    JdbcHelper.setLocalDateTimeUTC(preparedStatement, index, utc);

    verify(preparedStatement).setTimestamp(eq(index), eq(expectedTimestamp), any(Calendar.class));
    verifyNoMoreInteractions(preparedStatement);
  }

  @Test
  void testSetLocalDateTimeUTC_EpochTime() throws SQLException {
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    LocalDateTime utc = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);
    int index = 1;
    Timestamp expectedTimestamp = Timestamp.from(utc.toInstant(ZoneOffset.UTC));

    JdbcHelper.setLocalDateTimeUTC(preparedStatement, index, utc);

    verify(preparedStatement).setTimestamp(eq(index), eq(expectedTimestamp), any(Calendar.class));
  }

  @Test
  void testSetLocalDateTimeUTC_FutureDate() throws SQLException {
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    LocalDateTime utc = LocalDateTime.of(2050, 12, 31, 23, 59, 59);
    int index = 3;

    JdbcHelper.setLocalDateTimeUTC(preparedStatement, index, utc);

    verify(preparedStatement).setTimestamp(eq(index), any(Timestamp.class), any(Calendar.class));
  }

  @Test
  void testSetLocalDateTimeUTC_SqlExceptionPropagated() throws SQLException {
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    LocalDateTime utc = LocalDateTime.now();
    int index = 1;
    SQLException expectedException = new SQLException("Database connection failed");

    doThrow(expectedException).when(preparedStatement).setTimestamp(anyInt(), any(Timestamp.class), any(Calendar.class));

    SQLException thrownException = assertThrows(SQLException.class, () -> {
      JdbcHelper.setLocalDateTimeUTC(preparedStatement, index, utc);
    });

    assertThat(thrownException, sameInstance(expectedException));
  }

  @Test
  void testSetLocalDateTimeUTC_CalendarUsesGMT() throws SQLException {
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    LocalDateTime utc = LocalDateTime.of(2023, 6, 15, 14, 30, 45);
    int index = 1;

    JdbcHelper.setLocalDateTimeUTC(preparedStatement, index, utc);

    verify(preparedStatement).setTimestamp(eq(index), any(Timestamp.class), argThat(calendar -> 
        "GMT".equals(calendar.getTimeZone().getID())
    ));
  }

  // ===== Tests for limitLength method =====

  @Test
  void testLimitLength_NullValue() {
    String result = JdbcHelper.limitLength(null, 10);
    assertThat(result, nullValue());
  }

  @Test
  void testLimitLength_StringShorterThanLimit() {
    String input = "short";
    String result = JdbcHelper.limitLength(input, 10);
    assertThat(result, equalTo("short"));
  }

  @Test
  void testLimitLength_StringEqualToLimit() {
    String input = "exactlimit";
    String result = JdbcHelper.limitLength(input, 10);
    assertThat(result, equalTo("exactlimit"));
  }

  @Test
  void testLimitLength_StringLongerThanLimit() {
    String input = "this string is much longer than the limit";
    String result = JdbcHelper.limitLength(input, 10);
    assertThat(result, equalTo("this st..."));
  }

  @Test
  void testLimitLength_EmptyString() {
    String input = "";
    String result = JdbcHelper.limitLength(input, 5);
    assertThat(result, equalTo(""));
  }

  @Test
  void testLimitLength_ZeroLimit() {
    String input = "anything";
    String result = JdbcHelper.limitLength(input, 0);
    assertThat(result, equalTo(""));
  }

  @Test
  void testLimitLength_NegativeLimit() {
    String input = "anything";
    String result = JdbcHelper.limitLength(input, -5);
    assertThat(result, equalTo(""));
  }

  @Test
  void testLimitLength_LimitOne() {
    String input = "test";
    String result = JdbcHelper.limitLength(input, 1);
    assertThat(result, equalTo("t"));
  }

  @Test
  void testLimitLength_UnicodeCharacters() {
    String input = "测试文本";
    String result = JdbcHelper.limitLength(input, 2);
    assertThat(result, equalTo("测试"));
  }

  @Test
  void testLimitLength_LargeLimit() {
    String input = "small";
    String result = JdbcHelper.limitLength(input, Integer.MAX_VALUE);
    assertThat(result, equalTo("small"));
  }

  // ===== Tests for toString method =====

  @Test
  void testToString_NullValue() {
    String result = JdbcHelper.toString(null);
    assertThat(result, nullValue());
  }

  @Test
  void testToString_StringValue() {
    String input = "test string";
    String result = JdbcHelper.toString(input);
    assertThat(result, equalTo("test string"));
  }

  @Test
  void testToString_IntegerValue() {
    Integer input = 42;
    String result = JdbcHelper.toString(input);
    assertThat(result, equalTo("42"));
  }

  @Test
  void testToString_LongValue() {
    Long input = 123456789L;
    String result = JdbcHelper.toString(input);
    assertThat(result, equalTo("123456789"));
  }

  @Test
  void testToString_DoubleValue() {
    Double input = 3.14159;
    String result = JdbcHelper.toString(input);
    assertThat(result, equalTo("3.14159"));
  }

  @Test
  void testToString_BooleanValue() {
    Boolean input = true;
    String result = JdbcHelper.toString(input);
    assertThat(result, equalTo("true"));
  }

  @Test
  void testToString_BigDecimalValue() {
    BigDecimal input = new BigDecimal("123.456");
    String result = JdbcHelper.toString(input);
    assertThat(result, equalTo("123.456"));
  }

  @Test
  void testToString_EmptyString() {
    String input = "";
    String result = JdbcHelper.toString(input);
    assertThat(result, equalTo(""));
  }

  @Test
  void testToString_CustomObject() {
    Object input = new Object() {
      @Override
      public String toString() {
        return "custom object";
      }
    };
    String result = JdbcHelper.toString(input);
    assertThat(result, equalTo("custom object"));
  }

  @Test
  void testToString_ArrayValue() {
    int[] input = {1, 2, 3};
    String result = JdbcHelper.toString(input);
    assertThat(result, notNullValue());
    assertThat(result, containsString("["));
  }

  // ===== Tests for closeConnection method =====

  @Test
  void testCloseConnection_NullConnection() {
    // Should not throw exception
    assertDoesNotThrow(() -> JdbcHelper.closeConnection(null));
  }

  @Test
  void testCloseConnection_ValidConnection() throws SQLException {
    Connection connection = mock(Connection.class);

    JdbcHelper.closeConnection(connection);

    verify(connection).close();
    verifyNoMoreInteractions(connection);
  }

  @Test
  void testCloseConnection_SqlExceptionSuppressed() throws SQLException {
    Connection connection = mock(Connection.class);
    doThrow(new SQLException("Connection already closed")).when(connection).close();

    // Should not throw exception, should suppress it
    assertDoesNotThrow(() -> JdbcHelper.closeConnection(connection));

    verify(connection).close();
  }

  @Test
  void testCloseConnection_RuntimeExceptionSuppressed() throws SQLException {
    Connection connection = mock(Connection.class);
    doThrow(new RuntimeException("Unexpected error")).when(connection).close();

    // Should not throw exception, should suppress it
    assertDoesNotThrow(() -> JdbcHelper.closeConnection(connection));

    verify(connection).close();
  }

  @Test
  void testCloseConnection_AlreadyClosedConnection() throws SQLException {
    Connection connection = mock(Connection.class);
    when(connection.isClosed()).thenReturn(true);

    JdbcHelper.closeConnection(connection);

    verify(connection).close();
  }

  // ===== Tests for closeStatement method =====

  @Test
  void testCloseStatement_NullStatement() {
    // Should not throw exception
    assertDoesNotThrow(() -> JdbcHelper.closeStatement(null));
  }

  @Test
  void testCloseStatement_ValidStatement() throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);

    JdbcHelper.closeStatement(statement);

    verify(statement).close();
    verifyNoMoreInteractions(statement);
  }

  @Test
  void testCloseStatement_SqlExceptionSuppressed() throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);
    doThrow(new SQLException("Statement already closed")).when(statement).close();

    // Should not throw exception, should suppress it
    assertDoesNotThrow(() -> JdbcHelper.closeStatement(statement));

    verify(statement).close();
  }

  @Test
  void testCloseStatement_RuntimeExceptionSuppressed() throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);
    doThrow(new RuntimeException("Unexpected error")).when(statement).close();

    // Should not throw exception, should suppress it
    assertDoesNotThrow(() -> JdbcHelper.closeStatement(statement));

    verify(statement).close();
  }

  @Test
  void testCloseStatement_AlreadyClosedStatement() throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);
    when(statement.isClosed()).thenReturn(true);

    JdbcHelper.closeStatement(statement);

    verify(statement).close();
  }

  @Test
  void testCloseStatement_CallableStatement() throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);

    JdbcHelper.closeStatement(statement);

    verify(statement).close();
  }
  
  @Test
  @SuppressWarnings("unchecked")
  void testShutdown_WithActiveFutures() {
    Vertx vertx = mock(Vertx.class);
    DataSource dataSource = mock(DataSource.class);
    JdbcHelper jdbcHelper = new JdbcHelper(vertx, dataSource);

    Promise<Void> promise = Promise.promise();
    
    // Start an operation to create an active future
    when(vertx.executeBlocking((Callable<Void>)any())).thenReturn(promise.future());
    jdbcHelper.runSqlUpdate("test", "SELECT 1", ps -> {});

    // Shutdown should not complete immediately
    Future<Void> shutdownFuture = jdbcHelper.shutdown();

    assertFalse(shutdownFuture.isComplete());
    
    promise.complete();

    assertTrue(shutdownFuture.isComplete());
  }
}