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
import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Vertx;
import java.util.List;
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.exec.procs.subquery.ProcessorGroupConcatInstance;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;

/**
 * Processor that combines multiple values from a child query into a single concatenated string value.
 * @author jtalbut
 */
@JsonDeserialize(builder = ProcessorGroupConcat.Builder.class)
@Schema(description = """
                      Processor that combines multiple values from a child query into a single concatenated string value.
                      <P>
                      There are three ways that the group concat can be performed:
                      <OL>
                      <LI>Specify the childValueColumn and the parentValueColumn.
                      A single field will be added to the parent stream, the value will be taken from the childValueColumns and the new field will be named parentValueColumn.
                      <LI>Specify only childValueColumn, do not specify parentValueColumn.
                      A single field will be added to the parent stream, the value will be taken from the childValueColumns and the new field will be named childValueColumn.
                      <LI>Do not specify childValueColumn.
                      All fields from the child stream will be (indepdently) concatenated and added to the parent stream (with the same names).
                      </OL>
                      """)
public class ProcessorGroupConcat implements Processor {

  private final ProcessorType type;
  private final Condition condition;
  private final String name;
  private final SourcePipeline input;
  private final boolean innerJoin;
  private final ImmutableList<String> parentIdColumns;
  private final ImmutableList<String> childIdColumns;
  private final String childValueColumn;
  private final String parentValueColumn;
  private final String delimiter;
  
  @Override
  public ProcessorGroupConcatInstance createInstance(Vertx vertx, RequestContext requestContext, MeterRegistry meterRegistry, String name) {
    return new ProcessorGroupConcatInstance(vertx, requestContext, meterRegistry, this, name);
  }

  @Override
  public void validate() {
    validateType(ProcessorType.GROUP_CONCAT, type);
    
    if (input == null) {
      throw new IllegalArgumentException("Input (input) pipeline not provided");
    }
    input.validate((Strings.isNullOrEmpty(name) ? "GroupConcat" : "GroupConcat " + name) + " fieldDefns");
    
    if (Strings.isNullOrEmpty(childValueColumn) && !Strings.isNullOrEmpty(parentValueColumn)) {
      throw new IllegalArgumentException("The parentValueColumn name is specified, but the childValueColumn is not");
    }
    if (parentIdColumns == null || parentIdColumns.isEmpty()) {
      throw new IllegalArgumentException("ID column(s) not specified for parent stream");      
    }
    if (childIdColumns == null || childIdColumns.isEmpty()) {
      throw new IllegalArgumentException("ID column(s) not specified for child stream");      
    }
    if (parentIdColumns.size() != childIdColumns.size()) {
      throw new IllegalArgumentException("ID column(s) specified for parent stream does not have the same number of fields as those specified for input stream");
    }
    if (condition != null) {
      condition.validate();
    }
    
  }
  
  @Override
  public ProcessorType getType() {
    return type;
  }
  
  @Override
  public Condition getCondition() {
    return condition;
  }
  
  @Override
  public String getName() {
    return name;
  }
  
  /**
   * Get the data feed.
   * 
   * This data feed should result in two columns childIdColumn and childValueColumn (any other columns will be ignored).
   * The data should be sorted by childIdColumn (and the parent feed should be sorted by parentIdColumn).
   * 
   * The values in childValueColumn for each value of childIdColumn will be concatenated together using delimiter as a delimiter and the result will be set as parentValueColumn in the parent feed.
   * 
   * @return the data feed.
   */
  @Schema(description = """
                        The data feed.
                        <P>
                        The data must be sorted by the childIdColumns (and the parent feed should be sorted by the parentIdColumns).
                        """
  )
  public SourcePipeline getInput() {
    return input;
  }

  /**
   * Get the inner join flag.
   * If set to true the parent row will only be output if the child feed has at least one matching row.
   * @return the inner join flag.
   */
  @Schema(description = """
                        The inner join flag.
                        <P>
                        If set to true the parent row will only be output if the child feed has at least one matching row.
                        """
  )
  public boolean isInnerJoin() {
    return innerJoin;
  }

  /**
   * Get the parent ID columns.
   * 
   * These are the names of the fields in the main stream that is to be used to match against child rows.
   * The main stream must be sorted by these fields.
   * 
   * @return the parent ID column.
   */
  @Schema(description = """
                        The parent ID columns.
                        <P>
                        These are the names of the fields in the main stream that is to be used to match against child rows.
                        The main stream must be sorted by these fields.
                        """
          , maxLength = 100
  )
  public List<String> getParentIdColumns() {
    return parentIdColumns;
  }

  /**
   * Get the child ID columns.
   * 
   * These are the names of the fields in the child stream that are to be used to match against parent rows.
   * The child stream must be sorted by these fields.
   * 
   * @return the parent ID column.
   */
  @Schema(description = """
                        The child ID columns.
                        <P>
                        These are the names of the fields in the child stream that are to be used to match against parent rows.
                        The child stream must be sorted by these fields.
                        """
          , maxLength = 100
  )
  public List<String> getChildIdColumns() {
    return childIdColumns;
  }

  /**
   * Get the child value column.
   * 
   * This is the name of the field in the child stream that contains the data to be extracted.
   * 
   * @return the child value column.
   */
  @Schema(description = """
                        The child value column.
                        <P>
                        This is the name of the field in the child stream that contains the data to be extracted.
                        <P>
                        If this is not provided all fields in the child stream that are not in the childIdColumns will be individually concatenated and brought over.
                        """
          , maxLength = 100
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  public String getChildValueColumn() {
    return childValueColumn;
  }

  /**
   * Get the parent value column.
   * 
   * This is the name of the field that will be created in the parent stream to contain the data from the child stream.
   * 
   * @return the parent value column.
   */
  @Schema(description = """
                        The parent value column.
                        <P>
                        This is the name of the field that will be created in the parent stream to contain the data from the child stream.
                        <P>
                        If this is not provided the parent stream fields will have the same name(s) as the child stream fields.
                        It is an error to provide this and not to provide childValueColumn.
                        """
          , maxLength = 100
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  public String getParentValueColumn() {
    return parentValueColumn;
  }
  
  /**
   * Get the delimiter to place between each value returned.
   * @return the delimiter to place between each value returned.
   */
  @Schema(description = """
                        The delimiter to place between each value returned.
                        """
          , maxLength = 10
  )
  public String getDelimiter() {
    return delimiter;
  }

  /**
   * Builder class for ProcessorGroupConcat.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Builder class should result in all instances being immutable when object is built")
  public static class Builder {

    private ProcessorType type = ProcessorType.GROUP_CONCAT;
    private Condition condition;
    private String name;
    private SourcePipeline input;
    private boolean innerJoin;
    private List<String> parentIdColumns;
    private List<String> childIdColumns;
    private String childValueColumn;
    private String parentValueColumn;
    private String delimiter;

    private Builder() {
    }

    /**
     * Set the {@link ProcessorGroupConcat#type} value in the builder.
     * @param value The value for the {@link ProcessorGroupConcat#type}, must be {@link ProcessorType#DYNAMIC_FIELD}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder type(final ProcessorType value) {
      this.type = value;
      return this;
    }

    /**
     * Set the {@link ProcessorGroupConcat#condition} value in the builder.
     * @param value The value for the {@link ProcessorGroupConcat#condition}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder condition(final Condition value) {
      this.condition = value;
      return this;
    }
    
    /**
     * Set the {@link ProcessorGroupConcat#name} value in the builder.
     * @param value The value for the {@link ProcessorGroupConcat#name}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder name(final String value) {
      this.name = value;
      return this;
    }

    /**
     * Set the {@link ProcessorGroupConcat#input} value in the builder.
     * @param value The value for the {@link ProcessorGroupConcat#input}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder input(final SourcePipeline value) {
      this.input = value;
      return this;
    }

    /**
     * Set the {@link ProcessorGroupConcat#innerJoin} value in the builder.
     * @param value The value for the {@link ProcessorGroupConcat#innerJoin}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder innerJoin(final boolean value) {
      this.innerJoin = value;
      return this;
    }

    /**
     * Set the {@link ProcessorGroupConcat#parentIdColumns} value in the builder.
     * @param value The value for the {@link ProcessorGroupConcat#parentIdColumns}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder parentIdColumns(final List<String> value) {
      this.parentIdColumns = value;
      return this;
    }

    /**
     * Set the {@link ProcessorGroupConcat#childIdColumns} value in the builder.
     * @param value The value for the {@link ProcessorGroupConcat#childIdColumns}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder childIdColumns(final List<String> value) {
      this.childIdColumns = value;
      return this;
    }

    /**
     * Set the {@link ProcessorGroupConcat#childValueColumn} value in the builder.
     * @param value The value for the {@link ProcessorGroupConcat#childValueColumn}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder childValueColumn(final String value) {
      this.childValueColumn = value;
      return this;
    }

    /**
     * Set the {@link ProcessorGroupConcat#parentValueColumn} value in the builder.
     * @param value The value for the {@link ProcessorGroupConcat#parentValueColumn}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder parentValueColumn(final String value) {
      this.parentValueColumn = value;
      return this;
    }

    /**
     * Set the {@link ProcessorGroupConcat#delimiter} value in the builder.
     * @param value The value for the {@link ProcessorGroupConcat#delimiter}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder delimiter(final String value) {
      this.delimiter = value;
      return this;
    }
    
    /**
     * Construct a new instance of the ProcessorGroupConcat class.
     * @return a new instance of the ProcessorGroupConcat class.
     */
    public ProcessorGroupConcat build() {
      ProcessorGroupConcat result = new ProcessorGroupConcat(type, condition, name, input, innerJoin, parentIdColumns, childIdColumns, childValueColumn, parentValueColumn, delimiter);
      result.validateType(ProcessorType.GROUP_CONCAT, type);
      return result;
    }
    
  }

  /**
   * Construct a new instance of the ProcessorGroupConcat.Builder class.
   * @return a new instance of the ProcessorGroupConcat.Builder class.
   */
  public static ProcessorGroupConcat.Builder builder() {
    return new ProcessorGroupConcat.Builder();
  }

  private ProcessorGroupConcat(ProcessorType type
          , final Condition condition
          , final String name
          , SourcePipeline input
          , boolean innerJoin
          , List<String> parentIdColumns
          , List<String> childIdColumns
          , String childValueColumn
          , String parentValueColumn
          , String delimiter
  ) {
    this.type = type;
    this.condition = condition;
    this.name = name;
    this.input = input;
    this.innerJoin = innerJoin;
    this.parentIdColumns = ImmutableCollectionTools.copy(parentIdColumns);
    this.childIdColumns = ImmutableCollectionTools.copy(childIdColumns);
    this.childValueColumn = childValueColumn;
    this.parentValueColumn = parentValueColumn;
    this.delimiter = delimiter == null ? ", " : delimiter;
  }
    
  
  
  
}
