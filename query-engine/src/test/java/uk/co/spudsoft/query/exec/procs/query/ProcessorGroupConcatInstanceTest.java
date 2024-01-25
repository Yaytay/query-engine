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
package uk.co.spudsoft.query.exec.procs.query;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.query.defn.Pipeline;
import uk.co.spudsoft.query.defn.ProcessorGroupConcat;
import uk.co.spudsoft.query.defn.ProcessorScript;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.defn.SourceTest;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineExecutorImpl;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.SourceInstance;
import uk.co.spudsoft.query.exec.SourceNameTracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import uk.co.spudsoft.query.defn.FormatDelimited;
import uk.co.spudsoft.query.exec.ColumnDefn;
import uk.co.spudsoft.query.exec.fmts.logger.LoggingWriteStream;
import uk.co.spudsoft.query.exec.FormatInstance;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProcessorGroupConcatInstanceTest {
  
  private static class TestProcessorGroupConcatInstance extends ProcessorGroupConcatInstance {

    public TestProcessorGroupConcatInstance(Vertx vertx, ProcessorGroupConcat definition) {
      super(vertx, new SourceNameTracker() {
        @Override
        public void addNameToContextLocalData(Context context) {
        }
      }, vertx.getOrCreateContext(), definition);
    }

    @Override
    public void processChildren(DataRow parentRow, List<DataRow> childRows) {
      super.processChildren(parentRow, childRows);
    }
    
  }
  
  @Test
  public void testNullchild(Vertx vertx) {
    DataRow parent = DataRow.create(new ArrayList<>());
    List<ColumnDefn> childTypes = new ArrayList<>();
    List<DataRow> children = Arrays.asList(
            DataRow.create(childTypes).put("value", 1)
            , DataRow.create(childTypes).put("value", null)
            , DataRow.create(childTypes).put("value", 3)
    );
    ProcessorGroupConcat definition = ProcessorGroupConcat.builder()
            .childValueColumn("value")
            .delimiter("#")
            .parentValueColumn("out")
            .build();
    TestProcessorGroupConcatInstance instance = new TestProcessorGroupConcatInstance(vertx, definition);
    assertNull(parent.get("out"));
    instance.processChildren(parent, children);
    assertEquals("1#3", parent.get("out"));

    assertNull(instance.getId(null, "id"));
    List<ColumnDefn> types = new ArrayList<>();
    assertEquals(17L, instance.getId(DataRow.create(types).put("id", 17L), "id"));
    assertNull(instance.getId(DataRow.create(types).put("id", null), "id"));
    assertNull(instance.getId(DataRow.create(types).put("id", 17L), "notId"));
  }
  
  @Test
  @Timeout(value = 5, timeUnit = TimeUnit.SECONDS)
  public void testChildEndsEarly(Vertx vertx, VertxTestContext testContext) {
    
    Pipeline pipeline = Pipeline.builder()
            .source(SourceTest.builder().name("test").rowCount(10).build())
            .processors(
                    Arrays.asList(
                            ProcessorGroupConcat.builder()
                                    .input(
                                            SourcePipeline.builder()
                                                    .source(SourceTest.builder().name("test").rowCount(7).build())
                                                    .build()                                            
                                    )
                                    .parentIdColumn("value")
                                    .childIdColumn("value")
                                    .childValueColumn("value")
                                    .parentValueColumn("child")
                                    .build()
                    )
            )
            .formats(Arrays.asList(FormatDelimited.builder().build()))
            .build();
    
    PipelineExecutor executor = new PipelineExecutorImpl(null);
    
    AtomicLong rowCount = new AtomicLong();
    FormatInstance dest = pipeline.getFormats().get(0).createInstance(vertx, vertx.getOrCreateContext(), new LoggingWriteStream<>(rows -> {rowCount.set(rows);}));
    SourceInstance sourceInstance = pipeline.getSource().createInstance(vertx, vertx.getOrCreateContext(), executor, "source");
    PipelineInstance pipelineInstance = new PipelineInstance(
            null
            , pipeline.getSourceEndpointsMap()
            , executor.createPreProcessors(vertx, Vertx.currentContext(), pipeline)
            , sourceInstance
            , executor.createProcessors(vertx, sourceInstance, vertx.getOrCreateContext(), pipeline)
            , dest
    );
    
    executor.initializePipeline(pipelineInstance)
            .compose(v -> pipelineInstance.getFinalPromise().future())
            .onComplete(ar -> {
              testContext.verify(() -> {
                assertEquals(11, rowCount.get());
              });
              testContext.<Void>succeedingThenComplete().handle(ar);
            })
            ;
  }  
  
  
  @Test
  @Timeout(value = 5, timeUnit = TimeUnit.SECONDS)
  public void testChildEndsLate(Vertx vertx, VertxTestContext testContext) {
    
    Pipeline pipeline = Pipeline.builder()
            .source(SourceTest.builder().name("test").rowCount(10).build())
            .processors(
                    Arrays.asList(
                            ProcessorGroupConcat.builder()
                                    .input(
                                            SourcePipeline.builder()
                                                    .source(SourceTest.builder().name("test").rowCount(17).build())
                                                    .build()                                            
                                    )
                                    .parentIdColumn("value")
                                    .childIdColumn("value")
                                    .childValueColumn("value")
                                    .parentValueColumn("child")
                                    .build()
                    )
            )
            .formats(Arrays.asList(FormatDelimited.builder().build()))
            .build();
    
    PipelineExecutor executor = new PipelineExecutorImpl(null);
    
    AtomicLong rowCount = new AtomicLong();
    FormatInstance dest = pipeline.getFormats().get(0).createInstance(vertx, vertx.getOrCreateContext(), new LoggingWriteStream<>(rows -> {rowCount.set(rows);}));
    SourceInstance sourceInstance = pipeline.getSource().createInstance(vertx, vertx.getOrCreateContext(), executor, "source");
    PipelineInstance pipelineInstance = new PipelineInstance(
            null
            , pipeline.getSourceEndpointsMap()
            , executor.createPreProcessors(vertx, Vertx.currentContext(), pipeline)
            , sourceInstance
            , executor.createProcessors(vertx, sourceInstance, vertx.getOrCreateContext(), pipeline)
            , dest
    );
    
    executor.initializePipeline(pipelineInstance)
            .compose(v -> pipelineInstance.getFinalPromise().future())
            .onComplete(ar -> {
              testContext.verify(() -> {
                assertEquals(11, rowCount.get());
              });
              testContext.<Void>succeedingThenComplete().handle(ar);
            })
            ;
  }  
  
  @Test
  @Timeout(value = 5, timeUnit = TimeUnit.SECONDS)
  public void testParentSuppliesEveryOther(Vertx vertx, VertxTestContext testContext) {
    
    Pipeline pipeline = Pipeline.builder()
            .source(SourceTest.builder().rowCount(10).name("parent").build())
            .processors(
                    Arrays.asList(
                            ProcessorScript.builder()
                                    .language("js")
                                    .predicate("row.value % 2 == 0")
                                    .process("const result = {count: row.count == null ? 1 : row.count + 1}; result")
                                    .build()
                            , ProcessorGroupConcat.builder()
                                    .input(
                                            SourcePipeline.builder()
                                                    .source(SourceTest.builder().rowCount(7).name("child").build())
                                                    .processors(
                                                            Arrays.asList(
                                                                    ProcessorScript.builder()
                                                                            .language("js")
                                                                            .process("const result = {count: row.count == null ? 1 : row.count + 1}; result")
                                                                            .build()
                                                            )
                                                    )
                                                    .build()                                            
                                    )
                                    .delimiter(", ")
                                    .parentIdColumn("value")
                                    .childIdColumn("value")
                                    .childValueColumn("value")
                                    .parentValueColumn("children")
                                    .build()
                    )
            )
            .formats(Arrays.asList(FormatDelimited.builder().build()))
            .build();
    
    PipelineExecutor executor = new PipelineExecutorImpl(null);
    
    AtomicLong rowCount = new AtomicLong();
    FormatInstance dest = pipeline.getFormats().get(0).createInstance(vertx, vertx.getOrCreateContext(), new LoggingWriteStream<>(rows -> {rowCount.set(rows);}));
    SourceInstance sourceInstance = pipeline.getSource().createInstance(vertx, vertx.getOrCreateContext(), executor, "source");
    PipelineInstance pipelineInstance = new PipelineInstance(
            null
            , pipeline.getSourceEndpointsMap()
            , executor.createPreProcessors(vertx, Vertx.currentContext(), pipeline)
            , sourceInstance
            , executor.createProcessors(vertx, sourceInstance, vertx.getOrCreateContext(), pipeline)
            , dest
    );
    
    executor.initializePipeline(pipelineInstance)
            .compose(v -> pipelineInstance.getFinalPromise().future())
            .onComplete(ar -> {
              testContext.verify(() -> {
                assertEquals(6, rowCount.get());
              });
              testContext.<Void>succeedingThenComplete().handle(ar);
            })
            ;
  }
   
  @Test
  @Timeout(value = 30, timeUnit = TimeUnit.SECONDS)
  public void testChildSuppliesEveryOther(Vertx vertx, VertxTestContext testContext) {
    
    Pipeline pipeline = Pipeline.builder()
            .source(SourceTest.builder().name("test").rowCount(10).build())
            .processors(
                    Arrays.asList(
                            ProcessorGroupConcat.builder()
                                    .input(
                                            SourcePipeline.builder()
                                                    .source(SourceTest.builder().name("test").rowCount(7).build())
                                                    .processors(
                                                            Arrays.asList(
                                                                    ProcessorScript.builder()
                                                                            .language("js")
                                                                            .predicate("data.value % 2 == 0")
                                                                            .build()
                                                            )
                                                    )
                                                    .build()                                            
                                    )
                                    .delimiter(", ")
                                    .parentIdColumn("value")
                                    .childIdColumn("value")
                                    .childValueColumn("value")
                                    .parentValueColumn("child")
                                    .build()
                    )
            )
            .formats(Arrays.asList(FormatDelimited.builder().build()))
            .build();
    
    PipelineExecutor executor = new PipelineExecutorImpl(null);
    
    AtomicLong rowCount = new AtomicLong();
    FormatInstance dest = pipeline.getFormats().get(0).createInstance(vertx, vertx.getOrCreateContext(), new LoggingWriteStream<>(rows -> {rowCount.set(rows);}));
    SourceInstance sourceInstance = pipeline.getSource().createInstance(vertx, vertx.getOrCreateContext(), executor, "source");
    PipelineInstance pipelineInstance = new PipelineInstance(
            null
            , pipeline.getSourceEndpointsMap()
            , executor.createPreProcessors(vertx, Vertx.currentContext(), pipeline)
            , sourceInstance
            , executor.createProcessors(vertx, sourceInstance, vertx.getOrCreateContext(), pipeline)
            , dest
    );
    
    executor.initializePipeline(pipelineInstance)
            .compose(v -> pipelineInstance.getFinalPromise().future())
            .onComplete(ar -> {
              testContext.verify(() -> {
                assertEquals(11, rowCount.get());
              });
              testContext.<Void>succeedingThenComplete().handle(ar);
            })
            ;
  }
  
}
