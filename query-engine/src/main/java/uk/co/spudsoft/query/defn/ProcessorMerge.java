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
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Vertx;
import java.util.List;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.procs.subquery.ProcessorMergeInstance;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;

/**
 * Processor that adds all fields from a child query into the primary stream.
 * 
 * If there are multiple rows in the child stream that match the parent row all except the first will be ignored.
 * 
 * @author jtalbut
 */
@JsonDeserialize(builder = ProcessorMerge.Builder.class)
@Schema(description = """
                      Processor that adds all fields from a child query into the primary stream.
                      <P>
                      If there are multiple rows in the child stream that match the parent row all except the first will be ignored.
                      """)
public class ProcessorMerge implements Processor {

  private final ProcessorType type;
  private final Condition condition;
  private final String name;
  private final SourcePipeline input;
  private final boolean innerJoin;
  private final ImmutableList<String> parentIdColumns;
  private final ImmutableList<String> childIdColumns;
  private final String delimiter;
  
  @Override
  public ProcessorMergeInstance createInstance(Vertx vertx, PipelineContext pipelineContext, MeterRegistry meterRegistry, String name) {
    return new ProcessorMergeInstance(vertx, pipelineContext, meterRegistry, this, name);
  }

  @Override
  public void validate() {
    validateType(ProcessorType.MERGE, type);
    if (parentIdColumns == null || parentIdColumns.isEmpty()) {
      throw new IllegalArgumentException("ID column(s) not specified for parent stream");      
    }
    if (childIdColumns == null || childIdColumns.isEmpty()) {
      throw new IllegalArgumentException("ID column(s) not specified for child stream");      
    }
    if (parentIdColumns.size() != childIdColumns.size()) {
      throw new IllegalArgumentException("ID column(s) specified for parent stream does not have the same number of fields as those specified for input stream");
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
                        This data feed should result in two columns childIdColumn and childValueColumn (any other columns will be ignored).
                        The data should be sorted by childIdColumn (and the parent feed should be sorted by parentIdColumn).
                        <P>
                        The values in childValueColumn for each value of childIdColumn will be concatenated together using delimiter as a delimiter and the result will be set as parentValueColumn in the parent feed.
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
   * Builder class for ProcessorMerge.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Builder class should result in all instances being immutable when object is built")
  public static class Builder {

    private ProcessorType type = ProcessorType.MERGE;
    private Condition condition;
    private String name;
    private SourcePipeline input;
    private boolean innerJoin;
    private List<String> parentIdColumns;
    private List<String> childIdColumns;
    private String delimiter;

    private Builder() {
    }

    /**
     * Set the {@link ProcessorMerge#type} value in the builder.
     * @param value The value for the {@link ProcessorMerge#type}, must be {@link ProcessorType#DYNAMIC_FIELD}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder type(final ProcessorType value) {
      this.type = value;
      return this;
    }

    /**
     * Set the {@link ProcessorMerge#condition} value in the builder.
     * @param value The value for the {@link ProcessorMerge#condition}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder condition(final Condition value) {
      this.condition = value;
      return this;
    }
    
    /**
     * Set the {@link ProcessorMerge#name} value in the builder.
     * @param value The value for the {@link ProcessorMerge#name}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder name(final String value) {
      this.name = value;
      return this;
    }

    /**
     * Set the {@link ProcessorMerge#input} value in the builder.
     * @param value The value for the {@link ProcessorMerge#input}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder input(final SourcePipeline value) {
      this.input = value;
      return this;
    }

    /**
     * Set the {@link ProcessorMerge#innerJoin} value in the builder.
     * @param value The value for the {@link ProcessorMerge#innerJoin}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder innerJoin(final boolean value) {
      this.innerJoin = value;
      return this;
    }

    /**
     * Set the {@link ProcessorMerge#parentIdColumns} value in the builder.
     * @param value The value for the {@link ProcessorMerge#parentIdColumns}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder parentIdColumns(final List<String> value) {
      this.parentIdColumns = value;
      return this;
    }

    /**
     * Set the {@link ProcessorMerge#childIdColumns} value in the builder.
     * @param value The value for the {@link ProcessorMerge#childIdColumns}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder childIdColumns(final List<String> value) {
      this.childIdColumns = value;
      return this;
    }

    /**
     * Set the {@link ProcessorMerge#delimiter} value in the builder.
     * @param value The value for the {@link ProcessorMerge#delimiter}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder delimiter(final String value) {
      this.delimiter = value;
      return this;
    }
    
    /**
     * Construct a new instance of the ProcessorMerge class.
     * @return a new instance of the ProcessorMerge class.
     */
    public ProcessorMerge build() {
      ProcessorMerge result = new ProcessorMerge(type, condition, name, input, innerJoin, parentIdColumns, childIdColumns, delimiter);
      result.validateType(ProcessorType.MERGE, type);
      return result;
    }
    
  }

  /**
   * Construct a new instance of the ProcessorMerge.Builder class.
   * @return a new instance of the ProcessorMerge.Builder class.
   */
  public static ProcessorMerge.Builder builder() {
    return new ProcessorMerge.Builder();
  }

  private ProcessorMerge(ProcessorType type
          , final Condition condition
          , final String name
          , SourcePipeline input
          , boolean innerJoin
          , List<String> parentIdColumns
          , List<String> childIdColumns
          , String delimiter
  ) {
    this.type = type;
    this.condition = condition;
    this.name = name;
    this.input = input;
    this.innerJoin = innerJoin;
    this.parentIdColumns = ImmutableCollectionTools.copy(parentIdColumns);
    this.childIdColumns = ImmutableCollectionTools.copy(childIdColumns);
    this.delimiter = delimiter == null ? ", " : delimiter;
  }
    
  
  
  
}
