/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import uk.co.spudsoft.query.main.exec.ProcessorInstance;
import uk.co.spudsoft.query.main.exec.procs.query.ProcessorGroupConcatInstance;

/**
 *
 * @author jtalbut
 */
@JsonDeserialize(builder = ProcessorGroupConcat.Builder.class)
public class ProcessorGroupConcat implements Processor {

  private final ProcessorType type;
  private final SourcePipeline input;
  private final boolean innerJoin;
  private final String parentIdColumn;
  private final String childIdColumn;
  private final String childValueColumn;
  private final String parentValueColumn;
  private final String delimiter;
  
  @Override
  public ProcessorInstance createInstance(Vertx vertx, Context context) {
    return new ProcessorGroupConcatInstance(vertx, context, this);
  }

  @Override
  public ProcessorType getType() {
    return type;
  }
  
  public SourcePipeline getInput() {
    return input;
  }

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
  public String getParentIdColumn() {
    return parentIdColumn;
  }

  /**
   * Get the parent ID column.
   * 
   * This is the name of the field in the child stream that is to be used to match against parent rows.
   * The child stream must be sorted by this field.
   * 
   * @return the parent ID column.
   */
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
  public String getParentValueColumn() {
    return parentValueColumn;
  }
  
  public String getDelimiter() {
    return delimiter;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private ProcessorType type = ProcessorType.GROUP_CONCAT;
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
      return new ProcessorGroupConcat(type, input, innerJoin, parentIdColumn, childIdColumn, childValueColumn, parentValueColumn, delimiter);
    }

    public Builder type(final ProcessorType value) {
      this.type = value;
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
          , SourcePipeline input
          , boolean innerJoin
          , String parentIdColumn
          , String childIdColumn
          , String childValueColumn
          , String parentValueColumn
          , String delimiter
  ) {
    this.type = type;
    this.input = input;
    this.innerJoin = innerJoin;
    this.parentIdColumn = parentIdColumn;
    this.childIdColumn = childIdColumn;
    this.childValueColumn = childValueColumn;
    this.parentValueColumn = parentValueColumn;
    this.delimiter = delimiter;
  }
    
  
  
  
}
