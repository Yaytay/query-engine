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

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jtalbut
 */
public class ExceptionToStringTest {
  
  private static final Logger logger = LoggerFactory.getLogger(ExceptionToStringTest.class);
  
  /**
   * Test of convert method, of class ExceptionToString.
   */
  @Test
  public void testConvert() {
    assertEquals(null, ExceptionToString.convert(null, "; ", ",  ", 8));    
    assertEquals("IllegalAccessError@uk.co.spudsoft.query.main.ExceptionToStringTest:38", ExceptionToString.convert(new IllegalAccessError(), ";", "@", 1));
    
    assertEquals("IllegalArgumentException: Failed 0 @uk.co.spudsoft.query.main.ExceptionToStringTest:40; cause: IllegalStateException: Bad @uk.co.spudsoft.query.main.ExceptionToStringTest:40"
            , ExceptionToString.convert(new IllegalArgumentException("Failed 0", new IllegalStateException("Bad")), "; cause: ", " @", 1));
    
    assertEquals("""
                 IllegalArgumentException: Failed 1
                     uk.co.spudsoft.query.main.ExceptionToStringTest:43
                     jdk.internal.reflect.DirectMethodHandleAccessor:104
                    Caused by IllegalStateException: Bad
                     uk.co.spudsoft.query.main.ExceptionToStringTest:43
                     jdk.internal.reflect.DirectMethodHandleAccessor:104"""
            , ExceptionToString.convert(new IllegalArgumentException("Failed 1", new IllegalStateException("Bad")), "\n   Caused by ", "\n    ", 2));
    
    assertEquals("""
                 IllegalArgumentException: Failed 2
                     at uk.co.spudsoft.query.main.ExceptionToStringTest:52
                     at jdk.internal.reflect.DirectMethodHandleAccessor:104
                   IllegalStateException: Bad
                     at uk.co.spudsoft.query.main.ExceptionToStringTest:52
                     at jdk.internal.reflect.DirectMethodHandleAccessor:104"""
            , ExceptionToString.convert(new IllegalArgumentException("Failed 2", new IllegalStateException("Bad")), "\n  ", "\n    at ", 2));
  }
  
}
