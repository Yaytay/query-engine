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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;

/**
 *
 * @author jtalbut
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class TemplatePerformanceTest {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(TemplatePerformanceTest.class);
  
  private final static int WARMUPS = 10000;
  private final static int TIMED = 10000;
  
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
  
  @BeforeAll
  public static void headers() {
    logger.debug("{}\t{}\t{}\t{}\t{}", " Method", "JVM", "Iterations", "Duration/s", "Rate (iterations/second)");
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
    logger.debug("{}\t{}\t{}\t{}\t{}", findTidyName(), System.getProperty("java.vendor.version"), TIMED, duration / 1000.0, TIMED / (duration / 1000.0));
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
    logger.debug("{}\t{}\t{}\t{}\t{}", findTidyName(), System.getProperty("java.vendor.version"), TIMED, duration / 1000.0, TIMED / (duration / 1000.0));
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
    logger.debug("{}\t{}\t{}\t{}\t{}", findTidyName(), System.getProperty("java.vendor.version"), TIMED, duration / 1000.0, TIMED / (duration / 1000.0));
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
    logger.debug("{}\t{}\t{}\t{}\t{}", findTidyName(), System.getProperty("java.vendor.version"), TIMED, duration / 1000.0, TIMED / (duration / 1000.0));
    assertEquals(EXPECTED_RESULTS, results);
  }
  
  @Test
  public void testPolyglot() {
    List<String> results = new ArrayList<>(Math.max(WARMUPS, TIMED));
    for (int i = 0; i < WARMUPS; ++i) {
      try (Context context = Context.newBuilder("js").option("engine.WarnInterpreterOnly", "false").build()) {
        context.getBindings("js").putMember("name", i);
        String result = context.eval("js", "'Hello, ' + name + '!'").as(String.class);
        results.add(result);
      }
    }
    results.clear();
    long start = System.currentTimeMillis();
    for (int i = 0; i < TIMED; ++i) {
      try (Context context = Context.newBuilder("js").option("engine.WarnInterpreterOnly", "false").build()) {
        context.getBindings("js").putMember("name", i);
        String result = context.eval("js", "'Hello, ' + name + '!'").as(String.class);
        results.add(result);
      }
    }
    long duration = System.currentTimeMillis() - start;
    logger.debug("{}\t{}\t{}\t{}\t{}", findTidyName(), System.getProperty("java.vendor.version"), TIMED, duration / 1000.0, TIMED / (duration / 1000.0));
    assertEquals(EXPECTED_RESULTS, results);
  }  
    
  @Test
  public void testPolyglotLatencyModeEngine() {
    List<String> results = new ArrayList<>(Math.max(WARMUPS, TIMED));
    Engine engine = null;
    try {
      engine = Engine.newBuilder().option("engine.WarnInterpreterOnly", "false").option("engine.Mode", "latency").build();
    } catch (Throwable ex) {
      return ;
    }
    Source source = Source.create("js", "'Hello, ' + name + '!'");
    for (int i = 0; i < WARMUPS; ++i) {
      try (Context context = Context.newBuilder("js").engine(engine).build()) {
        context.getBindings("js").putMember("name", i);
        String result = context.eval(source).as(String.class);
        results.add(result);
      }
    }
    results.clear();
    long start = System.currentTimeMillis();
    for (int i = 0; i < TIMED; ++i) {
      try (Context context = Context.newBuilder("js").engine(engine).build()) {
        context.getBindings("js").putMember("name", i);
        String result = context.eval(source).as(String.class);
        results.add(result);
      }
    }
    long duration = System.currentTimeMillis() - start;
    logger.debug("{}\t{}\t{}\t{}\t{}", findTidyName(), System.getProperty("java.vendor.version"), TIMED, duration / 1000.0, TIMED / (duration / 1000.0));
    assertEquals(EXPECTED_RESULTS, results);
  }
  
  @Test
  public void testPolyglotThroughputModeEngine() {
    List<String> results = new ArrayList<>(Math.max(WARMUPS, TIMED));
    Engine engine = null;
    try {
      engine = Engine.newBuilder().option("engine.WarnInterpreterOnly", "false").option("engine.Mode", "throughput").build();
    } catch (Throwable ex) {
      return ;
    }
    Source source = Source.create("js", "'Hello, ' + name + '!'");
    for (int i = 0; i < WARMUPS; ++i) {
      try (Context context = Context.newBuilder("js").engine(engine).build()) {
        context.getBindings("js").putMember("name", i);
        String result = context.eval(source).as(String.class);
        results.add(result);
      }
    }
    results.clear();
    long start = System.currentTimeMillis();
    for (int i = 0; i < TIMED; ++i) {
      try (Context context = Context.newBuilder("js").engine(engine).build()) {
        context.getBindings("js").putMember("name", i);
        String result = context.eval(source).as(String.class);
        results.add(result);
      }
    }
    long duration = System.currentTimeMillis() - start;
    logger.debug("{}\t{}\t{}\t{}\t{}", findTidyName(), System.getProperty("java.vendor.version"), TIMED, duration / 1000.0, TIMED / (duration / 1000.0));
    assertEquals(EXPECTED_RESULTS, results);
  }
  
  @Test
  public void testPolyglotThroughputModeEngineSourceCache() {
    List<String> results = new ArrayList<>(Math.max(WARMUPS, TIMED));
    Engine engine = null;
    try {
      engine = Engine.newBuilder().option("engine.WarnInterpreterOnly", "false").option("engine.Mode", "throughput").build();
    } catch (Throwable ex) {
      return ;
    }
    Source source = Source.newBuilder("js", "'Hello, ' + name + '!'", "test").cached(true).buildLiteral();
    for (int i = 0; i < WARMUPS; ++i) {
      try (Context context = Context.newBuilder("js").engine(engine).build()) {
        context.getBindings("js").putMember("name", i);
        String result = context.eval(source).as(String.class);
        results.add(result);
      }
    }
    results.clear();
    long start = System.currentTimeMillis();
    for (int i = 0; i < TIMED; ++i) {
      try (Context context = Context.newBuilder("js").engine(engine).build()) {
        context.getBindings("js").putMember("name", i);
        String result = context.eval(source).as(String.class);
        results.add(result);
      }
    }
    long duration = System.currentTimeMillis() - start;
    logger.debug("{}\t{}\t{}\t{}\t{}", findTidyName(), System.getProperty("java.vendor.version"), TIMED, duration / 1000.0, TIMED / (duration / 1000.0));
    assertEquals(EXPECTED_RESULTS, results);
  }
  
  
  @Test
  public void testPolyglotEngineSourceCache() {
    List<String> results = new ArrayList<>(Math.max(WARMUPS, TIMED));
    Engine engine = Engine.newBuilder().option("engine.WarnInterpreterOnly", "false").build();
    Source source = Source.newBuilder("js", "'Hello, ' + name + '!'", "test").cached(true).buildLiteral();
    for (int i = 0; i < WARMUPS; ++i) {
      try (Context context = Context.newBuilder("js").engine(engine).build()) {
        context.getBindings("js").putMember("name", i);
        String result = context.eval(source).as(String.class);
        results.add(result);
      }
    }
    results.clear();
    long start = System.currentTimeMillis();
    for (int i = 0; i < TIMED; ++i) {
      try (Context context = Context.newBuilder("js").engine(engine).build()) {
        context.getBindings("js").putMember("name", i);
        String result = context.eval(source).as(String.class);
        results.add(result);
      }
    }
    long duration = System.currentTimeMillis() - start;
    logger.debug("{}\t{}\t{}\t{}\t{}", findTidyName(), System.getProperty("java.vendor.version"), TIMED, duration / 1000.0, TIMED / (duration / 1000.0));
    assertEquals(EXPECTED_RESULTS, results);
  }
  
}
