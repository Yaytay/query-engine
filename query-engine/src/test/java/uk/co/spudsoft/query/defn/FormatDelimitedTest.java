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

    assertEquals("\"", FormatDelimited.builder().build().getOpenQuote());
    assertEquals("$", FormatDelimited.builder().openQuote("$").build().getOpenQuote());

    assertEquals("\"", FormatDelimited.builder().build().getCloseQuote());
    assertEquals("$", FormatDelimited.builder().closeQuote("$").build().getCloseQuote());

    assertEquals("", FormatDelimited.builder().build().getEscapeCloseQuote());
    assertEquals("$", FormatDelimited.builder().escapeCloseQuote("$").build().getEscapeCloseQuote());

    assertEquals("", FormatDelimited.builder().build().getReplaceCloseQuote());
    assertEquals("$", FormatDelimited.builder().replaceCloseQuote("$").build().getReplaceCloseQuote());
  }
  
  @Test
  public void testValidate() {
    FormatDelimited.builder().build().validate();
    assertThrows(IllegalArgumentException.class, () -> {
      FormatDelimited.builder().name(null).build().validate();
    });
    FormatDelimited.builder().name("name").build().validate();
  }
    
}
