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
package uk.co.spudsoft.query.main;

import static org.junit.Assert.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author jtalbut
 */
public class ExceptionToStringTest {
  
  /**
   * Test of convert method, of class ExceptionToString.
   */
  @Test
  public void testConvert() {
    assertEquals(null, ExceptionToString.convert(null, "; "));    
    assertEquals("IllegalAccessError@uk.co.spudsoft.query.main.ExceptionToStringTest:34", ExceptionToString.convert(new IllegalAccessError(), "; "));    
  }
  
}
