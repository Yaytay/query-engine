/*
 * Copyright (C) 2025 njt
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
package uk.co.spudsoft.query.exec.fmts.json;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import uk.co.spudsoft.query.defn.FormatJson;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.json.ObjectMapperConfiguration;

/**
 *
 * @author njt
 */
public class FormatJsonInstanceTest {

  @Test
  public void testToJson() throws IOException {
    FormatJson definition = FormatJson.builder()
            .outputNullValues(false)
            .build();
    FormatJsonInstance instance = new FormatJsonInstance(null, null, definition);
    // Create a test DataRow with different types of data
    Types types = new Types();
    DataRow row = DataRow.create(types,
            "stringValue", "test string",
            "intValue", 42,
            "doubleValue", 3.14,
            "boolValue", true,
            "dateValue", LocalDate.of(2023, 5, 15),
            "timeValue", LocalTime.of(13, 45, 30),
            "dateTimeValue", LocalDateTime.of(2023, 5, 15, 13, 45, 30),
            "dateTimeValueZeroSeconds", LocalDateTime.of(2023, 5, 15, 13, 45, 0),
            "nullValue", null
    );

    // Convert to JSON
    JsonObject result = new JsonObject(instance.toJsonBuffer(row, true));

    // Verify the JSON object contains all expected entries
    assertEquals("test string", result.getString("stringValue"));
    assertEquals(42, result.getInteger("intValue"));
    assertEquals(3.14, result.getDouble("doubleValue"));
    assertEquals(true, result.getBoolean("boolValue"));

    // Date/time values might be formatted as strings
    assertEquals("2023-05-15", result.getValue("dateValue"));
    assertEquals("13:45:30", result.getValue("timeValue"));
    assertEquals("2023-05-15T13:45:30", result.getValue("dateTimeValue"));
    assertEquals("2023-05-15T13:45", result.getValue("dateTimeValueZeroSeconds"));

    // Null value should be present but null
    assertTrue(result.containsKey("nullValue"));
    assertNull(result.getValue("nullValue"));
  }

  @Test
  public void testToJsonWithoutNullsSecondRow() throws IOException {
    FormatJson definition = FormatJson.builder()
            .outputNullValues(false)
            .build();
    FormatJsonInstance instance = new FormatJsonInstance(null, null, definition);
    // Create a test DataRow with different types of data
    Types types = new Types();
    DataRow row = DataRow.create(types,
            "stringValue", "test string",
            "intValue", 42,
            "doubleValue", 3.14,
            "boolValue", true,
            "dateValue", LocalDate.of(2023, 5, 15),
            "timeValue", LocalTime.of(13, 45, 30),
            "dateTimeValue", LocalDateTime.of(2023, 5, 15, 13, 45, 30),
            "dateTimeValueZeroSeconds", LocalDateTime.of(2023, 5, 15, 13, 45, 0),
            "nullValue", null
    );

    // Convert to JSON
    JsonObject result = new JsonObject(instance.toJsonBuffer(row, false));

    // Verify the JSON object contains all expected entries
    assertEquals("test string", result.getString("stringValue"));
    assertEquals(42, result.getInteger("intValue"));
    assertEquals(3.14, result.getDouble("doubleValue"));
    assertEquals(true, result.getBoolean("boolValue"));
    assertFalse(result.containsKey("nullValue"));

    // Date/time values might be formatted as strings
    assertEquals("2023-05-15", result.getValue("dateValue"));
    assertEquals("13:45:30", result.getValue("timeValue"));
    assertEquals("2023-05-15T13:45:30", result.getValue("dateTimeValue"));
    assertEquals("2023-05-15T13:45", result.getValue("dateTimeValueZeroSeconds"));

    // Null value should not be present
    assertFalse(result.containsKey("nullValue"));
    assertNull(result.getValue("nullValue"));
  }

  @Test
  public void testToJsonWithFormats() throws IOException {
    FormatJson definition = FormatJson.builder()
            .dateFormat("d MMMM uuuu")
            .dateTimeFormat("d MMMM uuuu h:mm a")
            .timeFormat("h:mm a")
            .decimalFormat("0.00")
            .build();
    FormatJsonInstance instance = new FormatJsonInstance(null, null, definition);
    // Create a test DataRow with different types of data
    Types types = new Types();
    DataRow row = DataRow.create(types,
            "dateValue", LocalDate.of(2023, 5, 15),
            "timeValue", LocalTime.of(13, 45, 30),
            "dateTimeValue", LocalDateTime.of(2023, 5, 15, 13, 45, 30),
            "decimalValue", 12.0
    );

    // Convert to JSON
    Buffer bufferJson = instance.toJsonBuffer(row, true);
    JsonObject result = new JsonObject(bufferJson);
    String stringJson = bufferJson.toString(StandardCharsets.UTF_8);
    
    assertEquals("{\"dateValue\":\"15 May 2023\",\"timeValue\":\"1:45 pm\",\"dateTimeValue\":\"15 May 2023 1:45 pm\",\"decimalValue\":12.00}", stringJson.replaceAll("PM", "pm"));

    // Date/time values might be formatted as strings
    assertEquals("15 May 2023", result.getValue("dateValue"));
    assertEquals("1:45 pm", ((String) result.getValue("timeValue")).toLowerCase());
    assertEquals("15 May 2023 1:45 pm", ((String) result.getValue("dateTimeValue")).replace("PM", "pm"));
  }

  @Test
  public void testToJsonWithIso8601FormatWithoutZ() throws IOException {
    FormatJson definition = FormatJson.builder()
            .dateFormat("uuuu-MM-dd")
            .dateTimeFormat("uuuu-MM-dd'T'HH:mm:ss")
            .timeFormat("HH:mm:ss")            
            .build();
    FormatJsonInstance instance = new FormatJsonInstance(null, null, definition);
    // Create a test DataRow with different types of data
    Types types = new Types();
    DataRow row = DataRow.create(types,
            "dateValue", LocalDate.of(2023, 5, 15),
            "timeValue", LocalTime.of(13, 45, 30),
            "dateTimeValue", LocalDateTime.of(2023, 5, 15, 13, 45, 30),
            "dateTimeValueNoSecs", LocalDateTime.of(2023, 5, 15, 13, 45, 00)
    );

    // Convert to JSON
    JsonObject result = new JsonObject(instance.toJsonBuffer(row, true));

    // Date/time values might be formatted as strings
    assertEquals("2023-05-15", result.getValue("dateValue"));
    assertEquals("13:45:30", ((String) result.getValue("timeValue")).toLowerCase());
    assertEquals("2023-05-15T13:45:30", ((String) result.getValue("dateTimeValue")));
    assertEquals("2023-05-15T13:45:00", ((String) result.getValue("dateTimeValueNoSecs")));
  }

  @Test
  public void testToJsonWithSecondsSinceEpoch() throws IOException {
    FormatJson definition = FormatJson.builder()
            .dateFormat("d MMMM uuuu")
            .dateTimeFormat("EPOCH_SECONDS")
            .timeFormat("h:mm a")
            .build();
    FormatJsonInstance instance = new FormatJsonInstance(null, null, definition);
    // Create a test DataRow with different types of data
    Types types = new Types();
    DataRow row = DataRow.create(types,
            "dateValue", LocalDate.of(2023, 5, 15),
            "timeValue", LocalTime.of(13, 45, 30),
            "dateTimeValue", LocalDateTime.of(2023, 5, 15, 13, 45, 30)
    );

    // Convert to JSON
    JsonObject result = new JsonObject(instance.toJsonBuffer(row, true));

    // Date/time values might be formatted as strings
    assertEquals("15 May 2023", result.getValue("dateValue"));
    assertEquals("1:45 pm", ((String) result.getValue("timeValue")).toLowerCase());
    assertEquals(1684158330, result.getValue("dateTimeValue"));
  }

  @Test
  public void testToJsonWithMillisecondsSinceEpoch() throws IOException {
    FormatJson definition = FormatJson.builder()
            .dateFormat("d MMMM uuuu")
            .dateTimeFormat("EPOCH_MILLISECONDS")
            .timeFormat("h:mm a")
            .build();
    FormatJsonInstance instance = new FormatJsonInstance(null, null, definition);
    // Create a test DataRow with different types of data
    Types types = new Types();
    DataRow row = DataRow.create(types,
            "dateValue", LocalDate.of(2023, 5, 15),
            "timeValue", LocalTime.of(13, 45, 30),
            "dateTimeValue", LocalDateTime.of(2023, 5, 15, 13, 45, 30)
    );

    // Convert to JSON
    JsonObject result = new JsonObject(instance.toJsonBuffer(row, true));

    // Date/time values might be formatted as strings
    assertEquals("15 May 2023", result.getValue("dateValue"));
    assertEquals("1:45 pm", ((String) result.getValue("timeValue")).toLowerCase());
    assertEquals(1684158330000L, result.getValue("dateTimeValue"));
  }

  @Test
  public void testToJsonWithEmptyRow() throws IOException {
    FormatJson definition = FormatJson.builder()
            .build();
    FormatJsonInstance instance = new FormatJsonInstance(null, null, definition);

    // Test with an empty row
    JsonObject result = new JsonObject(instance.toJsonBuffer(DataRow.EMPTY_ROW, true));

    // Result should be an empty JSON object
    assertEquals(0, result.size());
  }
  
  @Test
  public void testPredefinedFormat() throws IOException {
    ObjectMapperConfiguration.configureObjectMapper(DatabindCodec.mapper());
    
    assertEquals("\"2023-05-15T13:45:30\"", Json.encode(LocalDateTime.of(2023, 5, 15, 13, 45, 30)));
    assertEquals("\"2023-05-15T13:45:30.000123\"", Json.encode(LocalDateTime.of(2023, 5, 15, 13, 45, 30, 123000)));
    assertEquals("\"2023-05-15T13:45:30.123\"", Json.encode(LocalDateTime.of(2023, 5, 15, 13, 45, 30, 123000000)));
    assertEquals("\"2023-05-15T13:45:30.12\"", Json.encode(LocalDateTime.of(2023, 5, 15, 13, 45, 30, 120000000)));
    assertEquals("\"2023-05-15T13:45:30.1\"", Json.encode(LocalDateTime.of(2023, 5, 15, 13, 45, 30, 100000000)));
    assertEquals("\"2023-05-15T13:45:00\"", Json.encode(LocalDateTime.of(2023, 5, 15, 13, 45, 0)));
    
    
    assertEquals("2023-05-15T13:45:30", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.of(2023, 5, 15, 13, 45, 30)));
    assertEquals("2023-05-15T13:45:30.000123", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.of(2023, 5, 15, 13, 45, 30, 123000)));
    assertEquals("2023-05-15T13:45:30.123", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.of(2023, 5, 15, 13, 45, 30, 123000000)));
    assertEquals("2023-05-15T13:45:30.12", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.of(2023, 5, 15, 13, 45, 30, 120000000)));
    assertEquals("2023-05-15T13:45:30.1", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.of(2023, 5, 15, 13, 45, 30, 100000000)));
    assertEquals("2023-05-15T13:45:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.of(2023, 5, 15, 13, 45, 0)));
    
    
    FormatJson definition = FormatJson.builder()
            .dateTimeFormat("ISO_LOCAL_DATE_TIME")
            .build();
    FormatJsonInstance instance = new FormatJsonInstance(null, null, definition);
    // Create a test DataRow with different types of data
    Types types = new Types();
    DataRow row = DataRow.create(types,
            "dateTimeValue", LocalDateTime.of(2023, 5, 15, 13, 45, 30, 120000000)
    );

    // Convert to JSON
    JsonObject result = new JsonObject(instance.toJsonBuffer(row, true));

    // Date/time values might be formatted as strings
    assertEquals("2023-05-15T13:45:30.12", result.getValue("dateTimeValue"));
    
  }
}
