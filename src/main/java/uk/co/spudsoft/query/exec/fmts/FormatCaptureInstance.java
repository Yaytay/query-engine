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
package uk.co.spudsoft.query.exec.fmts;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.streams.WriteStream;
import java.util.List;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.FormatInstance;

/**
 *
 * A Format that captures all the data and makes it available.
 * 
 * This is of no use to end-user pipelines because it provides no way to access the data
 * , but is used by the DynamicEndpoint processing.
 * 
 * @author jtalbut
 */
public class FormatCaptureInstance implements FormatInstance {
  
  private final CapturingWriteStream capturingStream;
  private final Promise<List<DataRow>> finalPromise = Promise.promise();

  public FormatCaptureInstance() {
    this.capturingStream = new CapturingWriteStream(
            rows -> {
              finalPromise.tryComplete(rows);
            }
    );
  }

  public Future<List<DataRow>> getFuture() {
    return finalPromise.future();
  }
  
  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline) {
    return Future.succeededFuture();
  }

  @Override
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "The write stream must be accessible.  This is different from FormattingWriteStream because CapturingWriteStream manages its own ExceptionHandler")
  public WriteStream<DataRow> getWriteStream() {
    return capturingStream;
  }
  
  
  
}
