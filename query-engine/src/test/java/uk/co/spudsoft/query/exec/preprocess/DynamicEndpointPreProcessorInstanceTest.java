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
package uk.co.spudsoft.query.exec.preprocess;

import java.util.LinkedHashMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.exec.DataRow;
import static uk.co.spudsoft.query.exec.preprocess.DynamicEndpointPreProcessorInstance.getField;

/**
 *
 * @author jtalbut
 */
public class DynamicEndpointPreProcessorInstanceTest {
  
  /**
   * Test of initialize method, of class DynamicEndpointPreProcessorInstance.
   */
  @Test
  public void testGetField() {
    LinkedHashMap<String, DataType> dataTypes = new LinkedHashMap<>();
    dataTypes.put("string", DataType.String);
    dataTypes.put("int", DataType.Integer);
    DataRow dataRow = new DataRow(dataTypes);
    dataRow.put("string", "test");
    dataRow.put("int", 7);
    assertNull(getField(dataRow, null));
    assertNull(getField(dataRow, "null"));
    assertEquals("test", getField(dataRow, "string"));
    assertEquals("7", getField(dataRow, "int"));
  }
  
}
