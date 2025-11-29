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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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

  private static final int PRIMARY_ROWS = 1000;
  private static final int TEST_ITERATIONS = 1000;

  private Vertx vertx;

  @BeforeAll
  public void setup(Vertx vertx, VertxTestContext testContext) {
    this.vertx = vertx;
    testContext.completeNow();
  }

  @AfterAll
  public void teardown(VertxTestContext testContext) {
    testContext.completeNow();
  }
  
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

  static Stream<Integer> iterationProvider() {
    return IntStream.rangeClosed(1, TEST_ITERATIONS).boxed();
  }

  @ParameterizedTest
  @MethodSource("iterationProvider")
  public void testInnerJoin(int iteration, VertxTestContext testContext) {

    logger.info("Running inner join iteration {}", iteration);

    Context context = vertx.getOrCreateContext();

    ReadStream<DataRow> primaryRowsStream = createPrimaryRows(context);
    ReadStream<DataRow> secondaryRowsStream = createSecondaryRows(context);

    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);

    MergeStream<DataRow, DataRow, DataRow> ms = new MergeStream<>(context,
             pipelineContext,
             primaryRowsStream,
             secondaryRowsStream,
             this::merge,
             this::compareId,
             true,
             3,
             1,
             8,
             4
    );

    ReadStreamToList.capture(pipelineContext, ms)
            .onFailure(testContext::failNow)
            .onSuccess(rows -> {
              logger.trace("Inner join rows: {}", rows);
              testContext.verify(() -> {
                if (rows.size() != PRIMARY_ROWS - 1) {
                  logger.error("Should have {} rows, but got: {}", PRIMARY_ROWS - 1, rows);
                }
                assertEquals(PRIMARY_ROWS - 1, rows.size());
                for (DataRow row : rows) {
                  assertThat(row.get("sum"), instanceOf(Integer.class));
                  int i = (Integer) row.get("id");
                  assertThat(i, not(equalTo(5)));
                  assertThat(row.get("sum"), equalTo((i * (i + 1)) / 2));
                }
              });
              testContext.completeNow();
            });
  }

  @ParameterizedTest
  @MethodSource("iterationProvider")
  public void testLeftJoin(int iteration, VertxTestContext testContext) {

    logger.info("Running left join iteration {}", iteration);
    
    // Row 5 is the one that is skipped, so unless there are at least 5 rows this test doesn't work
    assertThat(PRIMARY_ROWS, greaterThan(4));

    Context context = vertx.getOrCreateContext();

    ReadStream<DataRow> primaryRowsStream = createPrimaryRows(context);
    ReadStream<DataRow> secondaryRowsStream = createSecondaryRows(context);

    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);

    MergeStream<DataRow, DataRow, DataRow> ms = new MergeStream<>(context,
             pipelineContext,
             primaryRowsStream,
             secondaryRowsStream,
             this::merge,
             this::compareId,
             false,
             3,
             1,
             8,
             4
    );

    ReadStreamToList.captureByBatch(pipelineContext, ms, 2, 2)
            .onFailure(testContext::failNow)
            .onSuccess(rows -> {
              logger.trace("Left join rows: {}", rows);
              testContext.verify(() -> {
                if (rows.size() != PRIMARY_ROWS) {
                  logger.error("Should have {} rows, but got: {}", PRIMARY_ROWS, rows);
                }
                assertEquals(PRIMARY_ROWS, rows.size());
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
            });
  }

  ReadStream<DataRow> createPrimaryRows(Context context) {
    Types tp = new Types();
    List<DataRow> pRowsList = new ArrayList<>();
    for (int parentIdx = 1; parentIdx <= PRIMARY_ROWS; ++parentIdx) {
      pRowsList.add(DataRow.create(tp, "id", parentIdx, "value", numName(parentIdx)));
    }
    ReadStream<DataRow> primaryRowsStream = new ListReadStream<>(null, context, pRowsList);
    return primaryRowsStream;
  }

  ReadStream<DataRow> createSecondaryRows(Context context) {
    Types ts = new Types();

    List<DataRow> sRowsList = new ArrayList<>();
    for (int parentIdx = 1; parentIdx <= PRIMARY_ROWS; ++parentIdx) {
      if (parentIdx != 5) {
        for (int childIdx = 1; childIdx <= parentIdx; ++childIdx) {
          sRowsList.add(DataRow.create(ts, "id", parentIdx, "number", childIdx));
        }
      }
    }
    return new ListReadStream<>(null, context, sRowsList);
  }

  private static final String[] BELOW_TWENTY = {
    "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
    "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen",
    "seventeen", "eighteen", "nineteen"
  };

  private static final String[] TENS = {
    "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"
  };

  private static String numName(int num) {
    if (num == 0) {
      return "zero";
    }
    return numNameInternal(num).trim();
  }

  private static String numNameInternal(int num) {
    if (num < 20) {
      return BELOW_TWENTY[num];
    } else if (num < 100) {
      return TENS[num / 10] + (num % 10 != 0 ? " " + BELOW_TWENTY[num % 10] : "");
    } else if (num < 1000) {
      return BELOW_TWENTY[num / 100] + " hundred"
              + (num % 100 != 0 ? " and " + numNameInternal(num % 100) : "");
    } else if (num < 1_000_000) {
      return numNameInternal(num / 1000) + " thousand"
              + (num % 1000 != 0 ? ", " + numNameInternal(num % 1000) : "");
    } else if (num < 1_000_000_000) {
      return numNameInternal(num / 1_000_000) + " million"
              + (num % 1_000_000 != 0 ? ", " + numNameInternal(num % 1_000_000) : "");
    } else {
      return numNameInternal(num / 1_000_000_000) + " billion"
              + (num % 1_000_000_000 != 0 ? ", " + numNameInternal(num % 1_000_000_000) : "");
    }
  }

}
