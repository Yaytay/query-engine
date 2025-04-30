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

import com.google.common.net.MediaType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/**
 *
 * @author jtalbut
 */
public class FormatJsonTest {
  
  @Test
  public void testBuilder() {
    FormatJson dj = FormatJson.builder().build();
    assertEquals(FormatType.JSON, dj.getType());
    try {
      FormatJson.builder().type(FormatType.HTML).build();
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
    }
    assertEquals("json", dj.getExtension());
    assertEquals("json", dj.getName());
    assertEquals(MediaType.parse("application/json"), dj.getMediaType());
    dj = FormatJson.builder().extension("extn").build();
    assertEquals("extn", dj.getExtension());
    dj = FormatJson.builder().name("format").build();
    assertEquals("format", dj.getName());
    dj = FormatJson.builder().mediaType("image/gif").build();
    assertEquals(MediaType.GIF, dj.getMediaType());
    
    assertNull(dj.getDataName());
    assertNull(dj.getMetadataName());
    assertFalse(dj.isCompatibleTypeNames());
    
    dj = FormatJson.builder().dataName("ddata").build();
    assertEquals("ddata", dj.getDataName());
    
    dj = FormatJson.builder().metadataName("mmeta").build();
    assertEquals("mmeta", dj.getMetadataName());
    
    dj = FormatJson.builder().compatibleTypeNames(Boolean.FALSE).build();
    assertFalse(dj.isCompatibleTypeNames());
    dj = FormatJson.builder().compatibleTypeNames(Boolean.TRUE).build();
    assertTrue(dj.isCompatibleTypeNames());
  }
  
  @Test
  public void testValidate() {
    FormatJson.builder().build().validate();
    assertThrows(IllegalArgumentException.class, () -> {
      FormatJson.builder().name(null).build().validate();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      FormatJson.builder().name("json").metadataName("meta").build().validate();
    });
    FormatJson.builder().name("name").build().validate();
    FormatJson.builder().name("json").dataName("data").build().validate();
    FormatJson.builder().name("json").dataName("data").metadataName("meta").build().validate();    
  }
  
}
