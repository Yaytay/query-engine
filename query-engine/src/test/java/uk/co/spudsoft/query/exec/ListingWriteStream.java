/*
 * Copyright (C) 2024 njt
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

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.streams.WriteStream;
import java.util.List;

/**
 *
 * @author njt
 */
public class ListingWriteStream<T> implements WriteStream<T> {

  private final List<T> list;

  public ListingWriteStream(List<T> list) {
    this.list = list;
  }

  @Override
  public WriteStream<T> exceptionHandler(Handler<Throwable> handler) {
    return this;
  }

  @Override
  public Future<Void> write(T data) {
    list.add(data);
    return Future.succeededFuture();
  }

  @Override
  public void write(T data, Handler<AsyncResult<Void>> handler) {
    write(data).andThen(handler);
  }

  @Override
  public void end(Handler<AsyncResult<Void>> handler) {
    handler.handle(Future.succeededFuture());
  }

  @Override
  public WriteStream<T> setWriteQueueMaxSize(int maxSize) {
    return  this;
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
