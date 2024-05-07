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
package uk.co.spudsoft.query.main;

import io.vertx.core.Promise;
import io.vertx.ext.healthchecks.Status;
import io.vertx.junit5.VertxExtension;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class UpCheckHandlerTest {
  
  @Test
  public void testHandle() {
    
    AtomicBoolean b = new AtomicBoolean();
    assertFalse(b.get());

    Promise<Status> promise;
    
    UpCheckHandler handler = new UpCheckHandler(b);
    promise = Promise.promise();    
    handler.handle(promise);
    assertTrue(promise.future().isComplete());
    assertFalse(promise.future().result().isOk());

    b.set(true);
    promise = Promise.promise();    
    handler.handle(promise);
    assertTrue(promise.future().isComplete());
    assertTrue(promise.future().result().isOk());

    b.set(false);
    promise = Promise.promise();    
    handler.handle(promise);
    assertTrue(promise.future().isComplete());
    assertFalse(promise.future().result().isOk());    
  }
  
}
