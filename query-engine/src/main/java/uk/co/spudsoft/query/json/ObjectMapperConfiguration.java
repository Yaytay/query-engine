/*
 * Copyright (C) 2023 jtalbut
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
package uk.co.spudsoft.query.json;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.net.MediaType;

/**
 * Helper class to make it easier to configure all Jackson ObjectMappers consistently.
 * 
 * @author jtalbut
 */
public class ObjectMapperConfiguration {

  private ObjectMapperConfiguration() {
  }
  
  /**
   * Configure the ObjectMapper in a standard way for the query engine.
   * 
   * If a specific instance of the ObjectMapper requires extra configuration they should be done after this set.
   * 
   * @param mapper The ObjectMapper to be configured.
   * @return The same ObjectMapper.
   */
  public static ObjectMapper configureObjectMapper(ObjectMapper mapper) {
    SimpleModule queryEngineModule = new SimpleModule("QueryEngineModule", new Version(1, 0, 0, null, "co.uk.spudsoft", "query-engine"));
    queryEngineModule.addSerializer(MediaType.class, new MediaTypeSerializer());
    
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    mapper.registerModules(
            new JavaTimeModule()
            , queryEngineModule            
    );
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    mapper.configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);
    mapper.setDefaultMergeable(Boolean.TRUE);
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    return mapper;
  }
  
}
