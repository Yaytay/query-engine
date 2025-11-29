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
 * Specify the data type for a column.
 * 
 * @author jtalbut
 */
@JsonDeserialize(builder = ColumnType.Builder.class)
@Schema(description = """
                      Specify the data type for a column.
                      """
)
public class ColumnType {
  
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
   * Builder class for ColumnType.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private String column;
    private DataType type;

    private Builder() {
    }

    /**
     * Set the {@link ColumnType#column} value in the builder.
     * @param value The value for the {@link ColumnType#column}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder column(final String value) {
      this.column = value;
      return this;
    }

    /**
     * Set the {@link ColumnType#type} value in the builder.
     * @param value The value for the {@link ColumnType#type}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder type(final DataType value) {
      this.type = value;
      return this;
    }

    /**
     * Create a new ColumnType object.
     * @return a new ColumnType object. 
     */
    public ColumnType build() {
      return new uk.co.spudsoft.query.defn.ColumnType(column, type);
    }
  }
  
  /**
   * Create a new ColumnType builder.
   * @return a new ColumnType builder. 
   */
  public static ColumnType.Builder builder() {
    return new ColumnType.Builder();
  }

  private ColumnType(final String column, final DataType type) {
    this.column = column;
    this.type = type;
  }
}
