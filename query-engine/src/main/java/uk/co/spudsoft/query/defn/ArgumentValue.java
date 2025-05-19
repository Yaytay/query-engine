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
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * An ArgumentValue represents a possible value for an Argument to a Pipeline.
 * ArgumentValues are not validated by the Query Engine at all, they exist solely to make life nicer for UIs.
 * The ID in an ArgumentValue is the value that is to be passed in, the Display field is purely for a UI to display.
 * It is expected that a UI will display the ID if the Display field is null or blank.
 * 
 * @author jtalbut
 */
@JsonDeserialize(builder = ArgumentValue.Builder.class)
@Schema(description = """
                      <P>An ArgumentValue represents a possible value for an Argument to a Pipeline.</P>
                      <P>
                      ArgumentValues are not validated by the Query Engine at all, they exist solely to make life nicer for UIs.
                      The &quot;value&quot; in an ArgumentValue is the value that is to be passed in, the &quot;label&quot; field is purely for a UI to display.
                      It is expected that a UI will display the &quot;value&quot; if the &quot;label&quot; field is null or blank.
                      """)
public class ArgumentValue {
  
  private final String value;
  private final String label;

  /**
   * Get the value that is the actual potential argument that the Pipeline expects to receive.
   * The value is considered required, the label field is not.
   * @return the ID that is the actual potential value that the Pipeline expects to receive.
   */
  @Schema(description = """
                        <P>
                        The value that is the actual potential argument that the Pipeline expects to receive.
                        </P>
                        <P>
                        The value is considered required, the label field is not.
                        </P>
                        """
          , minLength = 1
          , maxLength = 200
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public String getValue() {
    return value;
  }

  /**
   * Get the label that should be shown to the user for the given value.
   * This field is nullable, in which case the ID should be displayed to the user.
   * @return the Display value that should be shown to the user for the given ID.
   */
  @Schema(description = """
                        <P>
                        The label that should be shown to the user for the given value.
                        </P>
                        <P>
                        This label  is nullable, in which case the value should be displayed to the user.
                        </P>
                        """
          , maxLength = 100
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  public String getLabel() {
    return label;
  }

  /**
   * Builder class for {@link ArgumentValue}.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private String value;
    private String label;

    private Builder() {
    }
    
    /**
     * Public constructor to enable the specification of ArgumentValues as plain strings.
     * @param id The value to use as the ID of the ArgumentValue.
     * The UI is expected to use this as the display value too.
     */
    public Builder(String id) {
      this.value = id;
    }

    /**
     * Set the ID that is the actual potential value that the Pipeline expects to receive.
     * @param value the ID that is the actual potential value that the Pipeline expects to receive.
     * @return this, so that the builder may be used fluently.
     */
    public Builder value(final String value) {
      this.value = value;
      return this;
    }

    /**
     * Set the Display value that should be shown to the user for the given ID.
     * @param value the Display value that should be shown to the user for the given ID.
     * @return this, so that the builder may be used fluently.
     */
    public Builder label(final String value) {
      this.label = value;
      return this;
    }

    /**
     * Construct a new ArgumentValue object.
     * @return a new ArgumentValue object.
     */
    public ArgumentValue build() {
      return new uk.co.spudsoft.query.defn.ArgumentValue(value, label);
    }
  }

  /**
   * Construct a new Builder object.
   * @return a new Builder object.
   */
  public static ArgumentValue.Builder builder() {
    return new ArgumentValue.Builder();
  }

  private ArgumentValue(final String value, final String label) {
    this.value = value;
    this.label = label;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 17 * hash + Objects.hashCode(this.value);
    hash = 17 * hash + Objects.hashCode(this.label);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final ArgumentValue other = (ArgumentValue) obj;
    if (!Objects.equals(this.value, other.value)) {
      return false;
    }
    return Objects.equals(this.label, other.label);
  }
}
