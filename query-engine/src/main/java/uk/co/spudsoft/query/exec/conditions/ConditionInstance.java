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

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.Condition;
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
  
  private static final JexlEngine JEXL = new JexlBuilder()
          .permissions(
                  JexlPermissions.RESTRICTED
                          .compose(
                                  "io.vertx.core.http.impl.headers.*"
                                  , "uk.co.spudsoft.jwtvalidatorvertx.*"
                                  , "uk.co.spudsoft.query.exec.conditions.*"
                                   
                          )
          )
          .strict(false)
          .silent(true)
          .create();
  
  private final JexlExpression expression;

  /**
   * Return true if the condition or its expression is null or blank.
   * @param condition The condition being assessed.
   * @return true if the condition or its expression is null or blank.
   */
  public static boolean isNullOrBlank(Condition condition) {
    return condition == null
            || condition.getExpression() == null
            || condition.getExpression().isBlank()
            ;
  }
  
  /**
   * Constructor.
   * @param expression The <a href="https://commons.apache.org/proper/commons-jexl/">JEXL</a> expression.
   */
  public ConditionInstance(String expression) {
    this.expression = JEXL.createExpression(expression);
  }

  /**
   * Get the source text of the expression.
   * @return the source text of the expression.
   */
  public String getSourceText() {
    return expression.getSourceText();
  }

  /**
   * Get the parsed test of the expression.
   * @return the parsed test of the expression.
   */
  public String getParsedText() {
    return expression.getParsedText();
  }
  
  /**
   * Evaluate the expression for the given RequestContext and DataRow, which may be null.
   * @param request The context of the request.
   * @param row The current DataRow, if this expression is to be evaluated in the context of a row.
   * @return true is the expression evaluates to true.
   */
  // Compare the bindings with PipelineInstance#renderTemplate and ProcessorScriptInstance#runSource
  public boolean evaluate(RequestContext request, DataRow row) {
    JexlContext context = new MapContext();
    context.set("request", request);
    if (request != null) {
      context.set("uri", request.getUri());
      context.set("params", request.getParams());
      context.set("args", request.getArguments());
    }
    if (row != null) {
      context.set("row", row);
    } else {
      context.set("row", DataRow.EMPTY_ROW);
    }
    
    Object result = expression.evaluate(context);
    if (result instanceof Boolean b) {
      return b;
    } else if (result == null) {
      logger.trace("The result of expression \"{}\" was null", expression);
      return false;
    } else if (result instanceof String s) {
      logger.warn("The result of expression \"{}\" was \"{}\", avoid returning strings", expression, s);
      return Boolean.parseBoolean(s);
    } else {
      logger.warn("The result of expression \"{}\" was <{}>, should have been a Boolean", expression, result);
      return false;
    }
    
  }
  
}
