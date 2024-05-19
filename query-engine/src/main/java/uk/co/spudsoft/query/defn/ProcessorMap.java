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
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.procs.filters.ProcessorMapInstance;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;

/**
 * Processor that curtails the output after the configured number of rows.
 * @author jtalbut
 */
@JsonDeserialize(builder = ProcessorMap.Builder.class)
@Schema(description = """
                      Processor that renames or removes fields in the output.
                      """
)
public class ProcessorMap implements Processor {
  
  private final ProcessorType type;
  private final Condition condition;
  private final String id;
  private final ImmutableList<ProcessorMapLabel> relabels;

  @Override
  public ProcessorMapInstance createInstance(Vertx vertx, SourceNameTracker sourceNameTracker, Context context) {
    return new ProcessorMapInstance(vertx, sourceNameTracker, context, this);
  }

  @Override
  public void validate() {
    validateType(ProcessorType.MAP, type);
    if (relabels.isEmpty()) {
      throw new IllegalArgumentException("No relabels provided");
    }
    for (ProcessorMapLabel relabel : relabels) {
      relabel.validate();
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
  public String getId() {
    return id;
  }

  @Schema(description = """
                        The fields that will be renamed by this processor.
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public List<ProcessorMapLabel> getRelabels() {
    return relabels;
  }
  
  /**
   * Builder class for ProcessorMap.
   */
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Builder class should result in all instances being immutable when object is built")
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private ProcessorType type = ProcessorType.MAP;
    private Condition condition;
    private String id;
    private List<ProcessorMapLabel> relabels;

    private Builder() {
    }
    
    /**
     * Set the {@link ProcessorMap#type} value in the builder.
     * @param value The value for the {@link ProcessorMap#type}, must be {@link ProcessorType#DYNAMIC_FIELD}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder type(final ProcessorType value) {
      this.type = value;
      return this;
    }
    
    /**
     * Set the {@link ProcessorMap#condition} value in the builder.
     * @param value The value for the {@link ProcessorMap#condition}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder condition(final Condition value) {
      this.condition = value;
      return this;
    }

    /**
     * Set the {@link ProcessorMap#id} value in the builder.
     * @param value The value for the {@link ProcessorMap#id}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder id(final String value) {
      this.id = value;
      return this;
    }

    /**
     * Set the {@link ProcessorMap#relabels} value in the builder.
     * @param value The value for the {@link ProcessorMap#relabels}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder relabels(final List<ProcessorMapLabel> value) {
      this.relabels = value;
      return this;
    }

    /**
     * Construct a new instance of the ProcessorMap class.
     * @return a new instance of the ProcessorMap class.
     */
    public ProcessorMap build() {
      return new ProcessorMap(type, condition, id, relabels);
    }
  }

  /**
   * Construct a new instance of the ProcessorMap.Builder class.
   * @return a new instance of the ProcessorMap.Builder class.
   */
  public static ProcessorMap.Builder builder() {
    return new ProcessorMap.Builder();
  }

  private ProcessorMap(final ProcessorType type, final Condition condition, final String id, final List<ProcessorMapLabel> relabels) {
    validateType(ProcessorType.MAP, type);
    this.type = type;
    this.condition = condition;
    this.id = id;
    this.relabels = ImmutableCollectionTools.copy(relabels);
  }
  
  
  
  
}
