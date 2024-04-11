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

import com.google.common.collect.ImmutableMap;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxImpl;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.ArrayDeque;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 *
 * @author njt
 */
@ExtendWith(VertxExtension.class)
public class VertxMDCTest {
  
  @Test
  public void testKvp(Vertx vertx, VertxTestContext testContext) {
    
    Context context1 = ((VertxImpl) vertx).createEventLoopContext();
    Context context2 = ((VertxImpl) vertx).createEventLoopContext();
    
    VertxMDC.INSTANCE.put("thread", "main");
    // Only works if there is a Vertx context
    assertNull(VertxMDC.INSTANCE.get("thread"));
    
    Checkpoint checkpoint = testContext.checkpoint(2);
    
    context1.runOnContext(v -> {
      testContext.verify(() -> {
        assertNull(VertxMDC.INSTANCE.get("thread"));
        VertxMDC.INSTANCE.put("thread", "context1");
        assertEquals("context1", VertxMDC.INSTANCE.get("thread"));
        VertxMDC.INSTANCE.remove("thread");
        assertNull(VertxMDC.INSTANCE.get("thread"));
        VertxMDC.INSTANCE.put("thread", "context1");
        VertxMDC.INSTANCE.clear();
        assertNull(VertxMDC.INSTANCE.get("thread"));
        VertxMDC.INSTANCE.put("thread", "context1");
        
        assertEquals(ImmutableMap.<String, String>builder().put("thread", "context1").build(), VertxMDC.INSTANCE.getCopyOfContextMap());
        
        VertxMDC.INSTANCE.setContextMap(ImmutableMap.<String, String>builder().put("thing", "context1+").build());
        assertEquals("context1+", VertxMDC.INSTANCE.get("thing"));
        assertNull(VertxMDC.INSTANCE.get("thread"));
        VertxMDC.INSTANCE.put("thread", "context1");
      });
      checkpoint.flag();
    });
    context2.runOnContext(v -> {
      testContext.verify(() -> {
        assertNull(VertxMDC.INSTANCE.get("thread"));
        VertxMDC.INSTANCE.put("thread", "context2");
        assertEquals("context2", VertxMDC.INSTANCE.get("thread"));
        VertxMDC.INSTANCE.remove("thread");
        assertNull(VertxMDC.INSTANCE.get("thread"));
        VertxMDC.INSTANCE.put("thread", "context2");
        VertxMDC.INSTANCE.clear();
        assertNull(VertxMDC.INSTANCE.get("thread"));
        VertxMDC.INSTANCE.put("thread", "context2");
        
        assertEquals(ImmutableMap.<String, String>builder().put("thread", "context2").build(), VertxMDC.INSTANCE.getCopyOfContextMap());        
        
        VertxMDC.INSTANCE.setContextMap(ImmutableMap.<String, String>builder().put("thing", "context2+").build());
        assertEquals("context2+", VertxMDC.INSTANCE.get("thing"));
        assertNull(VertxMDC.INSTANCE.get("thread"));
        VertxMDC.INSTANCE.put("thread", "context2");
      });
      checkpoint.flag();
    });
  }

  @Test
  public void testDeque(Vertx vertx, VertxTestContext testContext) {
    
    Context context1 = ((VertxImpl) vertx).createEventLoopContext();
    Context context2 = ((VertxImpl) vertx).createEventLoopContext();
    
    assertNull(VertxMDC.INSTANCE.popByKey("thread"));
    VertxMDC.INSTANCE.pushByKey("thread", "main");
    // Only works if there is a Vertx context
    assertNull(VertxMDC.INSTANCE.popByKey("thread"));
    
    Checkpoint checkpoint = testContext.checkpoint(2);
    
    context1.runOnContext(v -> {
      testContext.verify(() -> {
        assertNull(VertxMDC.INSTANCE.popByKey("thread"));
        
        VertxMDC.INSTANCE.pushByKey("thread", "context1");
        assertEquals("context1", VertxMDC.INSTANCE.popByKey("thread"));
        
        VertxMDC.INSTANCE.pushByKey("thread", "context1");
        assertArrayEquals(new String[]{"context1"}, VertxMDC.INSTANCE.getCopyOfDequeByKey("thread").toArray());
        
        VertxMDC.INSTANCE.clearDequeByKey("thread");
        assertNull(VertxMDC.INSTANCE.popByKey("thread"));

        VertxMDC.INSTANCE.pushByKey("thread", "context1");
      });
      checkpoint.flag();
    });
    context2.runOnContext(v -> {
      testContext.verify(() -> {
        assertNull(VertxMDC.INSTANCE.popByKey("thread"));
        
        VertxMDC.INSTANCE.pushByKey("thread", "context2");
        assertEquals("context2", VertxMDC.INSTANCE.popByKey("thread"));
        
        VertxMDC.INSTANCE.pushByKey("thread", "context2");
        assertArrayEquals(new String[]{"context2"}, VertxMDC.INSTANCE.getCopyOfDequeByKey("thread").toArray());
        
        VertxMDC.INSTANCE.clearDequeByKey("thread");
        assertNull(VertxMDC.INSTANCE.popByKey("thread"));

        VertxMDC.INSTANCE.pushByKey("thread", "context2");
      });
      checkpoint.flag();
    });
    
  }
}
