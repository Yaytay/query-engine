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
package uk.co.spudsoft.query.exec.procs.subquery;

import inet.ipaddr.IPAddressString;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.streams.ReadStream;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.exec.fmts.ReadStreamToList;
import uk.co.spudsoft.query.exec.procs.ListReadStream;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MergeStreamTest {
  
  private static final Logger logger = LoggerFactory.getLogger(MergeStreamTest.class);

  private DataRow merge(DataRow parent, Collection<DataRow> children) {
    int sum = 0;
    for (DataRow child : children) {
      sum += (Integer) child.get("number");
    }
    parent.put("sum", sum);
    return parent;
  }
  
  private int compareId(DataRow lhs, DataRow rhs) {
    Integer i1 = (Integer) lhs.get("id");
    Integer i2 = (Integer) rhs.get("id");
    return i1.compareTo(i2);
  }
   
  @Test
  public void testInnerJoin(Vertx vertx, VertxTestContext testContext) {

    Context context = vertx.getOrCreateContext();
        
    ReadStream<DataRow> primaryRowsStream = createPrimaryRows(context);
    ReadStream<DataRow> secondaryRowsStream = createSecondaryRows(context);

    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    
    MergeStream<DataRow, DataRow, DataRow> ms = new MergeStream<>(context
            , pipelineContext
            , primaryRowsStream
            , secondaryRowsStream
            , this::merge
            , this::compareId
            , true
            , 3
            , 1
            , 8
            , 4
    );
    
    ReadStreamToList.capture(ms)
            .onFailure(testContext::failNow)
            .onSuccess(rows -> {
              logger.debug("Inner join rows: {}", rows);
              testContext.verify(() -> {
                assertEquals(5, rows.size());
                for (DataRow row : rows) {
                  assertThat(row.get("sum"), instanceOf(Integer.class));
                  int i = (Integer) row.get("id");
                  assertThat(i, not(equalTo(5)));
                  assertThat(row.get("sum"), equalTo((i * (i + 1)) / 2));
                }
              });
              testContext.completeNow();
            })
            ;
  }

   
  @Test
  public void testLeftJoin(Vertx vertx, VertxTestContext testContext) {

    Context context = vertx.getOrCreateContext();
        
    ReadStream<DataRow> primaryRowsStream = createPrimaryRows(context);
    ReadStream<DataRow> secondaryRowsStream = createSecondaryRows(context);

    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    
    MergeStream<DataRow, DataRow, DataRow> ms = new MergeStream<>(context
            , pipelineContext
            , primaryRowsStream
            , secondaryRowsStream
            , this::merge
            , this::compareId
            , false
            , 3
            , 1
            , 8
            , 4
    );
    
    ReadStreamToList.captureByBatch(ms, 2, 2)
            .onFailure(testContext::failNow)
            .onSuccess(rows -> {
              logger.debug("Left join rows: {}", rows);
              testContext.verify(() -> {
                if (rows.size() != 6) {
                  assertEquals(Arrays.asList(), rows);
                }
                assertEquals(6, rows.size());                
                for (DataRow row : rows) {
                  assertThat(row.get("sum"), instanceOf(Integer.class));
                  int i = (Integer) row.get("id");
                  if (i == 5) {
                    assertThat(row.get("sum"), equalTo(0));
                  } else {
                    assertThat(row.get("sum"), equalTo((i * (i + 1)) / 2));
                  }
                }
              });
              testContext.completeNow();
            })
            ;
  }

  ReadStream<DataRow> createPrimaryRows(Context context) {
    Types tp = new Types();
    List<DataRow> pRowsList = Arrays.asList(
            DataRow.create(tp, "id", 1, "value", "one")
            , DataRow.create(tp, "id", 2, "value", "two")
            , DataRow.create(tp, "id", 3, "value", "three")
            , DataRow.create(tp, "id", 4, "value", "four")
            , DataRow.create(tp, "id", 5, "value", "five")
            , DataRow.create(tp, "id", 6, "value", "six")
    );
    ReadStream<DataRow> primaryRowsStream = new ListReadStream<>(context, pRowsList);
    return primaryRowsStream;
  }

  ReadStream<DataRow> createSecondaryRows(Context context) {
    Types ts = new Types();
    List<DataRow> sRowsList = Arrays.asList(
            DataRow.create(ts, "id", 1, "number", 1)
            , DataRow.create(ts, "id", 2, "number", 1)
            , DataRow.create(ts, "id", 2, "number", 2)
            , DataRow.create(ts, "id", 3, "number", 1)
            , DataRow.create(ts, "id", 3, "number", 2)
            , DataRow.create(ts, "id", 3, "number", 3)
            , DataRow.create(ts, "id", 4, "number", 1)
            , DataRow.create(ts, "id", 4, "number", 2)
            , DataRow.create(ts, "id", 4, "number", 3)
            , DataRow.create(ts, "id", 4, "number", 4)
            , DataRow.create(ts, "id", 6, "number", 1)
            , DataRow.create(ts, "id", 6, "number", 2)
            , DataRow.create(ts, "id", 6, "number", 3)
            , DataRow.create(ts, "id", 6, "number", 4)
            , DataRow.create(ts, "id", 6, "number", 5)
            , DataRow.create(ts, "id", 6, "number", 6)
    );
    return new ListReadStream<>(context, sRowsList);
  }

}
