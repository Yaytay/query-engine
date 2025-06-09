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
package uk.co.spudsoft.query.exec.conditions;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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
 * 
 * <ul>
 * <li>request<br>
 * Details of the HTTP request, a {@link RequestContext} object.
 * 
 * <li>args<br>
 * Processed arguments to the pipeline, as a {@link java.util.Map Map} from argument name to either a single object or a {@link java.util.List List} of objects.
 * Each argument will be the converted argument passed in (or the result of evaluating the {@link uk.co.spudsoft.query.defn.Argument#defaultValueExpression defaultValueExpresson}.
 * 
 * <li>uri<br>
 * The full URI of the request as a Java {@link java.net.URI URI}.
 * 
 * <li>params<br>
 * The raw query string arguments to the HTTP request, a {@link io.vertx.core.MultiMap MultiMap} object.
 * Avoid using this variable, prefer the &quot;args&quot; variable.
 * 
 * <li>row<br>
 * If the condition is per-row it will include the {@link uk.co.spudsoft.query.exec.DataRow} object.
 * 
 * <li>iteration<br>
 * A number that is incremented each time this instance of the expression is evaluated.
 * This can be used as a surrogate for a row number on expressions evaluated whilst processing a stream, otherwise it is not very useful.
 * 
 * </ul>
 * 
 * Conditions are <a href="uk.co.spudsoft.query.defn.Condition">JEXL</a> expressions.
 * 
 * @author jtalbut
 */
public class JexlEvaluator {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(JexlEvaluator.class);
  
  private static final JexlEngine JEXL = createJexlEngine();
          
  static JexlEngine createJexlEngine() {
    Map<String, Object> namespaces = new HashMap<>();
    namespaces.put(null, new TopLevelJexlFunctions());
    
    JexlEngine jexl = new JexlBuilder()
          .permissions(
                  JexlPermissions.RESTRICTED
                          .compose(
                                  "io.vertx.core.http.impl.headers.*"
                                  , "uk.co.spudsoft.jwtvalidatorvertx.*"
                                  , "uk.co.spudsoft.query.exec.conditions.*"
                                  , "java.time.*"
                                   
                          )
          )
          .strict(false)
          .silent(true)
          .namespaces(namespaces)
          .create();
    return jexl;
  }

  private final JexlExpression expression;
  private final AtomicInteger iteration = new AtomicInteger();

  /**
   * Get the JEXL engine.
   * This engine must not be modified by callers.
   * @return the JEXL engine.
   */
  @SuppressFBWarnings("MS_EXPOSE_REP")
  public static JexlEngine getJexlEngine() {
    return JEXL;
  }  
  
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
  public JexlEvaluator(String expression) {
    this.expression = JEXL.createExpression(expression);
  }
  
  /**
   * Parse the JEXL expression and throw away the result.
   * This is for validation.
   * @param expression The <a href="https://commons.apache.org/proper/commons-jexl/">JEXL</a> expression to validate.
   */
  public static void parse(String expression) {
    JEXL.createExpression(expression);
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
    Object result = evaluateAsObject(request, row);
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
  

  /**
   * Evaluate the expression for the given RequestContext and DataRow, which may be null.
   * @param request The context of the request.
   * @param row The current DataRow, if this expression is to be evaluated in the context of a row.
   * @return the result of the expression, which may be of any type.
   */  
  public Object evaluateAsObject(RequestContext request, DataRow row) {
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
    context.set("iteration", iteration.getAndIncrement());
    
    return expression.evaluate(context);    
  }
  
}
