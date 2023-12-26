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

import io.swagger.v3.core.filter.AbstractSpecFilter;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 *
 * @author jtalbut
 */
public class OpenApiFilterClass extends AbstractSpecFilter {

  @Override
  public boolean isOpenAPI31Filter() {
    return true;
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})  
  public Optional<Schema> filterSchema(Schema schema, Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {
    if ("MediaType".equals(schema.getName())) {
      return Optional.empty();
    }
    return super.filterSchema(schema, params, cookies, headers); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
  }
  
  
  
}
