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

import com.google.common.base.Strings;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A helper class to work with DateTimeFormatters allowing them to be accessed by name as well as by pattern.
 * 
 * Also supports EPOCH_SECONDS and EPOCH_MILLISECONDS.
 * 
 * @author jtalbut
 */
public class CustomDateTimeFormatter {
  
  private final DateTimeFormatter formatter;
  private final boolean convertToUtcZone;
  private final boolean dateTimeAsEpochSeconds;
  private final boolean dateTimeAsEpochMillis;

  /**
   * Constructor.
   * 
   * @param format Either a DateTimeFormatter pattern or the name of a predefined DateTimeFormatter (excluding zoned or offset ones) or EPOCH_SECONDS and EPOCH_MILLISECONDS.
   */
  public CustomDateTimeFormatter(String format) {
    
    if (Strings.isNullOrEmpty(format)) {
      formatter = null;
      convertToUtcZone = false;
      dateTimeAsEpochSeconds = false;
      dateTimeAsEpochMillis = false;
    } else {
      switch (format) {
        case "EPOCH_SECONDS":
          formatter = null;
          convertToUtcZone = false;
          dateTimeAsEpochSeconds = true;
          dateTimeAsEpochMillis = false;
          break;
          
        case "EPOCH_MILLISECONDS":
          formatter = null;
          convertToUtcZone = false;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = true;
          break;
          
        case "BASIC_ISO_DATE":
          formatter = DateTimeFormatter.BASIC_ISO_DATE;
          convertToUtcZone = false;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          break;
          
        case "ISO_LOCAL_DATE":
          formatter = DateTimeFormatter.ISO_LOCAL_DATE;
          convertToUtcZone = false;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          break;
          
        case "ISO_DATE":
          formatter = DateTimeFormatter.ISO_DATE;
          convertToUtcZone = false;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          break;
          
        case "ISO_LOCAL_TIME":
          formatter = DateTimeFormatter.ISO_LOCAL_TIME;
          convertToUtcZone = false;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          break;
          
        case "ISO_TIME":
          formatter = DateTimeFormatter.ISO_TIME;
          convertToUtcZone = false;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          break;
          
        case "ISO_LOCAL_DATE_TIME":
          formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
          convertToUtcZone = false;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          break;
          
        case "ISO_ORDINAL_DATE":
          formatter = DateTimeFormatter.ISO_ORDINAL_DATE;
          convertToUtcZone = false;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          break;
          
        case "ISO_WEEK_DATE":
          formatter = DateTimeFormatter.ISO_WEEK_DATE;
          convertToUtcZone = false;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          break;
          
        case "ISO_OFFSET_DATE":
          formatter = DateTimeFormatter.ISO_OFFSET_DATE;
          convertToUtcZone = true;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          break;
          
        case "ISO_OFFSET_TIME":
          formatter = DateTimeFormatter.ISO_OFFSET_TIME;
          convertToUtcZone = true;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          break;
          
        case "ISO_OFFSET_DATE_TIME":
          formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
          convertToUtcZone = true;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          break;
          
        case "ISO_ZONED_DATE_TIME":
          formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
          convertToUtcZone = true;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          break;
          
        case "ISO_DATE_TIME":
          formatter = DateTimeFormatter.ISO_DATE_TIME;
          convertToUtcZone = true;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          break;
          
        case "ISO_INSTANT":
          formatter = DateTimeFormatter.ISO_INSTANT;
          convertToUtcZone = true;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          break;
          
        case "RFC_1123_DATE_TIME":
          formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
          convertToUtcZone = true;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          break;
        
        default:
          formatter = DateTimeFormatter.ofPattern(format);
          convertToUtcZone = format.matches(".*[VzOXxZ].*");
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          break;
      }
    }
  }
  
  /**
   * Format the date/time value according to the configured formatter.
   * 
   * @param value the date/time value to be formatted.
   * @return The formatted value, which will be either a String or a Long.
   */
  public Object format(LocalDateTime value) {
    if (value == null) {
      return null;
    } else {
      if (dateTimeAsEpochMillis) {
        return value.toInstant(ZoneOffset.UTC).toEpochMilli();
      } else if (dateTimeAsEpochSeconds) {
        return value.toEpochSecond(ZoneOffset.UTC);
      } else if (formatter == null) {
        return value.toString();
      } else if (convertToUtcZone) {
        ZonedDateTime zoned = value.atZone(ZoneOffset.UTC);
        return formatter.format(zoned);
      } else {
        return formatter.format(value);
      }
    }
  }
  
}
