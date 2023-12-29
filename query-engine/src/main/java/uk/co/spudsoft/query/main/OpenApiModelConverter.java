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
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.MediaType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author njt
 */
public class OpenApiModelConverter implements ModelConverter {

  private static final Logger logger = LoggerFactory.getLogger(OpenApiModelConverter.class);
  
  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})  
  public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
    if (chain.hasNext()) {
      Schema schema = chain.next().resolve(type, context, chain);
      if (schema == null) {
        return null;
      }
      JavaType javaType = Json.mapper().constructType(type.getType());
      if ("ImmutableListArgument".equals(schema.getName())) {
        return null;
      }
      if ("ImmutableListFormat".equals(schema.getName())) {
        return null;
      }
      if (javaType != null) {
        Class<?> cls = javaType.getRawClass();
        fixArrrayPropertyDescriptions(cls, schema);
        if (Map.class.isAssignableFrom(cls) || List.class.isAssignableFrom(cls)) {
          removeEmptyProperty(schema);
        }
        if (Duration.class.isAssignableFrom(cls)) {
          convertDuration(schema);
        }
        if (MediaType.class.isAssignableFrom(cls)) {
          convertMediaType(schema);
        }
        setSchemaType(schema);
      }
      return schema;
    } else {
      return null;
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})  
  void setSchemaType(Schema schema) {
    // logger.debug("{} {} ({} or {}): {}", schema.getClass(), schema.getName(), schema.getTypes(), schema.getType(), schema.getProperties() == null ? null : schema.getProperties().size());
    if (schema.getType() != null && schema.getTypes() == null) {
      // logger.debug("Adding {} type to {}", schema.getType(), schema.getName());
      schema.setTypes(ImmutableSet.builder().add(schema.getType()).build());
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  static void fixArrrayPropertyDescriptions(Class<?> cls, Schema schema) {
    Map props = schema.getProperties();
    if (props != null) {
      props.forEach((k, v) -> {
        if (v instanceof Schema s) {
          if (Strings.isNullOrEmpty(s.getDescription())) {
            String propName = s.getName();
            String methodName = "get" + propName.substring(0, 1).toUpperCase() + propName.substring(1);

            try {
              Method method = cls.getMethod(methodName);

              ArraySchema arraySchema = method.getAnnotation(ArraySchema.class);
              if (arraySchema != null) {
                if (arraySchema.arraySchema() != null) {
                  if (!Strings.isNullOrEmpty(arraySchema.arraySchema().description())) {
                    s.setDescription(arraySchema.arraySchema().description());
                    return ;
                  }
                } 
                if (arraySchema.schema() != null) {
                  if (!Strings.isNullOrEmpty(arraySchema.schema().description())) {
                    s.setDescription(arraySchema.schema().description());
                  }
                } 
              }
            } catch (NoSuchMethodException ex) {
            }
          }
        }
      });
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
  
  @SuppressWarnings({"unchecked", "rawtypes"})  
  static void convertDuration(Schema schema) {
    if (schema != null) {
      schema.setProperties(null);
      schema.setTypes(ImmutableSet.builder().add("string").build());
      schema.setMaxLength(40);
      schema.setPattern("^P(?!$)(\\\\d+Y)?(\\\\d+M)?(\\\\d+W)?(\\\\d+D)?(T(?=\\\\d)(\\\\d+H)?(\\\\d+M)?(\\\\d+S)?)?$");
    }
  }
  
  @SuppressWarnings({"unchecked", "rawtypes"})  
  static void convertMediaType(Schema schema) {
    if (schema != null) {
      schema.setProperties(null);
      schema.setTypes(ImmutableSet.builder().add("string").build());
      schema.setMaxLength(40);
      String restrictedName = "[A-Za-z0-9][A-Za-z0-9!#$&-^_.+]{0,126}";
      schema.setPattern("^(" + restrictedName + ")/(" + restrictedName + ")(; *" + restrictedName + "(=" + restrictedName + "))*$");
    }
  }
  
  @Override
  public boolean isOpenapi31() {
    return true;
  }
}
