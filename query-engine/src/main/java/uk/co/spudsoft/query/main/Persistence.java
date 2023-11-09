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

/**
 *
 * @author jtalbut
 */
public class Persistence {

  /**
   * JDBC data source for storing audit information.
   */
  private DataSourceConfig dataSource = new DataSourceConfig();
  /**
   * milliseconds to wait for re-attempting to connect to the datasource.
   */
  private int retryBaseMs = 1000;
  /**
   * additional milliseconds to wait for re-attempting to connect to the datasource for each retry.
   */
  private int retryIncrementMs;
  /**
   * maximum number of retries, zero => no retries, <0 => unlimited retries.
   */
  private int retryLimit;

  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public DataSourceConfig getDataSource() {
    return dataSource;
  }

  public int getRetryBaseMs() {
    return retryBaseMs;
  }

  public int getRetryIncrementMs() {
    return retryIncrementMs;
  }

  public int getRetryLimit() {
    return retryLimit;
  }

  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public Persistence setDataSource(DataSourceConfig dataSource) {
    this.dataSource = dataSource;
    return this;
  }

  public Persistence setRetryBaseMs(int retryBaseMs) {
    this.retryBaseMs = retryBaseMs;
    return this;
  }

  public Persistence setRetryIncrementMs(int retryIncrementMs) {
    this.retryIncrementMs = retryIncrementMs;
    return this;
  }

  public Persistence setRetryLimit(int retryLimit) {
    this.retryLimit = retryLimit;
    return this;
  }
  
}
