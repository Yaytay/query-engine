/*
 * Copyright (C) 2025 njt
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
package uk.co.spudsoft.query.exec.fmts.json;

import inet.ipaddr.IPAddressString;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import uk.co.spudsoft.query.defn.FormatJson;
import uk.co.spudsoft.query.defn.Pipeline;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.ListingWriteStream;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.exec.procs.ListReadStream;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FormatJsonInstanceStreamTest {

  @Test
  public void testOutputDefaults(Vertx vertx, VertxTestContext testContext) {
    // Create format definition
    FormatJson definition = FormatJson.builder()
            .build();
    assertNull(definition.getMetadataName());
    assertNull(definition.getDataName());
    assertEquals(1, definition.getPrettiness());

    doTest(vertx, testContext, definition, jsonOutput -> {
      assertEquals("""
                   [
                   {"name":"Alice","age":25},
                   {"name":"Bob","age":30}
                   ]
                   """.trim()
              , jsonOutput
              );
    });
  }

  @Test
  public void testOutputZeroPrettyNoDataNoMeta(Vertx vertx, VertxTestContext testContext) {
    // Create format definition
    FormatJson definition = FormatJson.builder()
            .prettiness(0)
            .build();
    assertNull(definition.getMetadataName());
    assertNull(definition.getDataName());
    assertEquals(0, definition.getPrettiness());

    doTest(vertx, testContext, definition, jsonOutput -> {
      assertEquals("""
                   [{"name":"Alice","age":25},{"name":"Bob","age":30}]
                   """.trim()
              , jsonOutput
              );
    });
  }

  @Test
  public void testOutputOnePrettyNoDataNoMeta(Vertx vertx, VertxTestContext testContext) {
    // Create format definition
    FormatJson definition = FormatJson.builder()
            .prettiness(1)
            .build();
    assertNull(definition.getMetadataName());
    assertNull(definition.getDataName());
    assertEquals(1, definition.getPrettiness());

    doTest(vertx, testContext, definition, jsonOutput -> {
      assertEquals("""
                   [
                   {"name":"Alice","age":25},
                   {"name":"Bob","age":30}
                   ]
                   """.trim()
              , jsonOutput
              );
    });
  }

  @Test
  public void testOutputTwoPrettyNoDataNoMeta(Vertx vertx, VertxTestContext testContext) {
    // Create format definition
    FormatJson definition = FormatJson.builder()
            .prettiness(2)
            .build();
    assertNull(definition.getMetadataName());
    assertNull(definition.getDataName());
    assertEquals(2, definition.getPrettiness());

    doTest(vertx, testContext, definition, jsonOutput -> {
      assertEquals("""
                   [
                     {
                       "name" : "Alice",
                       "age" : 25
                     },{
                       "name" : "Bob",
                       "age" : 30
                     }
                   ]
                   """.trim()
              , jsonOutput
              );
    });
  }


  @Test
  public void testOutputZeroPrettyWithDataWithMeta(Vertx vertx, VertxTestContext testContext) {
    // Create format definition
    FormatJson definition = FormatJson.builder()
            .prettiness(0)
            .dataName("data")
            .metadataName("meta")
            .build();
    assertEquals("meta", definition.getMetadataName());
    assertEquals("data", definition.getDataName());
    assertEquals(0, definition.getPrettiness());

    doTest(vertx, testContext, definition, jsonOutput -> {
      assertEquals("""
                   {"meta":{"name":"FormatJsonInstanceStreamTest","fields":{"name":"String","age":"Integer"}},"data":[{"name":"Alice","age":25},{"name":"Bob","age":30}]}
                   """.trim()
              , jsonOutput
              );
    });
  }

  @Test
  public void testOutputOnePrettyWithDataWithMeta(Vertx vertx, VertxTestContext testContext) {
    // Create format definition
    FormatJson definition = FormatJson.builder()
            .prettiness(1)
            .dataName("data")
            .metadataName("meta")
            .build();
    assertEquals("meta", definition.getMetadataName());
    assertEquals("data", definition.getDataName());
    assertEquals(1, definition.getPrettiness());

    doTest(vertx, testContext, definition, jsonOutput -> {
      assertEquals("""
                   {"meta":{"name":"FormatJsonInstanceStreamTest","fields":{"name":"String","age":"Integer"}},"data":[
                   {"name":"Alice","age":25},
                   {"name":"Bob","age":30}
                   ]}
                   """.trim()
              , jsonOutput
              );
    });
  }

  @Test
  public void testOutputTwoPrettyWithDataWithMeta(Vertx vertx, VertxTestContext testContext) {
    // Create format definition
    FormatJson definition = FormatJson.builder()
            .prettiness(2)
            .dataName("data")
            .metadataName("meta")
            .build();
    assertEquals("meta", definition.getMetadataName());
    assertEquals("data", definition.getDataName());
    assertEquals(2, definition.getPrettiness());

    doTest(vertx, testContext, definition, jsonOutput -> {
      assertEquals("""
                   {
                     "meta" : {
                       "name" : "FormatJsonInstanceStreamTest",
                       "fields" : {
                         "name" : "String",
                         "age" : "Integer"
                       }
                     },
                     "data" : [{
                         "name" : "Alice",
                         "age" : 25
                       },{
                         "name" : "Bob",
                         "age" : 30
                       }
                     ]
                   }
                   """.trim()
              , jsonOutput
              );
    });
  }

  @Test
  public void testOutputZeroPrettyWithDataNoMeta(Vertx vertx, VertxTestContext testContext) {
    // Create format definition
    FormatJson definition = FormatJson.builder()
            .prettiness(0)
            .dataName("data")
            .build();
    assertEquals("data", definition.getDataName());
    assertNull(definition.getMetadataName());
    assertEquals(0, definition.getPrettiness());

    doTest(vertx, testContext, definition, jsonOutput -> {
      assertEquals("""
                   {"data":[{"name":"Alice","age":25},{"name":"Bob","age":30}]}
                   """.trim()
              , jsonOutput
              );
    });
  }

  @Test
  public void testOutputOnePrettyWithDataNoMeta(Vertx vertx, VertxTestContext testContext) {
    // Create format definition
    FormatJson definition = FormatJson.builder()
            .prettiness(1)
            .dataName("data")
            .build();
    assertEquals("data", definition.getDataName());
    assertNull(definition.getMetadataName());
    assertEquals(1, definition.getPrettiness());

    doTest(vertx, testContext, definition, jsonOutput -> {
      assertEquals("""
                   {"data":[
                   {"name":"Alice","age":25},
                   {"name":"Bob","age":30}
                   ]}
                   """.trim()
              , jsonOutput
              );
    });
  }

  @Test
  public void testOutputTwoPrettyWithDataNoMeta(Vertx vertx, VertxTestContext testContext) {
    // Create format definition
    FormatJson definition = FormatJson.builder()
            .prettiness(2)
            .dataName("data")
            .build();
    assertEquals("data", definition.getDataName());
    assertNull(definition.getMetadataName());
    assertEquals(2, definition.getPrettiness());

    doTest(vertx, testContext, definition, jsonOutput -> {
      assertEquals("""
                   {
                     "data" : [{
                         "name" : "Alice",
                         "age" : 25
                       },{
                         "name" : "Bob",
                         "age" : 30
                       }
                     ]
                   }
                   """.trim()
              , jsonOutput
              );
    });
  }

  
  @Test
  public void testOutputZeroPrettyNoDataWithMeta(Vertx vertx, VertxTestContext testContext) {
    // Create format definition
    FormatJson definition = FormatJson.builder()
            .prettiness(0)
            .metadataName("meta")
            .build();
    assertEquals("meta", definition.getMetadataName());
    assertNull(definition.getDataName());
    assertEquals(0, definition.getPrettiness());

    doTest(vertx, testContext, definition, jsonOutput -> {
      assertEquals("""
                   [{"name":"Alice","age":25},{"name":"Bob","age":30}]
                   """.trim()
              , jsonOutput
              );
    });
  }

  @Test
  public void testOutputOnePrettyNoDataWithMeta(Vertx vertx, VertxTestContext testContext) {
    // Create format definition
    FormatJson definition = FormatJson.builder()
            .prettiness(1)
            .metadataName("meta")
            .build();
    assertEquals("meta", definition.getMetadataName());
    assertNull(definition.getDataName());
    assertEquals(1, definition.getPrettiness());

    doTest(vertx, testContext, definition, jsonOutput -> {
      assertEquals("""
                   [
                   {"name":"Alice","age":25},
                   {"name":"Bob","age":30}
                   ]
                   """.trim()
              , jsonOutput
              );
    });
  }

  @Test
  public void testOutputTwoPrettyNoDataWithMeta(Vertx vertx, VertxTestContext testContext) {
    // Create format definition
    FormatJson definition = FormatJson.builder()
            .prettiness(2)
            .metadataName("meta")
            .build();
    assertEquals("meta", definition.getMetadataName());
    assertNull(definition.getDataName());
    assertEquals(2, definition.getPrettiness());

    doTest(vertx, testContext, definition, jsonOutput -> {
      assertEquals("""
                   [
                     {
                       "name" : "Alice",
                       "age" : 25
                     },{
                       "name" : "Bob",
                       "age" : 30
                     }
                   ]
                   """.trim()
              , jsonOutput
              );
    });
  }

  /**
   * Interface to test the output JSON
   *
   * @see #verify(ExecutionBlock)
   */
  @FunctionalInterface
  public interface TestBlock {

    void apply(String json) throws Throwable;
  }
  
  void doTest(Vertx vertx, VertxTestContext testContext, FormatJson definition, TestBlock testBlock) {
    // Create a list to capture output buffers
    List<Buffer> outputBuffers = new ArrayList<>();
    WriteStream<Buffer> writeStream = new ListingWriteStream<>(outputBuffers);

    // Create request context
    RequestContext requestContext = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);

    // Create format instance
    FormatJsonInstance instance = new FormatJsonInstance(writeStream, requestContext, definition);

    // Create types and sample data rows
    Types types = new Types();
    List<DataRow> rows = Arrays.asList(
            DataRow.create(types, "name", "Alice", "age", 25),
            DataRow.create(types, "name", "Bob", "age", 30)
    );

    // Create read stream
    ReadStream<DataRow> readStream = new ListReadStream<>(vertx.getOrCreateContext(), rows);
    ReadStreamWithTypes streamWithTypes = new ReadStreamWithTypes(readStream, types);

    Pipeline pipeline = Pipeline.builder()
            .title(MethodHandles.lookup().lookupClass().getSimpleName())
            .build();            
    PipelineInstance pipelineInstance = mock(PipelineInstance.class);
    when(pipelineInstance.getDefinition()).thenReturn(pipeline);
    
    // Test the complete flow
    instance.initialize(null, pipelineInstance, streamWithTypes)
            .onComplete(testContext.succeeding(result -> {
              // Combine all output buffers into single string
              Buffer combined = Buffer.buffer();
              outputBuffers.forEach(combined::appendBuffer);
              String jsonOutput = combined.toString();

              testContext.verify(() -> {
                testBlock.apply(jsonOutput);
              });
              testContext.completeNow();
            }));
  }
}
