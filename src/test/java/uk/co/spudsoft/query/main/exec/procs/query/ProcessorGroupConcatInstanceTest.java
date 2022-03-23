/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.procs.query;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.query.main.defn.ProcessorGroupConcat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProcessorGroupConcatInstanceTest {
  
  @Test
  public void testGetId() {
    assertNull(ProcessorGroupConcatInstance.getId(null, "id"));
    assertEquals(17L, ProcessorGroupConcatInstance.getId(new JsonObject().put("id", 17L), "id"));
    assertNull(ProcessorGroupConcatInstance.getId(new JsonObject().put("id", new Object()), "id"));
    assertNull(ProcessorGroupConcatInstance.getId(new JsonObject().put("id", null), "id"));
    assertNull(ProcessorGroupConcatInstance.getId(new JsonObject().put("id", 17L), "notId"));
  }
  
  @Test
  public void testParentStartsLate(Vertx vertx, VertxTestContext testContext) {
    
    ProcessorGroupConcat definition = ProcessorGroupConcat.builder()
            .parentIdColumn("id")
            .childIdColumn("id")
            .childValueColumn("value")
            .build();
    ProcessorGroupConcatInstance instance = new ProcessorGroupConcatInstance(vertx, vertx.getOrCreateContext(), definition);
    
    // instance.initialize();
    testContext.completeNow();
  }
  
}
