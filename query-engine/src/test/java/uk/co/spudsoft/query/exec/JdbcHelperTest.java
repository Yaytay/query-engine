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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.mockito.MockedStatic;
import uk.co.spudsoft.query.main.Credentials;
import uk.co.spudsoft.query.main.DataSourceConfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

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

}
