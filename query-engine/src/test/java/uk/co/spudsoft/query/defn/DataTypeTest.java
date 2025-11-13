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

import inet.ipaddr.IPAddressString;
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
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.context.RequestContext;

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
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipctx = new PipelineContext("test", reqctx);   
    
    assertEquals(DataType.Boolean, DataType.fromJdbcType(pipctx, JDBCType.BOOLEAN));

    assertEquals(DataType.Integer, DataType.fromJdbcType(pipctx, JDBCType.BIT));
    assertEquals(DataType.Integer, DataType.fromJdbcType(pipctx, JDBCType.TINYINT));
    assertEquals(DataType.Integer, DataType.fromJdbcType(pipctx, JDBCType.SMALLINT));
    assertEquals(DataType.Integer, DataType.fromJdbcType(pipctx, JDBCType.INTEGER));

    assertEquals(DataType.Long, DataType.fromJdbcType(pipctx, JDBCType.BIGINT));

    assertEquals(DataType.Float, DataType.fromJdbcType(pipctx, JDBCType.FLOAT));

    assertEquals(DataType.Double, DataType.fromJdbcType(pipctx, JDBCType.REAL));
    assertEquals(DataType.Double, DataType.fromJdbcType(pipctx, JDBCType.DOUBLE));
    assertEquals(DataType.Double, DataType.fromJdbcType(pipctx, JDBCType.NUMERIC));
    assertEquals(DataType.Double, DataType.fromJdbcType(pipctx, JDBCType.DECIMAL));

    assertEquals(DataType.String, DataType.fromJdbcType(pipctx, JDBCType.CHAR));
    assertEquals(DataType.String, DataType.fromJdbcType(pipctx, JDBCType.VARCHAR));
    assertEquals(DataType.String, DataType.fromJdbcType(pipctx, JDBCType.LONGVARCHAR));
    assertEquals(DataType.String, DataType.fromJdbcType(pipctx, JDBCType.CLOB));
    assertEquals(DataType.String, DataType.fromJdbcType(pipctx, JDBCType.NCHAR));
    assertEquals(DataType.String, DataType.fromJdbcType(pipctx, JDBCType.NVARCHAR));
    assertEquals(DataType.String, DataType.fromJdbcType(pipctx, JDBCType.LONGNVARCHAR));
    assertEquals(DataType.String, DataType.fromJdbcType(pipctx, JDBCType.NCLOB));

    assertEquals(DataType.Date, DataType.fromJdbcType(pipctx, JDBCType.DATE));

    assertEquals(DataType.Time, DataType.fromJdbcType(pipctx, JDBCType.TIME));
    assertEquals(DataType.Time, DataType.fromJdbcType(pipctx, JDBCType.TIME_WITH_TIMEZONE));

    assertEquals(DataType.DateTime, DataType.fromJdbcType(pipctx, JDBCType.TIMESTAMP));
    assertEquals(DataType.DateTime, DataType.fromJdbcType(pipctx, JDBCType.TIMESTAMP_WITH_TIMEZONE));

    assertEquals(DataType.Null, DataType.fromJdbcType(pipctx, JDBCType.NULL));

    assertEquals(DataType.String, DataType.fromJdbcType(pipctx, JDBCType.BINARY));
    assertEquals(DataType.String, DataType.fromJdbcType(pipctx, JDBCType.VARBINARY));
    assertEquals(DataType.String, DataType.fromJdbcType(pipctx, JDBCType.LONGVARBINARY));
    assertEquals(DataType.String, DataType.fromJdbcType(pipctx, JDBCType.OTHER));
    assertEquals(DataType.String, DataType.fromJdbcType(pipctx, JDBCType.JAVA_OBJECT));
    assertEquals(DataType.String, DataType.fromJdbcType(pipctx, JDBCType.DISTINCT));
    assertEquals(DataType.String, DataType.fromJdbcType(pipctx, JDBCType.STRUCT));
    assertEquals(DataType.String, DataType.fromJdbcType(pipctx, JDBCType.ARRAY));
    assertEquals(DataType.String, DataType.fromJdbcType(pipctx, JDBCType.BLOB));
    assertEquals(DataType.String, DataType.fromJdbcType(pipctx, JDBCType.REF));
    assertEquals(DataType.String, DataType.fromJdbcType(pipctx, JDBCType.DATALINK));
    assertEquals(DataType.String, DataType.fromJdbcType(pipctx, JDBCType.ROWID));
    assertEquals(DataType.String, DataType.fromJdbcType(pipctx, JDBCType.SQLXML));
  }

  @Test
  public void testCast() throws Exception {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipctx = new PipelineContext("test", reqctx);   

    assertEquals(null, DataType.Null.cast(pipctx, "irrelevant"));

    assertEquals(null, DataType.Boolean.cast(pipctx, null));
    assertEquals(Boolean.TRUE, DataType.Boolean.cast(pipctx, Boolean.TRUE));
    assertEquals(Boolean.TRUE, DataType.Boolean.cast(pipctx, "true"));
    assertEquals(Boolean.TRUE, DataType.Boolean.cast(pipctx, 7.5));
    assertEquals(Boolean.TRUE, DataType.Boolean.cast(pipctx, new StringBuilder("true")));
    assertEquals(Boolean.TRUE, DataType.Boolean.cast(pipctx, 1));
    assertEquals(Boolean.FALSE, DataType.Boolean.cast(pipctx, 0));
    assertEquals(Boolean.TRUE, DataType.Boolean.cast(pipctx, 1L));
    assertEquals(Boolean.FALSE, DataType.Boolean.cast(pipctx, 0L));
    assertEquals(Boolean.TRUE, DataType.Boolean.cast(pipctx, (byte) 1));
    assertEquals(Boolean.FALSE, DataType.Boolean.cast(pipctx, (byte) 0));

    assertEquals(null, DataType.Date.cast(pipctx, null));
    assertEquals(LocalDate.of(1971, 5, 6), DataType.Date.cast(pipctx, LocalDate.of(1971, 5, 6)));
    assertEquals(LocalDate.of(1971, 5, 6), DataType.Date.cast(pipctx, LocalDateTime.of(1971, 5, 6, 7, 8)));
    assertEquals(LocalDate.of(1973, 2, 17), DataType.Date.cast(pipctx, Instant.ofEpochMilli(98765432100L)));
    assertEquals(LocalDate.of(1971, 5, 6), DataType.Date.cast(pipctx, "1971-05-06"));
    assertEquals(LocalDate.of(1971, 5, 6), DataType.Date.cast(pipctx, new StringBuilder("1971-05-06")));

    assertEquals(null, DataType.DateTime.cast(pipctx, null));
    assertEquals(LocalDateTime.of(1971, 5, 6, 7, 8), DataType.DateTime.cast(pipctx, LocalDateTime.of(1971, 5, 6, 7, 8)));
    assertEquals(LocalDateTime.of(1971, 5, 6, 0, 0), DataType.DateTime.cast(pipctx, LocalDate.of(1971, 5, 6)));
    assertEquals(LocalDateTime.of(1973, 2, 17, 2, 50, 32, 100000000), DataType.DateTime.cast(pipctx, Instant.ofEpochMilli(98765432100L)));
    assertEquals(LocalDateTime.of(1971, 5, 6, 7, 8), DataType.DateTime.cast(pipctx, "1971-05-06T07:08"));
    assertEquals(LocalDateTime.of(1971, 5, 6, 7, 8), DataType.DateTime.cast(pipctx, new StringBuilder("1971-05-06T07:08")));

    assertEquals(null, DataType.Double.cast(pipctx, null));
    assertEquals(Double.MAX_VALUE, DataType.Double.cast(pipctx, Double.MAX_VALUE));
    assertEquals(1.8, DataType.Double.cast(pipctx, 1.8));
    assertEquals(1.8, DataType.Double.cast(pipctx, "1.8"));
    assertEquals(1.8, DataType.Double.cast(pipctx, new StringBuilder("1.8")));

    assertEquals(null, DataType.Float.cast(pipctx, null));
    assertEquals(Float.MAX_VALUE, DataType.Float.cast(pipctx, Float.MAX_VALUE));
    assertEquals((float) 1.8, DataType.Float.cast(pipctx, 1.8));
    assertEquals((float) 1.8, DataType.Float.cast(pipctx, "1.8"));
    assertEquals((float) 1.8, DataType.Float.cast(pipctx, new StringBuilder("1.8")));

    assertEquals(null, DataType.Long.cast(pipctx, null));
    assertEquals(Long.MAX_VALUE, DataType.Long.cast(pipctx, Long.MAX_VALUE));
    assertEquals(18L, DataType.Long.cast(pipctx, 18));
    assertEquals(18L, DataType.Long.cast(pipctx, "18"));
    assertEquals(18L, DataType.Long.cast(pipctx, new StringBuilder("18")));

    assertEquals(null, DataType.Integer.cast(pipctx, null));
    assertEquals(Integer.MAX_VALUE, DataType.Integer.cast(pipctx, Integer.MAX_VALUE));
    assertEquals(18, DataType.Integer.cast(pipctx, 18));
    assertEquals(18, DataType.Integer.cast(pipctx, "18"));
    assertEquals(18, DataType.Integer.cast(pipctx, new StringBuilder("18")));

    assertEquals(null, DataType.String.cast(pipctx, null));
    assertEquals("wibble", DataType.String.cast(pipctx, "wibble"));
    assertEquals("07:08", DataType.String.cast(pipctx, LocalTime.of(7, 8)));

    assertEquals(null, DataType.Time.cast(pipctx, null));
    assertEquals(LocalTime.of(7, 8), DataType.Time.cast(pipctx, LocalTime.of(7, 8)));
    assertEquals(LocalTime.of(7, 8), DataType.Time.cast(pipctx, LocalDateTime.of(1971, 5, 6, 7, 8)));
    assertEquals(LocalTime.of(2, 50, 32, 100000000), DataType.Time.cast(pipctx, Instant.ofEpochMilli(98765432100L)));
    assertEquals(LocalTime.of(7, 8), DataType.Time.cast(pipctx, "07:08"));
    assertEquals(LocalTime.of(7, 8), DataType.Time.cast(pipctx, new StringBuilder("07:08")));
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
