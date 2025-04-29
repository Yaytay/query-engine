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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.DataType;

/**
 * Details of the fields types in a stream.
 * 
 * All the rows in a given stream should use the same Types object, but Processors can replace the Types object as they generate a new ReadStream.
 * 
 * Fields cannot be removed from the Types structure, nor can they change type (other than going from Null to not Null).
 * 
 * @author jtalbut
 */
public class Types {
  
  private static final Logger logger = LoggerFactory.getLogger(Types.class);
  
  private final List<ColumnDefn> defns = new ArrayList<>();
  private final HashMap<String, Integer> indices = new HashMap<>();
  
  /**
   * Constructor.
   */
  public Types() {
  }

  /**
   * Copy constructor.
   * @param types The Types object to copy from.
   */
  public Types(List<ColumnDefn> types) {
    int i = 0;
    for (ColumnDefn defn : types) {
      defns.add(defn);
      indices.put(defn.name(), i++);
    }
  }
  
  /**
   * Get the type of one field.
   * @param key The field being requested.
   * @return the type of the requested field.
   */
  public DataType get(String key) {
    Integer idx = indices.get(key);
    if (idx == null) {
      return null;
    } else {
      return defns.get(idx).type();
    }
  }
  
  /**
   * Get the set of all fields.
   * @return the set of all fields.
   */
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
  public final Types putIfAbsent(String key, DataType type) {
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
        }
      }
    }
    return this;
  }

  /**
   * Get the list of column definitions.
   * @return the list of column definitions.
   */
  public List<ColumnDescriptor> getColumnDescriptors() {
    return Collections.unmodifiableList(defns);
  }

  /**
   * Carry out action for each known column definition.
   * @param action The action to carry out.
   */
  public void forEach(Consumer<? super ColumnDefn> action) {
    defns.forEach(action);
  }
  
  /**
   * Create a new Iterator across the column definitions.
   * @return a newly created Iterator across the column definitions.
   */
  public Iterator<ColumnDefn> iterator() {
    return defns.iterator();
  }
  
  /**
   * True if the there are no column definitions.
   * @return true if the there are no column definitions.
   */
  public boolean isEmpty() {
    return defns.isEmpty();
  }

  /**
   * The number of column definitions known.
   * @return the number of column definitions known.
   */
  public int size() {
    return defns.size();
  }

  @Override
  public String toString() {
    return "{" + defns + '}';
  }
  
}
