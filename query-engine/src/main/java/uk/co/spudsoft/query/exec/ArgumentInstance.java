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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.Argument;
import uk.co.spudsoft.query.defn.ArgumentType;

/**
 * The realisation of an Argument passed in to a pipeline.
 * @author jtalbut
 */
public final class ArgumentInstance {
  
  private static final Logger logger = LoggerFactory.getLogger(ArgumentInstance.class);
  
  private final String name;
  private final Argument definition;
  private final ImmutableList<Comparable<?>> values;

  /**
   * Constructor.
   * <p>
   * The values passed in will be parsed to the appropriate type and some validation will be carried out.
   * Any failures will result in an IllegalArgumentException.
   * 
   * @param name The name of the argument.
   * @param definition The definition of the argument.
   * @param values The values for the argument as passed in as query string parameters.
   */
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
          if (i == 0) {
            throw new IllegalArgumentException("Argument " + name + " was given a value that could not be parsed as " + definition.getType().name());
          } else {
            throw new IllegalArgumentException("Argument " + name + " was given a value that could not be parsed");
          }
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
  
  /**
   * Parse a single string as a specific ArgumentType.
   * @param type The type to convert to.
   * @param value The string value to be parsed.
   * @return a single string parsed as a specific ArgumentType.
   */
  public static Comparable<?> parseValue(ArgumentType type, String value) {
    return switch (type) {
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
  
  /**
   * Parse a single string value as the correct type for this instance.
   * @param value The string value to be parsed.
   * @return the string value as the correct type for this instance.
   */
  public Comparable<?> parseValue(String value) {
    return parseValue(definition.getType(), value);
  } 
  
  /**
   * Validate each currently known value for minimum and maximum.
   * @throws IllegalArgumentException if any value exceeds the known minima/maxima.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void validateMinMax() throws IllegalArgumentException {
    Comparable min = Strings.isNullOrEmpty(definition.getMinimumValue()) ? null : parseValue(definition.getMinimumValue());
    Comparable max = Strings.isNullOrEmpty(definition.getMaximumValue()) ? null : parseValue(definition.getMaximumValue());
    
    for (Comparable<?> value : values) {
      if (min != null) {
        if (min.compareTo(value) > 0) {
          throw new IllegalArgumentException("Argument " + name + " has a minimum value of " + min + ", but \"" + value + "\" was specified.");
        }
      }
      if (max != null) {
        if (max.compareTo(value) < 0) {
          throw new IllegalArgumentException("Argument " + name + " has a maximum value of " + min + ", but \"" + value + "\" was specified.");
        }
      }
    }
  }
  
  /**
   * Get the name of this instance.
   * @return the name of this instance.
   */
  public String getName() {
    return name;
  }
  
  /**
   * Get the definition of this instance.
   * @return the definition of this instance.
   */
  public Argument getDefinition() {
    return definition;
  }

  /**
   * Get the values of this instance. 
   * @return the values of this instance.
   */
  public ImmutableList<Comparable<?>> getValues() {
    return values;
  }
    
}
