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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.Argument;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.exec.context.PipelineContext;

/**
 * The realisation of an Argument passed in to a pipeline.
 * @author jtalbut
 */
public final class ArgumentInstance {
  
  private static final Logger logger = LoggerFactory.getLogger(ArgumentInstance.class);
  
  private final Argument definition;
  private final ImmutableList<Comparable<?>> values;

  /**
   * Constructor.
   * <p>
   * The values passed in will be parsed to the appropriate type and some validation will be carried out.
   * Any failures will result in an IllegalArgumentException.
   * 
   * @param definition The definition of the argument.
   * @param values The values for the argument as passed in as query string parameters.
   */
  public ArgumentInstance(Argument definition, ImmutableList<Comparable<?>> values) {
    this.definition = definition;
    this.values = values == null ? ImmutableList.of() : values;
    for (Comparable<?> value : this.values) {
      if (definition.getType() != DataType.fromObject(value)) {
        throw new IllegalArgumentException("Argument " + definition.getName() + " set with the value \"" + value + "\" which is not " + definition.getType());
      }
    }
    if (!definition.isMultiValued() && this.values.size() > 1) {
      throw new IllegalArgumentException("Argument " + definition.getName() + " is not multi valued but " + this.values.size() + " values supplied");
    }
    if (!definition.isOptional() && !definition.isHidden() && this.values.isEmpty()) {
      throw new IllegalArgumentException("Argument " + definition.getName() + " is not optional but has no value");
    }
  }
  
  /**
   * Validate each currently known value for minimum and maximum.
   * @param pipelineContext The context in which this {@link SourcePipeline} is being run.
   * @throws IllegalArgumentException if any value exceeds the known minima/maxima.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void validateMinMax(PipelineContext pipelineContext) throws IllegalArgumentException {
    Comparable min;
    Comparable max;
    try {
      min = Strings.isNullOrEmpty(definition.getMinimumValue()) ? null : definition.getType().cast(pipelineContext, definition.getMinimumValue());
    } catch (Exception ex) {
      throw new IllegalArgumentException("Argument " + definition.getName() + " has a minimum value of \"" + definition.getMinimumValue() + "\", but that cannot be parsed as a " + definition.getType() + ".", ex);
    }
    try {
      max = Strings.isNullOrEmpty(definition.getMaximumValue()) ? null : definition.getType().cast(pipelineContext, definition.getMaximumValue());
    } catch (Exception ex) {
      throw new IllegalArgumentException("Argument " + definition.getName() + " has a maximum value of \"" + definition.getMaximumValue() + "\", but that cannot be parsed as a " + definition.getType() + ".", ex);
    }
    
    for (Comparable<?> value : values) {
      if (min != null) {
        if (min.compareTo(value) > 0) {
          throw new IllegalArgumentException("Argument " + definition.getName() + " has a minimum value of " + min + ", but \"" + value + "\" was specified.");
        }
      }
      if (max != null) {
        if (max.compareTo(value) < 0) {
          throw new IllegalArgumentException("Argument " + definition.getName() + " has a maximum value of " + min + ", but \"" + value + "\" was specified.");
        }
      }
    }
  }
  
  /**
   * Get the name of this instance.
   * @return the name of this instance.
   */
  public String getName() {
    return definition.getName();
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
