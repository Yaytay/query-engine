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
import java.text.DecimalFormat;

/**
 * A helper class to work with DateTimeFormatters allowing them to be accessed by name as well as by pattern.
 *
 * Also supports EPOCH_SECONDS and EPOCH_MILLISECONDS.
 *
 * @author jtalbut
 */
public class CustomDecimalFormatter {

  private final DecimalFormat formatter;
  private final boolean mustBeEncodedAsString;

  /**
   * Constructor.
   *
   * @param format Either a DateTimeFormatter pattern or the name of a predefined DateTimeFormatter (excluding zoned or offset
   * ones) or EPOCH_SECONDS and EPOCH_MILLISECONDS.
   */
  public CustomDecimalFormatter(String format) {

    if (Strings.isNullOrEmpty(format)) {
      formatter = null;
      mustBeEncodedAsString = false;
    } else {
      formatter = new DecimalFormat(format);
      mustBeEncodedAsString = mustBeEncodedAsString(format);
    }
  }

  
  
  private boolean mustBeEncodedAsString(String format) {

    int dashCount = 0;
    int eCount = 0;
    int semicolonCount = 0;
    for (int i = 0; i < format.length();) {
      int codePoint = format.codePointAt(i);
      switch (codePoint) {
        case (int) '0':
        case (int) '#':
        case (int) '.':
          break ;
        case (int) '-':
          if (++dashCount > 1) {
            return true;
          } 
          break;
        case (int) ';':
          if (++semicolonCount > 1) {
            return true;
          } 
          break;
        case (int) 'E':
        case (int) 'e':
          if (++eCount > 1) {
            return true;
          } 
          break;
        default:
          return true;
      }
      i += Character.charCount(codePoint);
    }
    
    return false;
  }

  /**
   * Return true if the output from the format pattern might contain things other than numbers.
   *
   * @return true if the output from the format pattern might contain things other than numbers.
   */
  public boolean mustBeEncodedAsString() {
    return mustBeEncodedAsString;
  }

  /**
   * Format the date/time value according to the configured formatter.
   *
   * @param value the date/time value to be formatted.
   * @return The formatted value.
   */
  public String format(Number value) {
    if (value == null) {
      return null;
    } else {
      if (formatter == null) {
        return value.toString();
      } else {
        return formatter.format(value);
      }
    }
  }

}
