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
package uk.co.spudsoft.query.exec.procs.subquery;

import io.vertx.core.Future;
import io.vertx.core.streams.ReadStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.Types;

/**
 *
 * @author jtalbut
 */
public class AbstractJoiningProcessorTest {
  
  private static final Logger logger = LoggerFactory.getLogger(AbstractJoiningProcessorTest.class);
  
  private static final class TestClass extends AbstractJoiningProcessor {

    public TestClass(List<String> parentIdColumns, List<String> childIdColumns) {
      super(logger, null, null, null, null, null, parentIdColumns, childIdColumns, true);
    }

    @Override
    public int compare(DataRow parentRow, DataRow childRow) {
      return super.compare(parentRow, childRow); 
    }

    @Override
    DataRow processChildren(DataRow parentRow, List<DataRow> childRows) {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    Future<ReadStream<DataRow>> initializeChild(PipelineExecutor executor, PipelineInstance pipeline, String parentSource, int processorIndex) {
      throw new UnsupportedOperationException("Not supported yet.");
    }
  }
  
  @Test
  public void testCompareIntInt() {
    Types typesParent = new Types();
    typesParent.putIfAbsent("id", DataType.Integer);
    
    Types typesChild = new Types();
    typesChild.putIfAbsent("idParent", DataType.Integer);
    
    TestClass tc = new TestClass(Arrays.asList("id"), Arrays.asList("idParent"));
    
    assertEquals(0
            , tc.compare(
                    DataRow.create(typesParent, "id", 1)
                    , 
                    DataRow.create(typesChild, "idParent", 1)
                    )
    );
  }
  
  @Test
  public void testCompareIntLong() {
    Types typesParent = new Types();
    typesParent.putIfAbsent("id", DataType.Integer);
    
    Types typesChild = new Types();
    typesChild.putIfAbsent("idParent", DataType.Long);
    
    TestClass tc = new TestClass(Arrays.asList("id"), Arrays.asList("idParent"));
    
    assertEquals(0
            , tc.compare(
                    DataRow.create(typesParent, "id", 1)
                    , 
                    DataRow.create(typesChild, "idParent", 1L)
                    )
    );
  }
  
  @Test
  public void testCompareLongInt() {
    Types typesParent = new Types();
    typesParent.putIfAbsent("id", DataType.Long);
    
    Types typesChild = new Types();
    typesChild.putIfAbsent("idParent", DataType.Integer);
    
    TestClass tc = new TestClass(Arrays.asList("id"), Arrays.asList("idParent"));
    
    assertEquals(0
            , tc.compare(
                    DataRow.create(typesParent, "id", 1L)
                    ,
                    DataRow.create(typesChild, "idParent", 1)
                    )
    );
  }
  
  @Test
  public void testCompareLongDate() {
    Types typesParent = new Types();
    typesParent.putIfAbsent("id", DataType.Long);
    
    Types typesChild = new Types();
    typesChild.putIfAbsent("idParent", DataType.Date);
    
    TestClass tc = new TestClass(Arrays.asList("id"), Arrays.asList("idParent"));
    
    assertEquals("No common type between Long and Date"
            , assertThrows(
                    IllegalArgumentException.class
                    , () -> tc.compare(
                            DataRow.create(typesParent, "id", 1L)
                            , 
                            DataRow.create(typesChild, "idParent", LocalDate.now())
                            )
            ).getMessage()
    );
  }
  
}
