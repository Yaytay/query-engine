/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.procs.script;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyDate;
import org.graalvm.polyglot.proxy.ProxyDuration;
import org.graalvm.polyglot.proxy.ProxyInstant;
import org.graalvm.polyglot.proxy.ProxyTime;
import org.graalvm.polyglot.proxy.ProxyTimeZone;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * @author jtalbut
 */
public class ProcessorScriptInstanceTest {
  
  public ProcessorScriptInstanceTest() {
  }

  @Test
  public void testMapToNativeObject() {    
    assertNull(ProcessorScriptInstance.mapToNativeObject(Value.asValue(null)));
    assertEquals(Long.valueOf("7"), ProcessorScriptInstance.mapToNativeObject(Value.asValue(7)));
    assertEquals(Double.valueOf("7.2"), ProcessorScriptInstance.mapToNativeObject(Value.asValue(7.2)));
    assertEquals(Boolean.FALSE, ProcessorScriptInstance.mapToNativeObject(Value.asValue(false)));
    assertEquals("Hello", ProcessorScriptInstance.mapToNativeObject(Value.asValue("Hello")));
    
    try (org.graalvm.polyglot.Context context = org.graalvm.polyglot.Context.newBuilder("js").option("engine.WarnInterpreterOnly", "false").build()) {
      Value bindings = context.getBindings("js");
      
      bindings.putMember("date", ProxyDate.from(LocalDate.of(1971, 05, 06)));
      bindings.putMember("time", ProxyTime.from(LocalTime.of(01, 23, 45)));
      bindings.putMember("instant", ProxyInstant.from(LocalDateTime.of(1971, 05, 06, 01, 23).toInstant(ZoneOffset.UTC)));
      bindings.putMember("zoneid", ProxyTimeZone.from(ZoneId.of("Europe/London")));
      bindings.putMember("duration", ProxyDuration.from(Duration.ofHours(7)));
      assertEquals(LocalDate.of(1971, 05, 06), ProcessorScriptInstance.mapToNativeObject(bindings.getMember("date")));      
      assertEquals(LocalTime.of(01, 23, 45), ProcessorScriptInstance.mapToNativeObject(bindings.getMember("time")));
      assertEquals(Instant.ofEpochSecond(42340980), ProcessorScriptInstance.mapToNativeObject(bindings.getMember("instant")));
      assertEquals(ZoneId.of("Europe/London"), ProcessorScriptInstance.mapToNativeObject(bindings.getMember("zoneid")));
      assertEquals(Duration.ofHours(7), ProcessorScriptInstance.mapToNativeObject(bindings.getMember("duration")));
    }
    
    try {
      ProcessorScriptInstance.mapToNativeObject(Value.asValue(new IllegalStateException("Bad value")));
      fail("Expected IllegalStateException");
    } catch(Throwable ex) {
    }
  }
  
}
