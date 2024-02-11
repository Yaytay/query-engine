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
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import java.util.List;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.procs.sort.ProcessorSortInstance;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;

/**
 * Processor that curtails the output after the configured number of rows.
 * @author jtalbut
 */
@JsonDeserialize(builder = ProcessorSort.Builder.class)
@Schema(description = """
                      Processor that sorts the data stream.
                      <P>
                      Note that this pipeline, unlike most others, has to buffer the entire stream before it can sort it.
                      Additionally, if the data consists of too many rows it will be sorted on disc.
                      </P>
                      <P>
                      This processor is inherently slow, if you need to use it please discuss options with the pipeline designer.
                      </P>
                      """
)
public class ProcessorSort implements Processor {
  
  private final ProcessorType type;
  private final Condition condition;
  private final ImmutableList<String> fields;

  @Override
  public ProcessorInstance createInstance(Vertx vertx, SourceNameTracker sourceNameTracker, Context context) {
    return new ProcessorSortInstance(vertx, sourceNameTracker, context, this);
  }

  @Override
  public void validate() {
    validateType(ProcessorType.SORT, type);
    if (fields == null || fields.isEmpty()) {
      throw new IllegalArgumentException("No fields provided for sorting");
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
                        The fields this processor will use to sort the data.
                        """
  )
  public List<String> getFields() {
    return fields;
  }
  
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Builder class should result in all instances being immutable when object is built")
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private ProcessorType type = ProcessorType.SORT;
    private Condition condition;
    private List<String> fields;

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

    public Builder fields(final List<String> value) {
      this.fields = value;
      return this;
    }

    public ProcessorSort build() {
      return new ProcessorSort(type, condition, fields);
    }
  }

  public static ProcessorSort.Builder builder() {
    return new ProcessorSort.Builder();
  }

  private ProcessorSort(final ProcessorType type, final Condition condition, final List<String> fields) {
    validateType(ProcessorType.SORT, type);
    this.type = type;
    this.condition = condition;
    this.fields = ImmutableCollectionTools.copy(fields);
  }
  
  
  
  
}
