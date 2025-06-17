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
package uk.co.spudsoft.query.exec.fmts;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Arrays;
import uk.co.spudsoft.query.defn.ColumnTextFormats;

/**
 * Unit tests for ValueFormatters class.
 */
public class ValueFormattersTest {

  private ValueFormatters defaultFormatters;
  private ValueFormatters formattersWithColumnSpecific;

  @BeforeEach
  void setUp() {
    // Create default formatters
    defaultFormatters = new ValueFormatters(
      "yyyy-MM-dd",           // dateFormat
      "yyyy-MM-dd HH:mm:ss",  // dateTimeFormat
      "HH:mm:ss",             // timeFormat
      "#,##0.00",             // decimalFormat
      null,                   // booleanFormat
      "\"",                   // openQuote
      "\"",                   // closeQuote
      true,                   // booleanLowerCaseOnly
      List.of()               // columnSpecificFormats
    );

    // Create column-specific formats
    ColumnTextFormats columnFormat1 = ColumnTextFormats.builder()
            .column("specialDate")
            .dateFormat("dd/MM/yyyy")
            .decimalFormat("#,##0.000")
            .build();

    ColumnTextFormats columnFormat2 = ColumnTextFormats.builder()
            .column("customTime")
            .timeFormat("HH:mm")
            .booleanFormat("[\"'yes'\",\"'no'\"]")
            .build();

    formattersWithColumnSpecific = new ValueFormatters(
      "yyyy-MM-dd",           // dateFormat
      "yyyy-MM-dd HH:mm:ss",  // dateTimeFormat
      "HH:mm:ss",             // timeFormat
      "#,##0.00",             // decimalFormat
      null,                   // booleanFormat
      "'",                    // openQuote
      "'",                    // closeQuote
      false,                  // booleanLowerCaseOnly
      Arrays.asList(columnFormat1, columnFormat2)
    );
  }

  @Test
  void testDefaultDateFormatter() {
    CustomDateFormatter formatter = defaultFormatters.getDateFormatter("anyColumn");
    assertNotNull(formatter);

    // Test that same column returns same instance
    CustomDateFormatter formatter2 = defaultFormatters.getDateFormatter("anyColumn");
    assertSame(formatter, formatter2);

    // Test different column returns same default instance
    CustomDateFormatter formatter3 = defaultFormatters.getDateFormatter("differentColumn");
    assertSame(formatter, formatter3);
  }

  @Test
  void testDefaultDateTimeFormatter() {
    CustomDateTimeFormatter formatter = defaultFormatters.getDateTimeFormatter("anyColumn");
    assertNotNull(formatter);

    CustomDateTimeFormatter formatter2 = defaultFormatters.getDateTimeFormatter("otherColumn");
    assertSame(formatter, formatter2);
  }

  @Test
  void testDefaultTimeFormatter() {
    CustomTimeFormatter formatter = defaultFormatters.getTimeFormatter("anyColumn");
    assertNotNull(formatter);

    CustomTimeFormatter formatter2 = defaultFormatters.getTimeFormatter("otherColumn");
    assertSame(formatter, formatter2);
  }

  @Test
  void testDefaultDecimalFormatter() {
    CustomDecimalFormatter formatter = defaultFormatters.getDecimalFormatter("anyColumn");
    assertNotNull(formatter);

    CustomDecimalFormatter formatter2 = defaultFormatters.getDecimalFormatter("otherColumn");
    assertSame(formatter, formatter2);
  }

  @Test
  void testDefaultBooleanFormatter() {
    CustomBooleanFormatter formatter = defaultFormatters.getBooleanFormatter("anyColumn");
    assertNotNull(formatter);

    CustomBooleanFormatter formatter2 = defaultFormatters.getBooleanFormatter("otherColumn");
    assertSame(formatter, formatter2);
  }

  @Test
  void testColumnSpecificDateFormatter() {
    // Test column with specific format
    CustomDateFormatter specialFormatter = formattersWithColumnSpecific.getDateFormatter("specialDate");
    assertNotNull(specialFormatter);

    // Test column without specific format uses default
    CustomDateFormatter defaultFormatter = formattersWithColumnSpecific.getDateFormatter("regularColumn");
    assertNotNull(defaultFormatter);

    // Verify they are different instances (column-specific vs default)
    assertNotSame(specialFormatter, defaultFormatter);
  }

  @Test
  void testColumnSpecificTimeFormatter() {
    // Test column with specific format
    CustomTimeFormatter customFormatter = formattersWithColumnSpecific.getTimeFormatter("customTime");
    assertNotNull(customFormatter);

    // Test column without specific format uses default
    CustomTimeFormatter defaultFormatter = formattersWithColumnSpecific.getTimeFormatter("regularColumn");
    assertNotNull(defaultFormatter);

    // Verify they are different instances
    assertNotSame(customFormatter, defaultFormatter);
  }

  @Test
  void testColumnSpecificDecimalFormatter() {
    // Test column with specific format
    CustomDecimalFormatter specialFormatter = formattersWithColumnSpecific.getDecimalFormatter("specialDate");
    assertNotNull(specialFormatter);

    // Test column without specific format uses default
    CustomDecimalFormatter defaultFormatter = formattersWithColumnSpecific.getDecimalFormatter("regularColumn");
    assertNotNull(defaultFormatter);

    // Verify they are different instances
    assertNotSame(specialFormatter, defaultFormatter);
  }

  @Test
  void testColumnSpecificBooleanFormatter() {
    // Test column with specific format
    CustomBooleanFormatter customFormatter = formattersWithColumnSpecific.getBooleanFormatter("customTime");
    assertNotNull(customFormatter);

    // Test column without specific format uses default
    CustomBooleanFormatter defaultFormatter = formattersWithColumnSpecific.getBooleanFormatter("regularColumn");
    assertNotNull(defaultFormatter);

    // Verify they are different instances
    assertNotSame(customFormatter, defaultFormatter);
  }

  @Test
  void testNullFormats() {
    ValueFormatters nullFormatters = new ValueFormatters(
      null,  // dateFormat
      null,  // dateTimeFormat
      null,  // timeFormat
      null,  // decimalFormat
      null,  // booleanFormat
      "\"",  // openQuote
      "\"",  // closeQuote
      true,  // booleanLowerCaseOnly
      List.of()
    );

    // All formatters should still be created (they handle null formats internally)
    assertNotNull(nullFormatters.getDateFormatter("column"));
    assertNotNull(nullFormatters.getDateTimeFormatter("column"));
    assertNotNull(nullFormatters.getTimeFormatter("column"));
    assertNotNull(nullFormatters.getDecimalFormatter("column"));
    assertNotNull(nullFormatters.getBooleanFormatter("column"));
  }

  @Test
  void testEmptyColumnSpecificFormats() {
    ValueFormatters emptyFormatters = new ValueFormatters(
      "yyyy-MM-dd",
      "yyyy-MM-dd HH:mm:ss",
      "HH:mm:ss",
      "#,##0.00",
      "['true','false']",
      "\"",
      "\"",
      true,
      List.of()  // Empty list
    );

    // Should behave same as default formatters
    CustomDateFormatter formatter1 = emptyFormatters.getDateFormatter("column1");
    CustomDateFormatter formatter2 = emptyFormatters.getDateFormatter("column2");
    assertSame(formatter1, formatter2);
  }

  @Test
  void testMultipleColumnSpecificFormats() {
    ColumnTextFormats format1 = ColumnTextFormats.builder()
            .column("col1")
            .dateFormat("dd-MM-yyyy")
            .build();

    ColumnTextFormats format2 = ColumnTextFormats.builder()
            .column("col2")
            .timeFormat("HH:mm:ss.SSS")
            .build();

    ColumnTextFormats format3 = ColumnTextFormats.builder()
            .column("col3")
            .decimalFormat("0.####")
            .build();

    ValueFormatters multiFormatters = new ValueFormatters(
      "yyyy-MM-dd",
      "yyyy-MM-dd HH:mm:ss",
      "HH:mm:ss",
      "#,##0.00",
      "['true','false']",
      "\"",
      "\"",
      true,
      Arrays.asList(format1, format2, format3)
    );

    // Each column should have its specific formatter
    CustomDateFormatter dateFormatter1 = multiFormatters.getDateFormatter("col1");
    CustomTimeFormatter timeFormatter2 = multiFormatters.getTimeFormatter("col2");
    CustomDecimalFormatter decimalFormatter3 = multiFormatters.getDecimalFormatter("col3");

    // Verify they are different from default
    CustomDateFormatter defaultDateFormatter = multiFormatters.getDateFormatter("col4");
    CustomTimeFormatter defaultTimeFormatter = multiFormatters.getTimeFormatter("col4");
    CustomDecimalFormatter defaultDecimalFormatter = multiFormatters.getDecimalFormatter("col4");

    assertNotSame(dateFormatter1, defaultDateFormatter);
    assertNotSame(timeFormatter2, defaultTimeFormatter);
    assertNotSame(decimalFormatter3, defaultDecimalFormatter);
  }
}
