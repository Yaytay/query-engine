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
package uk.co.spudsoft.query.exec.sources.test;

import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.SourceTest;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.exec.Auditor;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.procs.QueueReadStream;
import uk.co.spudsoft.query.exec.sources.AbstractSource;
import uk.co.spudsoft.query.logging.Log;

/**
 * {@link uk.co.spudsoft.query.exec.SourceInstance} class for generating a simple test stream.
 * <P>
 * Configuration is via a {@link uk.co.spudsoft.query.defn.SourceTest} object.
 * <P>
 * The stream will contain two fields "value" - a sequence of increasing integers - and "name" a static name set in the configuration.
 *
 * @author jtalbut
 */
public class SourceTestInstance extends AbstractSource {

  private static final Logger logger = LoggerFactory.getLogger(SourceTestInstance.class);

  private final int rowCount;
  private final int delayMs;
  private final String name;
  private final Types types;
  private final QueueReadStream<DataRow> stream;

  private final Context context;
  
  /**
   * Constructor.
   * @param vertx The vertx instance.
   * @param auditor The auditor that the source should use for recording details of the data accessed.
   * @param meterRegistry MeterRegistry for production of processor-specific metrics.
   * @param pipelineContext The context in which this {@link SourcePipeline} is being run.
   * @param definition The configuration of this source.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The requestContext should not be modified by this class")
  public SourceTestInstance(Vertx vertx, MeterRegistry meterRegistry, Auditor auditor, PipelineContext pipelineContext, SourceTest definition) {
    super(vertx, meterRegistry, auditor, pipelineContext);
    this.rowCount = definition.getRowCount();
    this.delayMs = definition.getDelayMs();
    this.types = new Types();
    this.types.putIfAbsent("value", DataType.Integer);
    if (!Strings.isNullOrEmpty(definition.getName())) {
      types.putIfAbsent("name", DataType.String);
      this.name = definition.getName();
    } else {
      this.name = null;
    }
    this.context = vertx.getOrCreateContext();
    this.stream = new QueueReadStream<>(pipelineContext, this.context);
  }

  @Override
  public Future<ReadStreamWithTypes> initialize(PipelineExecutor executor, PipelineInstance pipeline) {
    
    auditor.recordSource(pipelineContext, null, "test:" + this.name, null, null, null);
    
    stream.pause();
    if (delayMs == 0) {
      try {
        for (int i = 0; i < rowCount; ++i) {
          addNewRow(i);
        }
        stream.complete();
      } catch (Throwable ex) {
        return Future.failedFuture(ex);
      }
      return Future.succeededFuture(new ReadStreamWithTypes(stream, types));
    } else {
      AtomicInteger iteration = new AtomicInteger(rowCount);
      context.owner().setPeriodic(delayMs, delayMs, id -> {
        int i = iteration.decrementAndGet();
        if (i >= 0) {
          addNewRow(i);
        } else {
          stream.complete();
          context.owner().cancelTimer(id);
        }
      });
      return Future.succeededFuture(new ReadStreamWithTypes(stream, types));
    }
  }

  void addNewRow(int i) {
    Log.decorate(logger.atDebug(), pipelineContext).log("Creating row {}", i);
    DataRow data = DataRow.create(types);
    data.put("value", i);
    if (name != null) {
      data.put("name", name);
    }
    stream.add(data);
  }

}

