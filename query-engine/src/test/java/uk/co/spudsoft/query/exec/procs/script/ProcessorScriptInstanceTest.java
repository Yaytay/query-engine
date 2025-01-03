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
package uk.co.spudsoft.query.exec.procs.script;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyDate;
import org.graalvm.polyglot.proxy.ProxyDuration;
import org.graalvm.polyglot.proxy.ProxyInstant;
import org.graalvm.polyglot.proxy.ProxyTime;
import org.graalvm.polyglot.proxy.ProxyTimeZone;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import uk.co.spudsoft.query.defn.ProcessorScript;

/**
 *
 * @author jtalbut
 */
public class ProcessorScriptInstanceTest {

  @Test
  public void testGetId() {
    ProcessorScript definition = ProcessorScript.builder().name("id").build();
    ProcessorScriptInstance instance = definition.createInstance(null, null, null, "P0-Script");
    assertEquals("P0-Script", instance.getName());
  }

  @Test
  public void testNullableClass() {
    assertNull(ProcessorScriptInstance.nullableClass(null));
    assertEquals(String.class, ProcessorScriptInstance.nullableClass("This"));
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
      bindings.putMember("zonedtime", ProxyTime.from(LocalTime.of(01, 23, 45)));
      bindings.putMember("instant", ProxyInstant.from(LocalDateTime.of(1971, 05, 06, 01, 23).toInstant(ZoneOffset.UTC)));
      bindings.putMember("zoneid", ProxyTimeZone.from(ZoneId.of("Europe/London")));
      bindings.putMember("duration", ProxyDuration.from(Duration.ofHours(7)));
      bindings.putMember("ex", new IllegalArgumentException("bad"));

      assertEquals(LocalDate.of(1971, 05, 06), ProcessorScriptInstance.mapToNativeObject(bindings.getMember("date")));
      assertEquals(LocalTime.of(01, 23, 45), ProcessorScriptInstance.mapToNativeObject(bindings.getMember("time")));
      assertEquals(Instant.ofEpochSecond(42340980), ProcessorScriptInstance.mapToNativeObject(bindings.getMember("instant")));
      assertEquals(Duration.ofHours(7), ProcessorScriptInstance.mapToNativeObject(bindings.getMember("duration")));
      
      assertThrows(PolyglotException.class, () -> {
        ProcessorScriptInstance.mapToNativeObject(bindings.getMember("ex"));
      });
    }

    try {
      ProcessorScriptInstance.mapToNativeObject(Value.asValue(new IllegalStateException("Bad value")));
      fail("Expected IllegalStateException");
    } catch (Throwable ex) {
    }
  }

}
