/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

/**
 *
 * @author jtalbut
 */
public class SqlQuery {
  
  private final SqlSource source;
  private final String query;

  public SqlQuery(SqlSource source, String query) {
    this.source = source;
    this.query = query;
  }

  public SqlSource getSource() {
    return source;
  }

  public String getQuery() {
    return query;
  }
  
  
  
}
