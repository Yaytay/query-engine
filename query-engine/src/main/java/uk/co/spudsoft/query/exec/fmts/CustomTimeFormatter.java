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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A helper class to work with DateTimeFormatters for LocalTime values, allowing them to work without a pattern.
 *
 * @author jtalbut
 */
public class CustomTimeFormatter implements CustomFormatter {
  
  private static final Logger logger = LoggerFactory.getLogger(CustomTimeFormatter.class);
  
  private final DateTimeFormatter formatter;
  
  /**
   * Constructor.
   * 
   * @param format a DateTimeFormatter pattern.
   */
  public CustomTimeFormatter(String format) {
    if (Strings.isNullOrEmpty(format)) {
      formatter = null;
    } else {
      formatter = DateTimeFormatter.ofPattern(format);
    }
  }
  
  /**
   * Format the time value according to the configured formatter.
   * 
   * @param value the time value to be formatted.
   * @return The formatted value.
   */
  @Override
  public String format(Object value) {
    if (value == null) {
      return null;
    } else {
      if (value instanceof LocalTime lt) {
        if (formatter == null) {
          return value.toString();
        } else {
          return formatter.format(lt);
        }
      } else {
        logger.warn("Value {} of type {} passed to CustomDateFormatter", value, value.getClass());
        return value.toString();
      }
    }
  }

}
