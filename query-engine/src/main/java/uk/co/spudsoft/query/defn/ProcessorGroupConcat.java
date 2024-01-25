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
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.procs.query.ProcessorGroupConcatInstance;

/**
 * Processor that combines multiple values from a child query into a single concatenated string value.
 * @author jtalbut
 */
@JsonDeserialize(builder = ProcessorGroupConcat.Builder.class)
@Schema(description = """
                      Processor that combines multiple values from a child query into a single concatenated string value.
                      """)
public class ProcessorGroupConcat implements Processor {

  private final ProcessorType type;
  private final Condition condition;
  private final SourcePipeline input;
  private final boolean innerJoin;
  private final String parentIdColumn;
  private final String childIdColumn;
  private final String childValueColumn;
  private final String parentValueColumn;
  private final String delimiter;
  
  @Override
  public ProcessorInstance createInstance(Vertx vertx, SourceNameTracker sourceNameTracker, Context context) {
    return new ProcessorGroupConcatInstance(vertx, sourceNameTracker, context, this);
  }

  @Override
  public void validate() {
    validateType(ProcessorType.GROUP_CONCAT, type);
  }
  
  @Override
  public ProcessorType getType() {
    return type;
  }
  
  @Override
  public Condition getCondition() {
    return condition;
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
   * Get the parent ID column.
   * 
   * This is the name of the field in the main stream that is to be used to match against child rows.
   * The main stream must be sorted by this field.
   * 
   * @return the parent ID column.
   */
  @Schema(description = """
                        The parent ID column.
                        <P>
                        This is the name of the field in the main stream that is to be used to match against child rows.
                        The main stream must be sorted by this field.
                        """
          , maxLength = 100
  )
  public String getParentIdColumn() {
    return parentIdColumn;
  }

  /**
   * Get the child ID column.
   * 
   * This is the name of the field in the child stream that is to be used to match against parent rows.
   * The child stream must be sorted by this field.
   * 
   * @return the parent ID column.
   */
  @Schema(description = """
                        The child ID column.
                        <P>
                        This is the name of the field in the child stream that is to be used to match against parent rows.
                        The child stream must be sorted by this field.
                        """
          , maxLength = 100
  )
  public String getChildIdColumn() {
    return childIdColumn;
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
                        """
          , maxLength = 100
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
                        """
          , maxLength = 100
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

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private ProcessorType type = ProcessorType.GROUP_CONCAT;
    private Condition condition;
    private SourcePipeline input;
    private boolean innerJoin;
    private String parentIdColumn;
    private String childIdColumn;
    private String childValueColumn;
    private String parentValueColumn;
    private String delimiter;

    private Builder() {
    }
    public ProcessorGroupConcat build() {
      return new ProcessorGroupConcat(type, condition, input, innerJoin, parentIdColumn, childIdColumn, childValueColumn, parentValueColumn, delimiter);
    }

    public Builder type(final ProcessorType value) {
      this.type = value;
      return this;
    }

    /**
     * Set the condition on the Pipeline in the builder.
     * @param value the condition on the Endpoint.
     * @return this, so that the builder may be used fluently.
     */
    public Builder condition(final Condition value) {
      this.condition = value;
      return this;
    }
    
    public Builder input(final SourcePipeline value) {
      this.input = value;
      return this;
    }

    public Builder innerJoin(final boolean value) {
      this.innerJoin = value;
      return this;
    }

    public Builder parentIdColumn(final String value) {
      this.parentIdColumn = value;
      return this;
    }

    public Builder childIdColumn(final String value) {
      this.childIdColumn = value;
      return this;
    }

    public Builder childValueColumn(final String value) {
      this.childValueColumn = value;
      return this;
    }

    public Builder parentValueColumn(final String value) {
      this.parentValueColumn = value;
      return this;
    }

    public Builder delimiter(final String value) {
      this.delimiter = value;
      return this;
    }
  }

  public static ProcessorGroupConcat.Builder builder() {
    return new ProcessorGroupConcat.Builder();
  }

  private ProcessorGroupConcat(ProcessorType type
          , final Condition condition
          , SourcePipeline input
          , boolean innerJoin
          , String parentIdColumn
          , String childIdColumn
          , String childValueColumn
          , String parentValueColumn
          , String delimiter
  ) {
    validateType(ProcessorType.GROUP_CONCAT, type);
    this.type = type;
    this.condition = condition;
    this.input = input;
    this.innerJoin = innerJoin;
    this.parentIdColumn = parentIdColumn;
    this.childIdColumn = childIdColumn;
    this.childValueColumn = childValueColumn;
    this.parentValueColumn = parentValueColumn;
    this.delimiter = delimiter == null ? ", " : delimiter;
  }
    
  
  
  
}
