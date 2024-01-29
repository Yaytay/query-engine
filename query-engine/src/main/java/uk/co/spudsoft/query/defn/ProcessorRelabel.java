/*
 * Copyright (C) 2022 jtalbut
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but RELABEL ANY WARRANTY; without even the implied warranty of
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
import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import java.util.List;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.procs.filters.ProcessorRelabelInstance;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;

/**
 * Processor that curtails the output after the configured number of rows.
 * @author jtalbut
 */
@JsonDeserialize(builder = ProcessorRelabel.Builder.class)
@Schema(description = """
                      Processor that renames fields in the output.
                      """
)
public class ProcessorRelabel implements Processor {
  
  private final ProcessorType type;
  private final Condition condition;
  private final ImmutableList<ProcessorRelabelLabel> relabels;

  @Override
  public ProcessorInstance createInstance(Vertx vertx, SourceNameTracker sourceNameTracker, Context context) {
    return new ProcessorRelabelInstance(vertx, sourceNameTracker, context, this);
  }

  @Override
  public void validate() {
    validateType(ProcessorType.RELABEL, type);
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
                        The fields that will be removed by this processor.
                        """
  )
  public ImmutableList<ProcessorRelabelLabel> getRelabels() {
    return relabels;
  }
  
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Builder class should result in all instances being immutable when object is built")
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private ProcessorType type = ProcessorType.RELABEL;
    private Condition condition;
    private List<ProcessorRelabelLabel> relabels;

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

    public Builder relabels(final List<ProcessorRelabelLabel> value) {
      this.relabels = value;
      return this;
    }

    public ProcessorRelabel build() {
      return new ProcessorRelabel(type, condition, relabels);
    }
  }

  public static ProcessorRelabel.Builder builder() {
    return new ProcessorRelabel.Builder();
  }

  private ProcessorRelabel(final ProcessorType type, final Condition condition, final List<ProcessorRelabelLabel> relabels) {
    validateType(ProcessorType.RELABEL, type);
    this.type = type;
    this.condition = condition;
    this.relabels = ImmutableCollectionTools.copy(relabels);
  }
  
  
  
  
}
