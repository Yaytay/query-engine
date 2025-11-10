/*
 * Copyright (C) 2025 jtalbut
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
package uk.co.spudsoft.query.web;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

/**
 * An implementation of {@link WriteStream} that bridges Vert.x {@link Context}s.
 * <p>
 * Every request that can write to the delegate WriteStream is run using runOnContext, and if there is a return value 
 * it is returned by another runOnContext.
 * 
 * @author njt
 */
public class BufferingContextAwareWriteStream implements WriteStream<Buffer> {
  private final WriteStream<Buffer> delegate;
  private final Context context;
  private final int flushThreshold;
  private Buffer buffer = Buffer.buffer();

  /**
   * Constructor.
   * @param delegate The target WriteStream (typically RoutingContext.response().
   * @param context The context of the delegate, typically the HttpServer context.
   * @param flushThreshold The number of bytes to buffer before transferring them to the HttpServer context.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The aim is to correct marshall modifications of the WriteStream")
  public BufferingContextAwareWriteStream(WriteStream<Buffer> delegate, Context context, int flushThreshold) {
    this.delegate = delegate;
    this.context = context;
    this.flushThreshold = flushThreshold;
  }
  
  @Override
  public Future<Void> write(Buffer data) {
    buffer.appendBuffer(data);
    if (buffer.length() >= flushThreshold) {
      return flush();
    }
    return Future.succeededFuture();
  }

  private Future<Void> flush() {
    if (buffer.length() == 0) {
      return Future.succeededFuture();
    }
    Buffer toWrite = buffer;
    buffer = Buffer.buffer();
    Promise<Void> promise = Promise.promise();
    Context thisContext = Vertx.currentContext();
    context.runOnContext(v -> {
      delegate.write(toWrite)
              .onComplete(ar -> {
                thisContext.runOnContext(v2 -> {
                  if (ar.succeeded()) {
                    promise.complete(ar.result());
                  } else {
                    promise.fail(ar.cause());
                  }
                });
              });
    });
    return promise.future();
  }

  @Override
  public Future<Void> end() {
    return flush()
            .compose(v -> {
              Promise<Void> promise = Promise.promise();
              Context thisContext = Vertx.currentContext();
              context.runOnContext(v2 -> {
                delegate.end()
                        .onComplete(ar -> {
                          thisContext.runOnContext(v3 -> {
                            if (ar.succeeded()) {
                              promise.complete(ar.result());
                            } else {
                              promise.fail(ar.cause());
                            }
                          });
                        });
              });
              return promise.future();
            });
  }

  @Override
  public Future<Void> end(Buffer data) {
    buffer.appendBuffer(data);
    return end();
  }

  @Override
  public WriteStream<Buffer> setWriteQueueMaxSize(int maxSize) {
    context.runOnContext(v -> delegate.setWriteQueueMaxSize(maxSize));
    return this;
  }

  @Override
  public boolean writeQueueFull() {
    // Safe to call directly; doesn't mutate state
    return delegate.writeQueueFull();
  }

  @Override
  public WriteStream<Buffer> drainHandler(Handler<Void> handler) {
    context.runOnContext(v -> delegate.drainHandler(handler));
    return this;
  }

  @Override
  public WriteStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
    context.runOnContext(v -> delegate.exceptionHandler(handler));
    return this;
  }
}
