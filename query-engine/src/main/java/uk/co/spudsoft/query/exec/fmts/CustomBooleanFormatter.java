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

import com.google.common.base.Strings;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.exec.dynamic.JexlEvaluator;
import uk.co.spudsoft.query.exec.context.PipelineContext;

/**
 * Formatter to convert a Boolean Object into any string value.
 *
 * The expression must be a valid JEXL expression that evaluates to a two element array of strings.
 * The first element is the true value and the second element is the false value.
 *
 * The values from the array can be:
 * <UL>
 * <LI>true  (may, or may not, be required to be lowercase)
 * <LI>false  (may, or may not, be required to be lowercase)
 * <LI>A numeric value
 * <LI>A string value
 * </UL>
 * The following are all examples of valid expressions:
 * <UL>
 * <LI>['true', 'false']
 * Valid, but pointless, because this is the default behaviour.
 * <LI>['True', 'False']
 * Python formatting.
 * <LI>['1', '0']
 * Output a numeric 1 or 0.
 * <LI>['"1"', '"0"']
 * Output a quoted "1" or "0".
 * <LI>['"yes"', '"no"']
 * Output a quoted "yes" or "no".
 * </UL>
 * <P>
 * Validation is carried out on the output from the expression, but this validation is not perfect and it is possible to produce an invalid output with a bad format.
 *
 * If the expression is null the output will be "true" or "false".
 *
 * @author jtalbut
 */
public final class CustomBooleanFormatter implements CustomFormatter {

  private final String trueValue;
  private final String falseValue;

  /**
   * Create a formatter for Boolean values.
   * @param expression The JEXL expression that must evaluate to an array of two strings.
   * @param openQuote The quote that all string values must begin with.
   * @param closeQuote The quote that all string values must end with.
   * @param lowerCaseOnly If true then the validation will only accept "true" and "false" in lowercase, any other value must be quoted.
   */
  public CustomBooleanFormatter(String expression, String openQuote, String closeQuote, boolean lowerCaseOnly) {
    JexlExpression jexl;

    if (Strings.isNullOrEmpty(expression)) {
      trueValue = "true";
      falseValue = "false";
    } else {

      try {
        jexl = JexlEvaluator.getJexlEngine().createExpression(expression);
      } catch (Throwable ex) {
        String message = ex.getMessage();
        int idx = message.indexOf(" ");
        if (idx > 0) {
          message = message.substring(idx);
        }
        throw new IllegalArgumentException("Unable to parse \"" + expression + "\" as a JEXL expression: " + message.trim());
      }
      JexlContext context = new MapContext();
      Object value = jexl.evaluate(context);

      if (!(value instanceof Object[] array) || array.length != 2
              || !(array[0] instanceof String) || !(array[1] instanceof String)) {
        throw new IllegalArgumentException("Expression must evaluate to a two-element array of strings: " + expression);
      }

      this.trueValue = (String) array[0];
      this.falseValue = (String) array[1];

      if (openQuote == null) {
        openQuote = "\"";
      }
      if (closeQuote == null) {
        closeQuote = "\"";
      }
      validateValue("true", trueValue, openQuote, closeQuote, lowerCaseOnly);
      validateValue("false", falseValue, openQuote, closeQuote, lowerCaseOnly);
    }
  }

  private static void validateValue(String name, String value, String openQuote, String closeQuote, boolean lowerCaseOnly) {

    if (lowerCaseOnly) {
      if ("true".equals(value) || "false".equals(value)) {
        return ;
      }
    } else {
      if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
        return ;
      }
    }

    try {
      Double.parseDouble(value);
      return ;
    } catch (Throwable ex) {
    }

    if (value.startsWith(openQuote) && value.endsWith(closeQuote)) {
      return ;
    }

    throw new IllegalArgumentException("The " + name + " value is not valid (is not \"true\" or \"false\"; is not a number; does not begin with " + openQuote + " and end with " + closeQuote);

  }

  /**
   * Format an object as a Boolean value.
   * <P>
   * It is preferable if the value is a Boolean, but it need not be.
   *
   * @param pipelineContext The context in which this {@link SourcePipeline} is being run.
   * @param value The value to be formatted as a Boolean.
   * @return A string value that can be output.
   */
  @Override
  public String format(PipelineContext pipelineContext, Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Boolean boolValue) {
      return boolValue ? trueValue : falseValue;
    }
    if (value instanceof Number numberValue) {
      return numberValue.doubleValue() == 0.0 ? falseValue : trueValue;
    }
    String stringValue = value.toString();
    return "true".equalsIgnoreCase(stringValue) ? trueValue : falseValue;
  }
}
