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
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Vertx;
import uk.co.spudsoft.query.exec.context.PipelineContext;
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
  private final String name;
  private final int offset;

  @Override
  public ProcessorOffsetInstance createInstance(Vertx vertx, PipelineContext pipelineContext, MeterRegistry meterRegistry, String name) {
    return new ProcessorOffsetInstance(vertx, meterRegistry, pipelineContext, this, name);
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

  @Override
  public String getName() {
    return name;
  }

  /**
   * The number of rows that will be skipped by this processor.
   * @return the number of rows that will be skipped by this processor.
   */
  @Schema(description = """
                        The number of rows that will be skipped by this processor.
                        """
  )
  public int getOffset() {
    return offset;
  }
  
  /**
   * Builder class for ProcessorOffset.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private ProcessorType type = ProcessorType.OFFSET;
    private Condition condition;
    private String name;
    private int offset;

    private Builder() {
    }
    
    /**
     * Set the {@link ProcessorOffset#type} value in the builder.
     * @param value The value for the {@link ProcessorOffset#type}, must be {@link ProcessorType#DYNAMIC_FIELD}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder type(final ProcessorType value) {
      this.type = value;
      return this;
    }
    
    /**
     * Set the {@link ProcessorOffset#condition} value in the builder.
     * @param value The value for the {@link ProcessorOffset#condition}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder condition(final Condition value) {
      this.condition = value;
      return this;
    }

    /**
     * Set the {@link ProcessorOffset#name} value in the builder.
     * @param value The value for the {@link ProcessorOffset#name}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder name(final String value) {
      this.name = value;
      return this;
    }

    /**
     * Set the {@link ProcessorOffset#offset} value in the builder.
     * @param value The value for the {@link ProcessorOffset#offset}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder offset(final int value) {
      this.offset = value;
      return this;
    }

    /**
     * Construct a new instance of the ProcessorOffset class.
     * @return a new instance of the ProcessorOffset class.
     */
    public ProcessorOffset build() {
      ProcessorOffset result = new ProcessorOffset(type, condition, name, offset);
      result.validateType(ProcessorType.OFFSET, type);
      return result;
    }
  }

  /**
   * Construct a new instance of the ProcessorOffset.Builder class.
   * @return a new instance of the ProcessorOffset.Builder class.
   */
  public static ProcessorOffset.Builder builder() {
    return new ProcessorOffset.Builder();
  }

  private ProcessorOffset(final ProcessorType type, final Condition condition, final String name, final int offset) {
    this.type = type;
    this.condition = condition;
    this.name = name;
    this.offset = offset;
  }
  
  
  
  
}
