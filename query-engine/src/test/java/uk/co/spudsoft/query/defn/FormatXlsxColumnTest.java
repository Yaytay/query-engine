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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 *
 * @author njt
 */
public class FormatXlsxColumnTest {

  /**
   * Test of toColumnDefinition method, of class FormatXlsxColumn.
   */
  @Test
  public void testToColumnDefinition() {
    assertEquals(null, FormatXlsxColumn.builder().build().toColumnDefinition("key", DataType.String).format);
    assertEquals("key", FormatXlsxColumn.builder().build().toColumnDefinition("key", DataType.String).name);
    assertEquals(17.0, FormatXlsxColumn.builder().build().toColumnDefinition("key", DataType.String).width);
  }

  /**
   * Test of validate method, of class FormatXlsxColumn.
   */
  @Test
  public void testValidate() {
    FormatXlsxColumn.builder().name("h").header("Wibble").format("000000").width(7.0).build().validate();
    FormatXlsxColumn.builder().name("h").format("000000").width(7.0).build().validate();
    FormatXlsxColumn.builder().name("h").format("000000").build().validate();
    FormatXlsxColumn.builder().name("h").width(7.0).build().validate();
    assertThrows(IllegalArgumentException.class, () -> {
      FormatXlsxColumn.builder().build().validate();    
    });
    assertThrows(IllegalArgumentException.class, () -> {
      FormatXlsxColumn.builder().width(-8.0).build().validate();    
    });
  }

  /**
   * Test of defaultFormatFor method, of class FormatXlsxColumn.
   */
  @Test
  public void testDefaultFormatFor() {
    assertNull(FormatXlsxColumn.defaultFormatFor(DataType.Boolean));
    assertEquals("yyyy-mm-dd", FormatXlsxColumn.defaultFormatFor(DataType.Date));
    assertEquals("hh:mm:ss", FormatXlsxColumn.defaultFormatFor(DataType.Time));
    assertEquals("yyyy-mm-dd hh:mm:ss", FormatXlsxColumn.defaultFormatFor(DataType.DateTime));
    assertNull(FormatXlsxColumn.defaultFormatFor(DataType.Double));
    assertNull(FormatXlsxColumn.defaultFormatFor(DataType.Float));
    assertNull(FormatXlsxColumn.defaultFormatFor(DataType.Integer));
    assertNull(FormatXlsxColumn.defaultFormatFor(DataType.Long));
    assertNull(FormatXlsxColumn.defaultFormatFor(DataType.String));
  }

  /**
   * Test of formatLength method, of class FormatXlsxColumn.
   */
  @Test
  public void testFormatLength() {
    assertEquals(0, FormatXlsxColumn.formatLength(null));
    assertEquals(4, FormatXlsxColumn.formatLength("0.00"));
    assertEquals(8, FormatXlsxColumn.formatLength("0.00;-###0.00;zero"));
  }

  /**
   * Test of defaultWidthFor method, of class FormatXlsxColumn.
   */
  @Test
  public void testDefaultWidthFor() {
    assertEquals(9.0, FormatXlsxColumn.defaultWidthFor(DataType.Time, "header", null));
    assertEquals(10.0, FormatXlsxColumn.defaultWidthFor(DataType.Time, "LongHeader", null));
    assertEquals(12.0, FormatXlsxColumn.defaultWidthFor(DataType.Time, "header", "CustomFormat"));
    assertEquals(7.0, FormatXlsxColumn.defaultWidthFor(DataType.Boolean, "1", null));
    assertEquals(11.0, FormatXlsxColumn.defaultWidthFor(DataType.Date, "1", null));
    assertEquals(20.0, FormatXlsxColumn.defaultWidthFor(DataType.DateTime, "1", null));
    assertEquals(11.0, FormatXlsxColumn.defaultWidthFor(DataType.Double, "1", null));
    assertEquals(11.0, FormatXlsxColumn.defaultWidthFor(DataType.Float, "1", null));
    assertEquals(11.0, FormatXlsxColumn.defaultWidthFor(DataType.Integer, "1", null));
    assertEquals(11.0, FormatXlsxColumn.defaultWidthFor(DataType.Long, "1", null));
    assertEquals(17.0, FormatXlsxColumn.defaultWidthFor(DataType.String, "1", null));
    assertEquals(9.0, FormatXlsxColumn.defaultWidthFor(DataType.Time, "1", null));
  }

}
