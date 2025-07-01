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
package uk.co.spudsoft.query.main;

import com.google.common.base.Strings;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Configuration data for communicating with a data source.
 *
 * @author jtalbut
 */
public class DataSourceConfig {

  /**
   * The URL to use for accessing the datasource.
   */
  private String url;

  /**
   * The database schema to use when accessing the datasource.
   */
  private String schema;

  /**
   * The credentials to use for standard actions (DML).
   */
  private Credentials user;

  /**
   * The credentials to use for preparing the database (DDL).
   */
  private Credentials adminUser;

  // Connection Pool Sizing
  private int minPoolSize = 2;
  private int maxPoolSize = 10;
  private int minimumIdle = -1; // Use same as maxPoolSize if not set

  // Connection Timing
  private long connectionTimeout = 30000; // 30 seconds
  private long idleTimeout = 600000; // 10 minutes
  private long keepaliveTime = 0; // disabled
  private long maxLifetime = 1800000; // 30 minutes
  private long validationTimeout = 5000; // 5 seconds
  private long initializationFailTimeout = 1; // fast fail on startup
  private long leakDetectionThreshold = 0; // disabled

  // Connection Behavior
  private boolean autoCommit = true;
  private boolean readOnly = false;
  private String isolationLevel; // e.g., "TRANSACTION_READ_COMMITTED"
  private String catalog;

  // Pool Behavior
  private boolean allowPoolSuspension = false;
  private boolean registerMbeans = false;

  // JDBC Driver Configuration
  private String driverClassName;
  private String dataSourceClassName;
  private String connectionTestQuery;
  private String connectionInitSql;

  // Pool Identification
  private String poolName;

  /**
   * Constructor.
   */
  public DataSourceConfig() {
  }

  // Existing methods...

  /**
   * Set the URL to use for accessing the datasource.
   * @param url the URL to use for accessing the datasource.
   * @return this, so that this method may be used in a fluent manner.
   */
  public DataSourceConfig setUrl(String url) {
    this.url = url;
    return this;
  }

  /**
   * Set the schema to use for accessing the datasource.
   * @param schema the schema to use for accessing the datasource.
   * @return this, so that this method may be used in a fluent manner.
   */
  public DataSourceConfig setSchema(String schema) {
    this.schema = schema;
    return this;
  }

  /**
   * Set the user to use for accessing the datasource for CRUD operations.
   * @param user the user to use for accessing the datasource for CRUD operations.
   * @return this, so that this method may be used in a fluent manner.
   */
  public DataSourceConfig setUser(Credentials user) {
    this.user = user;
    return this;
  }

  /**
   * Set the user to use for accessing the datasource for DDL operations.
   * @param adminUser the user to use for accessing the datasource for DDL operations.
   * @return this, so that this method may be used in a fluent manner.
   */
  public DataSourceConfig setAdminUser(Credentials adminUser) {
    this.adminUser = adminUser;
    return this;
  }

  /**
   * Set the maximum number of concurrent connections that should be made to this datasource.
   * @param maxPoolSize the maximum number of concurrent connections that should be made to this datasource.
   * @return this, so that this method may be used in a fluent manner.
   */
  public DataSourceConfig setMaxPoolSize(int maxPoolSize) {
    this.maxPoolSize = maxPoolSize;
    return this;
  }

  /**
   * Set the minimum number of concurrent connections that should be made to this datasource.
   * @param minPoolSize the minimum number of concurrent connections that should be made to this datasource.
   * @return this, so that this method may be used in a fluent manner.
   */
  public DataSourceConfig setMinPoolSize(int minPoolSize) {
    this.minPoolSize = minPoolSize;
    return this;
  }

  // New HikariCP configuration methods...

  /**
   * Set the minimum number of idle connections that HikariCP tries to maintain in the pool.
   * @param minimumIdle the minimum number of idle connections in the pool
   * @return this, so that this method may be used in a fluent manner.
   */
  public DataSourceConfig setMinimumIdle(int minimumIdle) {
    this.minimumIdle = minimumIdle;
    return this;
  }

  /**
   * Set the maximum number of milliseconds that a client will wait for a connection from the pool.
   * @param connectionTimeout the connection timeout in milliseconds
   * @return this, so that this method may be used in a fluent manner.
   */
  public DataSourceConfig setConnectionTimeout(long connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
    return this;
  }

  /**
   * Set the maximum amount of time that a connection is allowed to sit idle in the pool.
   * @param idleTimeout the idle timeout in milliseconds
   * @return this, so that this method may be used in a fluent manner.
   */
  public DataSourceConfig setIdleTimeout(long idleTimeout) {
    this.idleTimeout = idleTimeout;
    return this;
  }

  /**
   * Set the frequency at which HikariCP will attempt to keep a connection alive.
   * @param keepaliveTime the keepalive time in milliseconds (0 = disabled)
   * @return this, so that this method may be used in a fluent manner.
   */
  public DataSourceConfig setKeepaliveTime(long keepaliveTime) {
    this.keepaliveTime = keepaliveTime;
    return this;
  }

  /**
   * Set the maximum lifetime of a connection in the pool.
   * @param maxLifetime the maximum lifetime in milliseconds
   * @return this, so that this method may be used in a fluent manner.
   */
  public DataSourceConfig setMaxLifetime(long maxLifetime) {
    this.maxLifetime = maxLifetime;
    return this;
  }

  /**
   * Set the maximum amount of time that a connection will be tested for aliveness.
   * @param validationTimeout the validation timeout in milliseconds
   * @return this, so that this method may be used in a fluent manner.
   */
  public DataSourceConfig setValidationTimeout(long validationTimeout) {
    this.validationTimeout = validationTimeout;
    return this;
  }

  /**
   * Set the time that pool initialization will fail fast if the pool cannot be seeded.
   * @param initializationFailTimeout the initialization fail timeout in milliseconds
   * @return this, so that this method may be used in a fluent manner.
   */
  public DataSourceConfig setInitializationFailTimeout(long initializationFailTimeout) {
    this.initializationFailTimeout = initializationFailTimeout;
    return this;
  }

  /**
   * Set the amount of time that a connection can be out of the pool before a message is logged.
   * @param leakDetectionThreshold the leak detection threshold in milliseconds (0 = disabled)
   * @return this, so that this method may be used in a fluent manner.
   */
  public DataSourceConfig setLeakDetectionThreshold(long leakDetectionThreshold) {
    this.leakDetectionThreshold = leakDetectionThreshold;
    return this;
  }

  /**
   * Set the default auto-commit behavior of connections returned from the pool.
   * @param autoCommit the auto-commit setting
   * @return this, so that this method may be used in a fluent manner.
   */
  public DataSourceConfig setAutoCommit(boolean autoCommit) {
    this.autoCommit = autoCommit;
    return this;
  }

  /**
   * Set the default read-only behavior of connections returned from the pool.
   * @param readOnly the read-only setting
   * @return this, so that this method may be used in a fluent manner.
   */
  public DataSourceConfig setReadOnly(boolean readOnly) {
    this.readOnly = readOnly;
    return this;
  }

  /**
   * Set the default transaction isolation level for connections returned from the pool.
   * @param isolationLevel the transaction isolation level (e.g., "TRANSACTION_READ_COMMITTED")
   * @return this, so that this method may be used in a fluent manner.
   */
  public DataSourceConfig setIsolationLevel(String isolationLevel) {
    this.isolationLevel = isolationLevel;
    return this;
  }

  /**
   * Set the default catalog for connections returned from the pool.
   * @param catalog the catalog name
   * @return this, so that this method may be used in a fluent manner.
   */
  public DataSourceConfig setCatalog(String catalog) {
    this.catalog = catalog;
    return this;
  }

  /**
   * Set whether the pool can be suspended and resumed through JMX.
   * @param allowPoolSuspension whether pool suspension is allowed
   * @return this, so that this method may be used in a fluent manner.
   */
  public DataSourceConfig setAllowPoolSuspension(boolean allowPoolSuspension) {
    this.allowPoolSuspension = allowPoolSuspension;
    return this;
  }

  /**
   * Set whether or not JMX Management Beans are registered.
   * @param registerMbeans whether to register MBeans
   * @return this, so that this method may be used in a fluent manner.
   */
  public DataSourceConfig setRegisterMbeans(boolean registerMbeans) {
    this.registerMbeans = registerMbeans;
    return this;
  }

  /**
   * Set the fully qualified class name of the JDBC driver.
   * @param driverClassName the JDBC driver class name
   * @return this, so that this method may be used in a fluent manner.
   */
  public DataSourceConfig setDriverClassName(String driverClassName) {
    this.driverClassName = driverClassName;
    return this;
  }

  /**
   * Set the name of the DataSource class provided by the JDBC driver.
   * @param dataSourceClassName the DataSource class name
   * @return this, so that this method may be used in a fluent manner.
   */
  public DataSourceConfig setDataSourceClassName(String dataSourceClassName) {
    this.dataSourceClassName = dataSourceClassName;
    return this;
  }

  /**
   * Set the SQL query to be executed to test the validity of connections.
   * @param connectionTestQuery the connection test query
   * @return this, so that this method may be used in a fluent manner.
   */
  public DataSourceConfig setConnectionTestQuery(String connectionTestQuery) {
    this.connectionTestQuery = connectionTestQuery;
    return this;
  }

  /**
   * Set the SQL statement that will be executed after every new connection creation.
   * @param connectionInitSql the connection initialization SQL
   * @return this, so that this method may be used in a fluent manner.
   */
  public DataSourceConfig setConnectionInitSql(String connectionInitSql) {
    this.connectionInitSql = connectionInitSql;
    return this;
  }

  /**
   * Set the user-defined name for the connection pool.
   * @param poolName the pool name
   * @return this, so that this method may be used in a fluent manner.
   */
  public DataSourceConfig setPoolName(String poolName) {
    this.poolName = poolName;
    return this;
  }

  // Getter methods...

  /**
   * The URL to use for accessing the datasource.
   * @return URL to use for accessing the datasource.
   */
  public String getUrl() {
    return url;
  }

  /**
   * The database schema to use when accessing the datasource.
   * @return database schema to use when accessing the datasource.
   */
  public String getSchema() {
    return schema;
  }

  /**
   * The credentials to use for standard actions (DML).
   * @return The credentials to use for standard actions (DML).
   */
  public Credentials getUser() {
    return user;
  }

  /**
   * The credentials to use for preparing the database (DDL).
   * @return The credentials to use for preparing the database (DDL).
   */
  public Credentials getAdminUser() {
    return adminUser;
  }

  /**
   * The maximum size of the connection pool.
   * @return The maximum size of the connection pool.
   */
  public int getMaxPoolSize() {
    return maxPoolSize;
  }

  /**
   * The minimum size of the connection pool.
   * @return the minimum size of the connection pool.
   */
  public int getMinPoolSize() {
    return minPoolSize;
  }

  /**
   * The minimum number of idle connections in the pool.
   * @return the minimum number of idle connections in the pool.
   */
  public int getMinimumIdle() {
    return minimumIdle;
  }

  /**
   * The connection timeout in milliseconds.
   * @return the connection timeout in milliseconds.
   */
  public long getConnectionTimeout() {
    return connectionTimeout;
  }

  /**
   * The idle timeout in milliseconds.
   * @return the idle timeout in milliseconds.
   */
  public long getIdleTimeout() {
    return idleTimeout;
  }

  /**
   * The keepalive time in milliseconds.
   * @return the keepalive time in milliseconds.
   */
  public long getKeepaliveTime() {
    return keepaliveTime;
  }

  /**
   * The maximum lifetime in milliseconds.
   * @return the maximum lifetime in milliseconds.
   */
  public long getMaxLifetime() {
    return maxLifetime;
  }

  /**
   * The validation timeout in milliseconds.
   * @return the validation timeout in milliseconds.
   */
  public long getValidationTimeout() {
    return validationTimeout;
  }

  /**
   * The initialization fail timeout in milliseconds.
   * @return the initialization fail timeout in milliseconds.
   */
  public long getInitializationFailTimeout() {
    return initializationFailTimeout;
  }

  /**
   * The leak detection threshold in milliseconds.
   * @return the leak detection threshold in milliseconds.
   */
  public long getLeakDetectionThreshold() {
    return leakDetectionThreshold;
  }

  /**
   * The auto-commit setting.
   * @return the auto-commit setting.
   */
  public boolean isAutoCommit() {
    return autoCommit;
  }

  /**
   * The read-only setting.
   * @return the read-only setting.
   */
  public boolean isReadOnly() {
    return readOnly;
  }

  /**
   * The transaction isolation level.
   * @return the transaction isolation level.
   */
  public String getIsolationLevel() {
    return isolationLevel;
  }

  /**
   * The catalog name.
   * @return the catalog name.
   */
  public String getCatalog() {
    return catalog;
  }

  /**
   * Whether pool suspension is allowed.
   * @return whether pool suspension is allowed.
   */
  public boolean isAllowPoolSuspension() {
    return allowPoolSuspension;
  }

  /**
   * Whether to register MBeans.
   * @return whether to register MBeans.
   */
  public boolean isRegisterMbeans() {
    return registerMbeans;
  }

  /**
   * The JDBC driver class name.
   * @return the JDBC driver class name.
   */
  public String getDriverClassName() {
    return driverClassName;
  }

  /**
   * The DataSource class name.
   * @return the DataSource class name.
   */
  public String getDataSourceClassName() {
    return dataSourceClassName;
  }

  /**
   * The connection test query.
   * @return the connection test query.
   */
  public String getConnectionTestQuery() {
    return connectionTestQuery;
  }

  /**
   * The connection initialization SQL.
   * @return the connection initialization SQL.
   */
  public String getConnectionInitSql() {
    return connectionInitSql;
  }

  /**
   * The pool name.
   * @return the pool name.
   */
  public String getPoolName() {
    return poolName;
  }

  /**
   * Validate the configuration.
   * <p>
   * The URL must be set to a valid URL, the remaining parameters are optional.
   *
   * @param path the configuration path to this set of properties.
   * @throws IllegalArgumentException if the configuration is not valid.
   */
  public void validate(String path) throws IllegalArgumentException {
    if (Strings.isNullOrEmpty(url)) {
      throw new IllegalArgumentException(path + ".url is not set");
    }
    try {
      new URI(url);
    } catch (URISyntaxException x) {
      String message = x.getMessage();
      if (Strings.isNullOrEmpty(message)) {
        throw new IllegalArgumentException(path + ".url is not a valid url");
      } else {
        throw new IllegalArgumentException(path + ".url is not a valid url: " + message);
      }
    }

    // Additional validation for HikariCP specific properties
    if (maxPoolSize <= 0) {
      throw new IllegalArgumentException(path + ".maxPoolSize must be greater than 0");
    }
    if (minimumIdle < -1) {
      throw new IllegalArgumentException(path + ".minimumIdle must be -1 or greater");
    }
    if (minimumIdle > maxPoolSize) {
      throw new IllegalArgumentException(path + ".minimumIdle cannot be greater than maxPoolSize");
    }
    if (connectionTimeout < 250) {
      throw new IllegalArgumentException(path + ".connectionTimeout must be at least 250ms");
    }
    if (validationTimeout < 250) {
      throw new IllegalArgumentException(path + ".validationTimeout must be at least 250ms");
    }
  }
}
