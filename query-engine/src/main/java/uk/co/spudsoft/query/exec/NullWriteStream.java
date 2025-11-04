/*
 * Copyright (C) 2024 jtalbut
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
package uk.co.spudsoft.query.exec;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.streams.WriteStream;

/**
 * A {@link io.vertx.core.streams.WriteStream} that will accept whatever you put into it and never fail (or do anything else).
 * 
 * @param <T> The type of data passed in to the WriteStream.
 * @author jtalbut
 */
public class NullWriteStream<T> implements WriteStream<T> {

  /**
   * Constructor.
   */
  public NullWriteStream() {
  }
  
  @Override
  public WriteStream<T> exceptionHandler(Handler<Throwable> handler) {
    return this;
  }

  @Override
  public Future<Void> write(T data) {
    return Future.succeededFuture();
  }

  @Override
  public Future<Void> end() {
    return Future.succeededFuture();
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
    return this;
  }
  
}
