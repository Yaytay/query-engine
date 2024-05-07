/*
 * Copyright (C) 2024 jtalbut
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
package uk.co.spudsoft.query.exec;

import java.sql.JDBCType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.defn.DataType;


/**
 *
 * @author jtalbut
 */
public class ColumnDefnTest {
  
  @Test
  public void testName() {
    ColumnDefn cd = new ColumnDefn("name", DataType.Time);
    assertEquals("name", cd.name());
  }

  @Test
  public void testIsArray() {
    ColumnDefn cd = new ColumnDefn("name", DataType.Time);
    assertFalse(cd.isArray());
  }

  @Test
  public void testTypeName() {
    ColumnDefn cd = new ColumnDefn("name", DataType.Time);
    assertEquals("Time", cd.typeName());
  }

  @Test
  public void testJdbcType() {
    ColumnDefn cd = new ColumnDefn("name", DataType.Time);
    assertEquals(JDBCType.TIME, cd.jdbcType());
  }

  @Test
  public void testType() {
    ColumnDefn cd = new ColumnDefn("name", DataType.Time);
    assertEquals(DataType.Time, cd.type());
  }
  
}
