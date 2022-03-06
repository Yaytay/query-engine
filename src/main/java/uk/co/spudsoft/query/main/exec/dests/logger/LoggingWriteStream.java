/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.dests.logger;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.streams.WriteStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jtalbut
 * @param <T> Type of object passed in to the WriteStream.
 */
public class LoggingWriteStream<T> implements WriteStream<T> {
  
  private static final Logger logger = LoggerFactory.getLogger(LoggingWriteStream.class);

  private Handler<Throwable> exceptionHandler;
  
  @Override
  public WriteStream<T> exceptionHandler(Handler<Throwable> handler) {
    this.exceptionHandler = handler;
    return this;
  }

  @Override
  public Future<Void> write(T data) {
    Promise<Void> promise = Promise.promise();
    write(data, promise);
    return promise.future();
  }

  @Override
  public void write(T data, Handler<AsyncResult<Void>> handler) {
    logger.debug("Received: {}", data);
    handler.handle(Future.succeededFuture());
  }

  @Override
  public void end(Handler<AsyncResult<Void>> handler) {
    handler.handle(Future.succeededFuture());
  }

  @Override
  public WriteStream<T> setWriteQueueMaxSize(int maxSize) {
    return this;
  }

  @Override
  public boolean writeQueueFull() {
    return false;
  }

  @Override
  public WriteStream<T> drainHandler(Handler<Void> handler) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  
}
