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

import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.tsegismont.streamutils.impl.MappingStream;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.streams.ReadStream;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.ProcessorMap;
import uk.co.spudsoft.query.defn.ProcessorMapLabel;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.Types;

/**
 *
 * @author jtalbut
 */
public class ProcessorMapInstance implements ProcessorInstance {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ProcessorMapInstance.class);
  
  private final SourceNameTracker sourceNameTracker;
  private final Context context;
  private final ProcessorMap definition;
  private MappingStream<DataRow, DataRow> stream;
  
  private final Map<String, String> relabels;
  
  private final Types types = new Types();
  
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Be aware that the point of sourceNameTracker is to modify the context")
  public ProcessorMapInstance(Vertx vertx, SourceNameTracker sourceNameTracker, Context context, ProcessorMap definition) {
    this.sourceNameTracker = sourceNameTracker;
    this.context = context;
    this.definition = definition;
    ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder();
    for (ProcessorMapLabel relabel : definition.getRelabels()) {
      builder.put(relabel.getSourceLabel(), relabel.getNewLabel());
    }
    this.relabels = builder.build();
  }

  @Override
  public String getId() {
    return definition.getId();
  }

  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline, String parentSource, int processorIndex, ReadStream<DataRow> input) {
    this.stream = new MappingStream<>(input, (oldData) -> {
      DataRow newData = DataRow.create(types);
      oldData.forEach((defn, value) -> {
        String newLabel = relabels.get(defn.name());
        if (newLabel == null) {
          newData.put(defn.name(), defn.type(), value);
        } else {
          newData.put(newLabel, defn.type(), value);
        }
      });
      return newData;
    });
    return Future.succeededFuture();
  }

  @Override
  public ReadStream<DataRow> getReadStream() {
    return stream;
  }
}
