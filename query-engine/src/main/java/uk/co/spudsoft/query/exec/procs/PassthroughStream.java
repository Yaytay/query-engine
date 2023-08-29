/*
 * Copyright (C) 2022 jtalbut
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
package uk.co.spudsoft.query.exec.procs;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.streams.WriteStream;
import io.vertx.sqlclient.desc.ColumnDescriptor;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.DataRowStream;
import uk.co.spudsoft.query.exec.SourceNameTracker;


/**
 * PassthroughStream makes available a WriteStream that calls an {@link AsyncHandler} for each data item written to it and optionally passes the data on to a ReadStream.
 * 
 * @author jtalbut
 */
public class PassthroughStream {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(PassthroughStream.class);
  
  private final AtomicBoolean readStreamPaused = new AtomicBoolean();

  private final SourceNameTracker sourceNameTracker;
  private final Object lock = new Object();
  private final ChainedAsyncHandler<DataRow> processor;
  private final Context context;
  private final Write write;
  private final Read read;
  
  private Handler<Throwable> exceptionHandler;    
  private Handler<Void> drainHandler;
  private Handler<Void> passthroughEndHandler;
  private Handler<DataRow> readHandler;

  private long demand;

  private static class DataAndPromise {
    final Promise<Void> promise;
    final DataRow data;

    DataAndPromise(Promise<Void> promise, DataRow data) {
      this.promise = promise;
      this.data = data;
    }        
  }
  
  private DataAndPromise pending;
  private DataAndPromise inflight;
  
  /*
  This is the endhandler that will be called when the current in-flight item has been processed.
  */
  private Handler<Void> end;

  /**
   * Constructor.
   * 
   * @param sourceNameTracker Update the logging context with the name of the current source.
   * @param handler The handler that will be called with each data item to be processed.
   * @param context The Vertx context of any processing that is carried out.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Be aware that the point of sourceNameTracker is to modify the context")
  public PassthroughStream(SourceNameTracker sourceNameTracker, ChainedAsyncHandler<DataRow> handler, Context context) {
    this.sourceNameTracker = sourceNameTracker;
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
  public WriteStream<DataRow> writeStream() {
    return write;
  }
  
  /**
   * Set the end handler that will be called when the write stream ends.
   * This end handler is independent of the Read.endHandler, and will be called immediately prior to it.
   * @param handler The end handler.
   * @return this.
   */
  public PassthroughStream endHandler(Handler<Void> handler) {
    passthroughEndHandler = handler;
    return this;
  }
  
  /**
   * Get the ReadStream interface for this PassthroughStream.
   * There is only ever a single instance of the ReadStream, this method does not create a new instance.
   * @return the ReadStream interface for this PassthroughStream.
   */
  public DataRowStream<DataRow> readStream() {
    return read;
  }
  
  private void processCurrent() {
    sourceNameTracker.addNameToContextLocalData(context);
    logger.trace("{}@{}: processCurrent({})", getClass().getSimpleName(), hashCode(), inflight.data);
    context.runOnContext(v -> {
      try {
        logger.trace("{}@{}: processCurrent on context({})", getClass().getSimpleName(), hashCode(), inflight.data);
        processor.handle(inflight.data, d -> read.handle(d))
                .onComplete(ar -> {
                  completeCurrent(ar);
                })
                ;
      } catch (Throwable ex) {
        Handler<Throwable> handler = exceptionHandler;
        if (handler != null) {
          handler.handle(ex);
        }
        completeCurrent(Future.failedFuture(ex));
      }
    });
  }

  private void completeCurrent(AsyncResult<Void> ar) {
    Handler<Void> dh;
    Handler<Void> eh;
    Promise<Void> promise;
    boolean reprocess = false;
    synchronized (lock) {
      logger.trace("completeCurrent({}) with {}", ar, inflight == null ? null : inflight.data);
      dh = drainHandler;
      eh = end;
      promise = inflight == null ? null : inflight.promise;
      inflight = null;
      if (pending != null) {
        inflight = pending;
        pending = null;
        reprocess = true;
      }
    }
    if (promise != null) {
      promise.handle(ar);
    }
    if (reprocess) {
      processCurrent();
    } else {
      if (dh != null) {
        dh.handle(null);
      }
      if (eh != null) {
        eh.handle(null);
      }
    }
  }
  
  private class Write implements WriteStream<DataRow> {

    @Override
    public WriteStream<DataRow> exceptionHandler(Handler<Throwable> handler) {
      exceptionHandler = handler;
      return this;
    }

    @Override
    public Future<Void> write(DataRow data) {
      logger.trace("{}@{}: write({})", getClass().getSimpleName(), hashCode(), data);
      boolean isPaused;
      Promise<Void> promise = Promise.promise();
      boolean processNow = false;
      synchronized (lock) {
        if (pending != null) {
          throw new IllegalStateException("write(" + data + ") called whilst pending is " + pending);
        }
        pending = new DataAndPromise(promise, data);
        isPaused = readStreamPaused.get();
        if (!isPaused) {
          inflight = pending;
          pending = null;
          processNow = true;
        }
      }        
      if (processNow) {
        logger.trace("{}@{}: write processNow({})", getClass().getSimpleName(), hashCode(), data);        
        processCurrent();
      }
      return promise.future();
    }

    @Override
    public void write(DataRow data, Handler<AsyncResult<Void>> handler) {
      write(data).onComplete(handler);
    }

    @Override
    public void end(Handler<AsyncResult<Void>> handler) {
      boolean endNow;
      synchronized (lock) {
        end = v -> {
          Handler<Void> eh1;
          Handler<Void> eh2;
          synchronized (lock) {
            eh1 = passthroughEndHandler;
            eh2 = read.readStreamEndHandler;
          }
          if (eh1 != null) {
            eh1.handle(null);
          }
          if (eh2 != null) {
            eh2.handle(null);
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
    public WriteStream<DataRow> setWriteQueueMaxSize(int maxSize) {
      // This cannot do anything, there is no queue.
      throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean writeQueueFull() {
      synchronized (lock) {
        logger.trace("{}@{}: writeQueueFull() {}, {}, {}", getClass().getSimpleName(), hashCode(), readStreamPaused.get(), (inflight != null), (pending != null));        
        return readStreamPaused.get() || (inflight != null) || (pending != null);
      }
    }

    @Override
    public WriteStream<DataRow> drainHandler(Handler<Void> handler) {
      boolean callNow = false;
      synchronized (lock) {
        drainHandler = handler;
        if (inflight == null && pending == null) {
          callNow = true;
        }
      }
      if (callNow) {
        handler.handle(null);
      }
      return this;
    }
    
  }
  
  private class Read implements DataRowStream<DataRow> {

    private Handler<Void> readStreamEndHandler;
    private Handler<Throwable> exceptionHandler;
    
    Future<Void> handle(DataRow data) {
      Future<Void> result;
      try {
        readHandler.handle(data);
        result = Future.succeededFuture();
      } catch (Throwable ex) {
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
    public DataRowStream<DataRow> exceptionHandler(Handler<Throwable> handler) {
      this.exceptionHandler = handler;
      return this;
    }

    @Override
    public DataRowStream<DataRow> handler(Handler<DataRow> handler) {
      readHandler = handler;
      return this;
    }

    @Override
    public DataRowStream<DataRow> pause() {
      readStreamPaused.set(true);
      return this;
    }

    @Override
    public DataRowStream<DataRow> resume() {
      logger.trace("{}:{} Resume", this.getClass().getSimpleName(), this.hashCode());
      fetch(Long.MAX_VALUE);
      return this;
    }

    @Override
    public DataRowStream<DataRow> fetch(long amount) {
      if (amount < 0L) {
        throw new IllegalArgumentException();
      }
      readStreamPaused.set(false);
      boolean processNow = false;
      synchronized (lock) {
        demand += amount;
        if (demand < 0L) {
          demand = Long.MAX_VALUE;
        }
        if (pending != null && inflight == null) {
          inflight = pending;
          pending = null;
          processNow = true;
        }
      }
      if (processNow) {
        logger.trace("{}@{}: fetch({}) with {}", getClass().getSimpleName(), hashCode(), amount, inflight.data);
        processCurrent();
      }
      return this;
    }

    @Override
    public DataRowStream<DataRow> endHandler(Handler<Void> handler) {
      readStreamEndHandler = handler;
      return this;
    }

    @Override
    public List<ColumnDescriptor> getColumnDescriptors() {
      throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Future<Void> close() {
      throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void close(Handler<AsyncResult<Void>> completionHandler) {
      throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
   
    
  }
  
}
