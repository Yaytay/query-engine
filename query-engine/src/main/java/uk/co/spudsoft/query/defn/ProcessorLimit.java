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
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.procs.limit.ProcessorLimitInstance;

/**
 *
 * @author jtalbut
 */
@JsonDeserialize(builder = ProcessorLimit.Builder.class)
public class ProcessorLimit implements Processor {
  
  private final ProcessorType type;
  private final int limit;

  @Override
  public ProcessorInstance createInstance(Vertx vertx, SourceNameTracker sourceNameTracker, Context context) {
    return new ProcessorLimitInstance(vertx, sourceNameTracker, context, this);
  }

  @Override
  public void validate() {
    validateType(ProcessorType.LIMIT, type);
  }
  
  @Override
  public ProcessorType getType() {
    return type;
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
    validateType(ProcessorType.LIMIT, type);
    this.type = type;
    this.limit = limit;
  }
  
  
  
  
}
