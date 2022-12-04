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
package uk.co.spudsoft.query.exec.procs;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Future;
import io.vertx.core.streams.WriteStream;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.FormatInstance;

/**
 * A simple wrapper class so that a Processor may be used as a Format in a child pipeline.
 * 
 * @author jtalbut
 */
public class ProcessorFormat implements FormatInstance {

  private final WriteStream<DataRow> writeStream;

  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "ProcessorFormat exists to pass around the WriteStream")
  public ProcessorFormat(WriteStream<DataRow> writeStream) {
    this.writeStream = writeStream;
  }
  
  /**
   * Do nothing, the Processor is assumed to have initialized itself in the call to {@link uk.co.spudsoft.query.exec.ProcessorInstance#initialize(uk.co.spudsoft.query.exec.PipelineExecutor, uk.co.spudsoft.query.exec.PipelineInstance, java.lang.String, int)  }.
   * @param executor
   * @param pipeline
   * @return 
   */
  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline) {
    return Future.succeededFuture();
  }

  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "ProcessorFormat exists to pass around the WriteStream")
  @Override
  public WriteStream<DataRow> getWriteStream() {
    return writeStream;
  }
  
}
