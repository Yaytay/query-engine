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
package uk.co.spudsoft.query.exec.procs.sort;

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jtalbut
 */
public class RandomIntegerReadStream implements ReadStream<Integer> {

  private static final Logger logger = LoggerFactory.getLogger(RandomIntegerReadStream.class);
  
  private final Context context;
  private final int limit;
  
  private final Object lock = new Object();
  
  private Handler<Integer> handler;
  private Handler<Void> endHandler;
  private Handler<Throwable> exceptionHandler;
  
  private int count;
  
  private boolean emitting = false;
  private boolean ended = false;
  private long demand = 0;

  private final Random rand = new Random();
  
  public RandomIntegerReadStream(Context context, int limit) {
    this.context = context;
    this.limit = limit;
  }

  @Override
  public ReadStream<Integer> exceptionHandler(Handler<Throwable> handler) {
    this.exceptionHandler = handler;
    return this;
  }

  @Override
  public ReadStream<Integer> handler(Handler<Integer> handler) {
    this.handler = handler;
    return this;
  }

  @Override
  public ReadStream<Integer> endHandler(Handler<Void> endHandler) {
    this.endHandler = endHandler;
    return this;
  }
  
  private void processOutput() {
    while (!ended) {
      Handler<Throwable> exceptionHandlerCaptured;
      Handler<Void> endHandlerCaptured = null;
      Handler<Integer> handlerCaptured = null;
      Integer item = null;
      synchronized (lock) {
        if (demand <= 0) {
          emitting = false;
          return ;
        } else if (demand < Long.MAX_VALUE) {
          --demand;
        }
        exceptionHandlerCaptured = exceptionHandler;
        if (count < limit) {
          ++count;
          item = rand.nextInt(0, 2000000);
          handlerCaptured = handler;
        } else {
          emitting = false;
          ended = true;
          endHandlerCaptured = endHandler;
        }
      }
      if (item != null && handlerCaptured != null) {
        try {
          handlerCaptured.handle(item);
        } catch (Throwable ex) {
          if (exceptionHandlerCaptured != null) {
            exceptionHandlerCaptured.handle(ex);
          } else {
            logger.warn("Exception handling item in RandomIntegerReadStream: ", ex);
          }
        }
      }
      if (ended && endHandlerCaptured != null) {
        endHandlerCaptured.handle(null);
        return ;
      }
    }
  }

  @Override
  public ReadStream<Integer> pause() {
    synchronized (lock) {
      demand = 0;
    }
    return this;
  }

  @Override
  public ReadStream<Integer> resume() {
    synchronized (lock) {
      demand = Long.MAX_VALUE;
      if (emitting) {
        return this;
      }
      emitting = true;
    }
    context.runOnContext(v -> {
      processOutput();
    });
    return this;
  }

  @Override
  public ReadStream<Integer> fetch(long amount) {
    if (amount < 0L) {
      throw new IllegalArgumentException();
    }
    synchronized (lock) {
      demand += amount;
      if (demand < 0L) {
        demand = Long.MAX_VALUE;
      }
      if (emitting) {
        return this;
      }
      emitting = true;
    }
    context.runOnContext(v -> {
      processOutput();
    });
    return this;
  }
  
  
  
}
