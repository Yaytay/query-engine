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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Vertx;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.context.PipelineContext;

/**
 *
 * Base class for all Processors.
 * 
 * Processors modify the data stream in flight.
 * 
 * @author jtalbut
 */
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME, 
  include = JsonTypeInfo.As.EXISTING_PROPERTY, 
  property = "type")
@JsonSubTypes({ 
  @JsonSubTypes.Type(value = ProcessorLimit.class, name = "LIMIT") 
  , @JsonSubTypes.Type(value = ProcessorDynamicField.class, name = "DYNAMIC_FIELD") 
  , @JsonSubTypes.Type(value = ProcessorExpression.class, name = "EXPRESSION") 
  , @JsonSubTypes.Type(value = ProcessorGroupConcat.class, name = "GROUP_CONCAT") 
  , @JsonSubTypes.Type(value = ProcessorLookup.class, name = "LOOKUP") 
  , @JsonSubTypes.Type(value = ProcessorMap.class, name = "MAP") 
  , @JsonSubTypes.Type(value = ProcessorMerge.class, name = "MERGE") 
  , @JsonSubTypes.Type(value = ProcessorOffset.class, name = "OFFSET") 
  , @JsonSubTypes.Type(value = ProcessorQuery.class, name = "QUERY") 
  , @JsonSubTypes.Type(value = ProcessorScript.class, name = "SCRIPT") 
  , @JsonSubTypes.Type(value = ProcessorSort.class, name = "SORT") 
})
@Schema(
        discriminatorProperty = "type"
        , discriminatorMapping = {
          @DiscriminatorMapping(schema = ProcessorLimit.class, value = "LIMIT") 
          , @DiscriminatorMapping(schema = ProcessorDynamicField.class, value = "DYNAMIC_FIELD") 
          , @DiscriminatorMapping(schema = ProcessorGroupConcat.class, value = "GROUP_CONCAT") 
          , @DiscriminatorMapping(schema = ProcessorLookup.class, value = "LOOKUP") 
          , @DiscriminatorMapping(schema = ProcessorMap.class, value = "MAP") 
          , @DiscriminatorMapping(schema = ProcessorOffset.class, value = "OFFSET") 
          , @DiscriminatorMapping(schema = ProcessorQuery.class, value = "QUERY") 
          , @DiscriminatorMapping(schema = ProcessorScript.class, value = "SCRIPT") 
          , @DiscriminatorMapping(schema = ProcessorSort.class, value = "SORT") 
        }
        , description = """
                      Processors modify the data stream in flight.
                      """
)
public interface Processor {
  
  /**
   * Throw an IllegalArgumentException if the Processor is not usable.
   * @throws IllegalArgumentException if the Processor is not usable.
   */
  void validate();
    
  /**
   * Create a new instance of the appropriate {@link ProcessorInstance}.
   * @param vertx The Vert.x instance.
   * @param pipelineContext The context in which this {@link SourcePipeline} is being run.
   * @param meterRegistry MeterRegistry for production of metrics.
   * @param name The name of this processor, either from the definition or generated.
   * @return a newly created {@link ProcessorInstance}.
   */
  @JsonIgnore
  ProcessorInstance createInstance(Vertx vertx, PipelineContext pipelineContext, MeterRegistry meterRegistry, String name);
  
  /**
   * The type of Processor being configured.
   * @return the type of Processor being configured.
   */
  @Schema(description = """
                        <P>The type of Processor being configured.</P>
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  ProcessorType getType();
  
  /**
   * Name that uniquely identifies this processor within the pipeline.
   * @return a name that uniquely identifies this processor within the pipeline.
   */
  @Schema(description = """
                        <P>Name that uniquely idenfities this processor within the pipeline.</P>
                        """
          , minLength = 1
          , maxLength = 60
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  String getName();
  
  /**
   * Optional condition that controls whether the processor will be run.
   * @return an optional condition that controls whether the processor will be run.
   */
  @Schema(description = """
                        <P>Optional condition that controls whether the processor will be run.</P>
                        """
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  Condition getCondition();
  
  /**
   * Validate that the {@link ProcessorType} configured is correct.
   * @param required the required {@link ProcessorType}.
   * @param actual the configured {@link ProcessorType}.
   * @throws IllegalArgumentException if the two types do not match.
   */
  default void validateType(ProcessorType required, ProcessorType actual) throws IllegalArgumentException {
    if (required != actual) {
      throw new IllegalArgumentException("Processor of type " + required + " configured with type " + actual);
    }
  }
  
}
