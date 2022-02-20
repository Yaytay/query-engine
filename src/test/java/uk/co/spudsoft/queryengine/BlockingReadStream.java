/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.queryengine;

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 *
 * @author jtalbut
 */
public class BlockingReadStream<T> implements ReadStream<T> {
  
  private final BlockingQueue<T> queue;
  private final Context context;

  private Handler<Void> endHandler;
  private Handler<T> itemHandler;
  private Handler<Throwable> exceptionHandler;
  private long demand;
  private boolean emitting;
  
  public BlockingReadStream(Context context, int queueSize) {
    this.queue = new ArrayBlockingQueue<>(queueSize);
    this.context = context;
    this.emitting = true;
  }
  
  public void put(T value) throws InterruptedException {
    queue.put(value);
    checkPending();
  }
  
  public void end() {
    endHandler.handle(null);
  }
  
  private void checkPending() {
    context.executeBlocking(p -> {
      while (emitting) {
        T value = queue.poll();
        if (value != null) {
          try {
            itemHandler.handle(value);
          } catch(Throwable ex) {
            exceptionHandler.handle(ex);
          }
        } else {
          break ;
        }
      }
    });
  }

  @Override
  public ReadStream<T> exceptionHandler(Handler<Throwable> handler) {
    this.exceptionHandler = handler;
    return this;
  }

  @Override
  public ReadStream<T> handler(Handler<T> handler) {
    this.itemHandler = handler;
    return this;
  }

  @Override
  public ReadStream<T> pause() {
    emitting = false;
    return this;
  }

  @Override
  public ReadStream<T> resume() {
    emitting = true;
    checkPending();
    return this;
  }

  @Override
  public ReadStream<T> fetch(long amount) {
    return this;
  }

  @Override
  public ReadStream<T> endHandler(Handler<Void> endHandler) {
    this.endHandler = endHandler;
    return this;
  }
  
  
   
    
  
}
