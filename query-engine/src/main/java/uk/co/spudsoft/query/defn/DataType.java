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
package uk.co.spudsoft.query.defn;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * The basic data types that values can have in Query Engine.
 * 
 * @author jtalbut
 */
@Schema(description = """
                      <P>The basic data types that values can have in Query Engine.</P>
                      """
)
public enum DataType {

  Integer
  , Long
  , Float
  , Double
  , String
  , Boolean
  , Date
  , DateTime
  , Time
  ;
  
  public static DataType fromObject(Object value) {
    if (value == null) {
      return null;
    } else if (value instanceof Integer) {
      return Integer;
    } else if (value instanceof Long) {
      return Long;
    } else if (value instanceof Float) {
      return Float;
    } else if (value instanceof Double) {
      return Double;
    } else if (value instanceof String) {
      return String;
    } else if (value instanceof Boolean) {
      return Boolean;
    } else if (value instanceof LocalDate) {
      return Date;
    } else if (value instanceof LocalDateTime) {
      return DateTime;
    } else if (value instanceof LocalTime) {
      return Time;
    } else {
      throw new IllegalArgumentException("Unhandled value type: " + value.getClass());
    }
  }
  
}
