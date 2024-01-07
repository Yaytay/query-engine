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
import java.util.List;
import uk.co.spudsoft.query.defn.Argument;

/**
 * The realisation of an Argument passed in to a pipeline.
 * @author jtalbut
 */
public final class ArgumentInstance {
  
  private final String name;
  private final Argument definition;
  private final ImmutableList<String> values;

  public ArgumentInstance(String name, Argument definition, ImmutableList<String> values) {
    this.name = name;
    this.definition = definition;
    this.values = values == null ? ImmutableList.of() : values;
    if (!definition.isMultiValued() && this.values.size() > 1) {
      throw new IllegalArgumentException("Argument " + name + " is not multi valued but " + this.values.size() + " values supplied");
    }
    if (!definition.isOptional() && this.values.isEmpty()) {
      throw new IllegalArgumentException("Argument " + name + " is not optional but has no value");
    }
  }

  public String getName() {
    return name;
  }
  
  public Argument getDefinition() {
    return definition;
  }

  public List<String> getValues() {
    return values;
  }
    
}
