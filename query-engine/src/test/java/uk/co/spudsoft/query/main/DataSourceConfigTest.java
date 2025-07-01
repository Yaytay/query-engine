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
package uk.co.spudsoft.query.main;

import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for DataSourceConfig.
 */
public class DataSourceConfigTest {

  @Test
  public void testDefaults() {
    DataSourceConfig config = new DataSourceConfig();
    
    // Test default values
    assertThat(config.getMinPoolSize(), equalTo(2));
    assertThat(config.getMaxPoolSize(), equalTo(10));
    assertThat(config.getMinimumIdle(), equalTo(-1));
    assertThat(config.getConnectionTimeout(), equalTo(30000L));
    assertThat(config.getIdleTimeout(), equalTo(600000L));
    assertThat(config.getKeepaliveTime(), equalTo(0L));
    assertThat(config.getMaxLifetime(), equalTo(1800000L));
    assertThat(config.getValidationTimeout(), equalTo(5000L));
    assertThat(config.getInitializationFailTimeout(), equalTo(1L));
    assertThat(config.getLeakDetectionThreshold(), equalTo(0L));
    assertThat(config.isAutoCommit(), equalTo(true));
    assertThat(config.isReadOnly(), equalTo(false));
    assertThat(config.isAllowPoolSuspension(), equalTo(false));
    assertThat(config.isRegisterMbeans(), equalTo(false));
    
    // Null defaults
    assertThat(config.getUrl(), nullValue());
    assertThat(config.getSchema(), nullValue());
    assertThat(config.getUser(), nullValue());
    assertThat(config.getAdminUser(), nullValue());
    assertThat(config.getIsolationLevel(), nullValue());
    assertThat(config.getCatalog(), nullValue());
    assertThat(config.getDriverClassName(), nullValue());
    assertThat(config.getDataSourceClassName(), nullValue());
    assertThat(config.getConnectionTestQuery(), nullValue());
    assertThat(config.getConnectionInitSql(), nullValue());
    assertThat(config.getPoolName(), nullValue());
  }

  @Test
  public void testConnectionPoolSizing() {
    DataSourceConfig config = new DataSourceConfig()
        .setMinPoolSize(5)
        .setMaxPoolSize(20)
        .setMinimumIdle(8);
    
    assertThat(config.getMinPoolSize(), equalTo(5));
    assertThat(config.getMaxPoolSize(), equalTo(20));
    assertThat(config.getMinimumIdle(), equalTo(8));
  }

  @Test
  public void testConnectionTiming() {
    DataSourceConfig config = new DataSourceConfig()
        .setConnectionTimeout(45000L)
        .setIdleTimeout(900000L)
        .setKeepaliveTime(60000L)
        .setMaxLifetime(3600000L)
        .setValidationTimeout(10000L)
        .setInitializationFailTimeout(5L)
        .setLeakDetectionThreshold(300000L);
    
    assertThat(config.getConnectionTimeout(), equalTo(45000L));
    assertThat(config.getIdleTimeout(), equalTo(900000L));
    assertThat(config.getKeepaliveTime(), equalTo(60000L));
    assertThat(config.getMaxLifetime(), equalTo(3600000L));
    assertThat(config.getValidationTimeout(), equalTo(10000L));
    assertThat(config.getInitializationFailTimeout(), equalTo(5L));
    assertThat(config.getLeakDetectionThreshold(), equalTo(300000L));
  }

  @Test
  public void testConnectionBehavior() {
    DataSourceConfig config = new DataSourceConfig()
        .setAutoCommit(false)
        .setReadOnly(true)
        .setIsolationLevel("TRANSACTION_READ_UNCOMMITTED")
        .setCatalog("test_catalog");
    
    assertThat(config.isAutoCommit(), equalTo(false));
    assertThat(config.isReadOnly(), equalTo(true));
    assertThat(config.getIsolationLevel(), equalTo("TRANSACTION_READ_UNCOMMITTED"));
    assertThat(config.getCatalog(), equalTo("test_catalog"));
  }

  @Test
  public void testPoolBehavior() {
    DataSourceConfig config = new DataSourceConfig()
        .setAllowPoolSuspension(true)
        .setRegisterMbeans(true);
    
    assertThat(config.isAllowPoolSuspension(), equalTo(true));
    assertThat(config.isRegisterMbeans(), equalTo(true));
  }

  @Test
  public void testJdbcDriverConfiguration() {
    DataSourceConfig config = new DataSourceConfig()
        .setDriverClassName("org.postgresql.Driver")
        .setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource")
        .setConnectionTestQuery("SELECT 1")
        .setConnectionInitSql("SET TIME ZONE 'UTC'");
    
    assertThat(config.getDriverClassName(), equalTo("org.postgresql.Driver"));
    assertThat(config.getDataSourceClassName(), equalTo("org.postgresql.ds.PGSimpleDataSource"));
    assertThat(config.getConnectionTestQuery(), equalTo("SELECT 1"));
    assertThat(config.getConnectionInitSql(), equalTo("SET TIME ZONE 'UTC'"));
  }

  @Test
  public void testPoolIdentification() {
    DataSourceConfig config = new DataSourceConfig()
        .setPoolName("TestPool");
    
    assertThat(config.getPoolName(), equalTo("TestPool"));
  }

  @Test
  public void testFluentSetters() {
    // Test that all setters return this for fluent chaining
    DataSourceConfig config = new DataSourceConfig()
        .setUrl("jdbc:postgresql://localhost:5432/test")
        .setSchema("test_schema")
        .setUser(new Credentials().setUsername("user"))
        .setAdminUser(new Credentials().setUsername("admin"))
        .setMinPoolSize(5)
        .setMaxPoolSize(15)
        .setMinimumIdle(3)
        .setConnectionTimeout(25000L)
        .setIdleTimeout(500000L)
        .setKeepaliveTime(30000L)
        .setMaxLifetime(2000000L)
        .setValidationTimeout(8000L)
        .setInitializationFailTimeout(3L)
        .setLeakDetectionThreshold(120000L)
        .setAutoCommit(false)
        .setReadOnly(true)
        .setIsolationLevel("TRANSACTION_SERIALIZABLE")
        .setCatalog("main_catalog")
        .setAllowPoolSuspension(true)
        .setRegisterMbeans(true)
        .setDriverClassName("org.h2.Driver")
        .setDataSourceClassName("org.h2.jdbcx.JdbcDataSource")
        .setConnectionTestQuery("SELECT 1 FROM DUAL")
        .setConnectionInitSql("SET SCHEMA test")
        .setPoolName("FluentTestPool");
    
    // Verify all values were set correctly
    assertThat(config.getUrl(), equalTo("jdbc:postgresql://localhost:5432/test"));
    assertThat(config.getSchema(), equalTo("test_schema"));
    assertThat(config.getUser().getUsername(), equalTo("user"));
    assertThat(config.getAdminUser().getUsername(), equalTo("admin"));
    assertThat(config.getMinPoolSize(), equalTo(5));
    assertThat(config.getMaxPoolSize(), equalTo(15));
    assertThat(config.getMinimumIdle(), equalTo(3));
    assertThat(config.getConnectionTimeout(), equalTo(25000L));
    assertThat(config.getIdleTimeout(), equalTo(500000L));
    assertThat(config.getKeepaliveTime(), equalTo(30000L));
    assertThat(config.getMaxLifetime(), equalTo(2000000L));
    assertThat(config.getValidationTimeout(), equalTo(8000L));
    assertThat(config.getInitializationFailTimeout(), equalTo(3L));
    assertThat(config.getLeakDetectionThreshold(), equalTo(120000L));
    assertThat(config.isAutoCommit(), equalTo(false));
    assertThat(config.isReadOnly(), equalTo(true));
    assertThat(config.getIsolationLevel(), equalTo("TRANSACTION_SERIALIZABLE"));
    assertThat(config.getCatalog(), equalTo("main_catalog"));
    assertThat(config.isAllowPoolSuspension(), equalTo(true));
    assertThat(config.isRegisterMbeans(), equalTo(true));
    assertThat(config.getDriverClassName(), equalTo("org.h2.Driver"));
    assertThat(config.getDataSourceClassName(), equalTo("org.h2.jdbcx.JdbcDataSource"));
    assertThat(config.getConnectionTestQuery(), equalTo("SELECT 1 FROM DUAL"));
    assertThat(config.getConnectionInitSql(), equalTo("SET SCHEMA test"));
    assertThat(config.getPoolName(), equalTo("FluentTestPool"));
  }

  @Test
  public void testValidationSuccess() {
    DataSourceConfig config = new DataSourceConfig()
        .setUrl("jdbc:postgresql://localhost:5432/test");
    
    // Should not throw
    config.validate("test");
  }

  @Test
  public void testValidationFailureNoUrl() {
    DataSourceConfig config = new DataSourceConfig();
    
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> config.validate("test")
    );
    assertThat(exception.getMessage(), equalTo("test.url is not set"));
  }

  @Test
  public void testValidationFailureInvalidUrl() {
    DataSourceConfig config = new DataSourceConfig()
        .setUrl("not a valid url");
    
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> config.validate("test")
    );
    assertThat(exception.getMessage().startsWith("test.url is not a valid url"), equalTo(true));
  }

  @Test
  public void testValidationFailureInvalidMaxPoolSize() {
    DataSourceConfig config = new DataSourceConfig()
        .setUrl("jdbc:postgresql://localhost:5432/test")
        .setMaxPoolSize(0);
    
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> config.validate("test")
    );
    assertThat(exception.getMessage(), equalTo("test.maxPoolSize must be greater than 0"));
  }

  @Test
  public void testValidationFailureInvalidMinimumIdle() {
    DataSourceConfig config = new DataSourceConfig()
        .setUrl("jdbc:postgresql://localhost:5432/test")
        .setMinimumIdle(-2);
    
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> config.validate("test")
    );
    assertThat(exception.getMessage(), equalTo("test.minimumIdle must be -1 or greater"));
  }

  @Test
  public void testValidationFailureMinimumIdleGreaterThanMaxPoolSize() {
    DataSourceConfig config = new DataSourceConfig()
        .setUrl("jdbc:postgresql://localhost:5432/test")
        .setMaxPoolSize(5)
        .setMinimumIdle(10);
    
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> config.validate("test")
    );
    assertThat(exception.getMessage(), equalTo("test.minimumIdle cannot be greater than maxPoolSize"));
  }

  @Test
  public void testValidationFailureConnectionTimeoutTooLow() {
    DataSourceConfig config = new DataSourceConfig()
        .setUrl("jdbc:postgresql://localhost:5432/test")
        .setConnectionTimeout(200L);
    
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> config.validate("test")
    );
    assertThat(exception.getMessage(), equalTo("test.connectionTimeout must be at least 250ms"));
  }

  @Test
  public void testValidationFailureValidationTimeoutTooLow() {
    DataSourceConfig config = new DataSourceConfig()
        .setUrl("jdbc:postgresql://localhost:5432/test")
        .setValidationTimeout(100L);
    
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> config.validate("test")
    );
    assertThat(exception.getMessage(), equalTo("test.validationTimeout must be at least 250ms"));
  }

  @Test
  public void testValidationEdgeCases() {
    // Test minimum valid values
    DataSourceConfig config = new DataSourceConfig()
        .setUrl("jdbc:h2:mem:test")
        .setMaxPoolSize(1)
        .setMinimumIdle(-1)
        .setConnectionTimeout(250L)
        .setValidationTimeout(250L);
    
    // Should not throw
    config.validate("test");
    
    // Test minimumIdle equals maxPoolSize
    config.setMinimumIdle(1);
    config.validate("test");
  }
}