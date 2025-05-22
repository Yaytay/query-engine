/*
 * Copyright (C) 2025 njt
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

/**
 *
 * @author njt
 */
public class ColumnTypeOverrideTest {
  
  @Test
  public void testGetColumn() {
    ColumnTypeOverride cto = ColumnTypeOverride.builder().build();
    assertNotNull(cto);
    assertNull(cto.getColumn());
    cto = ColumnTypeOverride.builder().column("col").build();
    assertEquals("col", cto.getColumn());
  }

  @Test
  public void testGetType() {
    ColumnTypeOverride cto = ColumnTypeOverride.builder().build();
    assertNotNull(cto);
    assertNull(cto.getType());
    cto = ColumnTypeOverride.builder().type(DataType.Double).build();
    assertEquals(DataType.Double, cto.getType());
  }

}
