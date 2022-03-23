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
import io.vertx.core.streams.WriteStream;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * PassthroughWriteStream makes available a WriteStream that calls an {@link AsyncHandler} for each data item written to it.
 * 
 * @param <T> The class of object being streamed.
 * @author jtalbut
 */
public class PassthroughWriteStream<T> {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(PassthroughWriteStream.class);
    
  private final AtomicBoolean writeStreamDataBeingProcessed = new AtomicBoolean();

  private final Object lock = new Object();
  private final AsyncHandler<T> processor;
  private final Context context;
  private final Write write;
  
  private Promise<Void> inflight;
  private Handler<Throwable> exceptionHandler;    
  private Handler<Void> drainHandler;
  
  /**
   * This is the endHandler set to be called after the {@link io.vertx.core.streams.WriteStream<T>#end(io.vertx.core.Handler)} 
   * has been called and any inflight data items have been processed.
   */
  private Handler<Void> endHandler;

  private T current;
  
  /**
   * This is the endhandler that will be called when the current in-flight item has been processed (if not null).
   */
  private Handler<Void> end;

  /**
   * Constructor.
   * @param handler Handler that will be called for each data item the WriteStream receives.
   * @param context The Vertx context of any processing that is carried out.
   */
  public PassthroughWriteStream(AsyncHandler<T> handler, Context context) {
    this.processor = handler;
    this.context = context;
    this.write = new Write();
  }
  
  /**
   * Get the WriteStream interface for this PassthroughWriteStream.
   * There is only ever a single instance of the WriteStream, this method does not create a new instance.
   * @return the WriteStream interface for this PassthroughWriteStream.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Exposure of the stream is required")
  public WriteStream<T> writeStream() {
    return write;
  }
  
  private void processCurrent(Promise<Void> promise, T data) {
    context.runOnContext(v -> {
      try {        
        processor.handle(data)
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
  
  /**
   * Set an end handler. 
   * Once the stream has ended, and there is no more data to be read, this handler will be called.
   *
   * @param handler the handler to be called when the stream has ended, and there is no more data to be read.
   * @return a reference to this, so the API can be used fluently
   */
  public PassthroughWriteStream<T> endHandler(Handler<Void> handler) {
    synchronized(lock) {
      this.endHandler = handler;
    }
    return this;
  }
  
  private class Write implements WriteStream<T> {

    @Override
    public WriteStream<T> exceptionHandler(Handler<Throwable> handler) {
      exceptionHandler = handler;
      return this;
    }

    @Override
    public Future<Void> write(T data) {
      synchronized(lock) {
        current = data;
      }
      inflight = Promise.promise();
      if (!writeStreamDataBeingProcessed.get()) {
        processCurrent(inflight, data);
      }
      return inflight.future();
    }

    @Override
    public void write(T data, Handler<AsyncResult<Void>> handler) {
      write(data).onComplete(handler);
    }

    @Override
    public void end(Handler<AsyncResult<Void>> handler) {     
      logger.debug("end({}) called ({})", handler, this.hashCode());
      Handler<Void> handlerToCallNow = null;
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
        if (inflight == null) {
          handlerToCallNow = end;
        } else {
          inflight.future().onComplete(v -> end.handle(null));
        }
      }
      if (handlerToCallNow != null) {
        handlerToCallNow.handle(null);
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
      drainHandler = handler;
      return this;
    }
    
  }
  
}
