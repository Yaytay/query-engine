/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * An Argument represents a named piece of data that will be passed in to a pipepline.
 * Typically these correspond to query string arguments.
 * 
 * @author jtalbut
 */
@JsonDeserialize(builder = Argument.Builder.class)
public class Argument {
  
  private final ArgumentType type;
  private final boolean optional;
  private final String defaultValue;

  /**
   * Get the data of the argument.
   * @return the data of the argument.
   */
  public ArgumentType getType() {
    return type;
  }

  /**
   * Return true if the argument is optional.
   * Optional arguments will be considered to have their default value, if the default value is not set they will be null.
   * @return true if the argument is optional.
   */
  public boolean isOptional() {
    return optional;
  }

  /**
   * Return the default value for the argument.
   * This has no effect if the argument is not optional.
   * @return the default value for the argument. 
   */
  public String getDefaultValue() {
    return defaultValue;
  }

  /**
   * Builder class for Argument objects.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private ArgumentType type;
    private boolean optional;
    private String defaultValue;

    private Builder() {
    }

    /**
     * Set the data type of the Argument in the builder.
     * @param value the data type of the Argument.
     * @return this, so that the builder may be used fluently.
     */
    public Builder type(final ArgumentType value) {
      this.type = value;
      return this;
    }

    /**
     * Set the optional flag of the Argument in the builder.
     * Optional arguments will be considered to have their default value, if the default value is not set they will be null.
     * @param value the optional flag of the Argument.
     * @return this, so that the builder may be used fluently.
     */
    public Builder optional(final boolean value) {
      this.optional = value;
      return this;
    }

    /**
     * Set the default value for the Argument in the builder.
     * @param value the default value for the Argument.
     * @return this, so that the builder may be used fluently.
     */
    public Builder defaultValue(final String value) {
      this.defaultValue = value;
      return this;
    }

    /**
     * Construct a new Argument object.
     * @return a new Argument object.
     */
    public Argument build() {
      return new uk.co.spudsoft.query.main.defn.Argument(type, optional, defaultValue);
    }
  }

  /**
   * Construct a new Builder object.
   * @return a new Builder object.
   */
  public static Argument.Builder builder() {
    return new Argument.Builder();
  }

  private Argument(final ArgumentType type, final boolean optional, final String defaultValue) {
    this.type = type;
    this.optional = optional;
    this.defaultValue = defaultValue;
  }

  
}
