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
package uk.co.spudsoft.query.exec.procs;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @param <T> The type of item being streamed.
 * @author jtalbut
 */
public class ListReadStream<T> implements ReadStream<T> {
  
  private static final Logger logger = LoggerFactory.getLogger(ListReadStream.class);
  
  private final Context context;
  private final Iterator<T> iter;
  
  private final Object lock = new Object();

  private Handler<Throwable> exceptionHandler;
  private Handler<T> handler;
  private Handler<Void> endHandler;

  private long demand;
  private boolean emitting;
  private boolean ended;
  
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public ListReadStream(Context context, List<T> items) {
    this.context = context;
    this.iter = items.iterator();
  }
  
  private void process() {
    while (!ended) {
      Handler<Throwable> exceptionHandlerCaptured;
      Handler<Void> endHandlerCaptured = null;
      Handler<T> handlerCaptured = null;
      T item = null;
      synchronized (lock) {
        if (demand <= 0) {
          emitting = false;
          return ;
        } else if (demand < Long.MAX_VALUE) {
          --demand;
        }
        exceptionHandlerCaptured = exceptionHandler;
        if (iter.hasNext()) {
          item =  iter.next();
          handlerCaptured = handler;
        } else {
          emitting = false;
          ended = true;
          endHandlerCaptured = endHandler;
        }
      }
      if (item != null && handlerCaptured != null) {
        try {
          logger.trace("Handling {}", item);
          handlerCaptured.handle(item);
        } catch (Throwable ex) {
          if (exceptionHandlerCaptured != null) {
            exceptionHandlerCaptured.handle(ex);
          } else {
            logger.warn("Exception handling item in ListReadStream: ", ex);
          }
        }
      }
      if (ended && endHandlerCaptured != null) {
        logger.trace("Ending");
        endHandlerCaptured.handle(null);
        return ;
      }
    }
  }
  
  @Override
  public ListReadStream<T> exceptionHandler(Handler<Throwable> handler) {
    synchronized (lock) {
      this.exceptionHandler = handler;
    }
    return this;
  }

  @Override
  public ListReadStream<T> handler(Handler<T> handler) {
    synchronized (lock) {
      this.handler = handler;
    }
    return this;
  }

  @Override
  public ListReadStream<T> endHandler(Handler<Void> endHandler) {
    synchronized (lock) {
      this.endHandler = endHandler;
    }
    return this;
  }
  
  @Override
  public ListReadStream<T> pause() {
    synchronized (lock) {
      demand = 0;
    }
    return this;
  }

  @Override
  public ListReadStream<T> resume() {
    synchronized (lock) {
      demand = Long.MAX_VALUE;
      if (emitting) {
        return this;
      }
      emitting = true;
    }
    context.runOnContext(v -> process());
    return this;
  }

  @Override
  public ListReadStream<T> fetch(long amount) {
    if (amount < 0L) {
      throw new IllegalArgumentException();
    }
    synchronized (lock) {
      demand += amount;
      if (demand < 0L) {
        demand = Long.MAX_VALUE;
      }
      if (emitting) {
        return this;
      }
      emitting = true;
    }
    context.runOnContext(v -> process());
    return this;
  }

}
