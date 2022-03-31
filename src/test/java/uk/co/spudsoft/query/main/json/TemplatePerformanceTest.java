/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.json;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author jtalbut
 */
public class TemplatePerformanceTest {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(TemplatePerformanceTest.class);
  
  private final static int WARMUPS = 1000;
  private final static int TIMED = 1000;
  
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
    } catch(Throwable ex) {
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
    } catch(Throwable ex) {
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
    } catch(Throwable ex) {
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
