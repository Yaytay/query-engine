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

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.Mockito.mock;
import uk.co.spudsoft.query.defn.ProcessorExpression;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.Types;
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
    ProcessorExpression definition = ProcessorExpression.builder().id("id").build();
    ProcessorExpressionInstance instance = definition.createInstance(null, null, null);
    assertEquals("id", instance.getId());
  }

  @Test
  public void testPredicate(Vertx vertx, VertxTestContext testContext) {
    
    ProcessorExpression definition = ProcessorExpression.builder().id("id").predicate("iteration < 2").build();

    ProcessorExpressionInstance instance = definition.createInstance(vertx, mock(SourceNameTracker.class), vertx.getOrCreateContext());
    
    Types types = new Types();
    ListReadStream<DataRow> inputStream = new ListReadStream<>(vertx.getOrCreateContext(), Arrays.asList(
            DataRow.create(types).put("iteration", 0)
            , DataRow.create(types).put("iteration", 1)
            , DataRow.create(types).put("iteration", 2)
            , DataRow.create(types).put("iteration", 3)
    ));
    ReadStreamWithTypes input = new ReadStreamWithTypes(inputStream, types);
    
    instance.initialize(null, null, null, 0, input)
            .compose(output -> {
              return ReadStreamToList.capture(output.getStream());
            })
            .onSuccess(rows -> {
              testContext.verify(() -> {
                assertEquals(2, rows.size());
                assertEquals(0, rows.get(0).get("iteration"));
                assertEquals(1, rows.get(1).get("iteration"));
              });
              testContext.completeNow();
            })
            .onFailure(ar -> {
              testContext.failNow(ar);
            })
            ;
    
  }
  
}
