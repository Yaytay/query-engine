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

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class VertxContextLogbackConverterTest {
  
  private Future<String> convert(Vertx vertx, VertxContextLogbackConverter converter) {
    Context context = vertx.getOrCreateContext();
    Promise<String> promise = Promise.promise();
    context.runOnContext(v -> {
      context.putLocal("bob", "fred");
      promise.complete(converter.convert(null));
    });
    return promise.future();
  }    
  
  @Test
  public void testNoConfigValue(Vertx vertx, VertxTestContext testContext) {
    VertxContextLogbackConverter converter = new VertxContextLogbackConverter();
    converter.setOptionList(null);
    converter.start();
    assertEquals(null, converter.getKey());
    assertEquals("", converter.getDefaultValue());

    convert(vertx, converter)
            .compose(result -> {
              testContext.verify(() -> {
                assertEquals("", result);
              });
              return Future.succeededFuture();
            })
            .onComplete(testContext.succeedingThenComplete())
            ;
  }
  
  @Test
  public void testNoDefaultValue(Vertx vertx, VertxTestContext testContext) {
    VertxContextLogbackConverter converter = new VertxContextLogbackConverter();
    converter.setOptionList(Arrays.asList("bob"));
    converter.start();
    assertEquals("bob", converter.getKey());
    assertEquals("", converter.getDefaultValue());

    convert(vertx, converter)
            .compose(result -> {
              testContext.verify(() -> {
                assertEquals("fred", result);
              });
              return Future.succeededFuture();
            })
            .onComplete(testContext.succeedingThenComplete())
            ;
  }
  
  @Test
  public void testDefaultValue(Vertx vertx, VertxTestContext testContext) {
    VertxContextLogbackConverter converter = new VertxContextLogbackConverter();
    converter.setOptionList(Arrays.asList("carol:-ted"));
    converter.start();
    assertEquals("carol", converter.getKey());
    assertEquals("ted", converter.getDefaultValue());

    convert(vertx, converter)
            .compose(result -> {
              testContext.verify(() -> {
                assertEquals("ted", result);
              });
              return Future.succeededFuture();
            })
            .onComplete(testContext.succeedingThenComplete())
            ;
  }

}
