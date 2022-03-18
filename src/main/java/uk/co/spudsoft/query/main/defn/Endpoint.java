/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Definition of an endpoint that can be used for querying data.
 * An Endpoint represents a connection to a data source, where a {@link Source} represents an actual data query.
 * For {@link EndpointType#HTTP} {@link Source}s there is often a one-to-one relationship between {@link Source} and Endpoint, but for {@link EndpointType#SQL} Sources there
 * are often multiple {@link Source}s for a single Endpoint.
 * @author jtalbut
 */
@JsonDeserialize(builder = Endpoint.Builder.class)
public class Endpoint {
  
  private final EndpointType type;
  private final String url;
  private final String username;
  private final String password;

  /**
   * Get the type of Endpoint being configured.
   * @return the type of Endpoint being configured.
   */
  public EndpointType getType() {
    return type;
  }

  /**
   * Get a URL that defined the Endpoint.
   * For security reasons the URL should not contain credentials - the URL may be logged but the username and password will not be.
   * @return a URL that defined the Endpoint.
   */
  public String getUrl() {
    return url;
  }

  /**
   * Get a username that should be used when communicating with the Endpoint.
   * @return a username that should be used when communicating with the Endpoint.
   */
  public String getUsername() {
    return username;
  }

  /**
   * Get a password that should be used when communicating with the Endpoint.
   * @return a password that should be used when communicating with the Endpoint.
   */
  public String getPassword() {
    return password;
  }

  /**
   * Builder class for {@link Endpoint} objects.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private EndpointType type;
    private String url;
    private String username;
    private String password;

    private Builder() {
    }

    /**
     * Set the type of the Endpoint in the builder.
     * @param value the type of the Endpoint.
     * @return this, so that the builder may be used fluently.
     */
    public Builder type(final EndpointType value) {
      this.type = value;
      return this;
    }

    /**
     * Set the URL of the Endpoint in the builder.
     * @param value the URL of the Endpoint.
     * @return this, so that the builder may be used fluently.
     */
    public Builder url(final String value) {
      this.url = value;
      return this;
    }

    /**
     * Set the username of the Endpoint in the builder.
     * @param value the username of the Endpoint.
     * @return this, so that the builder may be used fluently.
     */
    public Builder username(final String value) {
      this.username = value;
      return this;
    }

    /**
     * Set the password of the Endpoint in the builder.
     * @param value the password of the Endpoint.
     * @return this, so that the builder may be used fluently.
     */
    public Builder password(final String value) {
      this.password = value;
      return this;
    }

    /**
     * Construct a new Endpoint object.
     * @return a new Endpoint object.
     */
    public Endpoint build() {
      return new Endpoint(type, url, username, password);
    }
  }

  /**
   * Construct a new {@link uk.co.spudsoft.query.main.defn.Endpoint.Builder} object.
   * @return a new {@link uk.co.spudsoft.query.main.defn.Endpoint.Builder} object.
   */
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
