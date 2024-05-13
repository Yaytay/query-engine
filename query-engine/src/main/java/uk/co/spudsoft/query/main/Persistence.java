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
   * Maximum number of retries, zero => no retries, <0 => unlimited retries.
   */
  private int retryLimit;

  /**
   * The JDBC data source for storing audit information.
   * @return the JDBC data source for storing audit information.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public DataSourceConfig getDataSource() {
    return dataSource;
  }

  public Duration getRetryBase() {
    return retryBase;
  }

  public Duration getRetryIncrement() {
    return retryIncrement;
  }

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

  public Persistence setRetryBase(Duration retryBase) {
    this.retryBase = retryBase;
    return this;
  }

  public Persistence setRetryIncrement(Duration retryIncrement) {
    this.retryIncrement = retryIncrement;
    return this;
  }

  public Persistence setRetryLimit(int retryLimit) {
    this.retryLimit = retryLimit;
    return this;
  }
  
  public void validate(String path) throws IllegalArgumentException {
    if (retryLimit > 0 && retryBase == null) {
      throw new IllegalArgumentException(path + ".retryLimit is " + retryLimit + ", but " + path + ".retryBase is not set");
    }
    if (dataSource != null) {
      dataSource.validate(path + ".dataSource");
    }
    
  }
  
}
