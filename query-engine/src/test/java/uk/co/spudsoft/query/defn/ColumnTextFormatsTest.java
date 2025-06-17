/*
 * Copyright (C) 2025 jtalbut
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
package uk.co.spudsoft.query.defn;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * Unit tests for ColumnTextFormats class.
 */
public class ColumnTextFormatsTest {

  @Test
  public void testBuilderAndGetters() {
    // Test builder pattern and all getters
    ColumnTextFormats formats = ColumnTextFormats.builder()
            .column("testColumn")
            .dateFormat("dd/MM/yyyy")
            .dateTimeFormat("dd/MM/yyyy HH:mm:ss")
            .timeFormat("HH:mm:ss")
            .decimalFormat("#,##0.00")
            .booleanFormat("['true', 'false']")
            .build();

    assertEquals("testColumn", formats.getColumn());
    assertEquals("dd/MM/yyyy", formats.getDateFormat());
    assertEquals("dd/MM/yyyy HH:mm:ss", formats.getDateTimeFormat());
    assertEquals("HH:mm:ss", formats.getTimeFormat());
    assertEquals("#,##0.00", formats.getDecimalFormat());
    assertEquals("['true', 'false']", formats.getBooleanFormat());
  }

  @Test
  public void testBuilderFluentInterface() {
    // Test that builder methods return the same builder instance for fluent chaining
    ColumnTextFormats.Builder builder = ColumnTextFormats.builder();

    assertSame(builder, builder.column("test"));
    assertSame(builder, builder.dateFormat("yyyy-MM-dd"));
    assertSame(builder, builder.dateTimeFormat("yyyy-MM-dd HH:mm:ss"));
    assertSame(builder, builder.timeFormat("HH:mm:ss"));
    assertSame(builder, builder.decimalFormat("0.00"));
    assertSame(builder, builder.booleanFormat("['true', 'false']"));
  }

  @Test
  public void testValidateWithValidFormats() {
    // Test validation with all valid formats
    ColumnTextFormats formats = ColumnTextFormats.builder()
            .column("testColumn")
            .dateFormat("yyyy-MM-dd")
            .dateTimeFormat("yyyy-MM-dd HH:mm:ss")
            .timeFormat("HH:mm:ss")
            .decimalFormat("#,##0.00")
            .booleanFormat("['\"Yes\"', '\"No\"']")
            .build();

    // Should not throw any exception
    assertDoesNotThrow(() -> formats.validate("\"", "\""));
  }

  @Test
  public void testValidateWithSingleFormat() {
    // Test validation with only one format specified
    ColumnTextFormats formats = ColumnTextFormats.builder()
            .column("testColumn")
            .dateFormat("yyyy-MM-dd")
            .build();

    assertDoesNotThrow(() -> formats.validate(null, null));
  }

  @Test
  public void testValidateFailsWithNullColumn() {
    ColumnTextFormats formats = ColumnTextFormats.builder()
            .dateFormat("yyyy-MM-dd")
            .build();

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> formats.validate(null, null)
    );
    assertEquals("No column specified for column value formatters", exception.getMessage());
  }

  @Test
  public void testValidateFailsWithEmptyColumn() {
    ColumnTextFormats formats = ColumnTextFormats.builder()
            .column("")
            .dateFormat("yyyy-MM-dd")
            .build();

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> formats.validate(null, null)
    );
    assertEquals("No column specified for column value formatters", exception.getMessage());
  }

  @Test
  public void testValidateFailsWithNoFormats() {
    ColumnTextFormats formats = ColumnTextFormats.builder()
            .column("testColumn")
            .build();

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> formats.validate(null, null)
    );
    assertEquals("No formats specified for column \"testColumn\"", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {"invalid-pattern"})
  public void testValidateWithInvalidDateFormat(String invalidDateFormat) {
    ColumnTextFormats formats = ColumnTextFormats.builder()
            .column("testColumn")
            .dateFormat(invalidDateFormat)
            .build();

    if (invalidDateFormat.equals("dd/MM/yyyy HH:mm:ss")) {
      // This should actually be valid for date format
      assertDoesNotThrow(() -> formats.validate(null, null));
    } else {
      IllegalArgumentException exception = assertThrows(
              IllegalArgumentException.class,
              () -> formats.validate(null, null)
      );
      assertThat(exception.getMessage(), containsString("Invalid dateFormat"));
    }
  }

  @Test
  public void testValidateWithInvalidTimeFormat() {
    ColumnTextFormats formats = ColumnTextFormats.builder()
            .column("testColumn")
            .timeFormat("invalid-time-pattern")
            .build();

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> formats.validate(null, null)
    );
    assertThat(exception.getMessage(), containsString("Invalid timeFormat"));
  }

  @Test
  public void testValidateWithInvalidDecimalFormat() {
    ColumnTextFormats formats = ColumnTextFormats.builder()
            .column("testColumn")
            .decimalFormat("invalid%decimal%pattern")
            .build();

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> formats.validate(null, null)
    );
    assertThat(exception.getMessage(), containsString("Invalid decimalFormat"));
  }

  @Test
  public void testValidateWithInvalidBooleanFormat() {
    ColumnTextFormats formats = ColumnTextFormats.builder()
            .column("testColumn")
            .booleanFormat("invalid-boolean-format")
            .build();

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> formats.validate(null, null)
    );
    assertThat(exception.getMessage(), containsString("Invalid booleanFormat"));
  }

  @Test
  public void testValidateWithInvalidDateTimeFormat() {
    ColumnTextFormats formats = ColumnTextFormats.builder()
            .column("testColumn")
            .dateTimeFormat("invalid-datetime-pattern")
            .build();

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> formats.validate(null, null)
    );
    assertThat(exception.getMessage(), containsString("Invalid dateTimeFormat"));
  }

  @ParameterizedTest
  @CsvSource({
    "yyyy-MM-dd, true",
    "dd/MM/yyyy, true",
    "MM-dd-yyyy, true",
    "invalid, false"
  })
  public void testValidDateFormatPatterns(String pattern, boolean shouldBeValid) {
    ColumnTextFormats formats = ColumnTextFormats.builder()
            .column("testColumn")
            .dateFormat(pattern)
            .build();

    if (shouldBeValid) {
      assertDoesNotThrow(() -> formats.validate(null, null));
    } else {
      assertThrows(IllegalArgumentException.class, () -> formats.validate(null, null));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "HH:mm:ss, true",
    "HH:mm, true",
    "HH.mm.ss, true",
    "invalid, false"
  })
  public void testValidTimeFormatPatterns(String pattern, boolean shouldBeValid) {
    ColumnTextFormats formats = ColumnTextFormats.builder()
            .column("testColumn")
            .timeFormat(pattern)
            .build();

    if (shouldBeValid) {
      assertDoesNotThrow(() -> formats.validate(null, null));
    } else {
      assertThrows(IllegalArgumentException.class, () -> formats.validate(null, null));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "0.000, true",
    "#.##, true",
    "inv%%alid, false"
  })
  public void testValidDecimalFormatPatterns(String pattern, boolean shouldBeValid) {
    ColumnTextFormats formats = ColumnTextFormats.builder()
            .column("testColumn")
            .decimalFormat(pattern)
            .build();

    if (shouldBeValid) {
      assertDoesNotThrow(() -> formats.validate(null, null));
    } else {
      assertThrows(IllegalArgumentException.class, () -> formats.validate(null, null));
    }
  }

  @Test
  public void testValidateWithPredefinedDateTimeFormats() {
    // Test with predefined datetime formats
    String[] predefinedFormats = {
      "ISO_LOCAL_DATE_TIME",
      "ISO_OFFSET_DATE_TIME",
      "EPOCH_SECONDS",
      "EPOCH_MILLISECONDS"
    };

    for (String format : predefinedFormats) {
      ColumnTextFormats formats = ColumnTextFormats.builder()
              .column("testColumn")
              .dateTimeFormat(format)
              .build();

      assertDoesNotThrow(() -> formats.validate(null, null),
              "Format " + format + " should be valid");
    }
  }

  @Test
  public void testValidateWithNullFormats() {
    // Test that null formats are handled correctly
    ColumnTextFormats formats = ColumnTextFormats.builder()
            .column("testColumn")
            .dateFormat(null)
            .dateTimeFormat(null)
            .timeFormat(null)
            .decimalFormat(null)
            .booleanFormat("['true', 'false']") // At least one format must be specified
            .build();

    assertDoesNotThrow(() -> formats.validate(null, null));
  }

  @Test
  public void testValidateWithBooleanQuotes() {
    ColumnTextFormats formats = ColumnTextFormats.builder()
            .column("testColumn")
            .booleanFormat("['\"Yes\"', '\"No\"']")
            .build();

    // Should work with quotes
    assertDoesNotThrow(() -> formats.validate("\"", "\""));

    // Should also work without quotes in validation
    assertDoesNotThrow(() -> formats.validate(null, null));
  }

  @Test
  public void testBuilderWithNullValues() {
    // Test that builder handles null values correctly
    ColumnTextFormats formats = ColumnTextFormats.builder()
            .column("testColumn")
            .dateFormat(null)
            .dateTimeFormat(null)
            .timeFormat(null)
            .decimalFormat(null)
            .booleanFormat(null)
            .build();

    assertEquals("testColumn", formats.getColumn());
    assertNull(formats.getDateFormat());
    assertNull(formats.getDateTimeFormat());
    assertNull(formats.getTimeFormat());
    assertNull(formats.getDecimalFormat());
    assertNull(formats.getBooleanFormat());
  }

  @Test
  public void testValidateWithMixedFormats() {
    // Test with a mix of valid and null formats
    ColumnTextFormats formats = ColumnTextFormats.builder()
            .column("testColumn")
            .dateFormat("yyyy-MM-dd")
            .dateTimeFormat(null)
            .timeFormat("HH:mm:ss")
            .decimalFormat(null)
            .booleanFormat("['true', 'false']")
            .build();

    assertDoesNotThrow(() -> formats.validate(null, null));
  }

  @Test
  public void testValidateErrorMessages() {
    // Test that validation error messages are informative
    ColumnTextFormats formats = ColumnTextFormats.builder()
            .column("testColumn")
            .dateFormat("invalid-date-format")
            .build();

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> formats.validate(null, null)
    );

    assertThat(exception.getMessage(), containsString("Invalid dateFormat"));
    assertThat(exception.getMessage(), containsString("Unknown pattern letter: i"));
  }
}
