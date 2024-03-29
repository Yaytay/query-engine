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

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.common.base.Strings;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

import static ch.qos.logback.core.util.OptionHelper.extractDefaultReplacement;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

/**
 *
 * @author njt
 */
public class VertxTracingLogbackConverter extends ClassicConverter {
  
  /**
   * 
   */
  public static final String ACTIVE_SPAN = "vertx.tracing.zipkin.active_span";      // ZipkinTracer.ACTIVE_SPAN
  
  enum KeyType {
    trace
    , span
    , all
  }
  
  private String key;
  private String defaultValue;
  private KeyType keyType;

  String getKey() {
    return key;
  }

  String getDefaultValue() {
    return defaultValue;
  }

  KeyType getKeyType() {
    return keyType;
  }

  @Override
  public void start() {
    String[] keyInfo = extractDefaultReplacement(getFirstOption());
    key = keyInfo[0];
    defaultValue = keyInfo[1] == null ? "" : keyInfo[1];
    keyType = keyTypeFromKey(key);
    super.start();
  }  
  
  static KeyType keyTypeFromKey(String key) {
    if (Strings.isNullOrEmpty(key)) {
      return KeyType.trace;
    } else if ("trace".equalsIgnoreCase(key)) {
      return KeyType.trace;
    } else if ("span".equalsIgnoreCase(key)) {
      return KeyType.span;
    } else {
      return KeyType.all;
    }
  }

  @Override
  public String convert(ILoggingEvent event) {
    Context context = Vertx.currentContext();
    if (context != null) {
      Span span = Span.current();
      if (span != null) {
        return idFromSpan(span);
      }
    }
    return defaultValue;
  }

  String idFromSpan(Span span) {
    SpanContext spanContext = span.getSpanContext();
    return switch (keyType) {
      case all -> spanContext.getTraceId() + "/" + spanContext.getSpanId();
      case span -> spanContext.getSpanId();
      case trace -> spanContext.getTraceId();
    };
  }

  @Override
  public void stop() {
    key = null;
    super.stop();
  }  
}
