/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 *
 * @author jtalbut
 */
@JsonDeserialize(builder = Argument.Builder.class)
public class Argument {
  
  private final ArgumentType type;
  private final boolean optional;
  private final String defaultValue;

  public ArgumentType getType() {
    return type;
  }

  public boolean isOptional() {
    return optional;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private ArgumentType type;
    private boolean optional;
    private String defaultValue;

    private Builder() {
    }

    public Builder type(final ArgumentType value) {
      this.type = value;
      return this;
    }

    public Builder optional(final boolean value) {
      this.optional = value;
      return this;
    }

    public Builder defaultValue(final String value) {
      this.defaultValue = value;
      return this;
    }

    public Argument build() {
      return new uk.co.spudsoft.query.main.defn.Argument(type, optional, defaultValue);
    }
  }

  public static Argument.Builder builder() {
    return new Argument.Builder();
  }

  private Argument(final ArgumentType type, final boolean optional, final String defaultValue) {
    this.type = type;
    this.optional = optional;
    this.defaultValue = defaultValue;
  }

  
}
