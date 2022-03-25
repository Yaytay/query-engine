/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.procs;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * PassthroughStream makes available a WriteStream that calls an {@link AsyncHandler} for each data item written to it and optionally passes the data on to a ReadStream.
 * 
 * @param <T> The class of object being streamed.
 * @author jtalbut
 */
public class PassthroughStream<T> {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(PassthroughStream.class);
  
  private final AtomicBoolean writeStreamDataBeingProcessed = new AtomicBoolean();

  private final Object lock = new Object();
  private final ChainedAsyncHandler<T> processor;
  private final Context context;
  private final Write write;
  private final Read read;
  
  private Promise<Void> inflight;
  private Handler<Throwable> exceptionHandler;    
  private Handler<Void> drainHandler;
  private Handler<Void> endHandler;
  private Handler<T> readHandler;

  private T current;
  private long demand;
  
  /*
  This is the endhandler that will be called when the current in-flight item has been processed.
  */
  private Handler<Void> end;

  /**
   * Constructor.
   * 
   * @param handler The handler that will be called with each data item to be processed.
   * @param context The Vertx context of any processing that is carried out.
   */
  public PassthroughStream(ChainedAsyncHandler<T> handler, Context context) {
    this.processor = handler;
    this.context = context;
    this.write = new Write();
    this.read = new Read();
  }
  
  /**
   * Get the WriteStream interface for this PassthroughStream.
   * There is only ever a single instance of the WriteStream, this method does not create a new instance.
   * @return the WriteStream interface for this PassthroughStream.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Exposure of the stream is required")
  public WriteStream<T> writeStream() {
    return write;
  }
  
  /**
   * Get the ReadStream interface for this PassthroughStream.
   * There is only ever a single instance of the ReadStream, this method does not create a new instance.
   * @return the ReadStream interface for this PassthroughStream.
   */
  public ReadStream<T> readStream() {
    return read;
  }
  
  private void processCurrent(Promise<Void> promise, T data) {
    context.runOnContext(v -> {
      try {
        processor.handle(data, d -> read.handle(d))
                .onComplete(ar -> {
                  completeCurrent(promise, ar);
                })
                ;
      } catch(Throwable ex) {
        Handler<Throwable> handler = exceptionHandler;
        if (handler != null) {
          handler.handle(ex);
        }
        completeCurrent(promise, Future.failedFuture(ex));
      }
    });
  }

  private void completeCurrent(Promise<Void> promise, AsyncResult<Void> ar) {
    Handler<Void> dh;
    Handler<Void> eh;
    synchronized(lock) {
      dh = drainHandler;
      eh = end;
      current = null;
      inflight = null;
    }
    promise.handle(ar);
    if (dh != null) {
      dh.handle(null);
    }
    if (eh != null) {
      eh.handle(null);
    }
  }
  
  private class Write implements WriteStream<T> {

    @Override
    public WriteStream<T> exceptionHandler(Handler<Throwable> handler) {
      exceptionHandler = handler;
      return this;
    }

    @Override
    public Future<Void> write(T data) {
      Promise<Void> promise;
      boolean isPaused;
      synchronized(lock) {
        current = data;
        promise = Promise.promise();
        inflight = promise;
        isPaused = writeStreamDataBeingProcessed.get();
      }        
      if (!isPaused) {
        processCurrent(promise, data);
      }
      return promise.future();
    }

    @Override
    public void write(T data, Handler<AsyncResult<Void>> handler) {
      write(data).onComplete(handler);
    }

    @Override
    public void end(Handler<AsyncResult<Void>> handler) {
      boolean endNow;
      synchronized(lock) {
        end = v -> {
          Handler<Void> eh;
          synchronized(lock) {
            eh = endHandler;
          }
          if (eh != null) {
            eh.handle(null);
          }
          handler.handle(Future.succeededFuture());
        };
        endNow = inflight == null;
      }
      if (endNow) {
        end.handle(null);
      }
    }

    @Override
    public WriteStream<T> setWriteQueueMaxSize(int maxSize) {
      // This cannot do anything, there is no queue.
      throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean writeQueueFull() {
      synchronized(lock) {
        return writeStreamDataBeingProcessed.get() || (current != null);
      }
    }

    @Override
    public WriteStream<T> drainHandler(Handler<Void> handler) {
      boolean callNow = false;
      synchronized(lock) {
        drainHandler = handler;
        if (inflight == null || inflight.future().isComplete()) {
          callNow = true;
        }
      }
      if (callNow) {
        handler.handle(null);
      }
      return this;
    }
    
  }
  
  private class Read implements ReadStream<T> {

    private Handler<Throwable> exceptionHandler;
    
    Future<Void> handle(T data) {
      Future<Void> result;
      try {
        readHandler.handle(data);
        result = Future.succeededFuture();
      } catch(Throwable ex) {
        Handler<Throwable> handler = exceptionHandler;
        if (handler != null) {
          handler.handle(ex);
        }
        result = Future.failedFuture(ex);
      }
      synchronized (lock) {
        if (demand != Long.MAX_VALUE) {
          if (--demand == 0) {
            this.pause();
          }
        }
      }
      return result;
    }
    
    @Override
    public ReadStream<T> exceptionHandler(Handler<Throwable> handler) {
      this.exceptionHandler = handler;
      return this;
    }

    @Override
    public ReadStream<T> handler(Handler<T> handler) {
      readHandler = handler;
      return this;
    }

    @Override
    public ReadStream<T> pause() {
      writeStreamDataBeingProcessed.set(true);
      return this;
    }

    @Override
    public ReadStream<T> resume() {
      logger.debug("Resume");
      fetch(Long.MAX_VALUE);
      return this;
    }

    @Override
    public ReadStream<T> fetch(long amount) {
      if (amount < 0L) {
        throw new IllegalArgumentException();
      }
      writeStreamDataBeingProcessed.set(false);
      T curr = null;
      Promise<Void> promise;
      synchronized (lock) {
        demand += amount;
        if (demand < 0L) {
          demand = Long.MAX_VALUE;
        }
        curr = current;
        promise = inflight;
      }
      if (curr != null) {
        processCurrent(promise, curr);
      }
      return this;
    }

    @Override
    public ReadStream<T> endHandler(Handler<Void> handler) {
      endHandler = handler;
      return this;
    }
    
  }
  
}
