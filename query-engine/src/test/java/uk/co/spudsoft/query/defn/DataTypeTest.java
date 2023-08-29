/*
 * Copyright (C) 2023 njt
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static uk.co.spudsoft.query.defn.DataType.Null;


/**
 *
 * @author njt
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
    
    assertThrows(IllegalArgumentException.class, () -> {DataType.fromJdbcType(JDBCType.BINARY);});
    assertThrows(IllegalArgumentException.class, () -> {DataType.fromJdbcType(JDBCType.VARBINARY);});
    assertThrows(IllegalArgumentException.class, () -> {DataType.fromJdbcType(JDBCType.LONGVARBINARY);});
    assertThrows(IllegalArgumentException.class, () -> {DataType.fromJdbcType(JDBCType.OTHER);});
    assertThrows(IllegalArgumentException.class, () -> {DataType.fromJdbcType(JDBCType.JAVA_OBJECT);});
    assertThrows(IllegalArgumentException.class, () -> {DataType.fromJdbcType(JDBCType.DISTINCT);});
    assertThrows(IllegalArgumentException.class, () -> {DataType.fromJdbcType(JDBCType.STRUCT);});
    assertThrows(IllegalArgumentException.class, () -> {DataType.fromJdbcType(JDBCType.ARRAY);});
    assertThrows(IllegalArgumentException.class, () -> {DataType.fromJdbcType(JDBCType.BLOB);});
    assertThrows(IllegalArgumentException.class, () -> {DataType.fromJdbcType(JDBCType.REF);});
    assertThrows(IllegalArgumentException.class, () -> {DataType.fromJdbcType(JDBCType.DATALINK);});
    assertThrows(IllegalArgumentException.class, () -> {DataType.fromJdbcType(JDBCType.ROWID);});
    assertThrows(IllegalArgumentException.class, () -> {DataType.fromJdbcType(JDBCType.SQLXML);});
  }

  /**
   * Test of toColumnDescriptor method, of class DataType.
   */
  @Test
  public void testToColumnDescriptor() {
    
    assertEquals(false, DataType.Boolean.toColumnDescriptor("one").isArray());
    assertEquals("Boolean", DataType.Boolean.toColumnDescriptor("one").typeName());
    assertEquals(JDBCType.BOOLEAN, DataType.Boolean.toColumnDescriptor("one").jdbcType());
  }
  
}
