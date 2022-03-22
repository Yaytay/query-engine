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

  private final String delimiter;
  private final String parentId;
  private final boolean innerJoin;
  private final String idColumn;
  private final String columnName;
  private final String childColumnName;
  private final Source source;
  
  @Override
  public ProcessorInstance<? extends Processor> createInstance(Vertx vertx, Context context) {
    return new ProcessorGroupConcatInstance(vertx, context, this);
  }

  public String getDelimiter() {
    return delimiter;
  }

  public String getParentId() {
    return parentId;
  }

  public boolean isInnerJoin() {
    return innerJoin;
  }

  public String getIdColumn() {
    return idColumn;
  }

  public Source getSource() {
    return source;
  }
  
  public String getColumnName() {
    return columnName;
  }

  public String getChildColumnName() {
    return childColumnName;
  }

  

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private ProcessorType type = ProcessorType.GROUP_CONCAT;
    private String delimiter;
    private String columnName;
    private String parentId;
    private String childColumnName;
    private boolean innerJoin;
    private String idColumn;
    private Source source;

    private Builder() {
    }

    public Builder type(final ProcessorType value) {
      this.type = value;
      return this;
    }

    public Builder delimiter(final String value) {
      this.delimiter = value;
      return this;
    }

    public Builder parentId(final String value) {
      this.parentId = value;
      return this;
    }

    public Builder innerJoin(final boolean value) {
      this.innerJoin = value;
      return this;
    }

    public Builder idColumn(final String value) {
      this.idColumn = value;
      return this;
    }

    public Builder source(final Source value) {
      this.source = value;
      return this;
    }

    public Builder columnName(final String value) {
      this.columnName = value;
      return this;
    }

    public Builder childColumnName(final String value) {
      this.childColumnName = value;
      return this;
    }

    public ProcessorGroupConcat build() {
      return new uk.co.spudsoft.query.main.defn.ProcessorGroupConcat(type, delimiter, parentId, innerJoin, idColumn, source, columnName, childColumnName);
    }
  }

  public static ProcessorGroupConcat.Builder builder() {
    return new ProcessorGroupConcat.Builder();
  }

  private ProcessorGroupConcat(final ProcessorType type, final String delimiter, final String parentId, final boolean innerJoin, final String idColumn
          , final Source source
          , final String columnName
          , final String childColumnName
  ) {
    super(type);
    this.delimiter = delimiter;
    this.parentId = parentId;
    this.innerJoin = innerJoin;
    this.idColumn = idColumn;
    this.source = source;
    this.columnName = columnName;
    this.childColumnName = childColumnName;
  }
    
  
  
}
