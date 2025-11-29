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
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Vertx;
import java.util.List;
import uk.co.spudsoft.query.exec.Auditor;
import uk.co.spudsoft.query.exec.SharedMap;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.sources.SourceStaticInstance;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;

/**
 *
 * Source producing a fixed set of data without any need to communicate with a database.
 * <P>
 * The data stream will have two fields:
 * <UL>
 * <LI>value
 * A monotonically increasing integer.
 * <LI>name
 * The name of the source.
 * </UL>
 * @author jtalbut
 */
@JsonDeserialize(builder = SourceStatic.Builder.class)
@Schema(description = """
                      Source producing a fixed set of data without any need to communicate with a database.
                      <P>
                      The data stream will contain the defined values only.
                      """)
public class SourceStatic implements Source {

  private final SourceType type;
  private final String name;
  private final ImmutableList<ColumnType> types;
  private final ImmutableList<List<Object>> rows;

  @Override
  public void validate(PipelineContext pipelineContext) {    
    validateType(SourceType.STATIC, type);
    if (types == null || types.isEmpty()) {
      throw new IllegalArgumentException(type + " Source has no fields defined");
    }
    for (int typeIdx = 0; typeIdx < types.size(); ++typeIdx) {
      ColumnType columnType = types.get(typeIdx);
      if (Strings.isNullOrEmpty(columnType.getColumn())) {
        throw new IllegalArgumentException("Column " + typeIdx + " has no name");
      }
      if (columnType.getType() == null) {
        throw new IllegalArgumentException("Column " + typeIdx + "(" + columnType.getColumn()+ ") has no type");
      }
    }
    if (rows == null || rows.isEmpty()) {
      throw new IllegalArgumentException(type + " Source has no rows to output");
    }
    for (int rowIdx = 0; rowIdx < rows.size(); ++rowIdx) {
      List<Object> row = rows.get(rowIdx);
      if (row.size() != types.size()) {
        throw new IllegalArgumentException("Row " + rowIdx + " has " + rows.size() + " columns, but the source is defined to have " + types.size());
      }
      for (int typeIdx = 0; typeIdx < types.size(); ++typeIdx) {
        ColumnType columnType = types.get(typeIdx);
        try {
          columnType.getType().cast(pipelineContext,  row.get(typeIdx));
        } catch (Throwable ex) {
          throw new IllegalArgumentException("Row " + rowIdx + " column " + typeIdx + "(" + columnType.getColumn() + ") value (" + row.get(typeIdx) + ") cannot be converted to " + columnType.getType());
        }
      }
    }
  }
  
  @Override
  public SourceType getType() {
    return type;
  }

  @Override
  public String getName() {
    return name;
  }  

  /**
   * Get the column definitions for the dataset.
   * @return the column definitions for the dataset.
   */
  @ArraySchema(
          arraySchema = @Schema(
                  type = "array"
                  , description = """
                                <P>The column definitions for the dataset.</P>
                                """
          )
          , minItems = 1
          , uniqueItems = true
  )  
  public List<ColumnType> getTypes() {
    return types;
  }

  /**
   * Get the rows of the dataset.
   * 
   * Each row must have the same number of columns as the types definition
   * , and each value must be convertible to the matching DataType.
   * 
   * @return the rows of the dataset.
  */
  @ArraySchema(
          arraySchema = @Schema(
                  type = "array"
                  , description = """
                                <P>The rows of the dataset.</P>
                                <P>
                                Each row must have the same number of columns as the types definition
                                , and each value must be convertible to the matching DataType.
                                </P>
                                """
          )
          , minItems = 1
          , uniqueItems = false
  )  
  public List<List<Object>> getRows() {
    return rows;
  }

  @Override
  public SourceStaticInstance createInstance(Vertx vertx, MeterRegistry meterRegistry, Auditor auditor, PipelineContext pipelineContext, SharedMap sharedMap) {
    return new SourceStaticInstance(vertx, meterRegistry, auditor, pipelineContext, this);
  }
  
  /**
   * Builder class for SourceStatic.
   */
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Builder class should result in all instances being immutable when object is built")
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private SourceType type = SourceType.STATIC;
    private String name;
    private List<ColumnType> types;
    private List<List<Object>> rows;

    private Builder() {
    }

    /**
     * Construct a new {@link SourceStatic} instance.
     * @return a newly created {@link SourceStatic} instance.
     */
    public SourceStatic build() {
      return new SourceStatic(type, name, types, rows);
    }

    /**
     * Set the {@link SourceStatic#type} value on the builder.
     * @param value the type value.
     * @return this, so that the builder may be used in a fluent manner.
     */
    public Builder type(final SourceType value) {
      this.type = value;
      return this;
    }

    /**
     * Set the {@link SourceStatic#name} value on the builder.
     * @param value the name value.
     * @return this, so that the builder may be used in a fluent manner.
     */
    public Builder name(final String value) {
      this.name = value;
      return this;
    }

    /**
     * Set the {@link SourceStatic#types} value on the builder.
     * @param value the types value.
     * @return this, so that the builder may be used in a fluent manner.
     */
    public Builder types(final List<ColumnType> value) {
      this.types = value;
      return this;
    }

    /**
     * Set the {@link SourceStatic#rows} value on the builder.
     * @param value the rows value.
     * @return this, so that the builder may be used in a fluent manner.
     */
    public Builder rows(final List<List<Object>> value) {
      this.rows = value;
      return this;
    }
  }

  /**
   * Construct a new instance of the SourceStatic.Builder class.
   * @return a new instance of the SourceStatic.Builder class.
   */
  public static Builder builder() {
    return new Builder();
  }

  private SourceStatic(final SourceType type, final String name, final List<ColumnType> types, final List<List<Object>> rows) {
    validateType(SourceType.STATIC, type);
    this.type = type;
    this.name = name;
    
    this.types = ImmutableCollectionTools.copy(types);
    this.rows = ImmutableCollectionTools.copy(rows);
  }
}
