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
import uk.co.spudsoft.query.main.exec.procs.limit.ProcessorLimitInstance;

/**
 *
 * @author jtalbut
 */
@JsonDeserialize(builder = ProcessorLimit.Builder.class)
public class ProcessorLimit extends Processor {
  
  private final int limit;

  @Override
  public ProcessorInstance<? extends Processor> createInstance(Vertx vertx, Context context) {
    return new ProcessorLimitInstance(vertx, context, this);
  }

  public int getLimit() {
    return limit;
  }
  
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private ProcessorType type = ProcessorType.LIMIT;
    private int limit;

    private Builder() {
    }
    
    public Builder type(final ProcessorType value) {
      this.type = value;
      return this;
    }

    public Builder limit(final int value) {
      this.limit = value;
      return this;
    }

    public ProcessorLimit build() {
      return new ProcessorLimit(type, limit);
    }
  }

  public static ProcessorLimit.Builder builder() {
    return new ProcessorLimit.Builder();
  }

  private ProcessorLimit(final ProcessorType type, final int limit) {
    super(type);
    this.limit = limit;
  }
  
  
  
  
}
