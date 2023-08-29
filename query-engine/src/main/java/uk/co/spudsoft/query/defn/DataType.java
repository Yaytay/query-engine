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
import io.vertx.sqlclient.desc.ColumnDescriptor;
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

  Integer(JDBCType.INTEGER)
  , Long(JDBCType.BIGINT)
  , Float(JDBCType.FLOAT)
  , Double(JDBCType.DOUBLE)
  , String(JDBCType.NVARCHAR)
  , Boolean(JDBCType.BOOLEAN)
  , Date(JDBCType.DATE)
  , DateTime(JDBCType.TIMESTAMP)
  , Time(JDBCType.TIME)
  , Null(JDBCType.NULL)
  ;
  
  private final JDBCType jdbcType;

  DataType(JDBCType jdbcType) {
    this.jdbcType = jdbcType;
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
  
  private static class DataTypeDescriptor implements ColumnDescriptor {
    
    private final String name;
    private final DataType dataType;

    DataTypeDescriptor(String name, DataType dataType) {
      this.name = name;
      this.dataType = dataType;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public boolean isArray() {
      return false;
    }

    @Override
    public String typeName() {
      return dataType.name();
    }

    @Override
    public JDBCType jdbcType() {
      return dataType.jdbcType;
    }
    
  }
  
  public ColumnDescriptor toColumnDescriptor(String columnName) {
    return new DataTypeDescriptor(columnName, this);
  }
  
}
