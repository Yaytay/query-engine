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
package uk.co.spudsoft.query.main;

import com.google.common.collect.ImmutableSet;
import io.swagger.v3.core.filter.AbstractSpecFilter;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * OpenAPI filter for removing some types from the schema, either because they are mappable from strings or have no specific schema.
 * 
 * @author jtalbut
 */
public class OpenApiFilterClass extends AbstractSpecFilter {

  private static final Set<String> UNDESIRABLE = ImmutableSet.<String>builder()
          .add("MediaType")
          .add("ObjectNode")
          .build();

  /**
   * Constructor.
   */
  public OpenApiFilterClass() {
  }
  
  @Override
  public boolean isOpenAPI31Filter() {
    return true;
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})  
  public Optional<Schema> filterSchema(Schema schema, Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {
    if (UNDESIRABLE.contains(schema.getName())) {
      return Optional.empty();
    }
    return super.filterSchema(schema, params, cookies, headers); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
  }
  
}
