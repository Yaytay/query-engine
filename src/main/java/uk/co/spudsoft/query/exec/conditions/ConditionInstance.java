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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.Condition;

/**
 *
 * @author jtalbut
 */
public class ConditionInstance {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ConditionInstance.class);
  
  private static final JexlEngine JEXL = new JexlBuilder().create();
  
  private final JexlExpression expression;

  public static boolean isNullOrBlank(Condition condition) {
    return condition == null
            || condition.getExpression() == null
            || condition.getExpression().isBlank()
            ;
  }
  
  public ConditionInstance(String expression) {
    this.expression = JEXL.createExpression(expression);
  }

  public String getSourceText() {
    return expression.getSourceText();
  }

  public String getParsedText() {
    return expression.getParsedText();
  }
  
  public boolean evaluate(RequestContext request) {
    JexlContext context = new MapContext();
    context.set("req", request);
    
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
