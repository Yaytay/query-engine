/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Strings;
import uk.co.spudsoft.params4j.SecretsSerializer;

/**
 *
 * @author jtalbut
 */
public class Credentials {
  
  /**
   * The username.
   */
  protected String username;
  /**
   * The password.
   */
  protected String password;

  /**
   * Constructor.
   */
  public Credentials() {
  }

  /**
   * Constructor.
   * @param username The username to use, if any.
   * @param password The password to use, if any.
   */
  public Credentials(String username, String password) {
    this.username = username;
    this.password = password;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    if (!Strings.isNullOrEmpty(username)) {
      sb.append("username=").append(username);
    }
    sb.append("}");
    return sb.toString();
  }
  
  /**
   * The username.
   * @return The username.
   */
  public String getUsername() {
    return username;
  }

  /**
   * The username.
   * @param username the username.
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * The password.
   * @return the password.
   */
  @JsonSerialize(using = SecretsSerializer.class)
  public String getPassword() {
    return password;
  }

  /**
   * The password.
   * @param password the password.
   */
  @JsonSerialize(using = SecretsSerializer.class)
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Builder.
   */
  public static class Builder {

    private String username;
    private String password;

    private Builder() {
    }

    /**
     * Set the username.
     * @param value the username.
     * @return this.
     */
    public Builder username(final String value) {
      this.username = value;
      return this;
    }

    /**
     * Set the password.
     * @param value the password.
     * @return this.
     */
    public Builder password(final String value) {
      this.password = value;
      return this;
    }

    /**
     * Construct a new Credentials object.
     * @return a new Credentials object.
     */
    public Credentials build() {
      return new Credentials(username, password);
    }
  }

  /**
   * Create a new builder.
   * @return a new builder.
   */
  public static Credentials.Builder builder() {
    return new Credentials.Builder();
  }
  
}
