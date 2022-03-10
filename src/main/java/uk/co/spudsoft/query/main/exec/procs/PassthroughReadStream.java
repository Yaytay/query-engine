/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.procs;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.impl.InboundBuffer;

/**
 *
 * The PassthroughReadStream is intended to be a ReadStream implementation with a small buffer to be used in conjunction with the PassthroughStream.
 * 
 * @param <T> The class of object being streamed.
 * @author jtalbut
 */
class PassthroughReadStream<T> implements ReadStream<T> {

  private final InboundBuffer<T> pending;
  private Handler<Void> endHandler;

  /**
   * Constructor.
   * 
   * Note that the source is backed by an InboundBuffer that requires all writes to occur in the same vertx context.
   * 
   * @param context Vertx context.
   * @param maxQueueSize Maximum size of the queue.
   */
  PassthroughReadStream(Context context, int maxQueueSize) {
    this.pending = new InboundBuffer<>(context, maxQueueSize);
  }
  
  public Future<Void> handle(T data) {
    if (pending.write(data)) {
      return Future.succeededFuture();
    } else {
      Promise<Void> promise = Promise.promise();
      pending.drainHandler(v -> promise.complete());
      return promise.future();
    }
  }
  
  public Future<Void> end() {
    Handler<Void> handler = endHandler;
    if (handler != null) {
      if (pending.isEmpty()) {
        handler.handle(null);
        return Future.succeededFuture();
      } else {
        Promise<Void> promise = Promise.promise();
        pending.emptyHandler(v -> {
          handler.handle(null);
          promise.complete();
        });
        return promise.future();
      }
    }
    return Future.succeededFuture();
  }
  
  @Override
  public ReadStream<T> exceptionHandler(Handler<Throwable> handler) {
    this.pending.exceptionHandler(handler);
    return this;
  }

  @Override
  public ReadStream<T> handler(Handler<T> handler) {
    this.pending.handler(handler);
    return this;
  }

  @Override
  public ReadStream<T> pause() {
    this.pending.pause();
    return this;
  }

  @Override
  public ReadStream<T> resume() {
    this.pending.resume();
    return this;
  }

  @Override
  public ReadStream<T> fetch(long amount) {
    this.pending.fetch(amount);
    return this;
  }

  @Override
  public ReadStream<T> endHandler(Handler<Void> endHandler) {
    this.endHandler = endHandler;
    return this;
  }
  
}
