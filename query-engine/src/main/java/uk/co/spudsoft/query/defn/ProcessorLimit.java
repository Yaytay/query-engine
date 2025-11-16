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
import uk.co.spudsoft.query.exec.Auditor;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.procs.filters.ProcessorLimitInstance;

/**
 * Processor that curtails the output after the configured number of rows.
 * @author jtalbut
 */
@JsonDeserialize(builder = ProcessorLimit.Builder.class)
@Schema(description = """
                      Processor that curtails the output after the configured number of rows.
                      """
)
public class ProcessorLimit implements Processor {
  
  private final ProcessorType type;
  private final Condition condition;
  private final String name;
  private final int limit;

  @Override
  public ProcessorLimitInstance createInstance(Vertx vertx, MeterRegistry meterRegistry, Auditor auditor, PipelineContext pipelineContext, String name) {
    return new ProcessorLimitInstance(vertx, meterRegistry, auditor, pipelineContext, this, name);
  }

  @Override
  public void validate() {
    validateType(ProcessorType.LIMIT, type);
    if (limit < 0) {
      throw new IllegalArgumentException("Negative limit provided");
    } else if (limit == 0) {
      throw new IllegalArgumentException("Zero limit provided");
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
   * The limit on the number of rows that will be output by this processor.
   * @return the limit on the number of rows that will be output by this processor.
   */
  @Schema(description = """
                        The limit on the number of rows that will be output by this processor.
                        """
  )
  public int getLimit() {
    return limit;
  }
  
  /**
   * Builder class for ProcessorLimit.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private ProcessorType type = ProcessorType.LIMIT;
    private Condition condition;
    private String name;
    private int limit;

    private Builder() {
    }
    
    /**
     * Set the {@link ProcessorLimit#type} value in the builder.
     * @param value The value for the {@link ProcessorLimit#type}, must be {@link ProcessorType#DYNAMIC_FIELD}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder type(final ProcessorType value) {
      this.type = value;
      return this;
    }
    
    /**
     * Set the {@link ProcessorLimit#condition} value in the builder.
     * @param value The value for the {@link ProcessorLimit#condition}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder condition(final Condition value) {
      this.condition = value;
      return this;
    }

    /**
     * Set the {@link ProcessorLimit#name} value in the builder.
     * @param value The value for the {@link ProcessorLimit#name}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder name(final String value) {
      this.name = value;
      return this;
    }

    /**
     * Set the {@link ProcessorLimit#limit} value in the builder.
     * @param value The value for the {@link ProcessorLimit#limit}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder limit(final int value) {
      this.limit = value;
      return this;
    }

    /**
     * Construct a new instance of the ProcessorLimit class.
     * @return a new instance of the ProcessorLimit class.
     */
    public ProcessorLimit build() {
      ProcessorLimit result = new ProcessorLimit(type, condition, name, limit);
      result.validateType(ProcessorType.LIMIT, type);
      return result;
    }
  }

    /**
     * Construct a new instance of the ProcessorLimit.Builder class.
     * @return a new instance of the ProcessorLimit.Builder class.
     */
  public static ProcessorLimit.Builder builder() {
    return new ProcessorLimit.Builder();
  }

  private ProcessorLimit(final ProcessorType type, final Condition condition, final String name, final int limit) {
    this.type = type;
    this.condition = condition;
    this.name = name;
    this.limit = limit;
  }
  
  
  
  
}
