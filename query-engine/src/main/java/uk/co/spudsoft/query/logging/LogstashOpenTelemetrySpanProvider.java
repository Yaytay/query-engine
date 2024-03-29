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
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import java.io.IOException;
import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.FieldNamesAware;
import net.logstash.logback.fieldnames.LogstashCommonFieldNames;

/**
 *
 * @author jtalbut
 * @param <Event> type of event (ILoggingEvent or IAccessEvent).
 */
public class LogstashOpenTelemetrySpanProvider<Event extends DeferredProcessingAware> extends AbstractFieldJsonProvider<Event> implements FieldNamesAware<LogstashCommonFieldNames> {

  @Override
  public void writeTo(JsonGenerator generator, Event event) throws IOException {
    generator.writeObjectFieldStart(getFieldName());

    Span span = Span.current();
    if (span != Span.getInvalid()) {
      SpanContext spanContext = span.getSpanContext();
      generator.writeObjectFieldStart("span");
      generator.writeStringField("traceId", spanContext.getTraceId());
      generator.writeStringField("spanId", spanContext.getSpanId());
      generator.writeEndObject();
    }

    generator.writeEndObject();
  }

  @Override
  public void setFieldNames(LogstashCommonFieldNames fieldNames) {
  }

}
