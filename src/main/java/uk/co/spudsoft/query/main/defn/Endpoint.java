/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Definition of an endpoint that can be used for querying data.
 * @author jtalbut
 */
@JsonDeserialize(builder = Endpoint.Builder.class)
public class Endpoint {
  
  private final EndpointType type;
  private final String url;
  private final String username;
  private final String password;

  public EndpointType getType() {
    return type;
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

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private EndpointType type;
    private String url;
    private String username;
    private String password;

    private Builder() {
    }

    public Builder type(final EndpointType value) {
      this.type = value;
      return this;
    }

    public Builder url(final String value) {
      this.url = value;
      return this;
    }

    public Builder username(final String value) {
      this.username = value;
      return this;
    }

    public Builder password(final String value) {
      this.password = value;
      return this;
    }

    public Endpoint build() {
      return new uk.co.spudsoft.query.main.defn.Endpoint(type, url, username, password);
    }
  }

  public static Endpoint.Builder builder() {
    return new Endpoint.Builder();
  }

  private Endpoint(final EndpointType type, final String url, final String username, final String password) {
    this.type = type;
    this.url = url;
    this.username = username;
    this.password = password;
  }

  
}
