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
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import uk.co.spudsoft.query.exec.SharedMap;
import uk.co.spudsoft.query.exec.SourceInstance;

/**
 *
 * @author jtalbut
 */
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME, 
  include = JsonTypeInfo.As.EXISTING_PROPERTY, 
  property = "type",
  defaultImpl = SourceSql.class)
@JsonSubTypes({ 
  @Type(value = SourceSql.class, name = "SQL"), 
  @Type(value = SourceTest.class, name = "Test") 
})
@Schema(
        discriminatorProperty = "type"
        , discriminatorMapping = {
          @DiscriminatorMapping(schema = SourceSql.class, value = "SQL")
          , @DiscriminatorMapping(schema = SourceTest.class, value = "Test")
        }
)
public interface Source {
  
  @JsonIgnore
  SourceInstance createInstance(Vertx vertx, Context context, SharedMap sharedMap, String defaultName);
  
  @Schema(description = """
                        <P>The type of Source being configured.</P>
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  SourceType getType();
  
  /**
   * Throw an IllegalArgumentException if the Source is not usable.
   * @throws IllegalArgumentException if the Source is not usable.
   */
  void validate();
  
  /**
   * Get the name of the Source, that will be used in logging.
   * This is optional, if it is not set a numeric (or delimited numeric) name will be allocated.
   * @return the name of the Source, that will be used in logging.
   */
  @Schema(description = """
                        <P>Get the name of the Source, that will be used in logging.</P>
                        <P>
                        This is optional, if it is not set a numeric (or delimited numeric) name will be allocated.
                        </P>
                        """
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  String getName();
    
  default void validateType(SourceType required, SourceType actual) {
    if (required != actual) {
      throw new IllegalArgumentException("Source of type " + required + " configured with type " + actual);
    }
  }
  
}
