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
package uk.co.spudsoft.query.json;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import com.googlecode.aviator.Expression;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorType;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.codehaus.janino.ExpressionEvaluator;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

/**
 *
 * @author jtalbut
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TemplatePerformanceTest {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(TemplatePerformanceTest.class);

  /**
   * Counts of iterations for the tests.
   * These values are too low to be meaningful, use much larger values for reasonable testing (10000+).
   * For reference, these figures were just generated on an Intel i9-12900HK:
   * Method                                   JVM                  Iterations Duration/s Rate (iters/s)
   * Jexl                                     Zulu21.38+21-CA           10000      0.480      20833.333
   * JexlCachedExpression                     Zulu21.38+21-CA           10000      0.004    2500000.000
   * Polyglot                                 Zulu21.38+21-CA           10000     18.910        528.821
   * PolyglotEngineSourceCache                Zulu21.38+21-CA           10000      4.191       2386.065
   * PolyglotLatencyModeEngine                Zulu21.38+21-CA           10000      4.368       2289.377
   * PolyglotThroughputModeEngine             Zulu21.38+21-CA           10000      4.912       2035.831
   * PolyglotThroughputModeEngineSourceCache  Zulu21.38+21-CA           10000      4.209       2375.861
   * StringTemplate                           Zulu21.38+21-CA           10000      0.108      92592.593
   * Velocity                                 Zulu21.38+21-CA           10000      0.393      25445.293
   *
   * New numbers on an Ubuntu Hyper-V VM with an Intel i9-13900H CPU on a Windows laptop:
   * Method                                   JVM                  Iterations Duration/s Rate (iters/s)
   * Aviator                                  Zulu25.36+15-CA          100000     14.271       7007.217
   * AviatorPrecompiledWithSandboxing         Zulu25.36+15-CA          100000      0.026    3846153.846
   * Janino                                   Zulu25.36+15-CA          100000     26.199       3816.940
   * JaninoPrecompiled                        Zulu25.36+15-CA          100000      0.007   14285714.286
   * Jexl                                     Zulu25.36+15-CA          100000      4.515      22148.394
   * JexlCachedExpression                     Zulu25.36+15-CA          100000      0.023    4347826.087
   * StringTemplate                           Zulu25.36+15-CA          100000      0.481     207900.208
   * Velocity                                 Zulu25.36+15-CA          100000      1.510      66225.166
   *
   */
  private final static int WARMUPS = TemplatePerformanceTest.class.getCanonicalName().equals(System.getProperty("test")) ? 100000 : 100;
  private final static int TIMED = TemplatePerformanceTest.class.getCanonicalName().equals(System.getProperty("test")) ? 100000 : 100;

  private final static List<String> EXPECTED_RESULTS = buildExpectedResults();

  private static List<String> buildExpectedResults() {
    List<String> results = new ArrayList<>(TIMED);
    for (int i = 0; i < TIMED; ++i) {
      String hello = "Hello, " + i + "!";
      results.add(hello);
    }
    return results;
  }

  private static String findTidyName() {
    StackWalker walker = StackWalker.getInstance();
    Optional<String> methodName = walker.walk(frames -> frames
      .skip(1)
      .findFirst()
      .map(StackWalker.StackFrame::getMethodName));
    return methodName.get().substring(4);
  }

  /**
   * Output the headers.
   * To be honest, this javadoc only exists to push the log line down to > line 100 so that the output lines up with the rest.
   */
  @BeforeAll
  public void headers() {
    logger.warn("{}", String.format("%-40s %-20s %10s %10s %14s", "Method", "JVM", "Iterations", "Duration/s", "Rate (iters/s)"));
  }

  @Test
  public void testStringTemplate() {

    List<String> results = new ArrayList<>(Math.max(WARMUPS, TIMED));
    for (int i = 0; i < WARMUPS; ++i) {
      ST hello = new ST("Hello, <name>!");
      hello.add("name", i);
      String result = hello.render();
      results.add(result);
    }
    results.clear();
    long start = System.currentTimeMillis();
    for (int i = 0; i < TIMED; ++i) {
      ST hello = new ST("Hello, <name>!");
      hello.add("name", i);
      String result = hello.render();
      results.add(result);
    }
    long duration = System.currentTimeMillis() - start;
    logger.info("{}", String.format("%-40s %-20s %10d %10.3f %14.3f", findTidyName(), System.getProperty("java.vendor.version"), TIMED, duration / 1000.0, TIMED / (duration / 1000.0)));
    assertEquals(EXPECTED_RESULTS, results);
  }

  @Test
  public void testJexl() {

    JexlEngine jexl = new JexlBuilder().create();

    List<String> results = new ArrayList<>(Math.max(WARMUPS, TIMED));
    for (int i = 0; i < WARMUPS; ++i) {
      JexlExpression e = jexl.createExpression("'Hello, ' + name + '!'");
      JexlContext jc = new MapContext();
      jc.set("name", i);
      String result = (String) e.evaluate(jc);
      results.add(result);
    }
    results.clear();
    long start = System.currentTimeMillis();
    for (int i = 0; i < TIMED; ++i) {
      JexlExpression e = jexl.createExpression("'Hello, ' + name + '!'");
      JexlContext jc = new MapContext();
      jc.set("name", i);
      String result = (String) e.evaluate(jc);
      results.add(result);
    }
    long duration = System.currentTimeMillis() - start;
    logger.info("{}", String.format("%-40s %-20s %10d %10.3f %14.3f", findTidyName(), System.getProperty("java.vendor.version"), TIMED, duration / 1000.0, TIMED / (duration / 1000.0)));
    assertEquals(EXPECTED_RESULTS, results);
  }

  @Test
  public void testJexlCachedExpression() {

    JexlEngine jexl = new JexlBuilder().create();

    List<String> results = new ArrayList<>(Math.max(WARMUPS, TIMED));
    JexlExpression e = jexl.createExpression("'Hello, ' + name + '!'");
    for (int i = 0; i < WARMUPS; ++i) {
      JexlContext jc = new MapContext();
      jc.set("name", i);
      String result = (String) e.evaluate(jc);
      results.add(result);
    }
    results.clear();
    long start = System.currentTimeMillis();
    for (int i = 0; i < TIMED; ++i) {
      JexlContext jc = new MapContext();
      jc.set("name", i);
      String result = (String) e.evaluate(jc);
      results.add(result);
    }
    long duration = System.currentTimeMillis() - start;
    logger.info("{}", String.format("%-40s %-20s %10d %10.3f %14.3f", findTidyName(), System.getProperty("java.vendor.version"), TIMED, duration / 1000.0, TIMED / (duration / 1000.0)));
    assertEquals(EXPECTED_RESULTS, results);
  }

  @Test
  public void testVelocity() throws Exception {

    VelocityEngine ve = new VelocityEngine();
    ve.setProperty(VelocityEngine.RESOURCE_LOADERS, "string");
    ve.addProperty("resource.loader.string.class", StringResourceLoader.class.getName());
    ve.addProperty("resource.loader.string.repository.static", "false");
    ve.init();

    StringResourceRepository repo = (StringResourceRepository) ve.getApplicationAttribute(StringResourceLoader.REPOSITORY_NAME_DEFAULT);
    repo.putStringResource("testVelocity", "Hello, ${name}!");

    List<String> results = new ArrayList<>(Math.max(WARMUPS, TIMED));
    for (int i = 0; i < WARMUPS; ++i) {
      VelocityContext context = new VelocityContext();
      context.put("name", i);

      Template template = ve.getTemplate("testVelocity");
      StringWriter sw = new StringWriter();
      template.merge( context, sw );
      String result = sw.toString();
      results.add(result);
    }
    results.clear();
    long start = System.currentTimeMillis();
    for (int i = 0; i < TIMED; ++i) {
      VelocityContext context = new VelocityContext();
      context.put("name", i);

      Template template = ve.getTemplate("testVelocity");
      StringWriter sw = new StringWriter();
      template.merge( context, sw );
      String result = sw.toString();
      results.add(result);
    }
    long duration = System.currentTimeMillis() - start;
    logger.info("{}", String.format("%-40s %-20s %10d %10.3f %14.3f", findTidyName(), System.getProperty("java.vendor.version"), TIMED, duration / 1000.0, TIMED / (duration / 1000.0)));
    assertEquals(EXPECTED_RESULTS, results);
  }

  @Test
  public void testAviator() {
    List<String> results = new ArrayList<>(Math.max(WARMUPS, TIMED));

    // Warmups
    for (int i = 0; i < WARMUPS; ++i) {
      Map<String, Object> env = new HashMap<>();
      env.put("name", i);

      String result = (String) AviatorEvaluator.execute("'Hello, ' + name + '!'", env);
      results.add(result);
    }

    results.clear();

    long start = System.currentTimeMillis();

    // Timed
    for (int i = 0; i < TIMED; ++i) {
      Map<String, Object> env = new HashMap<>();
      env.put("name", i);

      String result = (String) AviatorEvaluator.execute("'Hello, ' + name + '!'", env);
      results.add(result);
    }

    long duration = System.currentTimeMillis() - start;

    logger.info(
      "{}",
      String.format(
        "%-40s %-20s %10d %10.3f %14.3f",
        findTidyName(),
        System.getProperty("java.vendor.version"),
        TIMED,
        duration / 1000.0,
        TIMED / (duration / 1000.0)
      )
    );

    assertEquals(EXPECTED_RESULTS, results);
  }

  @Test
  public void testAviatorPrecompiledWithSandboxing() {
    // Create isolated evaluator instance
    AviatorEvaluatorInstance evaluator = AviatorEvaluator.newInstance();

    // Register LocalDateTime helper functions
    evaluator.addFunction(new AbstractFunction() {
      @Override
      public String getName() {
        return "ldt_now";
      }

      @Override
      public AviatorObject call() throws Exception {
        return new AviatorObject() {
          LocalDateTime now = LocalDateTime.now();

          @Override
          public int innerCompare(AviatorObject other, Map<String, Object> env) {
            Object value = other.getValue(null);
            if (value instanceof LocalDateTime) {
              return ((LocalDateTime)value).compareTo(now);
            }
            return 0;
          }

          @Override
          public AviatorType getAviatorType() {
            return AviatorType.JavaType;
          }

          @Override
          public Object getValue(Map<String, Object> env) {
            return now;
          }
        };
      }
    });

    // Precompile expression
    Expression expr = evaluator.compile("'Hello, ' + name + '!'", true);

    List<String> results = new ArrayList<>(Math.max(WARMUPS, TIMED));

    // Warmups
    for (int i = 0; i < WARMUPS; ++i) {
      Map<String, Object> env = Map.of("name", i);
      results.add((String) expr.execute(env));
    }

    results.clear();
    long start = System.currentTimeMillis();

    // Timed
    for (int i = 0; i < TIMED; ++i) {
      Map<String, Object> env = Map.of("name", i);
      results.add((String) expr.execute(env));
    }

    long duration = System.currentTimeMillis() - start;

    logger.info(
      "{}",
      String.format(
        "%-40s %-20s %10d %10.3f %14.3f",
        findTidyName(),
        System.getProperty("java.vendor.version"),
        TIMED,
        duration / 1000.0,
        TIMED / (duration / 1000.0)
      )
    );

    assertEquals(EXPECTED_RESULTS, results);
  }

  @Test
  public void testJanino() throws Exception {

    List<String> results = new ArrayList<>(Math.max(WARMUPS, TIMED));

    // Warmups
    for (int i = 0; i < WARMUPS; ++i) {
      // Compile the expression
      ExpressionEvaluator ee = new ExpressionEvaluator();
      ee.setExpressionType(String.class);
      ee.setParameters(new String[] { "name" }, new Class<?>[] { int.class });
      ee.cook("\"Hello, \" + name + \"!\"");

      String result = (String) ee.evaluate(new Object[]{ i });
      results.add(result);
    }

    results.clear();
    long start = System.currentTimeMillis();

    // Timed
    for (int i = 0; i < TIMED; ++i) {
      // Compile the expression
      ExpressionEvaluator ee = new ExpressionEvaluator();
      ee.setExpressionType(String.class);
      ee.setParameters(new String[] { "name" }, new Class<?>[] { int.class });
      ee.cook("\"Hello, \" + name + \"!\"");

      String result = (String) ee.evaluate(new Object[]{ i });
      results.add(result);
    }

    long duration = System.currentTimeMillis() - start;

    logger.info(
      "{}",
      String.format(
        "%-40s %-20s %10d %10.3f %14.3f",
        findTidyName(),
        System.getProperty("java.vendor.version"),
        TIMED,
        duration / 1000.0,
        TIMED / (duration / 1000.0)
      )
    );

    assertEquals(EXPECTED_RESULTS, results);
  }

  @Test
  public void testJaninoPrecompiled() throws Exception {

    // Precompile the expression once
    ExpressionEvaluator ee = new ExpressionEvaluator();
    ee.setExpressionType(String.class);
    ee.setParameters(new String[] { "name" }, new Class<?>[] { int.class });
    ee.cook("\"Hello, \" + name + \"!\"");

    List<String> results = new ArrayList<>(Math.max(WARMUPS, TIMED));

    // Warmups
    for (int i = 0; i < WARMUPS; ++i) {
      String result = (String) ee.evaluate(new Object[]{ i });
      results.add(result);
    }

    results.clear();
    long start = System.currentTimeMillis();

    // Timed
    for (int i = 0; i < TIMED; ++i) {
      String result = (String) ee.evaluate(new Object[]{ i });
      results.add(result);
    }

    long duration = System.currentTimeMillis() - start;

    logger.info(
      "{}",
      String.format(
        "%-40s %-20s %10d %10.3f %14.3f",
        findTidyName(),
        System.getProperty("java.vendor.version"),
        TIMED,
        duration / 1000.0,
        TIMED / (duration / 1000.0)
      )
    );

    assertEquals(EXPECTED_RESULTS, results);
  }
}
