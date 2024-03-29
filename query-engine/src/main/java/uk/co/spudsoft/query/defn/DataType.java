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
import java.sql.JDBCType;
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

  Null(JDBCType.NULL, 0)
  , Integer(JDBCType.INTEGER, 16)
  , Long(JDBCType.BIGINT, 24)
  , Float(JDBCType.FLOAT, 16)
  , Double(JDBCType.DOUBLE, 24)
  , String(JDBCType.NVARCHAR, -1)
  , Boolean(JDBCType.BOOLEAN, 16)
  , Date(JDBCType.DATE, 24)
  , DateTime(JDBCType.TIMESTAMP, 24)
  , Time(JDBCType.TIME, 24)
  ;
  
  private final JDBCType jdbcType;
  private final int bytes;
  private int index;
  
  private static final DataType[] VALUES = DataType.values();

  DataType(JDBCType jdbcType, int bytes) {
    this.jdbcType = jdbcType;
    this.bytes = bytes;
  }
  
  public static DataType fromOrdinal(int ord) {
    return VALUES[ord];
  }
  
  public static DataType fromObject(Object value) {
    if (value == null) {
      return Null;
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

  public JDBCType jdbcType() {
    return jdbcType;
  }

  public int bytes() {
    return bytes;
  }
  
  public static DataType fromJdbcType(JDBCType jdbcType) {
    switch (jdbcType) {
      case BOOLEAN:
        return Boolean;
      case BIT:
      case TINYINT:
      case SMALLINT:
      case INTEGER:
        return Integer;
      case BIGINT:
        return Long;
      case FLOAT:
        return Float;
      case REAL:
      case DOUBLE:
      case NUMERIC:
      case DECIMAL:
        return Double;
      case CHAR:
      case VARCHAR:
      case LONGVARCHAR:
      case CLOB:
      case NCHAR:
      case NVARCHAR:
      case LONGNVARCHAR:
      case NCLOB:
        return String;
      case DATE:
        return Date;
      case TIME:
      case TIME_WITH_TIMEZONE:
        return Time;
      case TIMESTAMP:
      case TIMESTAMP_WITH_TIMEZONE:
        return DateTime;
      case NULL:
        return Null;
      case BINARY:
      case VARBINARY:
      case LONGVARBINARY:
      case OTHER:
      case JAVA_OBJECT:
      case DISTINCT:
      case STRUCT:
      case ARRAY:
      case BLOB:
      case REF:
      case DATALINK:
      case ROWID:
      case SQLXML:
      default:
        throw new IllegalArgumentException("Cannot process fields of type " + jdbcType.getName());
    }
  }  
}
