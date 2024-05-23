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
package uk.co.spudsoft.query.defn;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.co.spudsoft.params4j.SecretsSerializer;

/**
 * Definition of an endpoint that can be used for querying data.
 * An Endpoint represents a connection to a data source, where a {@link Source} represents an actual data query.
 * For {@link EndpointType#HTTP} {@link Source}s there is often a one-to-one relationship between {@link Source} and Endpoint, but for {@link EndpointType#SQL} Sources there
 * are often multiple {@link Source}s for a single Endpoint.
 * 
 * The credentials for an Endpoint can be specified in three ways:
 * <ul>
 * <li>By including them in the URL specified in the Endpoint definition.
 * This is the least secure option as the URL value will be written to log entries.
 * <li>By explicitly setting username/password on the Endpoint.
 * The password will not be logged, but will be in your configuration files and thus in your source repo.
 * <li>By using secrets set in the configuration of the query engine.
 * This is the most secure option as it puts the responsibility on the deployment to protect the credentials.
 * </ul>
 * 
 * If the secret field is set it will take precedence over both the username and the password set in the Endpoint
 * , as a result it is not valid to set either username or password at the same time as secret.
 * The same does not apply to the condition field, that can be set on both the Endpoint and the Secret (and both conditions
 * must be met for the Endpoint to work).
 * 
 * @author jtalbut
 */
@JsonDeserialize(builder = Endpoint.Builder.class)
@Schema(description = """
                      <P>Definition of an endpoint that can be used for querying data.</P>
                      <P>
                      An Endpoint represents a connection to a data source, where a Source represents an actual data query.
                      For EndpointType.HTTP Sources there is often a one-to-one relationship between Source and Endpoint, but for EndpointType.SQL Sources there
                      are often multiple Sources for a single Endpoint (for SQL a Source is a query and an Endpoint is a database).
                      </P>
                      <P>
                      The credentials for an Endpoint can be specified in three ways:
                      <ul>
                      <li>By including them in the URL specified in the Endpoint definition.
                      This is the least secure option as the URL value will be written to log entries.
                      <li>By explicitly setting username/password on the Endpoint.
                      The password will not be logged, but will be in your configuration files and thus in your source repo.
                      <li>By using secrets set in the configuration of the query engine.
                      This is the most secure option as it puts the responsibility on the deployment to protect the credentials.
                      </ul>
                      </P>
                      <P>
                      If the secret field is set it will take precedence over both the username and the password set in the Endpoint
                      , as a result it is not valid to set either username or password at the same time as secret.
                      The same does not apply to the condition field, that can be set on both the Endpoint and the Secret (and both conditions
                      must be met for the Endpoint to work).
                      </P>
                      """
)

public class Endpoint {
  
  private final String name;
  private final EndpointType type;
  private final String url;
  private final String urlTemplate;
  private final String secret;
  private final String username;
  private final String password;
  private final Condition condition;

  /**
   * Validate the configuration.
   * 
   * @throws IllegalArgumentException if the definition is not valid.
   */
  public void validate() throws IllegalArgumentException {
    if (Strings.isNullOrEmpty(name)) {
      throw new IllegalArgumentException("Endpoint has no name.");
    }
    if (condition != null) {
      condition.validate();
    }
    if (Strings.isNullOrEmpty(url) && Strings.isNullOrEmpty(urlTemplate))  {
      throw new IllegalArgumentException("Endpoint " + name + " has neither url nor urlTemplate set.");
    }
    if (!Strings.isNullOrEmpty(url) && !Strings.isNullOrEmpty(urlTemplate))  {
      throw new IllegalArgumentException("Endpoint " + name + " has both url and urlTemplate set, they are mutually incompatible.");
    }
    if (!Strings.isNullOrEmpty(username) && !Strings.isNullOrEmpty(secret))  {
      throw new IllegalArgumentException("Endpoint " + name + " has both username and secret set, they are mutually incompatible.");
    }
    if (!Strings.isNullOrEmpty(password) && !Strings.isNullOrEmpty(secret))  {
      throw new IllegalArgumentException("Endpoint " + name + " has both password and secret set, they are mutually incompatible.");
    }
  }
  
  /**
   * Get the name of the Endpoint, that will be used to refer to it in Sources.
   * @return the name of the Endpoint.
   */
  @Schema(description = """
                        <P>The name of the Endpoint, that will be used to refer to it in Sources.
                        """
          , maxLength = 100
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public String getName() {
    return name;
  }
  
  /**
   * Get the type of Endpoint being configured.
   * @return the type of Endpoint being configured.
   */
  @Schema(description = """
                        <P>The type of Endpoint being configured</P>
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public EndpointType getType() {
    return type;
  }

  /**
   * Get a URL that defined the Endpoint.
   * Invalid if the URLTemplate value is set to a non-empty string.
   * For security reasons the URL should not contain credentials - the URL may be logged but the username and password will not be.
   * @return a URL that defined the Endpoint.
   */
  @Schema(description = """
                        <P>A URL that defines the Endpoint.</P>
                        <P>
                        Invalid if the URL template field is provided.
                        </P>
                        <P>
                        For security reasons the URL should not contain credentials - the URL may be logged but the username and password
                        fields of the Endpoint will not be.
                        </P>
                        """
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , maxLength = 1000
          , format = "uri"
  )
  public String getUrl() {
    return url;
  }

  /**
   * Get a StringTemplate that will be rendered as the URL for the Endpoint.
   * Invalid if the URL value is set to a non-empty string.
   * For security reasons the URL should not contain credentials - the URL may be logged but the username and password will not be.
   * @return a StringTemplate that will be rendered as the URL for the Endpoint.
   */
  @Schema(description = """
                        <P>A StringTemplate that will be rendered as the URL that defines the Endpoint.</P>
                        <P>
                        Invalid if the URL field is provided.
                        </P>
                        <P>
                        For security reasons the URL should not contain credentials - the URL may be logged but the username and password
                        fields of the Endpoint will not be.
                        </P>
                        """
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , maxLength = 1000000
  )
  public String getUrlTemplate() {
    return urlTemplate;
  }    

  /**
   * Get the name of the secret that contains the credentials to be used for the connection.
   * Invalid if the username or password are set.
   * @return the name of secret that contains the credentials to be used for the connection.
   */
  @Schema(description = """
                        <P>The name of the secret that contains the credentials to be used for the connection.</P>
                        <P>
                        Invalid if the username or password fields are provided.
                        </P>
                        <P>
                        The named secret must be configured in the instance of the query engine.
                        The currently running instance is in design mode and thus should not be your live instance,
                        which unfortuantely means it is not possible to list the known secrets of your live instance here.
                        Please ask your systems administrator for this information.
                        </P>
                        """
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , maxLength = 100
  )
  public String getSecret() {
    return secret;
  }
  
  /**
   * Get a username that should be used when communicating with the Endpoint.
   * @return a username that should be used when communicating with the Endpoint.
   */
  @Schema(description = """
                        <P>The username that should be used when communicating with the endpoint.</P>
                        <P>
                        Invalid if the secret field  provided.
                        </P>
                        <P>
                        The username will be logged.
                        </P>
                        """
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , maxLength = 100
  )
  public String getUsername() {
    return username;
  }

  /**
   * Get a password that should be used when communicating with the Endpoint.
   * @return a password that should be used when communicating with the Endpoint.
   */
  @JsonSerialize(using = SecretsSerializer.class)
  @Schema(description = """
                        <P>The password that should be used when communicating with the endpoint.</P>
                        <P>
                        Invalid if the secret field  provided.
                        </P>
                        <P>
                        The password will not be logged.
                        </P>
                        <P>
                        Any password entered here will inevitably end up in your pipeline repo.
                        This is not a security best practice.
                        Please use secrets instead of username/password for live deployments.
                        </P>
                        """
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , maxLength = 1000
  )
  public String getPassword() {
    return password;
  }

  /**
   * Get a condition that must be passed for the endpoint to be used.
   * @return a condition that must be passed for the endpoint to be used.
   */
  @Schema(description = """
                        <P>A condition that must be passed for the endpoint to be used.</P>
                        """
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  public Condition getCondition() {
    return condition;
  }
  
  /**
   * Builder class for {@link Endpoint} objects.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private String name;
    private EndpointType type;
    private String url;
    private String urlTemplate;
    private String secret;
    private String username;
    private String password;
    private Condition condition;

    private Builder() {
    }

    /**
     * Set the name of the Endpoint in the builder.
     * @param value the name of the Endpoint.
     * @return this, so that the builder may be used fluently.
     */
    public Builder name(final String value) {
      this.name = value;
      return this;
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
     * Set the UrlTemplate of the Endpoint in the builder.
     * @param value the UrlTemplate of the Endpoint.
     * @return this, so that the builder may be used fluently.
     */
    public Builder urlTemplate(final String value) {
      this.urlTemplate = value;
      return this;
    }

    /**
     * Set the name of secret that contains the credentials to be used for the Endpoint in the builder.
     * @param value the name of secret that contains the credentials to be used for the Endpoint.
     * @return this, so that the builder may be used fluently.
     */
    public Builder secret(final String value) {
      this.secret = value;
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
     * Set the condition on the Endpoint in the builder.
     * @param value the condition on the Endpoint.
     * @return this, so that the builder may be used fluently.
     */
    public Builder condition(final Condition value) {
      this.condition = value;
      return this;
    }

    /**
     * Construct a new Endpoint object.
     * @return a new Endpoint object.
     */
    public Endpoint build() {
      return new Endpoint(name, type, url, urlTemplate, secret, username, password, condition);
    }
  }

  /**
   * Construct a new {@link uk.co.spudsoft.query.defn.Endpoint.Builder} object.
   * @return a new {@link uk.co.spudsoft.query.defn.Endpoint.Builder} object.
   */
  public static Endpoint.Builder builder() {
    return new Endpoint.Builder();
  }

  private Endpoint(final String name, final EndpointType type, final String url, final String urlTemplate, final String secret, final String username, final String password, final Condition condition) {
    this.name = name;
    this.type = type;
    this.url = url;
    this.urlTemplate = urlTemplate;
    this.secret = secret;
    this.username = username;
    this.password = password;
    this.condition = condition;
  }

  
}
