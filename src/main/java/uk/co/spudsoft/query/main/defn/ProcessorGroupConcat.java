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
public class ProcessorGroupConcat extends Processor {

  private final Source source;
  private final boolean innerJoin;
  private final String parentIdColumn;
  private final String childIdColumn;
  private final String childValueColumn;
  private final String delimiter;
  
  @Override
  public ProcessorInstance<? extends Processor> createInstance(Vertx vertx, Context context) {
    return new ProcessorGroupConcatInstance(vertx, context, this);
  }

  public Source getSource() {
    return source;
  }

  public boolean isInnerJoin() {
    return innerJoin;
  }

  public String getParentIdColumn() {
    return parentIdColumn;
  }

  public String getChildIdColumn() {
    return childIdColumn;
  }

  public String getChildValueColumn() {
    return childValueColumn;
  }

  public String getDelimiter() {
    return delimiter;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private ProcessorType type = ProcessorType.GROUP_CONCAT;
    private Source source;
    private boolean innerJoin;
    private String parentIdColumn;
    private String childIdColumn;
    private String childValueColumn;
    private String delimiter;

    private Builder() {
    }

    public Builder type(final ProcessorType value) {
      this.type = value;
      return this;
    }

    public Builder source(final Source value) {
      this.source = value;
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

    public Builder delimiter(final String value) {
      this.delimiter = value;
      return this;
    }

    public ProcessorGroupConcat build() {
      return new ProcessorGroupConcat(type, source, innerJoin, parentIdColumn, childIdColumn, childValueColumn, delimiter);
    }
  }

  public static ProcessorGroupConcat.Builder builder() {
    return new ProcessorGroupConcat.Builder();
  }

  private ProcessorGroupConcat(ProcessorType type
          , Source source
          , boolean innerJoin
          , String parentIdColumn
          , String childIdColumn
          , String childValueColumn
          , String delimiter
  ) {
    super(type);
    this.source = source;
    this.innerJoin = innerJoin;
    this.parentIdColumn = parentIdColumn;
    this.childIdColumn = childIdColumn;
    this.childValueColumn = childValueColumn;
    this.delimiter = delimiter;
  }
    
  
  
  
}
