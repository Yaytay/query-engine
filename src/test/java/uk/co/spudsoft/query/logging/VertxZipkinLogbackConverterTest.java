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

import brave.propagation.TraceContext;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.tracing.zipkin.ZipkinTracer;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class VertxZipkinLogbackConverterTest {
  
  private Future<String> convertZipkin(Vertx vertx, VertxZipkinLogbackConverter converter, long span, long traceHi, long traceLo) {
    Context context = vertx.getOrCreateContext();
    Promise<String> promise = Promise.promise();
    context.runOnContext(v -> {
      context.putLocal(ZipkinTracer.ACTIVE_SPAN, zipkin2.Span.newBuilder().id(span).traceId(traceHi, traceLo).build());
      promise.complete(converter.convert(null));
    });
    return promise.future();
  }
  
  private Future<String> convertBrave(Vertx vertx, VertxZipkinLogbackConverter converter, long span, long traceHi, long traceLo) {
    Context context = vertx.getOrCreateContext();
    Promise<String> promise = Promise.promise();
    context.runOnContext(v -> {
      brave.Span bs = mock(brave.Span.class);
      when(bs.context()).thenReturn(TraceContext.newBuilder().spanId(span).traceIdHigh(traceHi).traceId(traceLo).build());
      context.putLocal(ZipkinTracer.ACTIVE_SPAN, bs);
      promise.complete(converter.convert(null));
    });
    return promise.future();
  }
  
  @Test  
  public void testNoParameters(Vertx vertx, VertxTestContext testContext) {
    VertxZipkinLogbackConverter converter = new VertxZipkinLogbackConverter();
    converter.setOptionList(null);
    converter.start();
    assertEquals(null, converter.getKey());
    assertEquals("", converter.getDefaultValue());
    assertEquals(VertxZipkinLogbackConverter.KeyType.trace, converter.getKeyType());
    convertZipkin(vertx, converter, 1, 2, 3)
            .compose(result -> {
              testContext.verify(() -> {
                assertEquals("00000000000000020000000000000003", result);
              });
              return convertBrave(vertx, converter, 1, 2, 3);
            })
            .compose(result -> {
              testContext.verify(() -> {
                assertEquals("00000000000000020000000000000003", result);
              });
              converter.stop();
              return Future.succeededFuture();
            })
            .onComplete(testContext.succeedingThenComplete())
            ;
  }

  @Test
  public void testTraceWithDefault(Vertx vertx, VertxTestContext testContext) {
    VertxZipkinLogbackConverter converter = new VertxZipkinLogbackConverter();
    converter.setOptionList(Arrays.asList("trace:-bob"));
    converter.start();
    assertEquals("trace", converter.getKey());
    assertEquals("bob", converter.getDefaultValue());
    assertEquals(VertxZipkinLogbackConverter.KeyType.trace, converter.getKeyType());
    convertZipkin(vertx, converter, 1, 2, 3)
            .compose(result -> {
              testContext.verify(() -> {
                assertEquals("00000000000000020000000000000003", result);
              });
              return convertBrave(vertx, converter, 1, 2, 3);
            })
            .compose(result -> {
              testContext.verify(() -> {
                assertEquals("00000000000000020000000000000003", result);
              });
              converter.stop();
              return Future.succeededFuture();
            })
            .onComplete(testContext.succeedingThenComplete())
            ;
  }

  @Test
  public void testSpanWithoutDefault(Vertx vertx, VertxTestContext testContext) {
    VertxZipkinLogbackConverter converter = new VertxZipkinLogbackConverter();
    converter.setOptionList(Arrays.asList("span"));
    converter.start();
    assertEquals("span", converter.getKey());
    assertEquals("", converter.getDefaultValue());
    assertEquals(VertxZipkinLogbackConverter.KeyType.span, converter.getKeyType());
    convertZipkin(vertx, converter, 1, 2, 3)
            .compose(result -> {
              testContext.verify(() -> {
                assertEquals("0000000000000001", result);
              });
              return convertBrave(vertx, converter, 1, 2, 3);
            })
            .compose(result -> {
              testContext.verify(() -> {
                assertEquals("0000000000000001", result);
              });
              converter.stop();
              return Future.succeededFuture();
            })
            .onComplete(testContext.succeedingThenComplete())
            ;
  }

  @Test
  public void testAllWithGarbage(Vertx vertx, VertxTestContext testContext) {
    VertxZipkinLogbackConverter converter = new VertxZipkinLogbackConverter();
    converter.setOptionList(Arrays.asList("bob"));
    converter.start();
    assertEquals("bob", converter.getKey());
    assertEquals("", converter.getDefaultValue());
    assertEquals(VertxZipkinLogbackConverter.KeyType.all, converter.getKeyType());
    convertZipkin(vertx, converter, 1, 2, 3)
            .compose(result -> {
              testContext.verify(() -> {
                assertEquals("00000000000000020000000000000003/0000000000000001", result);
              });
              return convertBrave(vertx, converter, 1, 2, 3);
            })
            .compose(result -> {
              testContext.verify(() -> {
                assertEquals("00000000000000020000000000000003/0000000000000001", result);
              });
              converter.stop();
              return Future.succeededFuture();
            })
            .onComplete(testContext.succeedingThenComplete())
            ;
  }
  
}
