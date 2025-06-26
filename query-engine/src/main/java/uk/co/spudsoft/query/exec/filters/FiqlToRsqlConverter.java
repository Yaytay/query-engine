
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static class for converting FIQL queries into RSQL queries.
 * At this time all this does is ensure appropriate quoting for == and != arguments.
 * @author jtalbut
 */
public class FiqlToRsqlConverter {

  private static final Pattern FIQL_PATTERN = Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_]*)(==|!=)(.*)");
  private static final Pattern MULTIPLE_TERMS_PATTERN = Pattern.compile(".*[a-zA-Z_][a-zA-Z0-9_]*\\s*(==|!=).*[;,].*[a-zA-Z_][a-zA-Z0-9_]*\\s*(==|!=).*");
  private static final Pattern QUOTED_VALUES_PATTERN = Pattern.compile(".*=='[^']*'.*");

  /**
   * Private constructor.
   */
  private FiqlToRsqlConverter() {
  }
  
  /**
   * Converts a FIQL expression to RSQL format by adding single quotes around unquoted values. Only modifies expressions that need
   * conversion and don't already contain multiple RSQL terms.
   *
   * @param fiqlExpression the original FIQL expression
   * @return the converted RSQL expression, or the original if no conversion needed
   */
  public static String convertFiqlToRsql(String fiqlExpression) {
    if (fiqlExpression == null || fiqlExpression.trim().isEmpty()) {
      return fiqlExpression;
    }

    // Check if expression already contains single-quoted values (likely already RSQL)
    if (containsQuotedValues(fiqlExpression)) {
      return fiqlExpression;
    }

    // Check if expression contains multiple terms (avoid complex cases)
    if (containsMultipleTerms(fiqlExpression)) {
      return fiqlExpression;
    }

    Matcher matcher = FIQL_PATTERN.matcher(fiqlExpression);
    if (matcher.find()) {
      String fieldName = matcher.group(1);
      String operator = matcher.group(2);
      String value = matcher.group(3);

      // Only quote if value contains spaces or special characters that need quoting
      if (needsQuoting(value)) {
        return fieldName + operator + "'" + value + "'";
      }
    }

    // Return original if no match or no quoting needed
    return fiqlExpression;
  }

  /**
   * Checks if the expression already contains single-quoted values
   */
  private static boolean containsQuotedValues(String expression) {
    return expression.contains("'") && QUOTED_VALUES_PATTERN.matcher(expression).matches();
  }

  /**
   * Checks if the expression contains multiple terms (logical operators)
   */
  private static boolean containsMultipleTerms(String expression) {
    // Look for patterns that indicate multiple field==value or field!=value expressions
    // separated by logical operators
    return MULTIPLE_TERMS_PATTERN.matcher(expression).matches() ||
           expression.contains(" and ") || expression.contains(" or ");
  }

  /**
   * Determines if a value needs to be quoted (contains spaces or special characters)
   */
  private static boolean needsQuoting(String value) {
    return value.contains(" ")
            || value.contains("'")
            || value.contains("\"")
            || value.contains(";")
            || value.contains(",")
            || value.contains("(")
            || value.contains(")");
  }
}