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
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.SourceNameTracker;

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
  , @JsonSubTypes.Type(value = ProcessorGroupConcat.class, name = "GROUP_CONCAT") 
  , @JsonSubTypes.Type(value = ProcessorLookup.class, name = "LOOKUP") 
  , @JsonSubTypes.Type(value = ProcessorMap.class, name = "MAP") 
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
    
  @JsonIgnore
  ProcessorInstance createInstance(Vertx vertx, SourceNameTracker sourceNameTracker, Context context);
  
  @Schema(description = """
                        <P>The type of Processor being configured.</P>
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  ProcessorType getType();
  
  @Schema(description = """
                        <P>ID that uniquely idenfities this processor within the pipeline.</P>
                        """
          , minLength = 1
          , maxLength = 60
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  String getId();
  
  @Schema(description = """
                        <P>Optional condition that controls whether the processor will be run.</P>
                        """
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  Condition getCondition();
  
  default void validateType(ProcessorType required, ProcessorType actual) {
    if (required != actual) {
      throw new IllegalArgumentException("Processor of type " + required + " configured with type " + actual);
    }
  }
  
}
