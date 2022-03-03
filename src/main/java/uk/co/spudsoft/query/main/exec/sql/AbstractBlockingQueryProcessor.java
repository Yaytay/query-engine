/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.sql;

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import uk.co.spudsoft.query.main.exec.QueryProcessor;

/**
 * The AbstractBlockingQueryProcessor implements QueryProcessor (which is just ReadStream&lt;JsonObject>) using a BlockingQueue.
 * 
 * This is designed specifically for SQL queries that feed data as a (non-reactive) collector.
 * This class is based very closely on the {@link io.vertx.core.streams.impl.InboundBuffer} but is blocking on input if the queue is full.
 * 
 * The (potentially blocking) add method will be called in a Vert.x event thread by a SqlClient collector.
 * This is bad, so try to size the queue and the subsequent pipeline to ensure it doesn't happen.
 * 
 * The handler will be called either on the same thread that called add or on a thread in the context passed in to the constructor.
 * 
 * @author jtalbut
 */
public class AbstractBlockingQueryProcessor implements QueryProcessor {

  private final BlockingQueue<JsonObject> queue;

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
  private Handler<JsonObject> itemHandler;
  
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
   * Runnable for draining the queue.
   */
  private final Handler<Handler<Void>> runOnContext;
  
  /**
   * Constructor.
   * 
   * @param context The vertx context upon which the handler will be called when processing a backlog of items.
   * @param queueSize The maximum size of the queue of items to be processed.
   */
  public AbstractBlockingQueryProcessor(Context context, int queueSize) {
    if (queueSize <= 0) {
      throw new IllegalArgumentException("queueSize (" + queueSize + ") must be >= 1");
    }
    this.queue = new ArrayBlockingQueue<>(queueSize);
    this.demand = Long.MAX_VALUE;
    // Capture the context runner rather that storing the context itself to avoid EI_EXPOSE_REP2.
    this.runOnContext = h -> context.runOnContext(h);
  }
  
  /**
   * Set the drain handler.
   * The drain handler is called whenever the queue has finished draining (i.e. the queue is empty).
   * There is no explicit watermark level required for the queue because the queue will block when it is too full.
   * @param handler the drain handler.
   * @return this.
   */
  public ReadStream<JsonObject> drainHandler(Handler<Void> handler) {
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
  public ReadStream<JsonObject> exceptionHandler(Handler<Throwable> handler) {
    synchronized (this) {
      this.exceptionHandler = handler;
    }
    return this;
  }

  /**
   * Set the item handler.
   * The item handler is called for each item added.
   * If the queue is empty the item handler will be called on the same thread that called {@link #add(io.vertx.core.json.JsonObject)}, 
   * otherwise it will be called on a context thread.
   * @param handler the item handler.
   * @return this.
   */
  @Override
  public ReadStream<JsonObject> handler(Handler<JsonObject> handler) {
    synchronized(this) {
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
  public ReadStream<JsonObject> endHandler(Handler<Void> handler) {
    synchronized(this) {
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
    synchronized(this) {
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
  public int add(JsonObject item) throws InterruptedException {
    Handler<JsonObject> handler = null;
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
      JsonObject element;
      Handler<JsonObject> h;
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
    } catch(Throwable ex) {
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

  private void handleEvent(Handler<JsonObject> handler, JsonObject element) {
    if (handler != null) {
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
  public ReadStream<JsonObject> pause() {
    synchronized (this) {
      demand = 0L;
    }
    return this;
  }

  @Override
  public ReadStream<JsonObject> resume() {
    return fetch(Long.MAX_VALUE);
  }

  @Override
  public ReadStream<JsonObject> fetch(long amount) {
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
  
  
   
    
  
}
