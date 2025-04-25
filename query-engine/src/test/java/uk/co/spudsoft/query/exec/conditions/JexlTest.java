/*
 * Copyright (C) 2024 jtalbut
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

import io.vertx.core.MultiMap;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static uk.co.spudsoft.query.exec.conditions.JexlEvaluator.createJexlEngine;

/**
 *
 * @author jtalbut
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class JexlTest {

  private static final Logger logger = LoggerFactory.getLogger(JexlTest.class);

  private static final JexlEngine JEXL = createJexlEngine();

  static String getCurrentMethodName() {
    return StackWalker.getInstance()
            .walk(s -> s.skip(1).findFirst())
            .get()
            .getMethodName();
  }

  @Test
  public void testNotInContext() {

    String exp = "args";
    JexlExpression expression = JEXL.createExpression(exp);

    JexlContext context = new MapContext();
    Object result = expression.evaluate(context);
    logger.debug("{} {}: result: {} ({})", getCurrentMethodName(), exp, result, result == null ? null : result.getClass());

  }

  @Test
  public void testInContextHashMap() {

    String exp = "args";
    JexlExpression expression = JEXL.createExpression(exp);

    JexlContext context = new MapContext();
    Map<String, Object> args = new HashMap<>();
    context.set("args", args);

    Object result = expression.evaluate(context);
    logger.debug("{} {}: result: {} ({})", getCurrentMethodName(), exp, result, result == null ? null : result.getClass());

  }

  @Test
  public void testInContextMultiMap() {

    String exp = "args";
    JexlExpression expression = JEXL.createExpression(exp);

    JexlContext context = new MapContext();
    MultiMap args = new HeadersMultiMap();
    context.set("args", args);

    Object result = expression.evaluate(context);
    logger.debug("{} {}: result: {} ({})", getCurrentMethodName(), exp, result, result == null ? null : result.getClass());

  }

  @Test
  public void testInContextHashMapWithValue() {

    String exp = "args['param1']";
    JexlExpression expression = JEXL.createExpression(exp);

    JexlContext context = new MapContext();
    Map<String, Object> args = new HashMap<>();
    context.set("args", args);
    args.put("param1", "true");

    Object result = expression.evaluate(context);
    logger.debug("{} {}: result: {} ({})", getCurrentMethodName(), exp, result, result == null ? null : result.getClass());

  }

  @Test
  public void testInContextMultiMapWithValue() {

    String exp = "args['param1']";
    JexlExpression expression = JEXL.createExpression(exp);

    JexlContext context = new MapContext();
    MultiMap args = new HeadersMultiMap();
    context.set("args", args);
    args.add("param1", "true");

    Object result = expression.evaluate(context);
    logger.debug("{} {}: result: {} ({})", getCurrentMethodName(), exp, result, result == null ? null : result.getClass());

  }

  @Test
  public void testInContextHashMapWithValueUsingGet() {

    String exp = "args.get('param1')";
    JexlExpression expression = JEXL.createExpression(exp);

    JexlContext context = new MapContext();
    Map<String, Object> args = new HashMap<>();
    context.set("args", args);
    args.put("param1", "true");

    Object result = expression.evaluate(context);
    logger.debug("{} {}: result: {} ({})", getCurrentMethodName(), exp, result, result == null ? null : result.getClass());

  }

  @Test
  public void testInContextMultiMapWithValueUsingGet() {

    String exp = "args.getAll('param1')";
    JexlExpression expression = JEXL.createExpression(exp);

    JexlContext context = new MapContext();
    MultiMap args = new HeadersMultiMap();
    context.set("args", args);
    args.add("param1", "true");

    Object result = expression.evaluate(context);
    logger.debug("{} {}: result: {} ({})", getCurrentMethodName(), exp, result, result == null ? null : result.getClass());

  }

  @Test
  public void testInContextHashMapWithoutValue() {

    String exp = "args['param2']";
    JexlExpression expression = JEXL.createExpression(exp);

    JexlContext context = new MapContext();
    Map<String, Object> args = new HashMap<>();
    context.set("args", args);
    args.put("param1", "true");

    Object result = expression.evaluate(context);
    logger.debug("{} {}: result: {} ({})", getCurrentMethodName(), exp, result, result == null ? null : result.getClass());

  }

  @Test
  public void testInContextMultiMapWithoutValue() {

    String exp = "args['param2']";
    JexlExpression expression = JEXL.createExpression(exp);

    JexlContext context = new MapContext();
    MultiMap args = new HeadersMultiMap();
    context.set("args", args);
    args.add("param1", "true");

    Object result = expression.evaluate(context);
    logger.debug("{} {}: result: {} ({})", getCurrentMethodName(), exp, result, result == null ? null : result.getClass());

  }

  @Test
  public void testAnd() {

    String exp = "andFn(true, true)";
    JexlExpression expression = JEXL.createExpression(exp);
    JexlContext context = new MapContext();
    Object result = expression.evaluate(context);
    logger.debug("{} {}: result: {} ({})", getCurrentMethodName(), exp, result, result == null ? null : result.getClass());
    assertEquals(Boolean.TRUE, result);

    exp = "andFn(true, false)";
    expression = JEXL.createExpression(exp);
    context = new MapContext();
    result = expression.evaluate(context);
    logger.debug("{} {}: result: {} ({})", getCurrentMethodName(), exp, result, result == null ? null : result.getClass());
    assertEquals(Boolean.FALSE, result);

    exp = "andFn(false, true)";
    expression = JEXL.createExpression(exp);
    context = new MapContext();
    result = expression.evaluate(context);
    logger.debug("{} {}: result: {} ({})", getCurrentMethodName(), exp, result, result == null ? null : result.getClass());
    assertEquals(Boolean.FALSE, result);

    exp = "andFn(false, false)";
    expression = JEXL.createExpression(exp);
    context = new MapContext();
    result = expression.evaluate(context);
    logger.debug("{} {}: result: {} ({})", getCurrentMethodName(), exp, result, result == null ? null : result.getClass());
    assertEquals(Boolean.FALSE, result);
  }

  @Test
  public void testOr() {

    String exp = "orFn(true, true)";
    JexlExpression expression = JEXL.createExpression(exp);
    JexlContext context = new MapContext();
    Object result = expression.evaluate(context);
    logger.debug("{} {}: result: {} ({})", getCurrentMethodName(), exp, result, result == null ? null : result.getClass());
    assertEquals(Boolean.TRUE, result);

    exp = "orFn(true, false)";
    expression = JEXL.createExpression(exp);
    context = new MapContext();
    result = expression.evaluate(context);
    logger.debug("{} {}: result: {} ({})", getCurrentMethodName(), exp, result, result == null ? null : result.getClass());
    assertEquals(Boolean.TRUE, result);

    exp = "orFn(false, true)";
    expression = JEXL.createExpression(exp);
    context = new MapContext();
    result = expression.evaluate(context);
    logger.debug("{} {}: result: {} ({})", getCurrentMethodName(), exp, result, result == null ? null : result.getClass());
    assertEquals(Boolean.TRUE, result);

    exp = "orFn(false, false)";
    expression = JEXL.createExpression(exp);
    context = new MapContext();
    result = expression.evaluate(context);
    logger.debug("{} {}: result: {} ({})", getCurrentMethodName(), exp, result, result == null ? null : result.getClass());
    assertEquals(Boolean.FALSE, result);
  }
  
  public static class MyMath {
    public double cos(final double x) {
      return Math.cos(x);
    }
  }

  /**
   * User namespace needs to be allowed through permissions.
   */
  @Test
  public void testCustomFunctionPermissions() {
      final Map<String, Object> funcs = new HashMap<>();
      funcs.put("math", new MyMath());
      final JexlEngine jexl = new JexlBuilder()
          .permissions(
                  JexlPermissions.RESTRICTED
                          .compose(
                                  "io.vertx.core.http.impl.headers.*"
                                  , "uk.co.spudsoft.jwtvalidatorvertx.*"
                                  , "uk.co.spudsoft.query.exec.conditions.*"
                                   
                          )
          )
          .strict(false)
          .silent(false)
          .namespaces(funcs)
          .create();
      
      final JexlContext jc = new MapContext();
      jc.set("pi", Math.PI);
      final JexlExpression e = jexl.createExpression("math:cos(pi)");
      final Number result = (Number) e.evaluate(jc);
      assertEquals(-1, result.intValue());
  }
  
}
