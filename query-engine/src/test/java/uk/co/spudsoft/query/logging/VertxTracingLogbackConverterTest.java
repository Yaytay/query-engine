/*
 * Copyright (C) 2024 njt
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

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author njt
 */
public class VertxTracingLogbackConverterTest {
  
  @Test
  public void testGetKey() {
    VertxTracingLogbackConverter instance = new VertxTracingLogbackConverter();
    assertNull(instance.getKey());
    assertNull(instance.getDefaultValue());
    instance.setOptionList(Arrays.asList("all:-#"));
    instance.start();
    assertEquals("all", instance.getKey());
    assertEquals("#", instance.getDefaultValue());
    assertEquals(VertxTracingLogbackConverter.KeyType.all, instance.getKeyType());
    instance.stop();
    assertNull(instance.getKey());
  }

  @Test
  public void testKeyTypeFromKey() {
    assertEquals(VertxTracingLogbackConverter.KeyType.trace, VertxTracingLogbackConverter.keyTypeFromKey(""));
    assertEquals(VertxTracingLogbackConverter.KeyType.all, VertxTracingLogbackConverter.keyTypeFromKey("all"));
    assertEquals(VertxTracingLogbackConverter.KeyType.trace, VertxTracingLogbackConverter.keyTypeFromKey("trace"));
    assertEquals(VertxTracingLogbackConverter.KeyType.span, VertxTracingLogbackConverter.keyTypeFromKey("span"));
  }

  @Test
  public void testIdFromSpan() {
    VertxTracingLogbackConverter instance;
    
    SpanContext spanContext = mock(SpanContext.class);
    Span span = mock(Span.class);
    when(span.getSpanContext()).thenReturn(spanContext);
    when(spanContext.getTraceId()).thenReturn("trace");
    when(spanContext.getSpanId()).thenReturn("span");
    
    instance = new VertxTracingLogbackConverter();
    instance.setOptionList(Arrays.asList("span:-#"));
    instance.start();
    assertEquals("span", instance.idFromSpan(span));

    instance = new VertxTracingLogbackConverter();
    instance.setOptionList(Arrays.asList("all:-#"));
    instance.start();
    assertEquals("trace/span", instance.idFromSpan(span));

    instance = new VertxTracingLogbackConverter();
    instance.setOptionList(Arrays.asList("trace:-#"));
    instance.start();
    assertEquals("trace", instance.idFromSpan(span));
  }
  
}
