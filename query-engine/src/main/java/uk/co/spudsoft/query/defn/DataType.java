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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

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

  /**
   * The value is null.
   */
  Null(JDBCType.NULL, 0),
  /**
   * The value is a java.lang.Integer.
   */
  Integer(JDBCType.INTEGER, 16), 
  /**
   * The value is a java.lang.Long.
   */
  Long(JDBCType.BIGINT, 24), 
  /**
   * The value is a java.lang.Float.
   */
  Float(JDBCType.FLOAT, 16), 
  /**
   * The value is a java.lang.Double.
   */
  Double(JDBCType.DOUBLE, 24), 
  /**
   * The value is a java.lang.String.
   */
  String(JDBCType.NVARCHAR, -1), 
  /**
   * The value is a java.lang.Boolean.
   */
  Boolean(JDBCType.BOOLEAN, 16),
  /**
   * The value is a java.time.LocalDate.
   */
  Date(JDBCType.DATE, 24), 
  /**
   * The value is a java.time.LocalDateTime.
   */
  DateTime(JDBCType.TIMESTAMP, 24), 
  /**
   * The value is a java.time.LocalTime.
   */
  Time(JDBCType.TIME, 24);

  private final JDBCType jdbcType;
  private final int bytes;
  private int index;

  private static final DataType[] VALUES = DataType.values();

  /**
   * Constructor.
   * @param jdbcType The type used to represent this datatype in JDBC.
   * @param bytes The size of this data type, in bytes.
   */
  DataType(JDBCType jdbcType, int bytes) {
    this.jdbcType = jdbcType;
    this.bytes = bytes;
  }

  /**
   * Return a DataType given its ordinal number.
   * <p>
   * This value is not guaranteed to be consistent between builds or platforms.
   * @param ord The ordinal.
   * @return the DataType with the given ordinal number.
   */
  public static DataType fromOrdinal(int ord) {
    return VALUES[ord];
  }

  /**
   * Return the DataType of the passed in object.
   * @param value The object being considered.
   * @return the DataType of the passed in object.
   * @throws IllegalArgumentException if the value is not of a recognized class.
   */
  public static DataType fromObject(Object value) throws IllegalArgumentException {
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

  /**
   * Return the passed in value in this data type.
   * <p>
   * If the value is already in the appropriate type it will be returned as is
   * ; if it's a compatible type if will be converted; if it's a string it will be parsed; otherwise it will be converted to a string and then parsed.
   * 
   * @param value the value to be cast.
   * @return the passed in value in this data type.
   * @throws java.lang.Exception if the value cannot be converted to this type.
   */
  public Comparable<?> cast(Object value) throws Exception {
    if (value == null) {
      return null;
    }
    if (this == Null) {
      return null;
    } else if (this == Integer) {
      switch (value) {
        case Integer tv -> {
          return tv;
        }
        case Number tv -> {
          return tv.intValue();
        }
        case String tv -> {
          return java.lang.Integer.valueOf(tv);
        }
        default -> {
          return java.lang.Integer.valueOf(value.toString());
        }
      }
    } else if (this == Long) {
      switch (value) {
        case Long tv -> {
          return tv;
        }
        case Number tv -> {
          return tv.longValue();
        }
        case String tv -> {
          return java.lang.Long.valueOf(tv);
        }
        default -> {
          return java.lang.Long.valueOf(value.toString());
        }
      }
    } else if (this == Float) {
      switch (value) {
        case Float tv -> {
          return tv;
        }
        case Number tv -> {
          return tv.floatValue();
        }
        case String tv -> {
          return java.lang.Float.valueOf(tv);
        }
        default -> {
          return java.lang.Float.valueOf(value.toString());
        }
      }
    } else if (this == Double) {
      switch (value) {
        case Double tv -> {
          return tv;
        }
        case Number tv -> {
          return tv.doubleValue();
        }
        case String tv -> {
          return java.lang.Double.valueOf(tv);
        }
        default -> {
          return java.lang.Double.valueOf(value.toString());
        }
      }
    } else if (this == String) {
      switch (value) {
        case String tv -> {
          return tv;
        }
        default -> {
          return value.toString();
        }
      }
    } else if (this == Boolean) {
      switch (value) {
        case Boolean tv -> {
          return tv;
        }
        case Number tv -> {
          return java.lang.Double.compare(tv.doubleValue(), 0.0) != 0;
        }
        case String tv -> {
          return java.lang.Boolean.valueOf(tv);
        }
        default -> {
          return java.lang.Boolean.valueOf(value.toString());
        }
      }
    } else if (this == Date) {
      switch (value) {
        case LocalDate tv -> {
          return tv;
        }
        case LocalDateTime tv -> {
          return tv.toLocalDate();
        }
        case Instant tv -> {
          return LocalDate.ofInstant(tv, ZoneOffset.UTC);
        }
        case java.util.Date tv -> {
          return LocalDate.ofInstant(tv.toInstant(), ZoneOffset.UTC);
        }
        case String tv -> {
          return LocalDate.parse(tv);
        }
        default -> {
          return LocalDate.parse(value.toString());
        }
      }
    } else if (this == DateTime) {
      switch (value) {
        case LocalDateTime tv -> {
          return tv;
        }
        case LocalDate tv -> {
          return tv.atStartOfDay();
        }
        case Instant tv -> {
          return LocalDateTime.ofInstant(tv, ZoneOffset.UTC);
        }
        case java.util.Date tv -> {
          return LocalDateTime.ofInstant(tv.toInstant(), ZoneOffset.UTC);
        }
        case String tv -> {
          return LocalDateTime.parse(tv);
        }
        default -> {
          return LocalDateTime.parse(value.toString());
        }
      }
    } else if (this == Time) {
      switch (value) {
        case LocalTime tv -> {
          return tv;
        }
        case LocalDateTime tv -> {
          return tv.toLocalTime();
        }
        case Instant tv -> {
          return LocalTime.ofInstant(tv, ZoneOffset.UTC);
        }
        case java.util.Date tv -> {
          return LocalTime.ofInstant(tv.toInstant(), ZoneOffset.UTC);
        }
        case String tv -> {
          return LocalTime.parse(tv);
        }
        default -> {
          return LocalTime.parse(value.toString());
        }
      }
    } else {
      throw new IllegalArgumentException("Unhandled value type: " + value.getClass());
    }
  }
  
  /**
   * Get the value uses to represent this datatype in JDBC.
   * @return the value uses to represent this datatype in JDBC.
   */
  public JDBCType jdbcType() {
    return jdbcType;
  }

  /**
   * Get the size of this datatype, in bytes.
   * @return the size of this datatype, in bytes.
   */
  public int bytes() {
    return bytes;
  }

  /**
   * Get the appropriate DataType for any JDBCType.
   * @param jdbcType The JDBCType being sought.
   * @return the appropriate DataType for the JDBCType.
   */
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
