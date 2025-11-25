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
package uk.co.spudsoft.query.exec.procs.filters;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.tsegismont.streamutils.impl.SkippingStream;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.ProcessorOffset;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.exec.Auditor;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.procs.AbstractProcessor;

/**
 * {@link uk.co.spudsoft.query.exec.ProcessorInstance} to skip initial rows from the output.
 * <P>
 * Not usually useful in a pipeline definition, aimed at use via the {@link uk.co.spudsoft.query.exec.filters.OffsetFilter}.
 *
 * @author jtalbut
 */
public class ProcessorOffsetInstance extends AbstractProcessor {

  @SuppressWarnings("constantname")
  private static final Logger slf4jlogger = LoggerFactory.getLogger(ProcessorOffsetInstance.class);

  private final ProcessorOffset definition;
  private SkippingStream<DataRow> stream;
  private Types types;

  /**
   * Constructor.
   * @param vertx the Vert.x instance.
   * @param meterRegistry MeterRegistry for production of metrics.
   * @param auditor The auditor that the source should use for recording details of the data accessed.
   * @param pipelineContext The context in which this {@link SourcePipeline} is being run.
   * @param definition the definition of this processor.
   * @param name the name of this processor, used in tracking and logging.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The requestContext should not be modified by this class")
  public ProcessorOffsetInstance(Vertx vertx, MeterRegistry meterRegistry, Auditor auditor, PipelineContext pipelineContext, ProcessorOffset definition, String name) {
    super(slf4jlogger, vertx, meterRegistry, auditor, pipelineContext, name);
    this.definition = definition;
  }

  /**
   * Purely for test purposes.
   * @return The configured offset for this processor instance.
   */
  public int getOffset() {
    return definition.getOffset();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Future<ReadStreamWithTypes> initialize(PipelineExecutor executor, PipelineInstance pipeline, String parentSource, int processorIndex, ReadStreamWithTypes input) {
    this.stream = new SkippingStream<>(input.getStream(), definition.getOffset());
    this.types = input.getTypes();
    return Future.succeededFuture(new ReadStreamWithTypes(stream, types));
  }

}
