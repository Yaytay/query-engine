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
 * An abstract implementation of WriteStream with a queue size of 1.
 * 
 * The write method must always be called on the same thread and the writeHandler.
 * 
 * @param <T> The class of object being streamed.
 * @author jtalbut
 */
public class PassthroughWriteStream<T> {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(PassthroughWriteStream.class);

  public interface AsyncProcessor<T> {

    /**
     * Handle this data and complete the Future when done.
     * 
     * @param data The data to process.
     * @return A Future that will be completed (possibly with modified data) when the work is done.
     */
    Future<Void> handle(T data);
  }
    
  private final AtomicBoolean paused = new AtomicBoolean();

  private final Object lock = new Object();
  private final AsyncProcessor<T> processor;
  private final Context context;
  private final Write write;
  
  private Promise<Void> inflight;
  private Handler<Throwable> exceptionHandler;    
  private Handler<Void> drainHandler;
  private Handler<Void> endHandler;

  private T current;
  
  /*
  This is the endhandler that will be called when the current in-flight item has been processed.
  */
  private Handler<Void> end;

  public PassthroughWriteStream(AsyncProcessor<T> processor, Context context) {
    this.processor = processor;
    this.context = context;
    this.write = new Write();
  }
  
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Exposure of the stream is required")
  public WriteStream<T> writeStream() {
    return write;
  }
  
  private void processCurrent(Promise<Void> promise, T data) {
    context.runOnContext(v -> {
      try {        
        logger.debug("Calling {} with {}", processor, data);
        processor.handle(data)
                .onComplete(ar -> {
                  logger.debug("Calling {} with {} returned {}", processor, data, ar);
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
    promise.handle(ar);
    Handler<Void> handler = drainHandler;
    if (handler != null) {
      handler.handle(null);
    }
    Handler<Void> eh = end;
    if (eh != null) {
      eh.handle(null);
    }
  }
  
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
      if (!paused.get()) {
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
          logger.debug("end lambda");
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
        return paused.get() || (current != null);
      }
    }

    @Override
    public WriteStream<T> drainHandler(Handler<Void> handler) {
      drainHandler = handler;
      return this;
    }
    
  }
  
}
