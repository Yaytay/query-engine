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
package uk.co.spudsoft.query.exec.fmts.logger;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.streams.WriteStream;
import java.util.concurrent.atomic.AtomicLong;
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
  private final Handler<Long> endHandler;
  private final AtomicLong count = new AtomicLong();

  public LoggingWriteStream(Handler<Long> endHandler) {
    this.endHandler = endHandler;
  }
    
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
    logger.trace("Received: {}", data);
    count.incrementAndGet();
    handler.handle(Future.succeededFuture());
  }

  public long getCount() {
    return count.get();
  }
  
  @Override
  public void end(Handler<AsyncResult<Void>> handler) {
    if (endHandler != null) {
      endHandler.handle(count.get());
    }
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
