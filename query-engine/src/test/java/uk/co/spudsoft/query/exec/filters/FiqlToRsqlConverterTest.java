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
package uk.co.spudsoft.query.exec.filters;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FiqlToRsqlConverter class.
 *
 * @author jtalbut
 */
public class FiqlToRsqlConverterTest {

  @Test
  public void testConvertSimpleExpressionWithSpaces() {
    String input = "Name==Fred Jones";
    String expected = "Name=='Fred Jones'";
    String result = FiqlToRsqlConverter.convertFiqlToRsql(input);
    assertEquals(expected, result);
  }

  @Test
  public void testDontConvertSimpleExpressionThatIsntEquals() {
    String input = "Name=le=Fred Jones";
    String expected = "Name=le=Fred Jones";
    String result = FiqlToRsqlConverter.convertFiqlToRsql(input);
    assertEquals(expected, result);
  }

  @Test
  public void testConvertSimpleExpressionWithoutSpaces() {
    String input = "Name==Fred";
    String expected = "Name==Fred"; // Should remain unchanged
    String result = FiqlToRsqlConverter.convertFiqlToRsql(input);
    assertEquals(expected, result);
  }

  @Test
  public void testConvertNotEqualsOperator() {
    String input = "Status!=Active User";
    String expected = "Status!='Active User'";
    String result = FiqlToRsqlConverter.convertFiqlToRsql(input);
    assertEquals(expected, result);
  }

  @Test
  public void testAlreadyQuotedExpression() {
    String input = "Name=='Fred Jones'";
    String expected = "Name=='Fred Jones'"; // Should remain unchanged
    String result = FiqlToRsqlConverter.convertFiqlToRsql(input);
    assertEquals(expected, result);
  }

  @Test
  public void testExpressionWithMultipleTermsLogicalAnd() {
    String input = "Name==Fred Jones;Age==25";
    String expected = "Name==Fred Jones;Age==25"; // Should remain unchanged
    String result = FiqlToRsqlConverter.convertFiqlToRsql(input);
    assertEquals(expected, result);
  }

  @Test
  public void testExpressionWithMultipleTermsLogicalOr() {
    String input = "Name==Fred Jones,Age==25";
    String expected = "Name==Fred Jones,Age==25"; // Should remain unchanged
    String result = FiqlToRsqlConverter.convertFiqlToRsql(input);
    assertEquals(expected, result);
  }

  @Test
  public void testExpressionWithAndKeyword() {
    String input = "Name==Fred Jones and Age==25";
    String expected = "Name==Fred Jones and Age==25"; // Should remain unchanged
    String result = FiqlToRsqlConverter.convertFiqlToRsql(input);
    assertEquals(expected, result);
  }

  @Test
  public void testExpressionWithOrKeyword() {
    String input = "Name==Fred Jones or Age==25";
    String expected = "Name==Fred Jones or Age==25"; // Should remain unchanged
    String result = FiqlToRsqlConverter.convertFiqlToRsql(input);
    assertEquals(expected, result);
  }

  @Test
  public void testNullInput() {
    String result = FiqlToRsqlConverter.convertFiqlToRsql(null);
    assertNull(result);
  }

  @Test
  public void testEmptyInput() {
    String input = "";
    String result = FiqlToRsqlConverter.convertFiqlToRsql(input);
    assertEquals(input, result);
  }

  @Test
  public void testWhitespaceOnlyInput() {
    String input = "   ";
    String result = FiqlToRsqlConverter.convertFiqlToRsql(input);
    assertEquals(input, result);
  }

  @Test
  public void testValueWithSpecialCharacters() {
    String input = "Description==Test (with parentheses)";
    String expected = "Description=='Test (with parentheses)'";
    String result = FiqlToRsqlConverter.convertFiqlToRsql(input);
    assertEquals(expected, result);
  }

  @Test
  public void testValueWithSemicolon() {
    String input = "Query==SELECT * FROM users; DROP TABLE users";
    String expected = "Query=='SELECT * FROM users; DROP TABLE users'";
    String result = FiqlToRsqlConverter.convertFiqlToRsql(input);
    assertEquals(expected, result);
  }

  @Test
  public void testValueWithComma() {
    String input = "Name==Smith, John";
    String expected = "Name=='Smith, John'";
    String result = FiqlToRsqlConverter.convertFiqlToRsql(input);
    assertEquals(expected, result);
  }

  @Test
  public void testValueWithSingleQuote() {
    String input = "Name==O'Connor";
    String expected = "Name=='O'Connor'";
    String result = FiqlToRsqlConverter.convertFiqlToRsql(input);
    assertEquals(expected, result);
  }

  @Test
  public void testValueWithDoubleQuote() {
    String input = "Message==He said \"Hello\"";
    String expected = "Message=='He said \"Hello\"'";
    String result = FiqlToRsqlConverter.convertFiqlToRsql(input);
    assertEquals(expected, result);
  }

  @Test
  public void testNumericValue() {
    String input = "Age==25";
    String expected = "Age==25"; // Should remain unchanged
    String result = FiqlToRsqlConverter.convertFiqlToRsql(input);
    assertEquals(expected, result);
  }

  @Test
  public void testFieldNameWithUnderscore() {
    String input = "first_name==John Doe";
    String expected = "first_name=='John Doe'";
    String result = FiqlToRsqlConverter.convertFiqlToRsql(input);
    assertEquals(expected, result);
  }

  @Test
  public void testFieldNameWithNumbers() {
    String input = "field123==Value with spaces";
    String expected = "field123=='Value with spaces'";
    String result = FiqlToRsqlConverter.convertFiqlToRsql(input);
    assertEquals(expected, result);
  }

  @Test
  public void testPartiallyQuotedExpression() {
    String input = "Name=='Fred' and Age==25";
    String expected = "Name=='Fred' and Age==25"; // Should remain unchanged
    String result = FiqlToRsqlConverter.convertFiqlToRsql(input);
    assertEquals(expected, result);
  }

  @Test
  public void testInvalidFieldName() {
    String input = "123field==value with spaces";
    String expected = "123field==value with spaces"; // Should remain unchanged (invalid field name)
    String result = FiqlToRsqlConverter.convertFiqlToRsql(input);
    assertEquals(expected, result);
  }

  @Test
  public void testNoOperator() {
    String input = "Name Fred Jones";
    String expected = "Name Fred Jones"; // Should remain unchanged (no operator)
    String result = FiqlToRsqlConverter.convertFiqlToRsql(input);
    assertEquals(expected, result);
  }

  @Test
  public void testMultipleSpacesInValue() {
    String input = "Name==Fred    Jones";
    String expected = "Name=='Fred    Jones'";
    String result = FiqlToRsqlConverter.convertFiqlToRsql(input);
    assertEquals(expected, result);
  }

  @Test
  public void testValueWithLeadingTrailingSpaces() {
    String input = "Name== Fred Jones ";
    String expected = "Name==' Fred Jones '";
    String result = FiqlToRsqlConverter.convertFiqlToRsql(input);
    assertEquals(expected, result);
  }
}
