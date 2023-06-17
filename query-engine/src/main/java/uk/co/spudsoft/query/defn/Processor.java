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
  , @JsonSubTypes.Type(value = ProcessorGroupConcat.class, name = "GROUP_CONCAT") 
  , @JsonSubTypes.Type(value = ProcessorDynamicField.class, name = "DYNAMIC_FIELD") 
  , @JsonSubTypes.Type(value = ProcessorScript.class, name = "SCRIPT") 
})
@Schema(
        discriminatorProperty = "type"
        , discriminatorMapping = {
          @DiscriminatorMapping(schema = ProcessorLimit.class, value = "LIMIT")
          , @DiscriminatorMapping(schema = ProcessorGroupConcat.class, value = "GROUP_CONCAT")
          , @DiscriminatorMapping(schema = ProcessorDynamicField.class, value = "DYNAMIC_FIELD")
          , @DiscriminatorMapping(schema = ProcessorScript.class, value = "SCRIPT")      
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
  
  default void validateType(ProcessorType required, ProcessorType actual) {
    if (required != actual) {
      throw new IllegalArgumentException("Processor of type " + required + " configured with type " + actual);
    }
  }
  
}
