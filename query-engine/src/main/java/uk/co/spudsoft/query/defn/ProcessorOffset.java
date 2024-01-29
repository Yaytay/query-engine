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
import uk.co.spudsoft.query.exec.procs.filters.ProcessorOffsetInstance;

/**
 * Processor that curtails the output after the configured number of rows.
 * @author jtalbut
 */
@JsonDeserialize(builder = ProcessorOffset.Builder.class)
@Schema(description = """
                      Processor that curtails the output after the configured number of rows.
                      """
)
public class ProcessorOffset implements Processor {
  
  private final ProcessorType type;
  private final Condition condition;
  private final int offset;

  @Override
  public ProcessorInstance createInstance(Vertx vertx, SourceNameTracker sourceNameTracker, Context context) {
    return new ProcessorOffsetInstance(vertx, sourceNameTracker, context, this);
  }

  @Override
  public void validate() {
    validateType(ProcessorType.OFFSET, type);
    if (offset < 0) {
      throw new IllegalArgumentException("Negative offset provided");
    } else if (offset == 0) {
      throw new IllegalArgumentException("Zero offset provided");
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

  @Schema(description = """
                        The offset on the number of rows that will be output by this processor.
                        """
  )
  public int getOffset() {
    return offset;
  }
  
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private ProcessorType type = ProcessorType.OFFSET;
    private Condition condition;
    private int offset;

    private Builder() {
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

    public Builder offset(final int value) {
      this.offset = value;
      return this;
    }

    public ProcessorOffset build() {
      return new ProcessorOffset(type, condition, offset);
    }
  }

  public static ProcessorOffset.Builder builder() {
    return new ProcessorOffset.Builder();
  }

  private ProcessorOffset(final ProcessorType type, final Condition condition, final int offset) {
    validateType(ProcessorType.OFFSET, type);
    this.type = type;
    this.condition = condition;
    this.offset = offset;
  }
  
  
  
  
}
