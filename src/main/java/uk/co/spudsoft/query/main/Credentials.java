/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import uk.co.spudsoft.params4j.SecretsSerializer;
import uk.co.spudsoft.query.main.Credentials.Builder;

/**
 *
 * @author jtalbut
 */
@JsonDeserialize(builder = Builder.class)
public class Credentials {
  
  /**
   * The username.
   */
  private final String username;
  /**
   * The password.
   */
  private final String password;

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
   * The password.
   * @return the password.
   */
  @JsonSerialize(using = SecretsSerializer.class)
  public String getPassword() {
    return password;
  }

  /**
   * Builder.
   */
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Builder class should result in all instances being immutable when object is built")
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
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
