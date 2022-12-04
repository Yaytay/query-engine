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

import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import java.io.IOException;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.FieldNamesAware;
import net.logstash.logback.fieldnames.LogstashCommonFieldNames;


/**
 *
 * @author jtalbut
 * @param <Event> type of event (ILoggingEvent or IAccessEvent).
 */
public class LogstashVertxContextJsonProvider<Event extends DeferredProcessingAware> extends AbstractFieldJsonProvider<Event> implements FieldNamesAware<LogstashCommonFieldNames> {

  private static final List<String> CONTEXT_FIELD_NAMES = new ArrayList<>(
          Arrays.asList(
                  "Pageview-Context-Id"
                  , VertxZipkinLogbackConverter.ACTIVE_SPAN
          )
  );
  
  private static final Map<String, String> FIELD_NAME_MAP = ImmutableMap.<String, String>builder()
          .put(VertxZipkinLogbackConverter.ACTIVE_SPAN, "span")
          .build()
  ;
  
  /**
   * Return the list of fields from the Vertx Local Context that will be output.
   * @return the list of fields from the Vertx Local Context that will be output.
   */
  public static Collection<String> getContextFieldNames() {
    return new ArrayList<>(CONTEXT_FIELD_NAMES);
  }
  
  /**
   * Add a new context field to those recorded as usual.
   * @param contextFieldName The name of the context field.
   */
  public static void addContextField(String contextFieldName) {
    CONTEXT_FIELD_NAMES.add(contextFieldName);
  }

  @Override
  public void writeTo(JsonGenerator generator, Event event) throws IOException {
    if (getContext() != null) {
      if (getFieldName() != null) {
        generator.writeObjectFieldStart(getFieldName());
      }
      Context vertxContext = Vertx.currentContext();
      if (vertxContext != null) {
        for (String field : CONTEXT_FIELD_NAMES) {
          Object value = vertxContext.getLocal(field);
          if (value != null) {
            String fieldName = FIELD_NAME_MAP.get(field);
            if (Strings.isNullOrEmpty(fieldName)) {
              fieldName = field;
            }
            generator.writeFieldName(fieldName);

            if (value instanceof String) {
              generator.writeString((String) value);
            } else if (value instanceof Boolean) {
              generator.writeBoolean((Boolean) value);
            } else if (value instanceof Integer) {
              generator.writeNumber((Integer) value);
            } else if (value instanceof Long) {
              generator.writeNumber((Long) value);
            } else if (value instanceof zipkin2.Span) {
              generator.writeObject(extractZipkinSpan((zipkin2.Span) value));
            } else if (value instanceof brave.Span) {
              generator.writeObject(extractBraveSpan((brave.Span) value));
            } else if (value instanceof Temporal) {
              generator.writeString(value.toString());
            } else {
              try {
                generator.writeObject(value);
              } catch (Throwable ex) {
                generator.writeString(ex.getMessage());
              }
            }
          }
        }
      }

      if (getFieldName() != null) {
        generator.writeEndObject();
      }
    }
  }
  
  private static class SpanId {
    public final String trace;
    public final String span;

    SpanId(String trace, String span) {
      this.trace = trace;
      this.span = span;
    }
    
  }

  private SpanId extractBraveSpan(brave.Span value) {
    return new SpanId(
            value.context().traceIdString()
            , value.context().spanIdString()
    );
  }

  private SpanId extractZipkinSpan(zipkin2.Span value) {
    return new SpanId(
            value.traceId()
            , value.id()
    );
  }

  @Override
  public void setFieldNames(LogstashCommonFieldNames fieldNames) {
  }

}
