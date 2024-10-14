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
import java.util.ArrayDeque;
import java.util.Deque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to make a number of items available as a {@link io.vertx.core.streams.ReadStream}.
 * <P>
 * An {@link java.util.ArrayDeque} is created in the constructor and used as a buffer for items as they are added.
 * <P>
 * The {@link java.util.Deque} itself is not exposed, the only permitted modification is the addition of items via the {@link #add(java.lang.Object)} method.
 * 
 * @param <T> The type of item being streamed.
 * @author jtalbut
 */
public class QueueReadStream<T> implements ReadStream<T> {
  
  private static final Logger logger = LoggerFactory.getLogger(QueueReadStream.class);
  
  private final Context context;
  private final Deque<T> items;
  
  private final Object lock = new Object();

  private Handler<Throwable> exceptionHandler;
  private Handler<T> handler;
  private Handler<Void> endHandler;

  private long demand;
  private boolean emitting;
  private boolean ended;
  private boolean completed;
  
  /**
   * Constructor.
   * @param context The Vert.x context to use for asynchronous method calls.
   */
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public QueueReadStream(Context context) {
    this.context = context;
    this.items = new ArrayDeque<>();
  }
  
  /**
   * Add an item to the queue of items to be sent (and emit it if appropriate).
   * @param item The item to add.
   * @return this, so that this method may be used in a fluent manner.
   */
  public QueueReadStream<T> add(T item) {
    if (completed) {
      throw new IllegalStateException("Last item has already been sent");
    }
    synchronized (lock) {
      items.add(item);
      if (emitting) {
        return this;
      }
      emitting = true;
    }
    context.runOnContext(v -> process());
    return this;
  }

  /**
   * Mark that no more items will be added.
   * @return this, so that this method may be used in a fluent manner.
   */
  public QueueReadStream<T> complete() {
    synchronized (lock) {
      this.completed = true;
      if (emitting) {
        return this;
      }
      emitting = true;
    }
    context.runOnContext(v -> process());
    return this;
  }
  
  private void process() {
    while (!ended && emitting) {
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
        if (!items.isEmpty()) {
          item =  items.pop();
          handlerCaptured = handler;
        } else {
          emitting = false;
          if (completed) {
            ended = true;
            endHandlerCaptured = endHandler;
          }
        }
      }
      if (item != null && handlerCaptured != null) {
        try {
          logger.debug("Handling {}", item);
          handlerCaptured.handle(item);
        } catch (Throwable ex) {
          if (exceptionHandlerCaptured != null) {
            exceptionHandlerCaptured.handle(ex);
          } else {
            logger.warn("Exception handling item in QueueReadStream: ", ex);
          }
        }
      }
      if (ended && endHandlerCaptured != null) {
        logger.debug("Calling endHandler"); 
        endHandlerCaptured.handle(null);
        return ;
      }
    }
  }
  
  @Override
  public QueueReadStream<T> exceptionHandler(Handler<Throwable> handler) {
    synchronized (lock) {
      this.exceptionHandler = handler;
    }
    return this;
  }

  @Override
  public QueueReadStream<T> handler(Handler<T> handler) {
    synchronized (lock) {
      this.handler = handler;
    }
    return this;
  }

  @Override
  public QueueReadStream<T> endHandler(Handler<Void> endHandler) {
    synchronized (lock) {
      this.endHandler = endHandler;
    }
    return this;
  }
  
  @Override
  public QueueReadStream<T> pause() {
    synchronized (lock) {
      demand = 0;
    }
    return this;
  }

  @Override
  public QueueReadStream<T> resume() {
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
  public QueueReadStream<T> fetch(long amount) {
    if (amount < 0L) {
      throw new IllegalArgumentException("Negative fetch amount");
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
