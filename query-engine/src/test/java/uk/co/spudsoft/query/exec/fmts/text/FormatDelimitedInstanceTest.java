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
package uk.co.spudsoft.query.exec.fmts.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.defn.FormatDelimited;

/**
 *
 * @author njt
 */
public class FormatDelimitedInstanceTest {
  
  @Test
  public void testEncodeCloseQuote() {
    assertEquals("bob", FormatDelimitedInstance.encodeCloseQuote(FormatDelimited.builder().build(), "bob"));
    assertEquals("\\\"bob\\\"", FormatDelimitedInstance.encodeCloseQuote(FormatDelimited.builder().escapeCloseQuote("\\").build(), "\"bob\""));
    assertEquals("&quot;bob&quot;", FormatDelimitedInstance.encodeCloseQuote(FormatDelimited.builder().replaceCloseQuote("&quot;").build(), "\"bob\""));    
  }
  
}
