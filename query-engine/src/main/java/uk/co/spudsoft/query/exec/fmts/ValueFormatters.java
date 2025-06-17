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

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import uk.co.spudsoft.query.defn.ColumnTextFormats;

/**
 * Collection of objects for formatting values for a Query Engine output format.
 * @author jtalbut
 */
public class ValueFormatters {
  
  private static class Formatters {
    final CustomDateFormatter dateFormatter;
    final CustomDateTimeFormatter dateTimeFormatter;
    final CustomTimeFormatter timeFormatter;
    final CustomDecimalFormatter decimalFormatter;
    private final CustomBooleanFormatter booleanFormatter;

    Formatters(String dateFormat, String dateTimeFormat, String timeFormat, String decimalFormat, String booleanFormat, String openQuote, String closeQuote, boolean booleanLowerCaseOnly) {
      this.dateFormatter = new CustomDateFormatter(dateFormat);
      this.dateTimeFormatter = new CustomDateTimeFormatter(dateTimeFormat);
      this.timeFormatter = new CustomTimeFormatter(timeFormat);
      this.decimalFormatter = new CustomDecimalFormatter(decimalFormat);
      this.booleanFormatter = new CustomBooleanFormatter(booleanFormat, openQuote, closeQuote, booleanLowerCaseOnly);
    }        
  }
  
  private final Formatters defaultFormatters;
  private final Map<String, Formatters> columnSpecificFormatters;

  /**
   * Constructor.
   * @param dateFormat The default format to use for dates (may be null).
   * @param dateTimeFormat The default format to use for date/time values (may be null).
   * @param timeFormat The default format to use for times (may be null).
   * @param decimalFormat The default format to use for decimal values (may be null).
   * @param booleanFormat The default format to use for Boolean values (may be null).
   * @param openQuote The string to require at the beginning of Boolean values that are not numeric or true/false.
   * @param closeQuote The string to require at the end of Boolean values that are not numeric or true/false.
   * @param booleanLowerCaseOnly If true then Boolean values are only permitted without quotes if they are lowercase true/false (i.e. when false True is a valid value).
   * @param columnSpecificFormats Formats for specific columns.
   */
  public ValueFormatters(String dateFormat, String dateTimeFormat, String timeFormat, String decimalFormat, String booleanFormat, String openQuote, String closeQuote, boolean booleanLowerCaseOnly
          , List<ColumnTextFormats> columnSpecificFormats) {
    
    this.defaultFormatters = new Formatters(dateFormat, dateTimeFormat, timeFormat, decimalFormat, booleanFormat, openQuote, closeQuote, booleanLowerCaseOnly);
    
    ImmutableMap.Builder<String, Formatters> mapBuilder = ImmutableMap.builder();
    
    columnSpecificFormats.forEach(ctf -> {
      mapBuilder.put(
              ctf.getColumn()
              , new Formatters(ctf.getDateFormat(), ctf.getDateTimeFormat(), ctf.getTimeFormat(), ctf.getDecimalFormat(), ctf.getBooleanFormat(), openQuote, closeQuote, booleanLowerCaseOnly)
      );
    });
    
    this.columnSpecificFormatters = mapBuilder.build();
  }

  private Formatters forColumn(String column) {
    Formatters formatters = columnSpecificFormatters.get(column);
    if (formatters == null) {
      return defaultFormatters;
    } else {
      return formatters;
    }
  }
  
  /**
   * Get the date formatter to use for the given column.
   * @param column The column whose value is to be formatted.
   * @return the date formatter to use for the given column.
   */
  public CustomDateFormatter getDateFormatter(String column) {
    return forColumn(column).dateFormatter;
  }
  
  /**
   * Get the date/time formatter to use for the given column.
   * @param column The column whose value is to be formatted.
   * @return the date/time formatter to use for the given column.
   */
  public CustomDateTimeFormatter getDateTimeFormatter(String column) {
    return forColumn(column).dateTimeFormatter;
  }
  
  /**
   * Get the time formatter to use for the given column.
   * @param column The column whose value is to be formatted.
   * @return the time formatter to use for the given column.
   */
  public CustomTimeFormatter getTimeFormatter(String column) {
    return forColumn(column).timeFormatter;
  }
  
  /**
   * Get the decimal formatter to use for the given column.
   * @param column The column whose value is to be formatted.
   * @return the decimal formatter to use for the given column.
   */
  public CustomDecimalFormatter getDecimalFormatter(String column) {
    return forColumn(column).decimalFormatter;
  }
  
  /**
   * Get the boolean formatter to use for the given column.
   * @param column The column whose value is to be formatted.
   * @return the boolean formatter to use for the given column.
   */
  public CustomBooleanFormatter getBooleanFormatter(String column) {
    return forColumn(column).booleanFormatter;
  }
  
}
