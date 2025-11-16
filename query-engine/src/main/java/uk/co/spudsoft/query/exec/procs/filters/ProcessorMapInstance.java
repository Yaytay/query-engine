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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.tsegismont.streamutils.impl.MappingStream;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.ProcessorMap;
import uk.co.spudsoft.query.defn.ProcessorMapLabel;
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
 * {@link uk.co.spudsoft.query.exec.ProcessorInstance} to alter the fields in a stream.
 * <P>
 * Configuration is via a {@link uk.co.spudsoft.query.defn.ProcessorMap} that has a list of {@link uk.co.spudsoft.query.defn.ProcessorMapLabel} instances.
 * <P>
 * Each {@link uk.co.spudsoft.query.defn.ProcessorMapLabel} can either rename a field or remove it from the stream (if {@link uk.co.spudsoft.query.defn.ProcessorMapLabel#newLabel} is not set).
 *
 * @author jtalbut
 */
public class ProcessorMapInstance extends AbstractProcessor {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ProcessorMapInstance.class);

  private final ProcessorMap definition;
  private MappingStream<DataRow, DataRow> stream;

  private final Map<String, String> relabels;

  private final Types types;

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
  public ProcessorMapInstance(Vertx vertx, MeterRegistry meterRegistry, Auditor auditor, PipelineContext pipelineContext, ProcessorMap definition, String name) {
    super(vertx, meterRegistry, auditor, pipelineContext, name);
    this.definition = definition;
    ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder();
    for (ProcessorMapLabel relabel : definition.getRelabels()) {
      builder.put(relabel.getSourceLabel(), relabel.getNewLabel());
    }
    this.relabels = builder.build();
    this.types = new Types();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Future<ReadStreamWithTypes> initialize(PipelineExecutor executor, PipelineInstance pipeline, String parentSource, int processorIndex, ReadStreamWithTypes input) {
    this.stream = new MappingStream<>(input.getStream(), (oldData) -> {
      DataRow newData = DataRow.create(types);
      oldData.forEach((defn, value) -> {
        String newLabel = relabels.get(defn.name());
        if (newLabel == null) {
          newData.put(defn.name(), defn.type(), value);
        } else if (!Strings.isNullOrEmpty(newLabel)) {
          newData.put(newLabel, defn.type(), value);
        }
      });
      return newData;
    });
     input.getTypes().forEach(cd -> {
        String newLabel = relabels.get(cd.name());
        if (newLabel == null) {
          types.putIfAbsent(cd.name(), cd.type());
        } else if (!Strings.isNullOrEmpty(newLabel)) {
          types.putIfAbsent(newLabel, cd.type());
        }
     });
    return Future.succeededFuture(new ReadStreamWithTypes(stream, types));
  }

}
