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
package uk.co.spudsoft.query.exec;

import com.google.common.collect.ImmutableList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.Argument;

/**
 * The realisation of an Argument passed in to a pipeline.
 * @author jtalbut
 */
public final class ArgumentInstance {
  
  private static final Logger logger = LoggerFactory.getLogger(ArgumentInstance.class);
  
  private final String name;
  private final Argument definition;
  private final ImmutableList<Comparable<?>> values;

  public ArgumentInstance(String name, Argument definition, ImmutableList<String> values) {
    this.name = name;
    this.definition = definition;
    
    ImmutableList.Builder<Comparable<?>> valueBuilder = ImmutableList.builder();
    if (values != null) {
      for (int i = 0; i < values.size(); ++i) {
        try {
          Comparable<?> parsed = parseValue(values.get(i));
          valueBuilder.add(parsed);
        } catch (Throwable ex) {
          logger.warn("Failed to parse argument {} value {} (\"{}\") as {}", name, i, values.get(i), definition.getType());
        }
      }
    }
    this.values = valueBuilder.build();
    if (!definition.isMultiValued() && this.values.size() > 1) {
      throw new IllegalArgumentException("Argument " + name + " is not multi valued but " + this.values.size() + " values supplied");
    }
    if (!definition.isOptional() && this.values.isEmpty()) {
      throw new IllegalArgumentException("Argument " + name + " is not optional but has no value");
    }
  }
  
  public Comparable<?> parseValue(String value) {
    return switch (definition.getType()) {
      case Boolean -> Boolean.parseBoolean(value);
      case Date -> LocalDate.parse(value);
      case DateTime -> LocalDateTime.parse(value);
      case Double -> Double.parseDouble(value);
      case Integer -> Integer.parseInt(value);
      case Long -> Long.parseLong(value);
      case String -> value;
      case Time -> LocalTime.parse(value);
    };
  } 
  
  public String getName() {
    return name;
  }
  
  public Argument getDefinition() {
    return definition;
  }

  public List<Comparable<?>> getValues() {
    return values;
  }
    
}
