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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author jtalbut
 */
public class FormatDelimitedTest {
  
  @Test
  public void testBuilder() {
    assertEquals(FormatType.Delimited, FormatDelimited.builder().build().getType());

    assertFalse(FormatDelimited.builder().build().isHidden());
    assertTrue(FormatDelimited.builder().hidden(true).build().isHidden());

    assertEquals(null, FormatDelimited.builder().build().getDescription());
    assertEquals("Stuff", FormatDelimited.builder().description("Stuff").build().getDescription());

    assertEquals("\"", FormatDelimited.builder().build().getOpenQuote());
    assertEquals("$", FormatDelimited.builder().openQuote("$").build().getOpenQuote());

    assertEquals("\"", FormatDelimited.builder().build().getCloseQuote());
    assertEquals("$", FormatDelimited.builder().closeQuote("$").build().getCloseQuote());

    assertNull(FormatDelimited.builder().build().getEscapeCloseQuote());
    assertEquals("$", FormatDelimited.builder().escapeCloseQuote("$").build().getEscapeCloseQuote());

    assertNull(FormatDelimited.builder().build().getReplaceCloseQuote());
    assertEquals("S", FormatDelimited.builder().replaceCloseQuote("S").build().getReplaceCloseQuote());

    assertNull(FormatDelimited.builder().build().getDateFormat());
    assertEquals("S", FormatDelimited.builder().dateFormat("S").build().getDateFormat());

    assertNull(FormatDelimited.builder().build().getDateTimeFormat());
    assertEquals("S", FormatDelimited.builder().dateTimeFormat("S").build().getDateTimeFormat());

    assertNull(FormatDelimited.builder().build().getTimeFormat());
    assertEquals("S", FormatDelimited.builder().timeFormat("S").build().getTimeFormat());
  }
  
  @Test
  public void testValidate() {
    FormatDelimited.builder().build().validate();

    assertThrows(IllegalArgumentException.class, () -> {
      FormatDelimited.builder().type(FormatType.Atom).build().validate();
    });

    assertThrows(IllegalArgumentException.class, () -> {
      FormatDelimited.builder().name(null).build().validate();
    });
    FormatDelimited.builder().name("name").build().validate();
    
    assertThrows(IllegalArgumentException.class, () -> {
      FormatDelimited.builder().dateFormat("T").build().validate();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      FormatDelimited.builder().dateTimeFormat("T").build().validate();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      FormatDelimited.builder().timeFormat("T").build().validate();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      FormatDelimited.builder().decimalFormat("a.b.c").build().validate();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      FormatDelimited.builder().booleanFormat("Bob").build().validate();
    });
    FormatDelimited.builder()
            .dateFormat(null)
            .dateTimeFormat(null)
            .timeFormat(null)
            .build().validate();
    FormatDelimited.builder()
            .dateFormat("")
            .dateTimeFormat("")
            .timeFormat("")
            .build().validate();
  }
    
}
