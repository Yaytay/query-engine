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
package uk.co.spudsoft.query.web.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.defn.Format;
import uk.co.spudsoft.query.defn.FormatDelimited;

/**
 *
 * @author njt
 */
public class FormatDetailsTest {
  
  @Test
  public void testConstructor() {
    Format fmt1 = FormatDelimited.builder().build();
    FormatDetails fd = new FormatDetails(fmt1);
    assertEquals("text/csv", fd.getMediaType().toString());
    
    Format fmt2 = FormatDelimited.builder().hidden(true).build();
    assertEquals("Attempt to output hidden format", assertThrows(IllegalStateException.class, () -> {
      new FormatDetails(fmt2);
    }).getMessage());
  }

}
