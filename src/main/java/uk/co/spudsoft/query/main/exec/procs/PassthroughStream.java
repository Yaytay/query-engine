/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.procs;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;

/**
 * An abstract implementation of WriteStream with a queue size of 1.
 * 
 * The write method must always be called on the same thread and the writeHandler.
 * 
 * @param <T> The class of object being streamed.
 * @author jtalbut
 */
public class PassthroughStream<T> implements WriteStream<T> {

  public interface AsyncProcessor<T> {

    /**
     * Handle this data and complete the Future when done.
     * 
     * @param data The data to process.
     * @return A Future that will be completed (possibly with modified data) when the work is done.
     */
    Future<T> handle(T data);

  }
  
  private Handler<Throwable> exceptionHandler;
  private Handler<Void> drainHandler;
  private Future<Void> inFlight;

  private final AsyncProcessor<T> processor;
  private final PassthroughReadStream<T> readStream;  
  
  public PassthroughStream(AsyncProcessor<T> processor, Context context, int maxQueueSize) {
    this.processor = processor;
    this.readStream = new PassthroughReadStream<>(context, maxQueueSize);
  }

  public ReadStream<T> getReadStream() {
    return readStream;
  }
  
  @Override
  public WriteStream<T> exceptionHandler(Handler<Throwable> handler) {
    this.exceptionHandler = handler;
    return this;
  }

  @Override
  public void write(T data, Handler<AsyncResult<Void>> handler) {
    write(data).onComplete(handler);
  }

  @Override
  public Future<Void> write(T data) {
    Future<Void> result = processor.handle(data)
            .onFailure(ex -> {
              Handler<Throwable> handler = exceptionHandler;
              if (handler != null) {
                handler.handle(ex);
              }
            })
            .compose(newData -> {
              if (newData != null) {
                return readStream.handle(newData);
              } else {
                return Future.succeededFuture();
              }
            })
            ;
    inFlight = result;
    result.onComplete(v -> {
              inFlight = null;
              if (drainHandler != null) {
                drainHandler.handle(null);
              }
            })
            ;
    return result;
  }
  
  @Override
  public void end(Handler<AsyncResult<Void>> handler) {
    Future<Void> result = inFlight;
    if (result == null) {
      readStream.end().onComplete(handler);
    } else {
      drainHandler(v -> {
        readStream.end().onComplete(handler);
      });
    }
  }

  @Override
  public WriteStream<T> setWriteQueueMaxSize(int maxSize) {
    // Noop, the maxSize is 1.
    return this;
  }

  @Override
  public boolean writeQueueFull() {    
    Future<Void> result = inFlight;
    return (result != null) && (!result.isComplete());
  }

  @Override
  public WriteStream<T> drainHandler(Handler<Void> handler) {
    this.drainHandler = handler;
    return this;
  }
    
}
