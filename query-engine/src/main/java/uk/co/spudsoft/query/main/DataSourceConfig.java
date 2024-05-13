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
 * Configuration data for commuicating with a data source.
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
  /**
   * The maximum size of the connection pool.
   */
  private int minPoolSize = 4;
  /**
   * The maximum size of the connection pool.
   */
  private int maxPoolSize = 10;

  public DataSourceConfig setUrl(String url) {
    this.url = url;
    return this;
  }

  public DataSourceConfig setSchema(String schema) {
    this.schema = schema;
    return this;
  }

  public DataSourceConfig setUser(Credentials user) {
    this.user = user;
    return this;
  }

  public DataSourceConfig setAdminUser(Credentials adminUser) {
    this.adminUser = adminUser;
    return this;
  }

  public DataSourceConfig setMaxPoolSize(int maxPoolSize) {
    this.maxPoolSize = maxPoolSize;
    return this;
  }

  public void setMinPoolSize(int minPoolSize) {
    this.minPoolSize = minPoolSize;
  }

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
  
  public void validate(String path) throws IllegalArgumentException {
    if (Strings.isNullOrEmpty(url)) {
      throw new IllegalArgumentException(path + ".url is not set");
    }
    try {
      new URI(url);
    } catch (URISyntaxException x) {
      throw new IllegalArgumentException(path + ".url is not a valid url");
    }
  }
  
}
