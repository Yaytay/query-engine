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

import com.google.common.collect.ImmutableMap;
import io.vertx.core.json.JsonObject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author jtalbut
 */
public class DataRowTest {
  
  private DataRow create() {
    return DataRow.create(new Types())
            .put("nullValue", null)
            .put("intValue", 7)
            .put("longValue", 1L << 40)
            .put("floatValue", 3.4f)
            .put("doubleValue", 3.1415926)
            .put("stringValue", "wibble")
            .put("boolValue", true)
            .put("timeValue", LocalTime.of(12, 34))
            .put("dateValue", LocalDate.of(1971, 05, 06))
            .put("dateTimeValue", LocalDateTime.of(1971, 05, 06, 12, 34))
            ;
  }
  
  @Test
  public void testGet() {
    DataRow row = create();
    assertThrows(IllegalArgumentException.class, () -> row.put("sqlDate", new java.sql.Date(42336000000L)));
    assertThrows(IllegalArgumentException.class, () -> row.put("sqlTime", new java.sql.Time(45296000L)));
    assertThrows(IllegalArgumentException.class, () -> row.put("javaDate", new java.util.Date(42381240000L)));
  }

  @Test
  public void testForEach_BiConsumer() {
    DataRow row = create();
    StringBuilder r = new StringBuilder();
    row.forEach((k, v) -> r.append(", ").append(k.name()).append(":").append(v));
    assertEquals(", nullValue:null, intValue:7, longValue:1099511627776, floatValue:3.4, doubleValue:3.1415926, stringValue:wibble, boolValue:true, timeValue:12:34, dateValue:1971-05-06, dateTimeValue:1971-05-06T12:34", r.toString());
  }

  @Test
  public void testForEach_Consumer() {
    DataRow row = create();
    StringBuilder r = new StringBuilder();
    // forEach(Entry) can modify the data
    row.forEach(e -> {
      if (e.getValue() instanceof Long) { 
        e.setValue(47L);
      }
    });
    row.forEach(e  -> r.append(", ").append(e.getKey()).append(":").append(e.getValue()));
    assertEquals(", nullValue:null, intValue:7, longValue:47, floatValue:3.4, doubleValue:3.1415926, stringValue:wibble, boolValue:true, timeValue:12:34, dateValue:1971-05-06, dateTimeValue:1971-05-06T12:34", r.toString());    
  }

  @Test
  public void testConvert() {
    assertEquals(null, DataRow.convert(null));
    assertEquals(7, DataRow.convert(7));
    assertEquals(1099511627776L, DataRow.convert(1L << 40));
    assertEquals(3.4f, DataRow.convert(3.4f));
    assertEquals(3.1415926, DataRow.convert(3.1415926));
    assertEquals("wibble", DataRow.convert("wibble"));
    assertEquals(Boolean.TRUE, DataRow.convert(true));
    assertEquals(LocalTime.of(12, 34), DataRow.convert(LocalTime.of(12, 34)));
    assertEquals(LocalDate.of(1971, 05, 06), DataRow.convert(LocalDate.of(1971, 05, 06)));
    assertEquals(LocalDateTime.of(1971, 05, 06, 12, 34), DataRow.convert(LocalDateTime.of(1971, 05, 06, 12, 34)));
    assertEquals(LocalDate.of(1971, 05, 06), DataRow.convert(new java.sql.Date(42336000000L)));               // 1971-05-06
    assertEquals(LocalTime.of(12, 34, 56), DataRow.convert(new java.sql.Time(45296000L)));                    // 12:34:56
    assertEquals(LocalDateTime.of(1971, 05, 06, 12, 34), DataRow.convert(new java.util.Date(42381240000L)));  // LocalDateTime.of(1971, 05, 06, 12, 34).toEpochSecond(ZoneOffset.UTC)
    assertEquals("{text=hello}", DataRow.convert(ImmutableMap.<String, String>builder().put("text", "hello").build()));
  }

  @Test
  public void testConvertPut() {
    DataRow row = DataRow.create(new Types())
            .convertPut("nullValue", null)
            .convertPut("intValue", 7)
            .convertPut("longValue", 1L << 40)
            .convertPut("floatValue", 3.4f)
            .convertPut("doubleValue", 3.1415926)
            .convertPut("stringValue", "wibble")
            .convertPut("boolValue", true)
            .convertPut("timeValue", LocalTime.of(12, 34))
            .convertPut("dateValue", LocalDate.of(1971, 05, 06))
            .convertPut("dateTimeValue", LocalDateTime.of(1971, 05, 06, 12, 34))
            .convertPut("sqlDate", new java.sql.Date(42336000000L))       // 1971-05-06
            .convertPut("sqlTime", new java.sql.Time(45296000L))          // 12:34:56
            .convertPut("javaDate", new java.util.Date(42381240000L))     // LocalDateTime.of(1971, 05, 06, 12, 34).toEpochSecond(ZoneOffset.UTC)
            .convertPut("convertedStringValue", ImmutableMap.<String, String>builder().put("text", "hello").build())
            ;
    assertEquals(LocalDateTime.of(1971, 05, 06, 12, 34), row.get("javaDate"));
    assertEquals(LocalTime.of(12, 34, 56), row.get("sqlTime"));
    assertEquals(LocalDate.of(1971, 05, 06), row.get("sqlDate"));
    assertEquals("{text=hello}", row.get("convertedStringValue"));
  }

  @Test
  public void testGetMap() {
  }

  @org.junit.Test
  public void testCreate_Types() {
  }

  @org.junit.Test
  public void testPutTypeIfAbsent() {
  }

  @org.junit.Test
  public void testCreate_List() {
  }

  @org.junit.Test
  public void testIsEmpty() {
  }

  @org.junit.Test
  public void testToString() {
  }

  @org.junit.Test
  public void testGetType() {
  }

  @org.junit.Test
  public void testSize() {
  }

  @org.junit.Test
  public void testPut_String_Object() {
  }

  @org.junit.Test
  public void testPut_3args() {
  }

  @org.junit.Test
  public void testContainsKey() {
  }
  
}
