/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

/**
 * Definition of an endpoint that can be used for querying data.
 * @author jtalbut
 */
public class Endpoint {
  
  private EndpointType type;
  private String driver;
  private String url;
  private String username;
  private String password;

  public EndpointType getType() {
    return type;
  }

  public void setType(EndpointType type) {
    this.type = type;
  }

  public String getDriver() {
    return driver;
  }

  public void setDriver(String driver) {
    this.driver = driver;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
  
  
}
