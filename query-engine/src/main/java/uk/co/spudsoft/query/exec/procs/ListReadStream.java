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
 * Helper class to make a {@link java.util.List} available as a {@link io.vertx.core.streams.ReadStream}.
 * <P>
 * A single iterator from the List will be taken in the constructor - anything that invalidates that iterator will break this implementation.
 * This class is not expected to handle changes to the List whilst it is running.
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

  /**
   * Constructor.
   * @param context the Vert.x {@link Context} for asynchronous operations.
   * @param items the items to send on the {@link ReadStream}.
   */
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
      boolean shouldContinue = false;

      synchronized (lock) {
        if (demand <= 0) {
          emitting = false;
          return;
        } else if (demand < Long.MAX_VALUE) {
          --demand;
        }
        exceptionHandlerCaptured = exceptionHandler;
        if (iter.hasNext()) {
          item = iter.next();
          handlerCaptured = handler;
          shouldContinue = true;
        } 
        if (!iter.hasNext()) {
          ended = true;
          endHandlerCaptured = endHandler;
          emitting = false; // Safe to set false here since we're ending
        }
      }

      callHandler(item, handlerCaptured, exceptionHandlerCaptured);

      if (ended && endHandlerCaptured != null) {
        logger.debug("Ending");
        endHandlerCaptured.handle(null);
        return;
      }

      // Check if we should continue or if another process() call has taken over
      synchronized (lock) {
        if (!shouldContinue || demand <= 0) {
          emitting = false;
          return;
        }
        // Continue the loop while holding the lock on emitting
      }
    }
  }

  /**
   * Call the handler with the item, and call the exception handler if that fails.
   * 
   * This is protected to allow for test classes to inject exceptions.
   * 
   * @param item The item to pass to the handler.
   * @param handler The handler.
   * @param exceptionHandler The handler to call with any exception if handler.handle fails.
   */
  protected void callHandler(T item, Handler<T> handler, Handler<Throwable> exceptionHandler) {
    if (item != null && handler != null) {
      try {
        logger.trace("Handling {}", item);
        handler.handle(item);
      } catch (Throwable ex) {
        if (exceptionHandler != null) {
          exceptionHandler.handle(ex);
        } else {
          logger.warn("Exception handling item in ListReadStream: ", ex);
        }
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
  public ListReadStream<T> fetch(long amount) {
    if (amount < 0L) {
      throw new IllegalArgumentException();
    }
    boolean shouldProcess = false;
    synchronized (lock) {
      if (ended) {
        return this;
      }
      demand += amount;
      if (demand < 0L) {
        demand = Long.MAX_VALUE;
      }
      if (!emitting) {
        emitting = true;
        shouldProcess = true;
      }
    }
    if (shouldProcess) {
      context.runOnContext(v -> process());
    }
    return this;
  }

  @Override
  public ListReadStream<T> resume() {
    boolean shouldProcess = false;
    synchronized (lock) {
      if (ended) {
        return this;
      }
      demand = Long.MAX_VALUE;
      if (!emitting) {
        emitting = true;
        shouldProcess = true;
      }
    }
    if (shouldProcess) {
      context.runOnContext(v -> process());
    }
    return this;
  }
}
