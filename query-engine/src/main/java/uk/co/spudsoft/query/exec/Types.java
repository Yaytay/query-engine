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
package uk.co.spudsoft.query.exec;

import io.vertx.sqlclient.desc.ColumnDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.DataType;

/**
 *
 * @author njt
 */
public class Types {
  
  private static final Logger logger = LoggerFactory.getLogger(Types.class);
  
  private final List<ColumnDefn> defns = new ArrayList<>();
  private final HashMap<String, Integer> indices = new HashMap<>();
  
  public Types() {
  }

  public Types(List<ColumnDefn> types) {
    int i = 0;
    for (ColumnDefn defn : types) {
      defns.add(defn);
      indices.put(defn.name(), i++);
    }
  }
  
  public DataType get(String key) {
    Integer idx = indices.get(key);
    if (idx == null) {
      return null;
    } else {
      return defns.get(idx).type();
    }
  }
  
  public Set<String> keySet() {
    return indices.keySet();
  }
  
  /**
   * Set the type for the key if it is not already set (or is currently set to null).
   * 
   * If it possible 
   * 
   * @param key The name of the column.
   * @param type The desired type of the column.
   * @return this.
   * @throws IllegalStateException if they type is already set to a non-null value and an attempt is made to set it to a different non-null value.
   */
  public final Types putIfAbsent(String key, DataType type) throws IllegalStateException {
    if (type == null) {
      type = DataType.Null;
    }
    synchronized (defns) {
      Integer idx = indices.get(key);
      if (idx == null) {
        indices.put(key, defns.size());
        defns.add(new ColumnDefn(key, type));
      } else {
        ColumnDefn current = defns.get(idx);
        if (current.type() == DataType.Null && type != DataType.Null) {
          defns.set(idx, new ColumnDefn(key, type));
//      } else if (current != type && type != DataType.Null) {
//        logger.warn("Attempt to change type of {} from {} to {}", key, current, type);
//        throw new IllegalStateException("Cannot change type (" + key + ")");
        }
      }
    }
    return this;
  }

  public List<ColumnDescriptor> getColumnDescriptors() {
    return Collections.unmodifiableList(defns);
  }

  public void forEach(Consumer<? super ColumnDefn> action) {
    defns.forEach(action);
  }
  
  public boolean isEmpty() {
    return defns.isEmpty();
  }
  
}
