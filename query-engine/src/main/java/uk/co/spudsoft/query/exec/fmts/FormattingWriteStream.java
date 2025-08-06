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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.SourceInstance;
import uk.co.spudsoft.query.exec.procs.AsyncHandler;
import uk.co.spudsoft.query.logging.VertxMDC;

/**
 * {@link io.vertx.core.streams.WriteStream}@lt;{@link uk.co.spudsoft.query.exec.DataRow}@gt; that formats the inbound DataRow values and writes them to a {@link io.vertx.core.streams.WriteStream}@lt;{@link io.vertx.core.buffer.Buffer}@gt;.
 * <p>
 * Actual formatting is carried out by a "process" method passed in to the constructor.
 * 
 * @author jtalbut
 */
public class FormattingWriteStream implements WriteStream<DataRow> {
  
  private static final Logger logger = LoggerFactory.getLogger(FormattingWriteStream.class);
  
  private final WriteStream<Buffer> outputStream;
  private boolean initialized;
  private final AsyncHandler<Void> initialize;
  private final AsyncHandler<DataRow> process;
  private final AsyncHandler<Long> terminate;
  private long rowCount = 0;
  
  private final Object lastProcessFutureLock = new Object();
  private Future<Void> lastProcessFuture;

  /**
   * Constructor.
   * @param outputStream The {@link WriteStream} that the formatted output is to be written to.
   * @param initialize {@link AsyncHandler} to call when the output must be initialized (output headers, etc.).
   * @param process {@link AsyncHandler} to call for each DataRow written.
   * @param terminate {@link AsyncHandler} to call when the output must be terminated (output footer).
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "FormattingWriteStream is a helper wrapper around WriteStream<Buffer>, it will make mutating calls to it")
  public FormattingWriteStream(
          WriteStream<Buffer> outputStream
          , AsyncHandler<Void> initialize
          , AsyncHandler<DataRow> process
          , AsyncHandler<Long> terminate
  ) {
    this.outputStream = outputStream;
    this.initialize = initialize;
    this.process = process;
    this.terminate = terminate;
    this.lastProcessFuture = Future.succeededFuture();
  }

  @Override
  public WriteStream<DataRow> exceptionHandler(Handler<Throwable> handler) {
    outputStream.exceptionHandler(handler);
    return this;
  }

  @Override
  public Future<Void> write(DataRow data) {
    return lastProcessFuture.compose(v -> {
      ++rowCount;
      Future<Void> result;
      if (initialized) {
        result = process.handle(data);
      } else {
        initialized = true;
        result = initialize.handle(null)
                .compose(v2 -> process.handle(data));
      }
      synchronized (lastProcessFutureLock) {
        lastProcessFuture = result;        
      }
      return result;
    });
  }

  @Override
  public void write(DataRow data, Handler<AsyncResult<Void>> handler) {
    write(data).onComplete(handler);
  }

  private Future<Void> handleTermination() {
    try {
      return terminate.handle(rowCount);
    } catch (Throwable ex) {
      logger.error("Termination callback failed: ", ex);
      return Future.failedFuture(ex);
    }
  }
  
  @Override
  public void end(Handler<AsyncResult<Void>> handler) {
    lastProcessFuture.andThen(v -> {
      if (initialized) {
        VertxMDC.INSTANCE.remove(SourceInstance.SOURCE_CONTEXT_KEY);
        handleTermination().onComplete(handler);      
      } else {
        initialize.handle(null).compose(v2 -> {
          VertxMDC.INSTANCE.remove(SourceInstance.SOURCE_CONTEXT_KEY);
          return handleTermination();
        }).onComplete(handler);
      }
    });
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
