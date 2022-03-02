/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

/**
 *
 * @author jtalbut
 */
public class SqlSource {
  
  private final String url;
  private final String username;
  private final String password;

  public SqlSource(String url, String username, String password) {
    this.url = url;
    this.username = username;
    this.password = password;
  }

  public String getUrl() {
    return url;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }
  
  
  
}
