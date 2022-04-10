/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main;

/**
 *
 * @author jtalbut
 */
public class DataSource {

  private String url;
  private String schema;
  private Credentials user;
  private Credentials adminUser;
  private int maxPoolSize = 10;

  /**
   * The URL to use for accessing the datasource.
   * @return URL to use for accessing the datasource.
   */
  public String getUrl() {
    return url;
  }

  /**
   * The URL to use for accessing the datasource.
   * @param url URL to use for accessing the datasource.
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * The database schema to use when accessing the datasource.
   * @return database schema to use when accessing the datasource.
   */
  public String getSchema() {
    return schema;
  }

  /**
   * The database schema to use when accessing the datasource.
   * @param schema database schema to use when accessing the datasource.
   */
  public void setSchema(String schema) {
    this.schema = schema;
  }

  /**
   * The credentials to use for standard actions (DML).
   * @return The credentials to use for standard actions (DML).
   */
  public Credentials getUser() {
    return user;
  }

  /**
   * The credentials to use for standard actions (DML).
   * @param user The credentials to use for standard actions (DML).
   */
  public void setUser(Credentials user) {
    this.user = user;
  }

  /**
   * The credentials to use for preparing the database (DDL).
   * @return The credentials to use for preparing the database (DDL).
   */
  public Credentials getAdminUser() {
    return adminUser;
  }

  /**
   * The credentials to use for preparing the database (DDL).
   * @param adminUser The credentials to use for preparing the database (DDL).
   */
  public void setAdminUser(Credentials adminUser) {
    this.adminUser = adminUser;
  }

  /**
   * The maximum size of the connection pool.
   * @return The maximum size of the connection pool.
   */
  public int getMaxPoolSize() {
    return maxPoolSize;
  }

  /**
   * The maximum size of the connection pool.
   * @param maxPoolSize The maximum size of the connection pool.
   */
  public void setMaxPoolSize(int maxPoolSize) {
    this.maxPoolSize = maxPoolSize;
  }
  
  
}
