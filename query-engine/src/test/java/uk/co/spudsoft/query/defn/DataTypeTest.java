/*
 * Copyright (C) 2023 jtalbut
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

import java.sql.JDBCType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 *
 * @author jtalbut
 */
public class DataTypeTest {

  /**
   * Test of fromJdbcType method, of class DataType.
   */
  @Test
  public void testFromJdbcType() {
    assertEquals(DataType.Boolean, DataType.fromJdbcType(JDBCType.BOOLEAN));

    assertEquals(DataType.Integer, DataType.fromJdbcType(JDBCType.BIT));
    assertEquals(DataType.Integer, DataType.fromJdbcType(JDBCType.TINYINT));
    assertEquals(DataType.Integer, DataType.fromJdbcType(JDBCType.SMALLINT));
    assertEquals(DataType.Integer, DataType.fromJdbcType(JDBCType.INTEGER));

    assertEquals(DataType.Long, DataType.fromJdbcType(JDBCType.BIGINT));

    assertEquals(DataType.Float, DataType.fromJdbcType(JDBCType.FLOAT));

    assertEquals(DataType.Double, DataType.fromJdbcType(JDBCType.REAL));
    assertEquals(DataType.Double, DataType.fromJdbcType(JDBCType.DOUBLE));
    assertEquals(DataType.Double, DataType.fromJdbcType(JDBCType.NUMERIC));
    assertEquals(DataType.Double, DataType.fromJdbcType(JDBCType.DECIMAL));

    assertEquals(DataType.String, DataType.fromJdbcType(JDBCType.CHAR));
    assertEquals(DataType.String, DataType.fromJdbcType(JDBCType.VARCHAR));
    assertEquals(DataType.String, DataType.fromJdbcType(JDBCType.LONGVARCHAR));
    assertEquals(DataType.String, DataType.fromJdbcType(JDBCType.CLOB));
    assertEquals(DataType.String, DataType.fromJdbcType(JDBCType.NCHAR));
    assertEquals(DataType.String, DataType.fromJdbcType(JDBCType.NVARCHAR));
    assertEquals(DataType.String, DataType.fromJdbcType(JDBCType.LONGNVARCHAR));
    assertEquals(DataType.String, DataType.fromJdbcType(JDBCType.NCLOB));

    assertEquals(DataType.Date, DataType.fromJdbcType(JDBCType.DATE));

    assertEquals(DataType.Time, DataType.fromJdbcType(JDBCType.TIME));
    assertEquals(DataType.Time, DataType.fromJdbcType(JDBCType.TIME_WITH_TIMEZONE));

    assertEquals(DataType.DateTime, DataType.fromJdbcType(JDBCType.TIMESTAMP));
    assertEquals(DataType.DateTime, DataType.fromJdbcType(JDBCType.TIMESTAMP_WITH_TIMEZONE));

    assertEquals(DataType.Null, DataType.fromJdbcType(JDBCType.NULL));

    assertEquals(DataType.String, DataType.fromJdbcType(JDBCType.BINARY));
    assertEquals(DataType.String, DataType.fromJdbcType(JDBCType.VARBINARY));
    assertEquals(DataType.String, DataType.fromJdbcType(JDBCType.LONGVARBINARY));
    assertEquals(DataType.String, DataType.fromJdbcType(JDBCType.OTHER));
    assertEquals(DataType.String, DataType.fromJdbcType(JDBCType.JAVA_OBJECT));
    assertEquals(DataType.String, DataType.fromJdbcType(JDBCType.DISTINCT));
    assertEquals(DataType.String, DataType.fromJdbcType(JDBCType.STRUCT));
    assertEquals(DataType.String, DataType.fromJdbcType(JDBCType.ARRAY));
    assertEquals(DataType.String, DataType.fromJdbcType(JDBCType.BLOB));
    assertEquals(DataType.String, DataType.fromJdbcType(JDBCType.REF));
    assertEquals(DataType.String, DataType.fromJdbcType(JDBCType.DATALINK));
    assertEquals(DataType.String, DataType.fromJdbcType(JDBCType.ROWID));
    assertEquals(DataType.String, DataType.fromJdbcType(JDBCType.SQLXML));
  }

  @Test
  public void testCast() throws Exception {
    assertEquals(null, DataType.Null.cast("irrelevant"));

    assertEquals(null, DataType.Boolean.cast(null));
    assertEquals(Boolean.TRUE, DataType.Boolean.cast(Boolean.TRUE));
    assertEquals(Boolean.TRUE, DataType.Boolean.cast("true"));
    assertEquals(Boolean.TRUE, DataType.Boolean.cast(7.5));
    assertEquals(Boolean.TRUE, DataType.Boolean.cast(new StringBuilder("true")));

    assertEquals(null, DataType.Date.cast(null));
    assertEquals(LocalDate.of(1971, 5, 6), DataType.Date.cast(LocalDate.of(1971, 5, 6)));
    assertEquals(LocalDate.of(1971, 5, 6), DataType.Date.cast(LocalDateTime.of(1971, 5, 6, 7, 8)));
    assertEquals(LocalDate.of(1973, 2, 17), DataType.Date.cast(Instant.ofEpochMilli(98765432100L)));
    assertEquals(LocalDate.of(1971, 5, 6), DataType.Date.cast("1971-05-06"));
    assertEquals(LocalDate.of(1971, 5, 6), DataType.Date.cast(new StringBuilder("1971-05-06")));

    assertEquals(null, DataType.DateTime.cast(null));
    assertEquals(LocalDateTime.of(1971, 5, 6, 7, 8), DataType.DateTime.cast(LocalDateTime.of(1971, 5, 6, 7, 8)));
    assertEquals(LocalDateTime.of(1971, 5, 6, 0, 0), DataType.DateTime.cast(LocalDate.of(1971, 5, 6)));
    assertEquals(LocalDateTime.of(1973, 2, 17, 2, 50, 32, 100000000), DataType.DateTime.cast(Instant.ofEpochMilli(98765432100L)));
    assertEquals(LocalDateTime.of(1971, 5, 6, 7, 8), DataType.DateTime.cast("1971-05-06T07:08"));
    assertEquals(LocalDateTime.of(1971, 5, 6, 7, 8), DataType.DateTime.cast(new StringBuilder("1971-05-06T07:08")));

    assertEquals(null, DataType.Double.cast(null));
    assertEquals(Double.MAX_VALUE, DataType.Double.cast(Double.MAX_VALUE));
    assertEquals(1.8, DataType.Double.cast(1.8));
    assertEquals(1.8, DataType.Double.cast("1.8"));
    assertEquals(1.8, DataType.Double.cast(new StringBuilder("1.8")));

    assertEquals(null, DataType.Float.cast(null));
    assertEquals(Float.MAX_VALUE, DataType.Float.cast(Float.MAX_VALUE));
    assertEquals((float) 1.8, DataType.Float.cast(1.8));
    assertEquals((float) 1.8, DataType.Float.cast("1.8"));
    assertEquals((float) 1.8, DataType.Float.cast(new StringBuilder("1.8")));

    assertEquals(null, DataType.Long.cast(null));
    assertEquals(Long.MAX_VALUE, DataType.Long.cast(Long.MAX_VALUE));
    assertEquals(18L, DataType.Long.cast(18));
    assertEquals(18L, DataType.Long.cast("18"));
    assertEquals(18L, DataType.Long.cast(new StringBuilder("18")));

    assertEquals(null, DataType.Integer.cast(null));
    assertEquals(Integer.MAX_VALUE, DataType.Integer.cast(Integer.MAX_VALUE));
    assertEquals(18, DataType.Integer.cast(18));
    assertEquals(18, DataType.Integer.cast("18"));
    assertEquals(18, DataType.Integer.cast(new StringBuilder("18")));

    assertEquals(null, DataType.String.cast(null));
    assertEquals("wibble", DataType.String.cast("wibble"));
    assertEquals("07:08", DataType.String.cast(LocalTime.of(7, 8)));

    assertEquals(null, DataType.Time.cast(null));
    assertEquals(LocalTime.of(7, 8), DataType.Time.cast(LocalTime.of(7, 8)));
    assertEquals(LocalTime.of(7, 8), DataType.Time.cast(LocalDateTime.of(1971, 5, 6, 7, 8)));
    assertEquals(LocalTime.of(2, 50, 32, 100000000), DataType.Time.cast(Instant.ofEpochMilli(98765432100L)));
    assertEquals(LocalTime.of(7, 8), DataType.Time.cast("07:08"));
    assertEquals(LocalTime.of(7, 8), DataType.Time.cast(new StringBuilder("07:08")));
  }

  @ParameterizedTest
  @EnumSource(DataType.class)
  public void testCommonType(DataType type) {
    EnumMap<DataType, Integer> typeRankings =  new EnumMap<>(DataType.class);
    typeRankings.put(DataType.Boolean, 1);
    typeRankings.put(DataType.Integer, 2);
    typeRankings.put(DataType.Long, 3);
    typeRankings.put(DataType.Float, 4);
    typeRankings.put(DataType.Double, 5);

    for (DataType other : DataType.values()) {
      
      if (other == type) {
        assertEquals(other, type.commonType(other), () -> type.name() + ".commonType(" + other.name() + ") returned " + type.commonType(other));
        assertEquals(other.commonType(type), type.commonType(other), () -> type.name() + ".commonType(" + other.name() + ")  returned " + type.commonType(other) + " but " + other.name() + ".commonType(" + type.name() + ")  returned " + other.commonType(type));
        
      } else if (type == DataType.Null) {
        assertEquals(other, type.commonType(other), () -> type.name() + ".commonType(" + other.name() + ") returned " + type.commonType(other));
        assertEquals(other.commonType(type), type.commonType(other), () -> type.name() + ".commonType(" + other.name() + ")  returned " + type.commonType(other) + " but " + other.name() + ".commonType(" + type.name() + ")  returned " + other.commonType(type));
        
      } else if (type == DataType.String) {
        assertEquals(type, type.commonType(other), () -> type.name() + ".commonType(" + other.name() + ") returned " + type.commonType(other));
        assertEquals(other.commonType(type), type.commonType(other), () -> type.name() + ".commonType(" + other.name() + ")  returned " + type.commonType(other) + " but " + other.name() + ".commonType(" + type.name() + ")  returned " + other.commonType(type));
        
      } else if (type == DataType.Date || type == DataType.DateTime || type == DataType.Time) {
        if (other == DataType.Null) {
          assertEquals(type, type.commonType(other), () -> type.name() + ".commonType(" + other.name() + ") returned " + type.commonType(other));
          assertEquals(other.commonType(type), type.commonType(other), () -> type.name() + ".commonType(" + other.name() + ")  returned " + type.commonType(other) + " but " + other.name() + ".commonType(" + type.name() + ")  returned " + other.commonType(type));
        } else if (other == DataType.String) {
          assertEquals(other, type.commonType(other), () -> type.name() + ".commonType(" + other.name() + ") returned " + type.commonType(other));
          assertEquals(other.commonType(type), type.commonType(other), () -> type.name() + ".commonType(" + other.name() + ")  returned " + type.commonType(other) + " but " + other.name() + ".commonType(" + type.name() + ")  returned " + other.commonType(type));
        } else {
          assertEquals("No common type between " + type.name() + " and " + other.name(), assertThrows(IllegalArgumentException.class, () -> type.commonType(other), () -> type.name() + ".commonType(" + other.name() + ")  returned " + type.commonType(other)).getMessage());
          assertEquals("No common type between " + other.name() + " and " + type.name(), assertThrows(IllegalArgumentException.class, () -> other.commonType(type), () -> type.name() + ".commonType(" + other.name() + ")  returned " + type.commonType(other)).getMessage());
        }
        
      } else {
        assert(typeRankings.containsKey(type));
        
        if (other == DataType.Null) {
          assertEquals(type, type.commonType(other), () -> type.name() + ".commonType(" + other.name() + ") returned " + type.commonType(other));
          assertEquals(other.commonType(type), type.commonType(other), () -> type.name() + ".commonType(" + other.name() + ")  returned " + type.commonType(other) + " but " + other.name() + ".commonType(" + type.name() + ")  returned " + other.commonType(type));
        } else if (other == DataType.String) {
          assertEquals(other, type.commonType(other), () -> type.name() + ".commonType(" + other.name() + ") returned " + type.commonType(other));
          assertEquals(other.commonType(type), type.commonType(other), () -> type.name() + ".commonType(" + other.name() + ")  returned " + type.commonType(other) + " but " + other.name() + ".commonType(" + type.name() + ")  returned " + other.commonType(type));
        } else if (typeRankings.containsKey(other)) {
          int typeRank = typeRankings.get(type);
          int otherRank = typeRankings.get(other);
          if (otherRank > typeRank) {
            assertEquals(other, type.commonType(other), () -> type.name() + ".commonType(" + other.name() + ") returned " + type.commonType(other));
          } else {
            assertEquals(type, type.commonType(other), () -> type.name() + ".commonType(" + other.name() + ") returned " + type.commonType(other));
          }
          assertEquals(other.commonType(type), type.commonType(other), () -> type.name() + ".commonType(" + other.name() + ")  returned " + type.commonType(other) + " but " + other.name() + ".commonType(" + type.name() + ")  returned " + other.commonType(type));
        } else {
          assertEquals("No common type between " + type.name() + " and " + other.name(), assertThrows(IllegalArgumentException.class, () -> type.commonType(other), () -> type.name() + ".commonType(" + other.name() + ")  returned " + type.commonType(other)).getMessage());
          assertEquals("No common type between " + other.name() + " and " + type.name(), assertThrows(IllegalArgumentException.class, () -> other.commonType(type), () -> type.name() + ".commonType(" + other.name() + ")  returned " + type.commonType(other)).getMessage());
        }
      }
    }
  }


}
