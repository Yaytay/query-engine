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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.DataType;

/**
 *
 * @author njt
 */
public class Types {
  
  private static final Logger logger = LoggerFactory.getLogger(Types.class);
  
  private final List<ColumnDescriptor> columnDescriptors = new ArrayList<>();
  private final LinkedHashMap<String, DataType> dataTypes = new LinkedHashMap<>();
  
  public Types() {
  }

  public Types(LinkedHashMap<String, DataType> types) {
    for (Entry<String, DataType> entry : types.entrySet()) {
      putIfAbsent(entry.getKey(), entry.getValue());
    }
  }
  
  public DataType get(String key) {
    return dataTypes.get(key);
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
    synchronized (dataTypes) {
      DataType current = dataTypes.get(key);
      if (current == null) {
        dataTypes.put(key, type);
        columnDescriptors.add(type.toColumnDescriptor(key));
      } else if (current == DataType.Null && type != DataType.Null) {
        dataTypes.put(key, type);
        for (int i = 0; i < columnDescriptors.size(); ++i) {
          if (columnDescriptors.get(i).name().equals(key)) {
            columnDescriptors.set(i, type.toColumnDescriptor(key));
          }
        }
//      } else if (current != type && type != DataType.Null) {
//        logger.warn("Attempt to change type of {} from {} to {}", key, current, type);
//        throw new IllegalStateException("Cannot change type (" + key + ")");
      }
    }
    return this;
  }

  public List<ColumnDescriptor> getColumnDescriptors() {
    return Collections.unmodifiableList(columnDescriptors);
  }
  
  public boolean isEmpty() {
    return dataTypes.isEmpty();
  }
  
}
