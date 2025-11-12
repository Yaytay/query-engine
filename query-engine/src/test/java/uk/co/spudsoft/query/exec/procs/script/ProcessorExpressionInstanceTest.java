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
package uk.co.spudsoft.query.exec.procs.script;

import inet.ipaddr.IPAddressString;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import uk.co.spudsoft.query.defn.ProcessorExpression;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.exec.fmts.ReadStreamToList;
import uk.co.spudsoft.query.exec.procs.ListReadStream;

/**
 *
 * @author njt
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProcessorExpressionInstanceTest {

  @Test
  public void testGetId() {
    ProcessorExpression definition = ProcessorExpression.builder().name("id").build();
    ProcessorExpressionInstance instance = definition.createInstance(null, null, null, "P0-Expression");
    assertEquals("P0-Expression", instance.getName());
  }

  @Test
  public void testPredicate(Vertx vertx, VertxTestContext testContext) {

    ProcessorExpression definition = ProcessorExpression.builder().name("id").predicate("iteration < 2").build();
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);

    ProcessorExpressionInstance instance = definition.createInstance(vertx, reqctx, null, "P0-Expression");

    Types types = new Types();
    ListReadStream<DataRow> inputStream = new ListReadStream<>(vertx.getOrCreateContext(), Arrays.asList(
            DataRow.create(types).put("rownum", 0),
             DataRow.create(types).put("rownum", 1),
             DataRow.create(types).put("rownum", 2),
             DataRow.create(types).put("rownum", 3)
    ));
    ReadStreamWithTypes input = new ReadStreamWithTypes(inputStream, types);

    RequestContext context = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);

    PipelineInstance pipeline = mock(PipelineInstance.class);
    when(pipeline.getRequestContext()).thenReturn(context);

    instance.initialize(null, pipeline, null, 0, input)
            .compose(output -> {
              return ReadStreamToList.capture(output.getStream());
            })
            .onSuccess(rows -> {
              testContext.verify(() -> {
                assertEquals(2, rows.size());
                assertEquals(0, rows.get(0).get("rownum"));
                assertEquals(1, rows.get(1).get("rownum"));
              });
              testContext.completeNow();
            })
            .onFailure(ar -> {
              testContext.failNow(ar);
            });

  }

  @Test
  public void testInitializeFieldTypeInsertionAndMapping(Vertx vertx, VertxTestContext testContext) {
    // definition sets a new field "result" with an expression and declares its type
    ProcessorExpression definition = ProcessorExpression.builder()
            .name("id")
            .field("result")
            .fieldType(uk.co.spudsoft.query.defn.DataType.Integer)
            .fieldValue("row['iter'] * 10")
            .build();

    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new inet.ipaddr.IPAddressString("127.0.0.1"), null);
    ProcessorExpressionInstance instance = definition.createInstance(vertx, reqctx, null, "P0-Expression");

    Types types = new Types();
    // input has only "iteration" initially, "result" should be inserted by initialize()
    ListReadStream<DataRow> inputStream = new ListReadStream<>(vertx.getOrCreateContext(), Arrays.asList(
            DataRow.create(types).put("iter", 1),
            DataRow.create(types).put("iter", 2),
            DataRow.create(types).put("iter", 3)
    ));
    ReadStreamWithTypes input = new ReadStreamWithTypes(inputStream, types);

    PipelineInstance pipeline = mock(PipelineInstance.class);
    when(pipeline.getRequestContext()).thenReturn(reqctx);

    instance.initialize(null, pipeline, null, 0, input)
            .compose(output -> {
              // assert the type for "result" got added before streaming
              testContext.verify(() -> {
                assertEquals(uk.co.spudsoft.query.defn.DataType.Integer, output.getTypes().get("result"));
              });
              return uk.co.spudsoft.query.exec.fmts.ReadStreamToList.capture(output.getStream());
            })
            .onSuccess(rows -> {
              testContext.verify(() -> {
                assertEquals(3, rows.size());
                assertEquals(10, rows.get(0).get("result"));
                assertEquals(20, rows.get(1).get("result"));
                assertEquals(30, rows.get(2).get("result"));
              });
              testContext.completeNow();
            })
            .onFailure(testContext::failNow);
  }

  @Test
  public void testInitializeFieldTypeConflictFails(Vertx vertx, VertxTestContext testContext) {
    // definition attempts to set "value" as Integer while existing type is String
    ProcessorExpression definition = ProcessorExpression.builder()
            .name("id")
            .field("value")
            .fieldType(uk.co.spudsoft.query.defn.DataType.Integer)
            .fieldValue("1")
            .build();

    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new inet.ipaddr.IPAddressString("127.0.0.1"), null);
    ProcessorExpressionInstance instance = definition.createInstance(vertx, reqctx, null, "P0-Expression");

    Types types = new Types();
    // predefine conflicting type
    types.putIfAbsent("value", uk.co.spudsoft.query.defn.DataType.String);

    ListReadStream<DataRow> inputStream = new ListReadStream<>(vertx.getOrCreateContext(), Arrays.asList(
            DataRow.create(types).put("value", "x")
    ));
    ReadStreamWithTypes input = new ReadStreamWithTypes(inputStream, types);

    PipelineInstance pipeline = mock(PipelineInstance.class);
    when(pipeline.getRequestContext()).thenReturn(reqctx);

    instance.initialize(null, pipeline, null, 0, input)
            .onSuccess(rs -> testContext.failNow(new AssertionError("Expected initialize to fail due to type conflict")))
            .onFailure(err -> {
              testContext.verify(() -> {
                // Verify message mentions attempted change
                String msg = err.getMessage();
                // Avoid overly strict assertion; just ensure it indicates change of type for "value"
                org.junit.jupiter.api.Assertions.assertTrue(msg.contains("value"));
                org.junit.jupiter.api.Assertions.assertTrue(msg.contains("Attempt to change type of field"));
              });
              testContext.completeNow();
            });
  }
}
