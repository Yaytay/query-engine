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
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.streams.WriteStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.ProcessorLimit;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.DataRowStream;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.procs.AsyncHandler;
import uk.co.spudsoft.query.exec.procs.PassthroughStream;

/**
 *
 * @author jtalbut
 */
public class ProcessorLimitInstance implements ProcessorInstance {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ProcessorLimitInstance.class);
  
  private final SourceNameTracker sourceNameTracker;
  private final Context context;
  private final ProcessorLimit definition;
  private final PassthroughStream stream;
  
  private int count;

  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Be aware that the point of sourceNameTracker is to modify the context")
  public ProcessorLimitInstance(Vertx vertx, SourceNameTracker sourceNameTracker, Context context, ProcessorLimit definition) {
    this.sourceNameTracker = sourceNameTracker;
    this.context = context;
    this.definition = definition;
    this.stream = new PassthroughStream(sourceNameTracker, this::passthroughStreamProcessor, context);
  }  

  /**
   * Purely for test purposes.
   * @return The configured limit for this processor instance.
   */
  public int getLimit() {
    return definition.getLimit();
  }
  
  private Future<Void> passthroughStreamProcessor(DataRow data, AsyncHandler<DataRow> chain) {
    sourceNameTracker.addNameToContextLocalData(context);
    try {
      logger.trace("process {}", data);
      if (++count <= definition.getLimit()) {
        return chain.handle(data);
      } else {
        return Future.succeededFuture();
      }
    } catch (Throwable ex) {
      logger.error("Failed to process {}: ", data, ex);
      return Future.failedFuture(ex);
    }
  }

  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline, String parentSource, int processorIndex) {
    return Future.succeededFuture();
  }

  @Override
  public DataRowStream<DataRow> getReadStream() {
    return stream.readStream();
  }

  @Override
  public WriteStream<DataRow> getWriteStream() {
    return stream.writeStream();
  }  
  
}
