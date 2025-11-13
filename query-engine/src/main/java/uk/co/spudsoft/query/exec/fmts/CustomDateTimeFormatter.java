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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.logging.Log;

/**
 * A helper class to work with DateTimeFormatters allowing them to be accessed by name as well as by pattern.
 *
 * Also supports EPOCH_SECONDS and EPOCH_MILLISECONDS.
 *
 * @author jtalbut
 */
public class CustomDateTimeFormatter {

  private static final Logger logger = LoggerFactory.getLogger(CustomDateTimeFormatter.class);
  
  private final DateTimeFormatter formatter;  
  private final boolean convertToUtcZone;
  private final boolean dateTimeAsEpochSeconds;
  private final boolean dateTimeAsEpochMillis;
  private final boolean customTrimSeconds;

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
      customTrimSeconds = false;
    } else {
      switch (format) {
        case "DEFAULT":
          formatter = null;
          convertToUtcZone = false;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          customTrimSeconds = false;
          break;

        case "EPOCH_SECONDS":
          formatter = null;
          convertToUtcZone = false;
          dateTimeAsEpochSeconds = true;
          dateTimeAsEpochMillis = false;
          customTrimSeconds = false;
          break;

        case "EPOCH_MILLISECONDS":
          formatter = null;
          convertToUtcZone = false;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = true;
          customTrimSeconds = false;
          break;

        case "BASIC_ISO_DATE":
          formatter = DateTimeFormatter.BASIC_ISO_DATE;
          convertToUtcZone = false;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          customTrimSeconds = false;
          break;

        case "ISO_LOCAL_DATE":
          formatter = DateTimeFormatter.ISO_LOCAL_DATE;
          convertToUtcZone = false;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          customTrimSeconds = false;
          break;

        case "ISO_DATE":
          formatter = DateTimeFormatter.ISO_DATE;
          convertToUtcZone = false;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          customTrimSeconds = false;
          break;

        case "ISO_LOCAL_TIME":
          formatter = DateTimeFormatter.ISO_LOCAL_TIME;
          convertToUtcZone = false;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          customTrimSeconds = false;
          break;

        case "ISO_TIME":
          formatter = DateTimeFormatter.ISO_TIME;
          convertToUtcZone = false;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          customTrimSeconds = false;
          break;

        case "ISO_LOCAL_DATE_TIME":
          formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
          convertToUtcZone = false;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          customTrimSeconds = false;
          break;

        case "ISO_ORDINAL_DATE":
          formatter = DateTimeFormatter.ISO_ORDINAL_DATE;
          convertToUtcZone = false;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          customTrimSeconds = false;
          break;

        case "ISO_WEEK_DATE":
          formatter = DateTimeFormatter.ISO_WEEK_DATE;
          convertToUtcZone = false;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          customTrimSeconds = false;
          break;

        case "ISO_OFFSET_DATE":
          formatter = DateTimeFormatter.ISO_OFFSET_DATE;
          convertToUtcZone = true;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          customTrimSeconds = false;
          break;

        case "ISO_OFFSET_TIME":
          formatter = DateTimeFormatter.ISO_OFFSET_TIME;
          convertToUtcZone = true;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          customTrimSeconds = false;
          break;

        case "ISO_OFFSET_DATE_TIME":
          formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
          convertToUtcZone = true;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          customTrimSeconds = false;
          break;

        case "ISO_ZONED_DATE_TIME":
          formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
          convertToUtcZone = true;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          customTrimSeconds = false;
          break;

        case "ISO_DATE_TIME":
          formatter = DateTimeFormatter.ISO_DATE_TIME;
          convertToUtcZone = true;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          customTrimSeconds = false;
          break;

        case "ISO_INSTANT":
          formatter = DateTimeFormatter.ISO_INSTANT;
          convertToUtcZone = true;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          customTrimSeconds = false;
          break;

        case "RFC_1123_DATE_TIME":
          formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
          convertToUtcZone = true;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          customTrimSeconds = false;
          break;

        case "ISO_LOCAL_DATE_TIME_TRIM":
          formatter = null;
          convertToUtcZone = false;
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          customTrimSeconds = true;
          break;

        default:
          formatter = DateTimeFormatter.ofPattern(format);
          convertToUtcZone = format.matches(".*[VzOXxZ].*");
          dateTimeAsEpochSeconds = false;
          dateTimeAsEpochMillis = false;
          customTrimSeconds = false;
          break;
      }
    }
  }

  /**
   * Format the date/time value according to the configured formatter.
   *
   * @param pipelineContext The context in which this {@link SourcePipeline} is being run.
   * @param value the date/time value to be formatted.
   * @return The formatted value, which will be either a String or a Long.
   */
  public Object format(PipelineContext pipelineContext, Object value) {
    if (value == null) {
      return null;
    } else {
      if (value instanceof LocalDateTime ldt) {
        if (dateTimeAsEpochMillis) {
          return ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
        } else if (dateTimeAsEpochSeconds) {
          return ldt.toEpochSecond(ZoneOffset.UTC);
        } else if (customTrimSeconds) {
          StringBuilder sb = new StringBuilder();
          sb.append(
                  String.format("%04d-%02d-%02dT%02d:%02d"
                          , ldt.getYear()
                          , ldt.getMonthValue()
                          , ldt.getDayOfMonth()
                          , ldt.getHour()
                          , ldt.getMinute()
                  )
          );

          if (ldt.getSecond() != 0 || ldt.getNano() != 0) {
            sb.append(String.format(":%02d", ldt.getSecond()));
            if (ldt.getNano() != 0) {
              String nanos = String.format("%09d", ldt.getNano()).replaceAll("0+$", "");
              sb.append('.').append(nanos);
            }
          }
          return sb.toString();
        } else if (formatter == null) {
          return value.toString();
        } else if (convertToUtcZone) {
          ZonedDateTime zoned = ldt.atZone(ZoneOffset.UTC);
          return formatter.format(zoned);
        } else {
          return formatter.format(ldt);
        }
      } else {
        Log.decorate(logger.atWarn(), pipelineContext).log("Value {} of type {} passed to CustomDateTimeFormatter", value, value.getClass());
        return value.toString();
      }
    }
  }

}
