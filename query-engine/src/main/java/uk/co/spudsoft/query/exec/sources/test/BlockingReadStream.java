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
package uk.co.spudsoft.query.exec.sources.test;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;
import io.vertx.sqlclient.desc.ColumnDescriptor;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import uk.co.spudsoft.query.exec.DataRowStream;
import uk.co.spudsoft.query.exec.Types;

/**
 * The BlockingReadStream implements QueryProcessor (which is just ReadStream&lt;T>) using a BlockingQueue.This is designed specifically for SQL queries that feed data as a (non-reactive) collector.
 * 
 * This class is based very closely on the {@link io.vertx.core.streams.impl.InboundBuffer} but is blocking on input if the queue is full.
 * 
 * The (potentially blocking) add method will be called in a Vert.x event thread by a SqlClient collector.
 * This is bad, so try to size the queue and the subsequent pipeline to ensure it doesn't happen.
 * 
 * The handler will be called either on the same thread that called add or on a thread in the context passed in to the constructor.
 * 
 * @author jtalbut
 * @param <T> The type of item processed by the ReadStream (usually JsonObject).
 */
public final class BlockingReadStream<T> implements DataRowStream<T> {

  private final Types types;
  private final BlockingQueue<T> queue;

  /**
   * Handler called when the stream is complete.
   */
  private Handler<Void> endHandler;
  
  /**
   * Handler called whenever the requested items have been feed to the handler.
   */
  private Handler<Void> drainHandler;
  
  /**
   * Handler for processing each individual item.
   */
  private Handler<T> itemHandler;
  
  /**
   * Handler called for any exceptions encountered during processing.
   */
  private Handler<Throwable> exceptionHandler;
  
  /**
   * The current number of items requested by called to {@link #fetch(long)}.
   * If demand is {@link Long#MAX_VALUE} it will not be decremented.
   * If demand is 0 the queue is paused, the item handler will not be called and all added items will be put on the queue.
   */
  private long demand;
  
  /**
   * When true the queue is being actively drained (so new items get added to the queue).
   */
  private boolean emitting;
  
  /**
   * When true the queue has been ended and adding any further items will cause an IllegalStateException.
   * As soon as the queue is empty after ended is set to true the endHandler will be called.
   */
  private boolean ended;
  
  /**
   * When true the read stream has been closed and no further callbacks will be called.
   */
  private boolean closed;

  /**
   * Runnable for draining the queue.
   */
  private final Handler<Handler<Void>> runOnContext;
  
  /**
   * Constructor.
   * 
   * @param context The vertx context upon which the handler will be called when processing a backlog of items.
   * @param queueSize The maximum size of the queue of items to be processed.
   * @param types The types of the columns in the stream.
   */
  public BlockingReadStream(Context context, int queueSize, Types types) {
    if (queueSize <= 0) {
      throw new IllegalArgumentException("queueSize (" + queueSize + ") must be >= 1");
    }
    this.queue = new ArrayBlockingQueue<>(queueSize);
    this.demand = Long.MAX_VALUE;
    // Capture the context runner rather that storing the context itself to avoid EI_EXPOSE_REP2.
    this.runOnContext = h -> context.runOnContext(h);
    this.types = types;
  }
  
  /**
   * Set the drain handler.
   * The drain handler is called whenever the queue has finished draining (i.e. the queue is empty).
   * There is no explicit watermark level required for the queue because the queue will block when it is too full.
   * @param handler the drain handler.
   * @return this.
   */
  public ReadStream<T> drainHandler(Handler<Void> handler) {
    synchronized (this) {
      this.drainHandler = handler;
    }
    return this;
  }
  
  /**
   * Set the exception handler.
   * The exception handler is called for any exceptions that happen during processing.
   * @param handler the exception handler.
   * @return this.
   */
  @Override
  public DataRowStream<T> exceptionHandler(Handler<Throwable> handler) {
    synchronized (this) {
      this.exceptionHandler = handler;
    }
    return this;
  }

  /**
   * Set the item handler.
   * The item handler is called for each item added.
   * If the queue is empty the item handler will be called on the same thread that called {@link #add(io.vertx.core.json.T)}, 
   * otherwise it will be called on a context thread.
   * @param handler the item handler.
   * @return this.
   */
  @Override
  public DataRowStream<T> handler(Handler<T> handler) {
    synchronized (this) {
      this.itemHandler = handler;
    }
    return this;
  }
  
  /**
   * Set the end handler.
   * The end handler is called when end() is called.
   * @param handler the end handler.
   * @return this.
   */
  @Override
  public DataRowStream<T> endHandler(Handler<Void> handler) {
    synchronized (this) {
      this.endHandler = handler;
    }
    return this;
  }
  

  /**
   * Return the number of items currently in the queue.
   * 
   * This method is intended solely for test purposes.
   * 
   * @return the number of items currently in the queue.
   */
  int size() {
    synchronized (this) {
      return queue.size();
    }
  }
  
  /**
   * Add an item onto the queue, or, if the queue is currently empty, pass the item directly to the handler.
   * 
   * If this method throws an InterruptedException the item will not have been added and will be lost if the caller does not handle this correctly.
   * 
   * @param item The item to add to the stream.
   * @return The approximate current count of items queued for processing, this intended as a hint to the caller to slow things down.
   * @throws InterruptedException if the thread is interrupted whilst waiting for the queue to have space.
   * @throws IllegalStateException if the stream has already been ended.
   */
  public int add(T item) throws InterruptedException {
    Handler<T> handler = null;
    boolean put = false;
    synchronized (this) {
      if (ended) {
        throw new IllegalStateException("Stream has already ended");
      }
      if (demand == 0L || emitting) {
        put = true;
      } else {
        if (demand != Long.MAX_VALUE) {
          --demand;
        }
        emitting = true;
        handler = this.itemHandler;
      }
    }
    if (put) {
      queue.put(item);
      return queue.size();
    } else {
      drainQueue();
      handleEvent(handler, item);
      return 0;
    }
  }
  
  /**
   * Drain the queue by calling the handler for each item in it until either the queue is empty or the demand has been satisfied.
   * @return The approximate current count of items queued for processing, this intended as a hint to the caller to slow things down.
   */
  private int drainQueue() {
    Handler<Void> configuredDrainHandler = null;
    Handler<Void> configuredEndHandler = null;
    int size;    
    while (true) {
      T element;
      Handler<T> h;
      synchronized (this) {
        size = queue.size();
        if (size == 0 && ended) {
          configuredEndHandler = endHandler;
        }
        if (demand == 0L) {
          emitting = false;
          configuredDrainHandler = drainHandler;
          break;
        } else if (size == 0) {
          emitting = false;
          break;
        }
        if (demand != Long.MAX_VALUE) {
          demand--;
        }
        element = queue.poll();
        h = this.itemHandler;
      }
      handleEvent(h, element);
    }
    if (configuredDrainHandler != null) {
      drained(configuredDrainHandler);
    }
    if (configuredEndHandler != null) {
      ended(configuredEndHandler);
    }
    return size;
  }
  
  /**
   * Mark the end of the stream, by calling the end handler after the queue has been emptied.
   */
  public void end() {
    synchronized (this) {
      ended = true;
    }
    drainQueue();
  }
  
  private void ended(Handler<Void> handler) {
    try {
      handler.handle(null);
    } catch (Throwable ex) {
      handleException(ex);
    }
  }

  private void handleException(Throwable ex) {
    Handler<Throwable> handler;
    synchronized (this) {
      handler = exceptionHandler;
    }
    if (handler != null) {
      handler.handle(ex);
    }
  }

  private void handleEvent(Handler<T> handler, T element) {
    if (handler != null && !closed) {
      try {
        handler.handle(element);
      } catch (Throwable ex) {
        handleException(ex);
      }
    }
  }

  private void drained(Handler<Void> handler) {
    try {
      handler.handle(null);
    } catch (Throwable ex) {
      handleException(ex);
    }
  }  

  @Override
  public DataRowStream<T> pause() {
    synchronized (this) {
      demand = 0L;
    }
    return this;
  }

  @Override
  public DataRowStream<T> resume() {
    return fetch(Long.MAX_VALUE);
  }

  @Override
  public DataRowStream<T> fetch(long amount) {
    if (amount < 0L) {
      throw new IllegalArgumentException();
    }
    synchronized (this) {
      demand += amount;
      if (demand < 0L) {
        demand = Long.MAX_VALUE;
      }
      if (!queue.isEmpty()) {
        emitting = true;
        runOnContext.handle(v -> drainQueue());
      }
    }
    return this;
  }

  @Override
  public List<ColumnDescriptor> getColumnDescriptors() {
    return types.getColumnDescriptors();
  }

  @Override
  public Future<Void> close() {
    synchronized (this) {
      closed = true;
    }
    return Future.succeededFuture();
  }

  @Override
  public void close(Handler<AsyncResult<Void>> completionHandler) {
    close().andThen(completionHandler);
  }
  
}
