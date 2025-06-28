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
package uk.co.spudsoft.query.exec.conditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.DataRow;

/**
 * An instance of a {@link uk.co.spudsoft.query.defn.Condition} to be evaluated before doing something else.
 * 
 * The context of a condition may include the following variables:
 * <ul>
 * <li>request
 * Details of the HTTP request, a {@link RequestContext} object.
 * <li>args
 * Query string arguments to the HTTP request, a {@link io.vertx.core.MultiMap} object.
 * <li>row
 * If the condition is per-row it will include the {@link uk.co.spudsoft.query.exec.DataRow} object.
 * </ul>
 * 
 * Conditions are <a href="uk.co.spudsoft.query.defn.Condition">JEXL</a> expressions.
 * 
 * @author jtalbut
 */
public class ConditionInstance {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ConditionInstance.class);
  
  private final JexlEvaluator evaluator;
  
  /**
   * Constructor.
   * @param expression The <a href="https://commons.apache.org/proper/commons-jexl/">JEXL</a> expression.
   */
  public ConditionInstance(String expression) {
    this.evaluator = new JexlEvaluator(expression);
  }

  /**
   * Get the source text of the expression.
   * @return the source text of the expression.
   */
  public String getSourceText() {
    return evaluator.getSourceText();
  }

  /**
   * Evaluate the expression for the given RequestContext and DataRow, which may be null.
   * @param request The context of the request.
   * @param row The current DataRow, if this expression is to be evaluated in the context of a row.
   * @return true is the expression evaluates to true.
   */
  // Compare the bindings with PipelineInstance#renderTemplate and ProcessorScriptInstance#runSource
  public boolean evaluate(RequestContext request, DataRow row) {
    boolean result = evaluator.evaluate(request, row);
    if (!result && logger.isDebugEnabled()) {
      String sourceText = evaluator.getSourceText();
      if (sourceText != null) {
        sourceText = sourceText.replaceAll("\\p{Cntrl}", "#");
      }
      logger.debug("Condition {} ({}) returned false", sourceText);
    }
    return result;
  }
  
}
