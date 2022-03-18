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
 * An abstract implementation of WriteStream with a queue size of 1.
 * 
 * The write method must always be called on the same thread and the writeHandler.
 * 
 * @param <T> The class of object being streamed.
 * @author jtalbut
 */
public class PassthroughStream<T> {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(PassthroughStream.class);

  public interface AsyncProcessor<T> {

    /**
     * Handle this data and complete the Future when done.
     * 
     * @param data The data to process.
     * @return A Future that will be completed (possibly with modified data) when the work is done.
     */
    Future<Void> handle(T data);
  }
  
  public interface PassthroughProcessor<T> {

    /**
     * Handle this data and complete the Future when done.
     * 
     * The implementation should either return a newly complected future (if the data is not to be passed on the pipeline) 
     * or it should return the future returned by {@link AsyncProcessor#handle(java.lang.Object)} to pass the data on.
     * Typically this means that the implementations has a structure like: 
     * <pre>
     * Future&lt;Void> handle(T data, AsyncProcessor&lt;T> chain) {
     *   return methodThatDoesStuffAsynchronousely(data)
     *     .compose(data -> chain.handle(data));
     * }
     * </pre>
     * 
     * @param data The data to process.
     * @param chain The processor that should be called to pass on the data.
     * @return A Future that will be completed (possibly with modified data) when the work is done.
     */
    Future<Void> handle(T data, AsyncProcessor<T> chain);
  }
  
  private final AtomicBoolean paused = new AtomicBoolean();

  private final Object lock = new Object();
  private final PassthroughProcessor<T> processor;
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

  public PassthroughStream(PassthroughProcessor<T> processor, Context context) {
    this.processor = processor;
    this.context = context;
    this.write = new Write();
    this.read = new Read();
  }
  
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Exposure of the stream is required")
  public WriteStream<T> writeStream() {
    return write;
  }
  
  public ReadStream<T> readStream() {
    return read;
  }
  
  private void processCurrent(Promise<Void> promise, T data) {
    context.runOnContext(v -> {
      try {        
        logger.debug("Calling {} with {}", processor, data);
        processor.handle(data, d -> read.handle(d))
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
      end = v -> {
        Handler<Void> eh = endHandler;
        if (eh != null) {
          eh.handle(null);
        }
      };
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
      paused.set(true);
      return this;
    }

    @Override
    public ReadStream<T> resume() {
      fetch(Long.MAX_VALUE);
      return this;
    }

    @Override
    public ReadStream<T> fetch(long amount) {
      if (amount < 0L) {
        throw new IllegalArgumentException();
      }
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
      if (current != null) {
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
