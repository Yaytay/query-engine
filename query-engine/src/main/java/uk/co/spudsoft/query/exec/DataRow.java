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

import uk.co.spudsoft.query.defn.DataType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.impl.Utils;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Representation of a single row in the stream.
 * @author jtalbut
 */
public class DataRow {
  
  private final Types types;
  private final LinkedHashMap<String, Comparable<?>> data;

  public static final DataRow EMPTY_ROW = new DataRow(new Types(), new LinkedHashMap<>());
  
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "It is expected that the types map change between instances of the DataRow")
  private DataRow(Types types, LinkedHashMap<String, Comparable<?>> data) {
    Objects.requireNonNull(types);
    Objects.requireNonNull(data);
    this.types = types;
    this.data = data;
  }
  
  public static DataRow create(Types types) {
    return new DataRow(types, new LinkedHashMap<>());
  }
  
  /**
   * Factory method for test cases.
   * 
   * @param types The Types that must be created externally to be shared amongst different instances.
   * @param entries Array of pairs made up of a string key followed by an Object value.
   * @return A newly created DataRow.
   */
  public static DataRow create(Types types, Object... entries) {
    assert(entries.length % 2 == 0);
    DataRow result = new DataRow(types, new LinkedHashMap<>());
    for (int i = 0; i < entries.length; i += 2) {
      String key = (String) entries[i];
      Comparable<?> value = (Comparable) entries[i + 1];
      result.put(key, value);
    }
    return result;
  }
  
  public final void putTypeIfAbsent(String key, DataType type) throws IllegalStateException {
    types.putIfAbsent(key, type);
  }

  public static DataRow create(List<ColumnDefn> types) {
    return new DataRow(new Types(types), new LinkedHashMap<>());
  }
  
  public boolean isEmpty() {
    return data.isEmpty();
  }

//  public static DataRow createNonRow(Types types) {
//    return new DataRow(types, null);
//  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    data.forEach((k, v) -> json.put(k, Utils.toJson(v)));
    return json;
  }

  @Override
  public String toString() {
    return toJson().toString();
  }

  public Comparable<?> get(String key) {
    return data.get(key);
  }
  
  public DataType getType(String key) {
    return types.get(key);
  }

  public int size() {
    return data == null ? 0 : data.size();
  }
  
  private static final Logger logger = LoggerFactory.getLogger(DataRow.class);
  
  public int bytesSize() {
    int total[] = {0};
    types.forEach(cd -> {
      Object value = get(cd.name());
      // logger.info("{} ({}): {} @ {} bytes or {} bytes", cd.name(), value, cd.type(), SizeOfAgent.fullSizeOf(value), value == null ? 0 : SizeOfAgent.sizeOf(value));
      if (value != null) {
        if (cd.type() == DataType.String) {
          if (value instanceof String str) {
            total[0] += str.length();
          }
        } else {
          total[0] += cd.type().bytes();
        }
      }
    });
    return total[0];
  }
  
  public void forEach(BiConsumer<ColumnDefn, ? super Comparable<?>> action) {
    types.forEach(cd -> {
      action.accept(cd, data.get(cd.name()));
    });
  }

  public void forEach(Consumer<Entry<? super String, ? super Comparable<?>>> action) {
    data.entrySet().forEach(action);
  }

  public DataRow put(String key, Comparable<?> value) {
    DataType type = DataType.fromObject(value);
    types.putIfAbsent(key, type);
    data.put(key, value);
    return this;
  }
  
  public DataRow put(String key, DataType type, Comparable<?> value) {
    types.putIfAbsent(key, type);
    data.put(key, value);
    return this;
  }
  
  public DataRow convertPut(String key, Object value) {
    if (value == null) {
      put(key, null);
    } else if (value instanceof Integer v) {
      put(key, v);
    } else if (value instanceof Long v) {
      put(key, v);
    } else if (value instanceof Float v) {
      put(key, v);
    } else if (value instanceof Double v) {
      put(key, v);
    } else if (value instanceof String v) {
      put(key, v);
    } else if (value instanceof Boolean v) {
      put(key, v);
    } else if (value instanceof LocalDate v) {
      put(key, v);
    } else if (value instanceof LocalDateTime v) {
      put(key, v);
    } else if (value instanceof LocalTime v) {
      put(key, v);    
    } else if (value instanceof java.sql.Date d) {
      put(key, d.toLocalDate());
    } else if (value instanceof java.sql.Time d) {
      // java.sql.Time.toLocalTime calls getHours internally, which normalizes (ie. assumes the Time value is in local timezone and converts to UTC).
      // This conversion does not normalize.
      long t = d.getTime();
      put(key, LocalTime.ofNanoOfDay(t * 1000000));
    } else if (value instanceof java.util.Date d) {
      put(key, LocalDateTime.ofInstant(d.toInstant(), ZoneOffset.UTC));
    } else {
      put(key, value.toString());
    }
    return this;
  }
  
  public static Comparable<?> convert(Object value) {
    if (value == null 
            || value instanceof Integer
            || value instanceof Long
            || value instanceof Float
            || value instanceof Double
            || value instanceof String
            || value instanceof Boolean
            || value instanceof LocalDate
            || value instanceof LocalDateTime
            || value instanceof LocalTime
            ) {
      return (Comparable<?>) value;
    } else if (value instanceof java.sql.Date d) {
      return d.toLocalDate();
    } else if (value instanceof java.sql.Time d) {
      // java.sql.Time.toLocalTime calls getHours internally, which normalizes (ie. assumes the Time value is in local timezone and converts to UTC).
      // This conversion does not normalize.
      long t = d.getTime();
      return LocalTime.ofNanoOfDay(t * 1000000);
    } else if (value instanceof java.util.Date d) {
      return LocalDateTime.ofInstant(d.toInstant(), ZoneOffset.UTC);
    } else if (value instanceof Duration d) {
      return LocalTime.MIDNIGHT.plus(d);
    } else {
      return value.toString();
    }
  }
  
  public Map<String, Object> getMap() {
    return Collections.unmodifiableMap(data);
  }

  public boolean containsKey(String key) {
    return data.containsKey(key);
  }

  public Set<String> keySet() {
    return types.keySet();
  }

  public Types types() {
    return types;
  }
  
}
