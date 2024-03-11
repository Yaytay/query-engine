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
package uk.co.spudsoft.query.exec.fmts.json;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import java.util.concurrent.atomic.AtomicBoolean;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.exec.fmts.FormattingWriteStream;
import uk.co.spudsoft.query.exec.FormatInstance;
import uk.co.spudsoft.query.web.RequestContextHandler;


/**
 *
 * @author jtalbut
 */
public class FormatJsonInstance implements FormatInstance {
 
  private final WriteStream<Buffer> outputStream;
  private final FormattingWriteStream formattingStream;
  
  private static final Buffer OPEN = Buffer.buffer("[");
  private static final Buffer COMMA = Buffer.buffer(",");
  private static final Buffer CLOSE = Buffer.buffer("]");
  private final AtomicBoolean started = new AtomicBoolean();
  
  /**
   * Constructor.
   * @param outputStream The WriteStream that the data is to be sent to.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "FormatJsonInstance is a wrapper around WriteStream<Buffer>, it will make mutating calls to it")
  public FormatJsonInstance(WriteStream<Buffer> outputStream) {
    this.outputStream = outputStream;
    this.formattingStream = new FormattingWriteStream(outputStream
            , v -> outputStream.write(OPEN)
            , row -> {
              if (row.isEmpty()) {
                return Future.succeededFuture();
              } else if (started.get()) {
                return outputStream.write(COMMA)
                        .compose(v -> outputStream.write(row.toJson().toBuffer()))
                        ;
              } else {
                started.set(true);
                return outputStream.write(row.toJson().toBuffer());
              }
            }
            , rows -> {
              Context vertxContext = Vertx.currentContext();
              if (vertxContext != null) {
                RequestContext requestContext = RequestContextHandler.getRequestContext(Vertx.currentContext());
                if (requestContext != null) {
                  requestContext.setRowsWritten(rows);
                }
              }
              return outputStream.write(CLOSE)
                      .compose(v2 -> outputStream.end())
                      ;
            }
    );
  }

  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline, ReadStream<DataRow> input) {
    return input.pipeTo(formattingStream);
  }
  
  @Override
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "The caller WILL modify the state of the returned WriteStream.")
  public WriteStream<DataRow> getWriteStream() {
    return formattingStream;
  }
  
}
