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
package uk.co.spudsoft.query.exec.fmts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author njt
 */
public class CustomDecimalFormatterTest {
  
  @Test
  public void testFormat() {
    
    CustomDecimalFormatter cdf;
    
    cdf = new CustomDecimalFormatter(null);
    assertFalse(cdf.mustBeEncodedAsString());
    assertEquals("1.23", cdf.format(1.23));
    assertEquals("12.3", cdf.format(12.3));
    assertEquals("123", cdf.format(123));
    
    cdf = new CustomDecimalFormatter("0.###");
    assertFalse(cdf.mustBeEncodedAsString());
    assertEquals("1.23", cdf.format(1.23));
    assertEquals("12.3", cdf.format(12.3));
    assertEquals("123", cdf.format(123));
    
    cdf = new CustomDecimalFormatter("0.000");
    assertFalse(cdf.mustBeEncodedAsString());
    assertEquals("1.230", cdf.format(1.23));
    assertEquals("12.300", cdf.format(12.3));
    assertEquals("123.000", cdf.format(123));
    
    cdf = new CustomDecimalFormatter("£0.000");
    assertTrue(cdf.mustBeEncodedAsString());
    assertEquals("£1.230", cdf.format(1.23));
    assertEquals("£12.300", cdf.format(12.3));
    assertEquals("£123.000", cdf.format(123));
    
    cdf = new CustomDecimalFormatter("0.###%");
    assertTrue(cdf.mustBeEncodedAsString());
    assertEquals("12.3%", cdf.format(.123));
    assertEquals("123%", cdf.format(1.23));
    assertEquals("1.23%", cdf.format(.0123));
    
  }
  
}
