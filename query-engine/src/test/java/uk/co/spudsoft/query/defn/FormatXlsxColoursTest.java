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
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 *
 * @author jtalbut
 */
public class FormatXlsxColoursTest {
  
  /**
   * Test of validate method, of class FormatXlsxColours.
   */
  @Test
  public void testValidate() {
    FormatXlsxColours.builder().build().validate();
    FormatXlsxColours.builder().fgColour("000000").bgColour("FFFFFF").build().validate();
    assertThrows(IllegalArgumentException.class, () -> {
      FormatXlsxColours.builder().fgColour("000000").bgColour("bob").build().validate();    
    });
    assertThrows(IllegalArgumentException.class, () -> {
      FormatXlsxColours.builder().fgColour("0000007").bgColour("123456").build().validate();    
    });
  }

  /**
   * Test of toColourDefinition method, of class FormatXlsxColours.
   */
  @Test
  public void testToColourDefinition() {
    assertEquals("FF000000", FormatXlsxColours.builder().build().toColourDefinition().fgColour);
    assertEquals("FFFFFFFF", FormatXlsxColours.builder().build().toColourDefinition().bgColour);
  }

}
