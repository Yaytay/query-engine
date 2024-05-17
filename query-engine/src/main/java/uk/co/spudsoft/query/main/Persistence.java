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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;

/**
 * Configuration for the internal datastore using by Query Engine for audit and for login sessions.
 * 
 * @author jtalbut
 */
public class Persistence {

  /**
   * JDBC data source for storing audit information.
   */
  private DataSourceConfig dataSource = null;
  /**
   * Time to wait for re-attempting to connect to the datasource.
   */
  private Duration retryBase = Duration.ofMillis(1000);
  /**
   * Additional time to wait for re-attempting to connect to the datasource for each retry.
   */
  private Duration retryIncrement;
  /**
   * Maximum number of retries, zero => no retries, &lt;0 implies unlimited retries.
   */
  private int retryLimit;

  /**
   * The JDBC data source for storing audit information.
   * 
   * @return the JDBC data source for storing audit information.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public DataSourceConfig getDataSource() {
    return dataSource;
  }

  /**
   * Time to wait for re-attempting to connect to the datasource.
   * 
   * @return the time to wait for re-attempting to connect to the datasource.
   */
  public Duration getRetryBase() {
    return retryBase;
  }

  /**
   * Additional time to wait for re-attempting to connect to the datasource for each retry.
   * @return the additional time to wait for re-attempting to connect to the datasource for each retry.
   */
  public Duration getRetryIncrement() {
    return retryIncrement;
  }

  /**
   * Maximum number of retries, zero => no retries, &lt;0 implies unlimited retries.
   * @return the maximum number of retries, zero => no retries, &lt;0 implies unlimited retries.
   */
  public int getRetryLimit() {
    return retryLimit;
  }

  /**
   * The JDBC data source for storing audit information.
   * @param dataSource the JDBC data source for storing audit information.
   * @return this, so the method may be used fluently.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public Persistence setDataSource(DataSourceConfig dataSource) {
    this.dataSource = dataSource;
    return this;
  }

  /**
   * Time to wait for re-attempting to connect to the datasource.
   * @param retryBase the time to wait for re-attempting to connect to the datasource.
   * @return this, so the method may be used fluently.
   */
  public Persistence setRetryBase(Duration retryBase) {
    this.retryBase = retryBase;
    return this;
  }

  /**
   * Additional time to wait for re-attempting to connect to the datasource for each retry.
   * @param retryIncrement the additional time to wait for re-attempting to connect to the datasource for each retry.
   * @return this, so the method may be used fluently.
   */
  public Persistence setRetryIncrement(Duration retryIncrement) {
    this.retryIncrement = retryIncrement;
    return this;
  }

  /**
   * Maximum number of retries, zero => no retries, &lt;0 implies unlimited retries.
   * @param retryLimit the maximum number of retries, zero => no retries, &lt;0 implies unlimited retries.
   * @return this, so the method may be used fluently.
   */
  public Persistence setRetryLimit(int retryLimit) {
    this.retryLimit = retryLimit;
    return this;
  }
  
  /**
   * Validate the provided parameters.
   * 
   * @param path The configuration path to this item, for reporting.
   * @throws IllegalArgumentException if anything in the parameters is invalid.
   */
  public void validate(String path) throws IllegalArgumentException {
    if (retryLimit > 0 && retryBase == null) {
      throw new IllegalArgumentException(path + ".retryLimit is " + retryLimit + ", but " + path + ".retryBase is not set");
    }
    if (dataSource != null) {
      dataSource.validate(path + ".dataSource");
    }
    
  }
  
}
