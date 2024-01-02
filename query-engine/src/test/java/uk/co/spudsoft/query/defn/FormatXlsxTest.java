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

import com.google.common.collect.ImmutableList;
import com.google.common.net.MediaType;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 *
 * @author jtalbut
 */
public class FormatXlsxTest {

  /**
   * Test of getType method, of class FormatXlsx.
   */
  @Test
  public void testGetType() {
    FormatXlsx instance = FormatXlsx.builder().build();
    assertEquals(FormatType.XLSX, instance.getType());
    FormatXlsx.Builder builder = FormatXlsx.builder();
    builder.type(FormatType.JSON);
    assertThrows(IllegalArgumentException.class, () -> {
      builder.build();
    });
  }
  
  @Test
  public void testBasics() {
    FormatXlsx dx = FormatXlsx.builder().build();
    assertEquals("xlsx", dx.getExtension());
    assertEquals("xlsx", dx.getName());
    assertEquals(MediaType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), dx.getMediaType());
    dx = FormatXlsx.builder().extension("extn").build();
    assertEquals("extn", dx.getExtension());
    dx = FormatXlsx.builder().name("format").build();
    assertEquals("format", dx.getName());
    dx = FormatXlsx.builder().mediaType("image/gif").build();
    assertEquals(MediaType.GIF, dx.getMediaType());
  }
  
  @Test
  public void testValidate() {
    FormatXlsx.builder().build().validate();
    assertThrows(IllegalArgumentException.class, () -> {
      FormatXlsx.builder().name(null).build().validate();
    });
    FormatXlsx.builder().name("name").build().validate();
    FormatXlsx.builder()
            .name("name")
            .headerFont(FormatXlsxFont.builder().fontName("Wingdings").fontSize(14).build())
            .bodyFont(FormatXlsxFont.builder().fontName("Times New Roman").fontSize(14).build())
            .headerColours(FormatXlsxColours.builder().fgColour("000000").bgColour("FFFFFF").build())
            .evenColours(FormatXlsxColours.builder().fgColour("000000").bgColour("FFFFFF").build())
            .oddColours(FormatXlsxColours.builder().fgColour("000000").bgColour("FFFFFF").build())
            .columns(Arrays.asList(FormatXlsxColumn.builder().name("one").header("First").build()))
            .build()
            .validate();    
    assertThrows(IllegalArgumentException.class, () -> {
      FormatXlsx.builder()
              .name("name")
              .headerFont(FormatXlsxFont.builder().fontName("Wingdings").fontSize(14).build())
              .bodyFont(FormatXlsxFont.builder().fontName("Times New Roman").fontSize(14).build())
              .headerColours(FormatXlsxColours.builder().fgColour("000000").bgColour("FFFFFF").build())
              .evenColours(FormatXlsxColours.builder().fgColour("000000").bgColour("FFFFFF").build())
              .oddColours(FormatXlsxColours.builder().fgColour("000000").bgColour("FFFFFF").build())
              .columns(Arrays.asList(FormatXlsxColumn.builder().header("First").build()))
              .build()
              .validate();    
    });
  }

  /**
   * Test of getName method, of class FormatXlsx.
   */
  @Test
  public void testGetName() {
    FormatXlsx instance = FormatXlsx.builder().build();
    assertEquals("xlsx", instance.getName());
    instance = FormatXlsx.builder().name("Excel").build();
    assertEquals("Excel", instance.getName());
  }

  /**
   * Test of getExtension method, of class FormatXlsx.
   */
  @Test
  public void testGetExtension() {
    FormatXlsx instance = FormatXlsx.builder().build();
    assertEquals("xlsx", instance.getExtension());
    instance = FormatXlsx.builder().extension("Just don't change the extention, alright?").build();
    assertEquals("Just don't change the extention, alright?", instance.getExtension());
  }

  /**
   * Test of getMediaType method, of class FormatXlsx.
   */
  @Test
  public void testGetMediaType() {
    FormatXlsx instance = FormatXlsx.builder().build();
    assertEquals(MediaType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), instance.getMediaType());
    instance = FormatXlsx.builder().mediaType("stupid/idea").build();
    assertEquals(MediaType.parse("stupid/idea"), instance.getMediaType());
  }

  /**
   * Test of getSheetName method, of class FormatXlsx.
   */
  @Test
  public void testGetSheetName() {
    FormatXlsx instance = FormatXlsx.builder().build();
    assertEquals("data", instance.getSheetName());
    instance = FormatXlsx.builder().sheetName("Excel").build();
    assertEquals("Excel", instance.getSheetName());
  }

  /**
   * Test of getCreator method, of class FormatXlsx.
   */
  @Test
  public void testGetCreator() {
    FormatXlsx instance = FormatXlsx.builder().build();
    assertEquals(null, instance.getCreator());
    instance = FormatXlsx.builder().creator("Bob").build();
    assertEquals("Bob", instance.getCreator());
  }

  /**
   * Test of isGridLines method, of class FormatXlsx.
   */
  @Test
  public void testIsGridLines() {
    FormatXlsx instance = FormatXlsx.builder().build();
    assertEquals(true, instance.isGridLines());
    instance = FormatXlsx.builder().gridLines(false).build();
    assertEquals(false, instance.isGridLines());
  }

  /**
   * Test of isHeaders method, of class FormatXlsx.
   */
  @Test
  public void testIsHeaders() {
    FormatXlsx instance = FormatXlsx.builder().build();
    assertEquals(true, instance.isHeaders());
    instance = FormatXlsx.builder().headers(false).build();
    assertEquals(false, instance.isHeaders());
  }

  /**
   * Test of getHeaderFont method, of class FormatXlsx.
   */
  @Test
  public void testGetHeaderFont() {
    FormatXlsx instance = FormatXlsx.builder().build();
    assertNull(instance.getHeaderFont());
    instance = FormatXlsx.builder().headerFont(FormatXlsxFont.builder().fontName("Wingdings").fontSize(14).build()).build();
    assertEquals("Wingdings", instance.getHeaderFont().toFontDefinition().typeface);
    assertEquals(14, instance.getHeaderFont().toFontDefinition().size);
  }

  /**
   * Test of getBodyFont method, of class FormatXlsx.
   */
  @Test
  public void testGetBodyFont() {
    FormatXlsx instance = FormatXlsx.builder().build();
    assertNull(instance.getBodyFont());
    instance = FormatXlsx.builder().bodyFont(FormatXlsxFont.builder().fontName("Wingdings").fontSize(14).build()).build();
    assertEquals("Wingdings", instance.getBodyFont().toFontDefinition().typeface);
    assertEquals(14, instance.getBodyFont().toFontDefinition().size);
  }

  /**
   * Test of getHeaderColours method, of class FormatXlsx.
   */
  @Test
  public void testGetHeaderColours() {
    FormatXlsx instance = FormatXlsx.builder().build();
    assertNull(instance.getBodyFont());
    instance = FormatXlsx.builder().headerColours(FormatXlsxColours.builder().fgColour("000000").bgColour("FFFFFF").build()).build();
    assertEquals("FF000000", instance.getHeaderColours().toColourDefinition().fgColour);
    assertEquals("FFFFFFFF", instance.getHeaderColours().toColourDefinition().bgColour);
  }

  /**
   * Test of getEvenColours method, of class FormatXlsx.
   */
  @Test
  public void testGetEvenColours() {
    FormatXlsx instance = FormatXlsx.builder().build();
    assertNull(instance.getBodyFont());
    instance = FormatXlsx.builder().evenColours(FormatXlsxColours.builder().fgColour("000000").bgColour("FFFFFF").build()).build();
    assertEquals("FF000000", instance.getEvenColours().toColourDefinition().fgColour);
    assertEquals("FFFFFFFF", instance.getEvenColours().toColourDefinition().bgColour);
  }

  /**
   * Test of getOddColours method, of class FormatXlsx.
   */
  @Test
  public void testGetOddColours() {
    FormatXlsx instance = FormatXlsx.builder().build();
    assertNull(instance.getBodyFont());
    instance = FormatXlsx.builder().oddColours(FormatXlsxColours.builder().fgColour("000000").bgColour("FFFFFF").build()).build();
    assertEquals("FF000000", instance.getOddColours().toColourDefinition().fgColour);
    assertEquals("FFFFFFFF", instance.getOddColours().toColourDefinition().bgColour);
  }

  /**
   * Test of getColumns method, of class FormatXlsx.
   */
  @Test
  public void testGetColumns() {
    FormatXlsx instance = FormatXlsx.builder().build();
    assertEquals(ImmutableList.builder().build(), instance.getColumns());
    instance = FormatXlsx.builder().columns(Arrays.asList(FormatXlsxColumn.builder().name("one").header("First").build())).build();
    assertEquals("First", instance.getColumns().get(0).toColumnDefinition("one", DataType.Time).name);
    assertEquals("First", instance.getColumnsMap().get("one").toColumnDefinition("one", DataType.Time).name);
  }

}
