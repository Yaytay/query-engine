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
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Open API {@link io.swagger.v3.core.converter.ModelConverter} for tidying up aspects of the generated schema.
 * <ul>
 * <li>Removes "empty" property from {@link java.util.List} and {@link java.util.Map} objects.
 * <li>Outputs correct schema for {@link java.time.Duration} type, rather than expecting an Object.
 * <li>Outputs correct schema for {@link com.google.common.net.MediaType} type, rather than expecting an Object.
 * <li>Sets the schema type correctly for arrays.
 * </ul>
 *
 * @author jtalbut
 */
public class OpenApiModelConverter implements ModelConverter {

  private static final Logger logger = LoggerFactory.getLogger(OpenApiModelConverter.class);

  private final AtomicInteger level = new AtomicInteger();

  /**
   * Constructor.
   */
  public OpenApiModelConverter() {
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
    if (chain.hasNext()) {
      logger.trace("Resolving type: {} {}:{} ({})", level.incrementAndGet(), type.getType().getTypeName(), type.getPropertyName(), type.getName());
      Schema schema = chain.next().resolve(type, context, chain);
      if (schema == null) {
        // logger.info("Got null type");
        level.decrementAndGet();
        return null;
      }
      // logger.info("Got type: {} ({})", schema.getName(), schema.getType());
      JavaType javaType = Json.mapper().constructType(type.getType());
      if (javaType != null) {
        Class<?> cls = javaType.getRawClass();
        fixArrrayPropertyDescriptions(cls, schema);
        ensureParentSchema(new HashSet<>(), cls, schema);
        if (Map.class.isAssignableFrom(cls) || List.class.isAssignableFrom(cls)) {
          removeEmptyProperty(schema);
        }
        if (Duration.class.isAssignableFrom(cls)) {
          convertDuration(schema);
        }
        if (MediaType.class.isAssignableFrom(cls)) {
          convertMediaType(schema);
        }
      }
      level.decrementAndGet();
      return schema;
    } else {
      level.decrementAndGet();
      return null;
    }
  }

  static void ensureParentSchema(Set<Class<?>> processed, Class<?> cls, Schema<?> schema) {

    if (cls != null && !cls.getPackageName().startsWith("java")) {
      augmentFromClass(processed, cls.getSuperclass(), schema);
      if (cls.getInterfaces() != null) {
        for (Class<?> iface : cls.getInterfaces()) {
          augmentFromClass(processed, iface, schema);
        }
      }
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static void augmentFromClass(Set<Class<?>> processed, Class<?> cls, Schema schema) {
    
    if (cls != null && !cls.getPackageName().startsWith("java")) {
      if (!processed.contains(cls)) {
        processed.add(cls);
        Map<String, Schema> props = schema.getProperties();
        if (props != null) {
          props.forEach((k, s) -> {
            String propName = s.getName();
            String methodName = "get" + propName.substring(0, 1).toUpperCase() + propName.substring(1);

            Method method;
            try {
              method = cls.getMethod(methodName);
            } catch (NoSuchMethodException ex) {
              return;
            }

            io.swagger.v3.oas.annotations.media.Schema schemaAnnotation = method.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
            if (schemaAnnotation != null) {
              if (Strings.isNullOrEmpty(s.getDescription())) {
                if (!Strings.isNullOrEmpty(schemaAnnotation.description())) {
                  s.description(schemaAnnotation.description());
                }
              }
              if (schema.getRequired() == null || !schema.getRequired().contains(propName)) {
                if (schemaAnnotation.requiredMode() == RequiredMode.REQUIRED) {
                  schema.addRequiredItem(propName);
                }
              }
              if (s.getMaxLength() == null) {
                if (schemaAnnotation.maxLength() >= 0) {
                  s.maxLength(schemaAnnotation.maxLength());
                }
              }
            }
          });
        }
        ensureParentSchema(processed, cls, schema);
      }
    }
  }
    
  @SuppressWarnings({"unchecked", "rawtypes"})
  static void fixArrrayPropertyDescriptions(Class<?> cls, Schema schema) {
    Map<String, Schema> props = schema.getProperties();
    if (props != null) {
      props.forEach((k, s) -> {
        String propName = s.getName();
        String methodName = "get" + propName.substring(0, 1).toUpperCase() + propName.substring(1);

        Method method;
        try {
          method = cls.getMethod(methodName);
        } catch (NoSuchMethodException ex) {
          return;
        }
        ArraySchema arraySchema = method.getAnnotation(ArraySchema.class);
        if (arraySchema != null) {
          if (s.getTypes() == null || s.getTypes().isEmpty()) {
            s.setTypes(ImmutableSet.builder().add("array").build());
          }
          if (arraySchema.arraySchema() != null) {
            if (Strings.isNullOrEmpty(s.getDescription()) && !Strings.isNullOrEmpty(arraySchema.arraySchema().description())) {
              s.setDescription(arraySchema.arraySchema().description());
            }
            if (arraySchema.arraySchema().requiredMode() == RequiredMode.REQUIRED && (schema.getRequired() == null || !schema.getRequired().contains(k))) {
              schema.addRequiredItem(k);
            }
          }
          if (arraySchema.schema() != null) {
            io.swagger.v3.oas.annotations.media.Schema itemSchemaAnnotation = arraySchema.schema();
            Schema<?> itemSchema = s.getItems();
            if (itemSchema == null) {
              itemSchema = new JsonSchema();
            } else {
              itemSchema.setMinItems(null);
              itemSchema.setMaxItems(null);
              itemSchema.setUniqueItems(null);
            }
            if (itemSchemaAnnotation.types() != null && itemSchemaAnnotation.types().length > 0) {
              itemSchema.setTypes(new HashSet<>(Arrays.asList(itemSchemaAnnotation.types())));
            }
            if (itemSchema.getTypes() == null || itemSchema.getTypes().isEmpty() || itemSchema.getTypes().contains("object")) {
              if (itemSchemaAnnotation.implementation() != null && itemSchemaAnnotation.implementation() != Void.class) {
                itemSchema.$ref("#/components/schemas/" + itemSchemaAnnotation.implementation().getSimpleName());
              } else if (method.getGenericReturnType() != null) {
                Type returnType = method.getGenericReturnType();

                if (returnType instanceof ParameterizedType parameterizedType) {
                  Type[] typeArguments = parameterizedType.getActualTypeArguments();

                  if (typeArguments.length > 0) {
                    if (typeArguments[0] instanceof Class<?> retClass) {
                      itemSchema.$ref("#/components/schemas/" + retClass.getSimpleName());
                    }
                  }
                }
              }
            }
            if (arraySchema.minItems() >= 0 && arraySchema.minItems() < Integer.MAX_VALUE) {
              schema.setMinItems(arraySchema.minItems());
            }
            s.setItems(itemSchema);
//            if (Strings.isNullOrEmpty(s.getDescription()) && !Strings.isNullOrEmpty(itemSchemaAnnotation.description())) {
//              s.setDescription(itemSchemaAnnotation.description());
//            }
          }
          if (arraySchema.schema() != null) {
            io.swagger.v3.oas.annotations.media.Schema itemSchemaAnnotation = arraySchema.schema();
            if (Strings.isNullOrEmpty(s.getDescription()) && !Strings.isNullOrEmpty(itemSchemaAnnotation.description())) {
              s.setDescription(itemSchemaAnnotation.description());
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
