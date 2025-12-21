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
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.procs.AsyncHandler;
import uk.co.spudsoft.query.logging.Log;

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

  private final Log log;

  private long rowCount = 0;

  private final Object lastProcessFutureLock = new Object();
  private Future<Void> lastProcessFuture = Future.succeededFuture();

  /**
   * Constructor.
   * @param pipelineContext The context in which this {@link SourcePipeline} is being run.  The container requestContext must have the rowCount updated at the end.
   * @param outputStream The {@link WriteStream} that the formatted output is to be written to.
   * @param initialize {@link AsyncHandler} to call when the output must be initialized (output headers, etc.).
   * @param process {@link AsyncHandler} to call for each DataRow written.
   * @param terminate {@link AsyncHandler} to call when the output must be terminated (output footer).
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "FormattingWriteStream is a helper wrapper around WriteStream<Buffer>, it will make mutating calls to it")
  public FormattingWriteStream(
          PipelineContext pipelineContext
          , WriteStream<Buffer> outputStream
          , AsyncHandler<Void> initialize
          , AsyncHandler<DataRow> process
          , AsyncHandler<Long> terminate
  ) {
    this.outputStream = outputStream;
    this.initialize = initialize;
    this.process = process;
    this.terminate = terminate;
    this.lastProcessFuture = Future.succeededFuture();
    this.log = new Log(logger, pipelineContext);
  }

  @Override
  public WriteStream<DataRow> exceptionHandler(Handler<Throwable> handler) {
    outputStream.exceptionHandler(handler);
    return this;
  }

  @Override
  public Future<Void> write(DataRow data) {
    Promise<Void> currentWritePromise = Promise.promise();
    Future<Void> previous;

    synchronized (lastProcessFutureLock) {
      previous = lastProcessFuture;
      lastProcessFuture = currentWritePromise.future();
    }

    previous.onComplete(ar -> {
      performWrite(ar, currentWritePromise, data);
    });
    return currentWritePromise.future();
  }

  void performWrite(AsyncResult<Void> ar, Promise<Void> currentWritePromise, DataRow data) {
    if (ar.failed()) {
      currentWritePromise.fail(ar.cause());
    } else {
      try {
        ++rowCount;
        Future<Void> result;
        if (initialized) {
          result = process.handle(data);
        } else {
          initialized = true;
          result = initialize.handle(null).compose(v -> process.handle(data));
        }
        result.onComplete(currentWritePromise);
      } catch (Throwable t) {
        currentWritePromise.fail(t);
      }
    }
  }

  private Future<Void> handleTermination() {
    try {
      log.info().log("WriteStream terminating");
      return terminate.handle(rowCount);
    } catch (Throwable ex) {
      log.error().log("Termination callback failed: ", ex);
      return Future.failedFuture(ex);
    }
  }

  @Override
  public Future<Void> end() {
    log.info().log("WriteStream end: {}", lastProcessFuture);
    // Ensure we run on the context to allow pending I/O tasks
    // a chance to execute before we try to compose the end.
    Promise<Void> endPromise = Promise.promise();

    lastProcessFuture.onComplete(ar -> {
      log.info().log("LastProcessFuture completed: {}", lastProcessFuture);
      Vertx.currentContext().runOnContext(v -> {
        log.info().log("Running on context: {}", initialized);
        Future<Void> finalStep;
        if (initialized) {
          finalStep = handleTermination();
        } else {
          finalStep = initialize.handle(null).compose(v2 -> handleTermination());
        }
        finalStep.onComplete(endPromise);
      });
    });

    return endPromise.future();
  }

  @Override
  public WriteStream<DataRow> setWriteQueueMaxSize(int maxSize) {
    outputStream.setWriteQueueMaxSize(maxSize);
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
