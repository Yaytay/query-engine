/*
 * Copyright (C) 2025 jtalbut
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.google.common.net.MediaType;
import org.junit.jupiter.api.Test;

/**
 *
 * @author jtalbut
 */
public class ObjectMapperConfigurationTest {
  
  @Test
  public void testConfigureObjectMapper() throws Exception {
    
    ObjectMapper mapper = new ObjectMapper();
    ObjectMapperConfiguration.configureObjectMapper(mapper);
        
    assertEquals(
            "{\"float\":1,\"double\":2,\"type\":\"application/x-bzip2\"}"
            , mapper.writeValueAsString(
                    ImmutableMap.<String, Object>builder()
                            .put("float", 1.0F)
                            .put("double", 2.0)
                            .put("type", MediaType.BZIP2)
                            .build()
            )
    );
    
  }
  
}
