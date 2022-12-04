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
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.procs.AsyncHandler;

/**
 *
 * @author jtalbut
 */
public class FormattingWriteStream implements WriteStream<DataRow> {
  
  private final WriteStream<Buffer> outputStream;
  private boolean initialized;
  private final AsyncHandler<Void> initialize;
  private final AsyncHandler<DataRow> process;
  private final AsyncHandler<Long> terminate;
  private long rowCount = 0;

  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "FormattingWriteStream is a helper wrapper around WriteStream<Buffer>, it will make mutating calls to it")
  public FormattingWriteStream(
          WriteStream<Buffer> outputStream
          , AsyncHandler<Void> initialize
          , AsyncHandler<uk.co.spudsoft.query.exec.DataRow> process
          , AsyncHandler<Long> terminate
  ) {
    this.outputStream = outputStream;
    this.initialize = initialize;
    this.process = process;
    this.terminate = terminate;
  }

  @Override
  public WriteStream<DataRow> exceptionHandler(Handler<Throwable> handler) {
    outputStream.exceptionHandler(handler);
    return this;
  }

  @Override
  public Future<Void> write(DataRow data) {
    ++rowCount;
    if (initialized) {
      return process.handle(data);
    } else {
      initialized = true;
      return initialize.handle(null)
              .compose(v -> process.handle(data));
    }
  }

  @Override
  public void write(DataRow data, Handler<AsyncResult<Void>> handler) {
    write(data).onComplete(handler);
  }

  @Override
  public void end(Handler<AsyncResult<Void>> handler) {
    terminate.handle(rowCount).onComplete(handler);
  }

  @Override
  public WriteStream<DataRow> setWriteQueueMaxSize(int maxSize) {
    return this;
  }

  @Override
  public boolean writeQueueFull() {
    return outputStream.writeQueueFull();
  }

  @Override
  public WriteStream<DataRow> drainHandler(Handler<Void> handler) {
    outputStream.drainHandler(handler);
    return this;
  }
  
  
  
}
