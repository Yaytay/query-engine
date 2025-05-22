/*
 * Copyright (C) 2025 jtalbut
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

/**
 * Override of the data type for a specific column.
 * 
 * This facility is rarely required, but can be useful when a database does not provide adequate information for Query Engine to correctly identify the type of a field.
 * 
 * This is known to be useful for boolean fields with MySQL.
 * 
 * Setting a column to use a type that the result does not fit is going to cause problems (loss of data or errors) - so be sure you do this with care.
 * 
 * @author jtalbut
 */
@JsonDeserialize(builder = ColumnTypeOverride.Builder.class)
@Schema(description = """
                      Override of the data type for a specific column.
                      
                      This facility is rarely required, but can be useful when a database does not provide adequate information for Query Engine to correctly identify the type of a field.
                      
                      This is known to be useful for boolean fields with MySQL.
                      
                      Setting a column to use a type that the result does not fit is going to cause problems (loss of data or errors) - so be sure you do this with care.
                      """
)
public class ColumnTypeOverride {
  
  private final String column;
  private final DataType type;

  /**
   * Get the name of the column that is to have its data type set.
   * @return the name of the column that is to have its data type set.
   */
  @Schema(description = """
                        <P>The name of the column that is to have its data type set.</P>
                        """
          , maxLength = 100
  )
  public String getColumn() {
    return column;
  }

  /**
   * Get the desired type of the column.
   * @return the desired type of the column.
   */
  @Schema(description = """
                        <P>The desired type of the column.</P>
                        """
  )
  public DataType getType() {
    return type;
  }
  
  /**
   * Builder class for ColumnTypeOverride.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private String column;
    private DataType type;

    private Builder() {
    }

    /**
     * Set the {@link ColumnTypeOverride#column} value in the builder.
     * @param value The value for the {@link ColumnTypeOverride#column}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder column(final String value) {
      this.column = value;
      return this;
    }

    /**
     * Set the {@link ColumnTypeOverride#type} value in the builder.
     * @param value The value for the {@link ColumnTypeOverride#type}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder type(final DataType value) {
      this.type = value;
      return this;
    }

    /**
     * Create a new ColumnTypeOverride object.
     * @return a new ColumnTypeOverride object. 
     */
    public ColumnTypeOverride build() {
      return new uk.co.spudsoft.query.defn.ColumnTypeOverride(column, type);
    }
  }
  
  /**
   * Create a new ColumnTypeOverride builder.
   * @return a new ColumnTypeOverride builder. 
   */
  public static ColumnTypeOverride.Builder builder() {
    return new ColumnTypeOverride.Builder();
  }

  private ColumnTypeOverride(final String column, final DataType type) {
    this.column = column;
    this.type = type;
  }
}
