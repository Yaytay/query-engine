/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.json;

import java.util.ArrayList;
import java.util.List;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.junit.jupiter.api.BeforeAll;
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
        
  
  @BeforeAll
  public static void dumnpJdkDetails() {
    logger.debug("Java vendor version: {}", System.getProperty("java.vendor.version"));
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
    logger.debug("StringTemplate: {} took {}s, {}/s", TIMED, duration / 1000.0, TIMED / (duration / 1000.0));
    assertEquals(EXPECTED_RESULTS, results);
  }
  
  @Test
  public void testGraalVm() {
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
    logger.debug("GraalVM Javascript: {} took {}s, {}/s", TIMED, duration / 1000.0, TIMED / (duration / 1000.0));
    assertEquals(EXPECTED_RESULTS, results);
  }
  
  @Test
  public void testGraalVmPrecompiled() {
    List<String> results = new ArrayList<>(Math.max(WARMUPS, TIMED));
    Source source = Source.create("js", "'Hello, ' + name + '!'");
    for (int i = 0; i < WARMUPS; ++i) {
      try (Context context = Context.newBuilder("js").option("engine.WarnInterpreterOnly", "false").build()) {
        context.getBindings("js").putMember("name", i);
        String result = context.eval(source).as(String.class);
        results.add(result);
      }
    }
    results.clear();
    long start = System.currentTimeMillis();
    for (int i = 0; i < TIMED; ++i) {
      try (Context context = Context.newBuilder("js").option("engine.WarnInterpreterOnly", "false").build()) {
        context.getBindings("js").putMember("name", i);
        String result = context.eval(source).as(String.class);
        results.add(result);
      }
    }
    long duration = System.currentTimeMillis() - start;
    logger.debug("GraalVM Javascript source: {} took {}s, {}/s", TIMED, duration / 1000.0, TIMED / (duration / 1000.0));
    assertEquals(EXPECTED_RESULTS, results);
  }
  
}
