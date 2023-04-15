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
package uk.co.spudsoft.query.exec.procs.query;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.streams.WriteStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.FormatInstance;

/**
 *
 * @param <T> The type that the mapper converts the data too.
 * @author jtalbut
 */
public class CollatingDestinationInstance<T> implements FormatInstance {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(CollatingDestinationInstance.class);
  
  private final Function<DataRow, T> mapper;
  private final Promise<List<T>> ended;
  private final List<T> rows = new ArrayList<>();
  private final CollatingStream stream;

  public CollatingDestinationInstance(Function<DataRow, T> mapper) {
    this.mapper = mapper;
    this.ended = Promise.promise();
    this.stream = new CollatingStream();
  }
  
  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline) {
    return Future.succeededFuture();
  }
  
  public Future<List<T>> ended() {
    return ended.future();
  }

  private class CollatingStream implements WriteStream<DataRow> {
    
    private Handler<Throwable> exceptionHandler;

    @Override
    public WriteStream<DataRow> exceptionHandler(Handler<Throwable> handler) {
      this.exceptionHandler = handler;
      return this;
    }

    @Override
    public Future<Void> write(DataRow data) {
      T mapped;
      try {
        mapped = mapper.apply(data);
      } catch (Throwable ex) {
        logger.warn("Failed to map data row: ", ex);
        exceptionHandler.handle(ex);
        return Future.succeededFuture();
      }
      rows.add(mapped);
      return Future.succeededFuture();
    }

    @Override
    public void write(DataRow data, Handler<AsyncResult<Void>> handler) {
      write(data).onComplete(handler);
    }

    @Override
    public void end(Handler<AsyncResult<Void>> handler) {
      ended.complete(rows);
      handler.handle(Future.succeededFuture());
    }

    @Override
    public WriteStream<DataRow> setWriteQueueMaxSize(int maxSize) {
      return this;
    }

    @Override
    public boolean writeQueueFull() {
      return false;
    }

    @Override
    public WriteStream<DataRow> drainHandler(Handler<Void> handler) {
      return this;
    }
    
  }
  
  @Override
  public WriteStream<DataRow> getWriteStream() {
    return stream;
  }
  
  
  
}
