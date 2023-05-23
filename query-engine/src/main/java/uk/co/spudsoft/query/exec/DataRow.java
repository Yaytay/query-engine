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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


/**
 *
 * @author jtalbut
 */
public class DataRow {
  
  private final LinkedHashMap<String, DataType> types;
  private final LinkedHashMap<String, Object> data = new LinkedHashMap<>();

  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "It is expected that the types map change between instances of the DataRow")
  public DataRow(LinkedHashMap<String, DataType> types) {
    this.types = types;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    data.forEach((k, v) -> json.put(k, Utils.toJson(v)));
    return json;
  }

  @Override
  public String toString() {
    return toJson().toString();
  }

  public Object get(String key) {
    return data.get(key);
  }
  
  public DataType getType(String key) {
    return types.get(key);
  }

  public int size() {
    return data.size();
  }
  
  public void forEach(BiConsumer<? super String, ? super Object> action) {
    data.forEach(action);
  }

  public void forEach(Consumer<Entry<? super String, ? super Object>> action) {
    data.entrySet().forEach(action);
  }

  public DataRow put(String key, Object value) {
    DataType type = DataType.fromObject(value);
    types.putIfAbsent(key, type);
    data.put(key, value);
    return this;
  }
  
  public DataRow put(String key, DataType type, Object value) {
    types.putIfAbsent(key, type);
    data.put(key, value);
    return this;
  }
  
  public DataRow convertPut(String key, Object value) {
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
      put(key, value);
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
  
  public static Object convert(Object value) {
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
      return value;
    } else if (value instanceof java.sql.Date d) {
      return d.toLocalDate();
    } else if (value instanceof java.sql.Time d) {
      // java.sql.Time.toLocalTime calls getHours internally, which normalizes (ie. assumes the Time value is in local timezone and converts to UTC).
      // This conversion does not normalize.
      long t = d.getTime();
      return LocalTime.ofNanoOfDay(t * 1000000);
    } else if (value instanceof java.util.Date d) {
      return LocalDateTime.ofInstant(d.toInstant(), ZoneOffset.UTC);
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

}
