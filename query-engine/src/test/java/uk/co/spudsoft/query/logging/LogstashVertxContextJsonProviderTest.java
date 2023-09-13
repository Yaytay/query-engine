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
package uk.co.spudsoft.query.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.Collection;
import static org.junit.Assert.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.query.main.Credentials;
import uk.co.spudsoft.query.main.Main;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class LogstashVertxContextJsonProviderTest {
  
  @Test
  public void testGetContextFieldNames() {
    Collection<String> result = LogstashVertxContextJsonProvider.getContextFieldNames();
    assertEquals(Arrays.asList("Pageview-Context-Id", "vertx.tracing.zipkin.active_span"), result);
  }

  @Test
  public void testAddContextField() {
    Collection<String> result = LogstashVertxContextJsonProvider.getContextFieldNames();
    assertEquals(Arrays.asList("Pageview-Context-Id", "vertx.tracing.zipkin.active_span"), result);
    LogstashVertxContextJsonProvider.addContextField("Fred");
    result = LogstashVertxContextJsonProvider.getContextFieldNames();
    assertEquals(Arrays.asList("Pageview-Context-Id", "vertx.tracing.zipkin.active_span", "Fred"), result);
  }

  @Test
  public void testWriteTo(Vertx vertx, VertxTestContext testContext) {
    vertx.executeBlocking(() -> {
      testContext.verify(() -> {
        LogstashVertxContextJsonProvider<ILoggingEvent> cp = new LogstashVertxContextJsonProvider<>();
        cp.setContext(new LoggerContext());

        ObjectMapper objectMapper = new ObjectMapper();

        JsonFactory factory = new JsonFactory(objectMapper);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
          try (JsonGenerator generator = factory.createGenerator(os)) {
            vertx.getOrCreateContext().putLocal("Pageview-Context-Id", "String");
            generator.writeStartObject();
            cp.writeTo(generator, null);
            generator.writeEndObject();
          }
          assertEquals("{\"Pageview-Context-Id\":\"String\"}", os.toString(StandardCharsets.UTF_8));
        }
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
          try (JsonGenerator generator = factory.createGenerator(os)) {
            vertx.getOrCreateContext().putLocal("Pageview-Context-Id", Boolean.TRUE);
            generator.writeStartObject();
            cp.writeTo(generator, null);
            generator.writeEndObject();
          }
          assertEquals("{\"Pageview-Context-Id\":true}", os.toString(StandardCharsets.UTF_8));
        }
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
          try (JsonGenerator generator = factory.createGenerator(os)) {
            vertx.getOrCreateContext().putLocal("Pageview-Context-Id", 7);
            generator.writeStartObject();
            cp.writeTo(generator, null);
            generator.writeEndObject();
          }
          assertEquals("{\"Pageview-Context-Id\":7}", os.toString(StandardCharsets.UTF_8));
        }
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
          try (JsonGenerator generator = factory.createGenerator(os)) {
            vertx.getOrCreateContext().putLocal("Pageview-Context-Id", 7L);
            generator.writeStartObject();
            cp.writeTo(generator, null);
            generator.writeEndObject();
          }
          assertEquals("{\"Pageview-Context-Id\":7}", os.toString(StandardCharsets.UTF_8));
        }
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
          try (JsonGenerator generator = factory.createGenerator(os)) {
            vertx.getOrCreateContext().putLocal("Pageview-Context-Id", LocalDateTime.of(1971, Month.MAY, 6, 10, 21));
            generator.writeStartObject();
            cp.writeTo(generator, null);
            generator.writeEndObject();
          }
          assertEquals("{\"Pageview-Context-Id\":\"1971-05-06T10:21\"}", os.toString(StandardCharsets.UTF_8));
        }
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
          try (JsonGenerator generator = factory.createGenerator(os)) {
            vertx.getOrCreateContext().putLocal("Pageview-Context-Id", new Credentials("username", "password"));
            generator.writeStartObject();
            cp.writeTo(generator, null);
            generator.writeEndObject();
          }
          assertEquals("{\"Pageview-Context-Id\":{\"username\":\"username\",\"password\":\"********\"}}", os.toString(StandardCharsets.UTF_8));
        }
      });
      return null;
    }).onComplete(testContext.succeedingThenComplete());
  }
  
}
