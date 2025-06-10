/*
 * Copyright (C) 2025 jtalbut
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 *
 * @author jtalbut
 */
public class CustomBooleanFormatterTest {
  
  @Test
  public void testFormatValidExpressionBad() {
    assertEquals("Unable to parse \"...\" as a JEXL expression: parsing error in '...'", assertThrows(IllegalArgumentException.class, () -> {
      new CustomBooleanFormatter("...", "<", ">", true);
    }).getMessage());
  }
  
  @Test
  public void testFormatValidExpressionNotArrays() {
    assertEquals("Expression must evaluate to a two-element array of strings.", assertThrows(IllegalArgumentException.class, () -> {
      new CustomBooleanFormatter("1", "<", ">", true);
    }).getMessage());
  }
  
  @Test
  public void testFormatTwoStringsNotValid() {
    assertEquals("The true value is not valid (is not \"true\" or \"false\"; is not a number; does not begin with < and end with >", assertThrows(IllegalArgumentException.class, () -> {
      new CustomBooleanFormatter("['bob', 'fred']", "<", ">", true);
    }).getMessage());
  }
  
  @Test
  public void testFormatQuotingBad() {
    assertEquals("The false value is not valid (is not \"true\" or \"false\"; is not a number; does not begin with < and end with >", assertThrows(IllegalArgumentException.class, () -> {
      new CustomBooleanFormatter("['<bob>', 'fred']", "<", ">", true);
    }).getMessage());
    assertEquals("The false value is not valid (is not \"true\" or \"false\"; is not a number; does not begin with < and end with >", assertThrows(IllegalArgumentException.class, () -> {
      new CustomBooleanFormatter("['<bob>', '\"fred']", "<", ">", true);
    }).getMessage());
    assertEquals("The false value is not valid (is not \"true\" or \"false\"; is not a number; does not begin with < and end with >", assertThrows(IllegalArgumentException.class, () -> {
      new CustomBooleanFormatter("['<bob>', 'fred\"']", "<", ">", true);
    }).getMessage());
    assertEquals("The false value is not valid (is not \"true\" or \"false\"; is not a number; does not begin with < and end with >", assertThrows(IllegalArgumentException.class, () -> {
      new CustomBooleanFormatter("['true', 'False']", "<", ">", false);
    }).getMessage());
  }
  
  @Test
  public void testFormatValid() {
    CustomBooleanFormatter formatter = new CustomBooleanFormatter("['\"1\"', '\"0\"']", "\"", "\"", true);
    
    assertNull(formatter.format(null));
    
    assertEquals("\"1\"", formatter.format(Boolean.TRUE));
    assertEquals("\"0\"", formatter.format(Boolean.FALSE));

    formatter = new CustomBooleanFormatter("['true', 'false']", "<", ">", true);

    assertEquals("true", formatter.format(1));
    assertEquals("false", formatter.format(0));

    assertEquals("true", formatter.format("true"));
    assertEquals("false", formatter.format("not true"));

    formatter = new CustomBooleanFormatter("['true', 'fALSe']", "<", ">", false);

    assertEquals("true", formatter.format(1));
    assertEquals("fALSe", formatter.format(0));

    formatter = new CustomBooleanFormatter("['1', '0']", "<", ">", true);

    assertEquals("1", formatter.format(1));
    assertEquals("0", formatter.format(0));

    assertEquals("1", formatter.format("true"));
    assertEquals("0", formatter.format("not true"));
  }
  
  
}
