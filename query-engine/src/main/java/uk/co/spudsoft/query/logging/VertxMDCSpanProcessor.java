/*
 * Copyright (C) 2024 jtalbut
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

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SpanProcessor to put span details into {@link VertxMDC}.
 * 
 * @author jtalbut
 */
public class VertxMDCSpanProcessor implements SpanProcessor {

  private static final Logger logger = LoggerFactory.getLogger(VertxMDCSpanProcessor.class);
  
  /**
   * Constructor.
   */
  public VertxMDCSpanProcessor() {
  }
  
  /**
   * Store the OpenTelemetry SpanContext into the {@link VertxMDC} where it can be picked up by logs.
   * 
   * It can be necessary to call this if the OpenTelemetry Span is created before there is a VertxMDC context
   * to store the IDs into.
   * 
   * @param spanContext The OpenTelemetry SpanContext to be recorded.
   */
  public static void toMdc(SpanContext spanContext) {
    VertxMDC.INSTANCE.put("traceId", spanContext.getTraceId());
    VertxMDC.INSTANCE.put("spanId", spanContext.getSpanId());
  }

  @Override
  public void onStart(Context cntxt, ReadWriteSpan rws) {
    toMdc(rws.getSpanContext());
  }

  @Override
  public boolean isStartRequired() {
    return true;
  }

  @Override
  public void onEnd(ReadableSpan span) {
  }

  @Override
  public boolean isEndRequired() {
    return false;
  }

}
