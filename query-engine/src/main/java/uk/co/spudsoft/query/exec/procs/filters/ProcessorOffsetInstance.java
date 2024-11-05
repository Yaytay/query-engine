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
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.ProcessorOffset;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.Types;

/**
 * {@link uk.co.spudsoft.query.exec.ProcessorInstance} to skip initial rows from the output.
 * <P>
 * Not usually useful in a pipeline definition, aimed at use via the {@link uk.co.spudsoft.query.exec.filters.OffsetFilter}.
 *
 * @author jtalbut
 */
public class ProcessorOffsetInstance implements ProcessorInstance {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ProcessorOffsetInstance.class);
  
  private final SourceNameTracker sourceNameTracker;
  private final Context context;
  private final ProcessorOffset definition;
  private SkippingStream<DataRow> stream;
  private Types types;
  private final String name;
  
  /**
   * Constructor.
   * @param vertx the Vert.x instance.
   * @param sourceNameTracker the name tracker used to record the name of this source at all entry points for logger purposes.
   * @param context the Vert.x context.
   * @param definition the definition of this processor.
   * @param name the name of this processor, used in tracking and logging.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Be aware that the point of sourceNameTracker is to modify the context")
  public ProcessorOffsetInstance(Vertx vertx, SourceNameTracker sourceNameTracker, Context context, ProcessorOffset definition, String name) {
    this.sourceNameTracker = sourceNameTracker;
    this.context = context;
    this.definition = definition;
    this.name = name;
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
