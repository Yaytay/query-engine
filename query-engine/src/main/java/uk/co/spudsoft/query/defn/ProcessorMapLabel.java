/*
 * Copyright (C) 2024 jtalbut
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

/**
 * Argument to the MapProcessor that renames or removes fields in the output.
 * @author jtalbut
 */
@JsonDeserialize(builder = ProcessorMapLabel.Builder.class)
@Schema(description = """
                      Argument to the MapProcessor that renames or removes fields in the output.
                      """
)
public class ProcessorMapLabel {
  
  private final String sourceLabel;
  private final String newLabel;

  /**
   * Validate this definition.
   * @throws IllegalArgumentException if the definition is not valid.
   */
  public void validate() throws IllegalArgumentException {
    if (Strings.isNullOrEmpty(sourceLabel)) {
      throw new IllegalArgumentException("No source field name provided for relabel");
    }
  }
  
  /**
   * The name of the field to be renamed.
   * <p>
   * This value is not optional.
   * @return the name of the field to be renamed.
   */
  @Schema(description = """
                        The name of the field to be renamed.
                        """
          , maxLength = 100
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public String getSourceLabel() {
    return sourceLabel;
  }
  
  /**
   * The new name of the field, may be blank to remove a field.
   * <p>
   * This value may be null or blank, both of which will remove the field from the stream.
   * @return the new name of the field, may be blank to remove a field.
   */
  @Schema(description = """
                        The new name of the field, may be null or blank, both of which will remove the field from the stream.
                        """
          , maxLength = 100
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public String getNewLabel() {
    return newLabel;
  }

  /**
   * Builder class for ProcessorMapLabel.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private String sourceLabel;
    private String newLabel;

    private Builder() {
    }

    /**
     * Set the {@link ProcessorMapLabel#sourceLabel} value in the builder.
     * @param value The value for the {@link ProcessorMapLabel#sourceLabel}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder sourceLabel(final String value) {
      this.sourceLabel = value;
      return this;
    }

    /**
     * Set the {@link ProcessorMapLabel#newLabel} value in the builder.
     * @param value The value for the {@link ProcessorMapLabel#newLabel}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder newLabel(final String value) {
      this.newLabel = value;
      return this;
    }

    /**
     * Construct a new instance of the ProcessorMapLabel class.
     * @return a new instance of the ProcessorMapLabel class.
     */
    public ProcessorMapLabel build() {
      return new uk.co.spudsoft.query.defn.ProcessorMapLabel(sourceLabel, newLabel);
    }
  }

  /**
   * Construct a new instance of the ProcessorMapLabel.Builder class.
   * @return a new instance of the ProcessorMapLabel.Builder class.
   */
  public static ProcessorMapLabel.Builder builder() {
    return new ProcessorMapLabel.Builder();
  }

  private ProcessorMapLabel(final String sourceLabel, final String newLabel) {
    this.sourceLabel = sourceLabel;
    this.newLabel = newLabel;
  }
  
  
  
}
