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
package uk.co.spudsoft.query.exec.procs.query;

import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;
import java.util.Objects;
import java.util.function.Predicate;

/**
 *
 * @author njt
 */
public class FilteringStream<T> implements ReadStream<T> {

  private final ReadStream<T> source;
  private final Predicate<T> predicate;

  private long received;
  private boolean stopped;
  private Handler<Throwable> exceptionHandler;
  private Handler<Void> endHandler;

  public FilteringStream(ReadStream<T> source, Predicate<T> predicate) {
    Objects.requireNonNull(source, "Source cannot be null");
    Objects.requireNonNull(predicate, "Predicate cannot be null");
    this.source = source;
    this.predicate = predicate;
  }

  @Override
  public synchronized ReadStream<T> exceptionHandler(Handler<Throwable> handler) {
    exceptionHandler = handler;
    return this;
  }

  private synchronized Handler<Throwable> getExceptionHandler() {
    return exceptionHandler;
  }

  @Override
  public ReadStream<T> handler(Handler<T> handler) {
    if (handler == null) {
      source.handler(null);
      return this;
    }
    source
      .exceptionHandler(throwable -> notifyTerminalHandler(getExceptionHandler(), throwable))
      .endHandler(v -> notifyTerminalHandler(getEndHandler(), null))
      .handler(item -> {
        boolean emit, terminate;
        synchronized (this) {
          received++;
          emit = !stopped && predicate.test(item);
        }
        if (emit) {
          handler.handle(item);
        }
      });
    return this;
  }

  @Override
  public ReadStream<T> pause() {
    source.pause();
    return this;
  }

  @Override
  public ReadStream<T> resume() {
    return fetch(Long.MAX_VALUE);
  }

  @Override
  public ReadStream<T> fetch(long l) {
    source.fetch(l);
    return this;
  }

  @Override
  public synchronized ReadStream<T> endHandler(Handler<Void> handler) {
    endHandler = handler;
    return this;
  }

  private synchronized Handler<Void> getEndHandler() {
    return endHandler;
  }

  private <V> void notifyTerminalHandler(Handler<V> handler, V value) {
    Handler<V> h;
    synchronized (this) {
      if (!stopped) {
        stopped = true;
        source.handler(null).exceptionHandler(null).endHandler(null);
        h = handler;
      } else {
        h = null;
      }
    }
    if (h != null) {
      h.handle(value);
    }
  }
}