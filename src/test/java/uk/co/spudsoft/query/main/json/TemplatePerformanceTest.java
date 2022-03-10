/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.json;

import java.util.ArrayList;
import java.util.List;
import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.*;

/**
 *
 * @author jtalbut
 */
public class TemplatePerformanceTest {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(TemplatePerformanceTest.class);
  
  private final static int WARMUPS = 1000;
  private final static int TIMED = 1000;
  
  @Test
  public void testStringTemplate() {
    List<String> results = new ArrayList<>(Math.max(WARMUPS, TIMED));
    for (int i = 0; i < WARMUPS; ++i) {
      ST hello = new ST("Hello, <name>");
      hello.add("name", i);
      String result = hello.render();
      results.add(result);
    }
    results.clear();
    long start = System.currentTimeMillis();
    for (int i = 0; i < TIMED; ++i) {
      ST hello = new ST("Hello, <name>");
      hello.add("name", i);
      String result = hello.render();
      results.add(result);
    }
    long duration = System.currentTimeMillis() - start;
    logger.debug("StringTemplate: {} took {}s, {}/s", TIMED, duration / 1000.0, TIMED / (duration / 1000.0));
    logger.debug("StringTemplate results: {}", results);
  }
  
  @Test
  public void testGraalVm() {
    List<String> results = new ArrayList<>(Math.max(WARMUPS, TIMED));
    for (int i = 0; i < WARMUPS; ++i) {
      try (Context context = Context.newBuilder("js").option("engine.WarnInterpreterOnly", "false").build()) {
        context.getBindings("js").putMember("name", i);
        String result = context.eval("js", "'Hello ' + name + '!'").as(String.class);
        results.add(result);
      }
    }
    results.clear();
    long start = System.currentTimeMillis();
    for (int i = 0; i < TIMED; ++i) {
      try (Context context = Context.newBuilder("js").option("engine.WarnInterpreterOnly", "false").build()) {
        context.getBindings("js").putMember("name", i);
        String result = context.eval("js", "'Hello ' + name + '!'").as(String.class);
        results.add(result);
      }
    }
    long duration = System.currentTimeMillis() - start;
    logger.debug("GraalVM Javascript: {} took {}s, {}/s", TIMED, duration / 1000.0, TIMED / (duration / 1000.0));
    logger.debug("GraalVM results: {}", results);
  }
  
}
