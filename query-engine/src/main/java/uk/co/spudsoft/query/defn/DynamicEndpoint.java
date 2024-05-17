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
import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import uk.co.spudsoft.query.exec.PreProcessorInstance;
import uk.co.spudsoft.query.exec.preprocess.DynamicEndpointPreProcessorInstance;

/**
 * Represents a pipeline that can used to generate endpoints before the main pipeline is run.
 * 
 * The expected use is for the source to query a database that contains connection strings (in vertx format, not JDBC format)
 * based on information contained in the request (usually extracted from a JWT).
 * In this way a single pipeline can support multiple databases based upon request content.
 * 
 * Most of the properties of the DynamicEndpointSource have default values and any fields that do not exist in the 
 * results stream from the source pipeline will be silently ignored, so the DynamicEndpointSource usually requires minimal configuration.
 * 
 * If generated endpoints have a condition they will be silently dropped unless the condition is met.
 * All remaining endpoints generated by the DynamicEndpointSource will be added to the endpoints usable by the outer query in the order they are returned by the source.
 * If endpoints do not have unique keys this does mean that later ones will overwrite earlier ones.
 * 
 * The original endpoints that existed before the DynamicEndpointSource do not have special protection
 * , if the DynamicEndpointSource generates endpoints with the same key as existing endpoints they will be overwritten.
 * 
 * Storing credentials unencrypted in a database is not ideal security, but it's better than putting them in a git repository.
 * The recommendation would be to not start storing credentials in a database in order to satisfy the needs of the Query Engine,
 * but if the credentials are already there then there is no reason to avoid using them.
 * Ideally the endpoints generated should reference secrets that the query engine knows.
 * 
 * @author jtalbut
 */
@JsonDeserialize(builder = DynamicEndpoint.Builder.class)
@Schema(
        description = """
                      <P>
                      Represents a pipeline that can used to generate endpoints before the main pipeline is run.
                      </P>
                      <P>
                      The expected use is for the source to query a database that contains connection strings (in vertx format, not JDBC format)
                      based on information contained in the request (usually extracted from a JWT).
                      In this way a single pipeline can support multiple databases based upon request content.
                      </P>
                      <P>
                      Most of the properties of the DynamicEndpointSource have default values and any fields that do not exist in the
                      results stream from the source pipeline will be silently ignored, so the DynamicEndpointSource usually requires minimal configuration.
                      </P>
                      <P>
                      If generated endpoints have a condition they will be silently dropped unless the condition is met.
                      All remaining endpoints generated by the DynamicEndpointSource will be added to the endpoints usable by the outer query in the order they are returned by the source.
                      If endpoints do not have unique keys this does mean that later ones will overwrite earlier ones.
                      </P>
                      <P>
                      The original endpoints that existed before the DynamicEndpointSource do not have special protection
                      , if the DynamicEndpointSource generates endpoints with the same key as existing endpoints they will be overwritten.
                      </P>
                      <P>
                      Storing credentials unencrypted in a database is not ideal security, but it's better than putting them in a git repository.
                      The recommendation would be to not start storing credentials in a database in order to satisfy the needs of the Query Engine,
                      but if the credentials are already there then there is no reason to avoid using them.
                      </P>
                      """
)
public class DynamicEndpoint {
  
  private final SourcePipeline input;
  private final String key;
  private final String typeField;
  private final String keyField;
  private final String urlField;
  private final String urlTemplateField;
  private final String secretField;
  private final String usernameField;
  private final String passwordField;
  private final String conditionField;

  /**
   * Validate the configuration.
   * <p>
   * The input must be a valid {@link SourcePipeline}; either key or keyField must be set; and either urlField or urlTemplateField must be set.
   */
  public void validate() {
    if (input == null) {
      throw new IllegalArgumentException("Input not specified in dynamic endpoint");
    }
    input.validate();
    if (Strings.isNullOrEmpty(key) && Strings.isNullOrEmpty(keyField)) {
      throw new IllegalArgumentException("Neither key nor keyField specified in dynamic endpoint");
    }
    if (Strings.isNullOrEmpty(urlField) && Strings.isNullOrEmpty(urlTemplateField)) {
      throw new IllegalArgumentException("Neither urlField nor urlTemplateField specified in dynamic endpoint");
    }
  }
  
  /**
   * Create a {@link DynamicEndpointPreProcessorInstance} based on this configuration.
   * @param vertx The Vert.x instance.
   * @param context The Vert.x context.
   * @return a newly created {@link DynamicEndpointPreProcessorInstance} object.
   */
  public PreProcessorInstance createInstance(Vertx vertx, Context context) {
    return new DynamicEndpointPreProcessorInstance(vertx, context, this);
  }
  
  /**
   * Get the pipeline used to generate the endpoints.
   * 
   * This pipeline can only use endpoints already in existence.
   * This usually means those defined statically in the outer pipeline, but there is nothing to prevent a series of DynamicEndpointSource configurations
   * with later ones using endpoints generated in earlier ones.
   * 
   * If the data stream generated by the source is missing any fields those fields will be silently set to null in the generated endpoint.
   * This means that the default configuration is usually adequate, just requiring key/keyField and source.
   * 
   * A DynamicEndpointSource may return multiple endpoints.
   * If multiple endpoints are returned all of those that pass their conditions will be added to the pipeline.
   * For efficiency reasons the pipeline used in the DynamicEndpointSource should return as few endpoints as possible.
   * 
   * If multiple endpoints are returned they should specify the keyField and have a unique key for each endpoint.
   * In the absence of this all the key value will be used for all of the endpoints and only the last will be accessible.
   * 
   * @return the pipeline used to generate the endpoints.
   */
  @Schema(description = """
                        <P>Get the pipeline used to generate the endpoints.</P>
                        <P>
                        This pipeline can only use endpoints already in existence.
                        This usually means those defined statically in the outer pipeline, but there is nothing to prevent a series of DynamicEndpointSource configurations
                        with later ones using endpoints generated in earlier ones.
                        </P>
                        <P>
                        If the data stream generated by the source is missing any fields those fields will be silently set to null in the generated endpoint.
                        This means that the default configuration is usually adequate, just requiring key/keyField and source.
                        </P>
                        <P>
                        A DynamicEndpointSource may return multiple endpoints.
                        If multiple endpoints are returned all of those that pass their conditions will be added to the pipeline.
                        For efficiency reasons the pipeline used in the DynamicEndpointSource should return as few endpoints as possible.
                        </P>
                        <P>
                        If multiple endpoints are returned they should specify the keyField and have a unique key for each endpoint.
                        In the absence of this all the key value will be used for all of the endpoints and only the last will be accessible.
                        </P>
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public SourcePipeline getInput() {
    return input;
  }

  /**
   * Get the key used to identify all endpoints found by this DynamicEndpointSource.
   * To be used when the source is only going to return a single endpoint and the key is not part of the query.
   * There is no default value, either key or the keyField must be set.
   * @return the key used to identify all endpoints found by this DynamicEndpointSource.
   */
  @Schema(
          description = """
                        <P>The key used to identify all endpoints found by this DynamicEndpointSource.</P>
                        <P>
                        To be used when the source is only going to return a single endpoint and the key is not part of the query.
                        There is no default value, either key or the keyField must be set.
                        </P>
                        """
          , maxLength = 100
  )
  public String getKey() {
    return key;
  }

  /**
   * Get the name of the field that will contain the type of each endpoint.
   * The default value is "type".
   * @return the name of the field that will contain the type of the endpoint.
   */
  @Schema(
          description = """
                        <P>The name of the field that will contain the type of each endpoint.</P>
                        """
          , defaultValue = "type"
          , maxLength = 100
  )
  public String getTypeField() {
    return typeField;
  }

  /**
   * Get the name of the field that will contain the key for each endpoint.
   * There is no default value, either key or the keyField must be set.
   * @return the name of the field that will contain the key for each endpoint.
   */
  @Schema(
          description = """
                        <P>The name of the field that will contain the key for each endpoint.</P>
                        <P>
                        There is no default value, either key or the keyField must be set.
                        </P>
                        """
          , maxLength = 100
  )
  public String getKeyField() {
    return keyField;
  }

  /**
   * Get the name of the field that contains the url for each endpoint.
   * The default value is "url".
   * @return the name of the field that contains the url for each endpoint.
   */
  @Schema(
          description = """
                        <P>The name of the field that will contain the URL for each endpoint.</P>
                        <P>
                        Note that it is entirely valid for both urlField and urlTemplateField to have values
                        , but if the pipeline produces rows in which both fields have values the resulting Endpoint
                        will be invalid.
                        </P>
                        """
          , defaultValue = "url"
          , maxLength = 100
  )
  public String getUrlField() {
    return urlField;
  }

  /**
   * Get the name of the field that contains the urlTemplate for each endpoint.
   * The default value is "urlTemplate".
   * @return the name of the field that contains the urlTemplate for each endpoint.
   */
  @Schema(
          description = """
                        <P>The name of the field that will contain the URL template for each endpoint.</P>
                        <P>
                        Note that it is entirely valid for both urlField and urlTemplateField to have values
                        , but if the pipeline produces rows in which both fields have values the resulting Endpoint
                        will be invalid.
                        </P>
                        """
          , defaultValue = "urlTemplate"
          , maxLength = 100
  )
  public String getUrlTemplateField() {
    return urlTemplateField;
  }

  /**
   * Get the name of the field that contains the secret for each endpoint.
   * The default value is "secret".
   * @return the name of the field that contains the secret for each endpoint.
   */
  @Schema(
          description = """
                        <P>The name of the field that will contain the secret for each endpoint.</P>
                        <P>
                        Note that it is entirely valid for both secretField and usernameField/passwordField to have values
                        , but if the pipeline produces rows in which both secretField and either of the other two have values
                        the resulting Endpoint will be invalid.
                        </P>
                        """
          , defaultValue = "secret"
          , maxLength = 100
  )
  public String getSecretField() {
    return secretField;
  }

  /**
   * Get the name of the field that contains the username for each endpoint.
   * The default value is "username".
   * @return the name of the field that contains the username for each endpoint.
   */
  @Schema(
          description = """
                        <P>The name of the field that will contain the username for each endpoint.</P>
                        <P>
                        Note that it is entirely valid for both secretField and usernameField/passwordField to have values
                        , but if the pipeline produces rows in which both secretField and either of the other two have values
                        the resulting Endpoint will be invalid.
                        </P>
                        """
          , defaultValue = "username"
          , maxLength = 100
  )
  public String getUsernameField() {
    return usernameField;
  }

  /**
   * Get the name of the field that contains the password for each endpoint.
   * The default value is "password".
   * @return the name of the field that contains the password for each endpoint.
   */
  @Schema(
          description = """
                        <P>The name of the field that will contain the password for each endpoint.</P>
                        <P>
                        Note that it is entirely valid for both secretField and usernameField/passwordField to have values
                        , but if the pipeline produces rows in which both secretField and either of the other two have values
                        the resulting Endpoint will be invalid.
                        </P>
                        """
          , defaultValue = "password"
          , maxLength = 100
  )
  public String getPasswordField() {
    return passwordField;
  }

  /**
   * Get the name of the field that contains the condition for each endpoint.
   * The default value is "condition".
   * @return the name of the field that contains the condition for each endpoint.
   */
  @Schema(
          description = """
                        <P>The name of the field that will contain the condition for each endpoint.</P>
                        """
          , defaultValue = "condition"
          , maxLength = 100
  )
  public String getConditionField() {
    return conditionField;
  }

  /**
   * Builder class.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private SourcePipeline input;
    private String key;
    private String typeField = "type";
    private String keyField;
    private String urlField = "url";
    private String urlTemplateField = "urlTemplate";
    private String secretField = "secret";
    private String usernameField = "username";
    private String passwordField = "password";
    private String conditionField = "condition";

    private Builder() {
    }

    /**
     * Set the input value on the builder.
     * @param value the input value.
     * @return this, so that the builder may be used in a fluent manner.
     */
    public Builder input(final SourcePipeline value) {
      this.input = value;
      return this;
    }

    /**
     * Set the key value on the builder.
     * @param value the key value.
     * @return this, so that the builder may be used in a fluent manner.
     */
    public Builder key(final String value) {
      this.key = value;
      return this;
    }

    /**
     * Set the typeField value on the builder.
     * @param value the typeField value.
     * @return this, so that the builder may be used in a fluent manner.
     */
    public Builder typeField(final String value) {
      this.typeField = value;
      return this;
    }

    /**
     * Set the keyField value on the builder.
     * @param value the keyField value.
     * @return this, so that the builder may be used in a fluent manner.
     */
    public Builder keyField(final String value) {
      this.keyField = value;
      return this;
    }

    /**
     * Set the urlField value on the builder.
     * @param value the urlField value.
     * @return this, so that the builder may be used in a fluent manner.
     */
    public Builder urlField(final String value) {
      this.urlField = value;
      return this;
    }

    /**
     * Set the urlTemplateField value on the builder.
     * @param value the urlTemplateField value.
     * @return this, so that the builder may be used in a fluent manner.
     */
    public Builder urlTemplateField(final String value) {
      this.urlTemplateField = value;
      return this;
    }

    /**
     * Set the secretField value on the builder.
     * @param value the secretField value.
     * @return this, so that the builder may be used in a fluent manner.
     */
    public Builder secretField(final String value) {
      this.secretField = value;
      return this;
    }

    /**
     * Set the usernameField value on the builder.
     * @param value the usernameField value.
     * @return this, so that the builder may be used in a fluent manner.
     */
    public Builder usernameField(final String value) {
      this.usernameField = value;
      return this;
    }

    /**
     * Set the passwordField value on the builder.
     * @param value the passwordField value.
     * @return this, so that the builder may be used in a fluent manner.
     */
    public Builder passwordField(final String value) {
      this.passwordField = value;
      return this;
    }

    /**
     * Set the conditionField value on the builder.
     * @param value the conditionField value.
     * @return this, so that the builder may be used in a fluent manner.
     */
    public Builder conditionField(final String value) {
      this.conditionField = value;
      return this;
    }

    public DynamicEndpoint build() {
      return new uk.co.spudsoft.query.defn.DynamicEndpoint(input, key, typeField, keyField, urlField, urlTemplateField, secretField, usernameField, passwordField, conditionField);
    }
  }

  public static DynamicEndpoint.Builder builder() {
    return new DynamicEndpoint.Builder();
  }

  private DynamicEndpoint(final SourcePipeline input, final String key, final String typeField, final String keyField, final String urlField, final String urlTemplateField, final String secretField, final String usernameField, final String passwordField, final String conditionField) {
    this.input = input;
    this.key = key;
    this.typeField = typeField;
    this.keyField = keyField;
    this.urlField = urlField;
    this.urlTemplateField = urlTemplateField;
    this.secretField = secretField;
    this.usernameField = usernameField;
    this.passwordField = passwordField;
    this.conditionField = conditionField;
  }
  
}
