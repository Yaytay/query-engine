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

import com.fasterxml.jackson.databind.JavaType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author njt
 */
public class OpenApiModelConverter implements ModelConverter {

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})  
  public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
    if (chain.hasNext()) {
      Schema schema = chain.next().resolve(type, context, chain);
      JavaType javaType = Json.mapper().constructType(type.getType());
      if (javaType != null) {
        Class<?> cls = javaType.getRawClass();
        if (Map.class.isAssignableFrom(cls) || List.class.isAssignableFrom(cls)) {
          removeEmptyProperty(schema);
        }
      }
      return schema;
    } else {
      return null;
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})  
  static void removeEmptyProperty(Schema schema) {
    if (schema != null && schema.getProperties() != null) {
      schema.getProperties().remove("empty");
      if (schema.getProperties().isEmpty()) {
        schema.setProperties(null);
      }
    }
  }
  
  @Override
  public boolean isOpenapi31() {
    return true;
  }
}
