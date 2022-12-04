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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
  }
  
  @Test
  public void testValidate() {
    FormatJson.builder().build().validate();
    assertThrows(IllegalArgumentException.class, () -> {
      FormatJson.builder().name(null).build().validate();
    });
    FormatJson.builder().name("name").build().validate();
  }
  
}
