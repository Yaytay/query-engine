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

import io.vertx.core.json.JsonObject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import uk.co.spudsoft.query.defn.FormatJson;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.Types;

/**
 *
 * @author njt
 */
public class FormatJsonInstanceTest {

  @Test
  public void testToJson() {
    FormatJson definition = FormatJson.builder()
            .build();
    FormatJsonInstance instance = new FormatJsonInstance(null, definition);
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
    JsonObject result = instance.toJson(row);

    // Verify the JSON object contains all expected entries
    assertEquals("test string", result.getString("stringValue"));
    assertEquals(42, result.getInteger("intValue"));
    assertEquals(3.14, result.getDouble("doubleValue"));
    assertEquals(true, result.getBoolean("boolValue"));

    // Date/time values might be formatted as strings
    assertEquals("2023-05-15", result.getValue("dateValue"));
    assertEquals("13:45:30", result.getValue("timeValue"));
    assertEquals("2023-05-15T13:45:30Z", result.getValue("dateTimeValue"));
    assertEquals("2023-05-15T13:45:00Z", result.getValue("dateTimeValueZeroSeconds"));

    // Null value should be present but null
    assertTrue(result.containsKey("nullValue"));
    assertNull(result.getValue("nullValue"));
  }

  @Test
  public void testToJsonWithFormats() {
    FormatJson definition = FormatJson.builder()
            .dateFormat("d MMMM uuuu")
            .dateTimeFormat("d MMMM uuuu h:mm a")
            .timeFormat("h:mm a")
            .build();
    FormatJsonInstance instance = new FormatJsonInstance(null, definition);
    // Create a test DataRow with different types of data
    Types types = new Types();
    DataRow row = DataRow.create(types,
            "dateValue", LocalDate.of(2023, 5, 15),
            "timeValue", LocalTime.of(13, 45, 30),
            "dateTimeValue", LocalDateTime.of(2023, 5, 15, 13, 45, 30)
    );

    // Convert to JSON
    JsonObject result = instance.toJson(row);

    // Date/time values might be formatted as strings
    assertEquals("15 May 2023", result.getValue("dateValue"));
    assertEquals("1:45 pm", ((String) result.getValue("timeValue")).toLowerCase());
    assertEquals("15 May 2023 1:45 pm", ((String) result.getValue("dateTimeValue")).replace("PM", "pm"));
  }

  @Test
  public void testToJsonWithIso8601FormatWithoutZ() {
    FormatJson definition = FormatJson.builder()
            .dateFormat("uuuu-MM-dd")
            .dateTimeFormat("uuuu-MM-dd'T'HH:mm:ss")
            .timeFormat("HH:mm:ss")
            .build();
    FormatJsonInstance instance = new FormatJsonInstance(null, definition);
    // Create a test DataRow with different types of data
    Types types = new Types();
    DataRow row = DataRow.create(types,
            "dateValue", LocalDate.of(2023, 5, 15),
            "timeValue", LocalTime.of(13, 45, 30),
            "dateTimeValue", LocalDateTime.of(2023, 5, 15, 13, 45, 30),
            "dateTimeValueNoSecs", LocalDateTime.of(2023, 5, 15, 13, 45, 00)
    );

    // Convert to JSON
    JsonObject result = instance.toJson(row);

    // Date/time values might be formatted as strings
    assertEquals("2023-05-15", result.getValue("dateValue"));
    assertEquals("13:45:30", ((String) result.getValue("timeValue")).toLowerCase());
    assertEquals("2023-05-15T13:45:30", ((String) result.getValue("dateTimeValue")));
    assertEquals("2023-05-15T13:45:00", ((String) result.getValue("dateTimeValueNoSecs")));
  }

  @Test
  public void testToJsonWithSecondsSinceEpoch() {
    FormatJson definition = FormatJson.builder()
            .dateFormat("d MMMM uuuu")
            .dateTimeFormat("EPOCH_SECONDS")
            .timeFormat("h:mm a")
            .build();
    FormatJsonInstance instance = new FormatJsonInstance(null, definition);
    // Create a test DataRow with different types of data
    Types types = new Types();
    DataRow row = DataRow.create(types,
            "dateValue", LocalDate.of(2023, 5, 15),
            "timeValue", LocalTime.of(13, 45, 30),
            "dateTimeValue", LocalDateTime.of(2023, 5, 15, 13, 45, 30)
    );

    // Convert to JSON
    JsonObject result = instance.toJson(row);

    // Date/time values might be formatted as strings
    assertEquals("15 May 2023", result.getValue("dateValue"));
    assertEquals("1:45 pm", ((String) result.getValue("timeValue")).toLowerCase());
    assertEquals(1684158330L, result.getValue("dateTimeValue"));
  }

  @Test
  public void testToJsonWithMillisecondsSinceEpoch() {
    FormatJson definition = FormatJson.builder()
            .dateFormat("d MMMM uuuu")
            .dateTimeFormat("EPOCH_MILLISECONDS")
            .timeFormat("h:mm a")
            .build();
    FormatJsonInstance instance = new FormatJsonInstance(null, definition);
    // Create a test DataRow with different types of data
    Types types = new Types();
    DataRow row = DataRow.create(types,
            "dateValue", LocalDate.of(2023, 5, 15),
            "timeValue", LocalTime.of(13, 45, 30),
            "dateTimeValue", LocalDateTime.of(2023, 5, 15, 13, 45, 30)
    );

    // Convert to JSON
    JsonObject result = instance.toJson(row);

    // Date/time values might be formatted as strings
    assertEquals("15 May 2023", result.getValue("dateValue"));
    assertEquals("1:45 pm", ((String) result.getValue("timeValue")).toLowerCase());
    assertEquals(1684158330000L, result.getValue("dateTimeValue"));
  }

  @Test
  public void testToJsonWithEmptyRow() {
    FormatJson definition = FormatJson.builder()
            .build();
    FormatJsonInstance instance = new FormatJsonInstance(null, definition);

    // Test with an empty row
    JsonObject result = instance.toJson(DataRow.EMPTY_ROW);

    // Result should be an empty JSON object
    assertEquals(0, result.size());
  }
}
