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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.streams.WriteStream;
import io.vertx.sqlclient.RowStream;
import io.vertx.sqlclient.desc.ColumnDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.ProcessorSort;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.DataRowStream;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.Types;

/**
 *
 * @author jtalbut
 */
@SuppressFBWarnings("URF_UNREAD_FIELD")
public class ProcessorSortInstance implements ProcessorInstance {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ProcessorSortInstance.class);
  
  private final SourceNameTracker sourceNameTracker;
  private final Context context;
  private final ProcessorSort definition;
  
  private Types types;
  private List<DataRow> rows;
  private long size;
  private int index;
  
  private final Object lock = new Object();

  private final SortWriteStream writeStream;
  private final SortReadStream readStream;
  
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Be aware that the point of sourceNameTracker is to modify the context")
  public ProcessorSortInstance(Vertx vertx, SourceNameTracker sourceNameTracker, Context context, ProcessorSort definition) {
    this.sourceNameTracker = sourceNameTracker;
    this.context = context;
    this.definition = definition;    
    this.rows = new ArrayList<>();
    this.size = 0L;
    this.index = 0;
    
    this.writeStream = new SortWriteStream();
    this.readStream = new SortReadStream();
    
    
  }  

  /**
   * Purely for test purposes.
   * @return The configured limit for this processor instance.
   */
  public List<String> getSort() {
    return definition.getFields();
  }
  
  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline, String parentSource, int processorIndex) {
    return Future.succeededFuture();
  }

  @Override
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public DataRowStream<DataRow> getReadStream() {
    return this.readStream;
  }

  @Override
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public WriteStream<DataRow> getWriteStream() {
    return this.writeStream;
  }
  
  private boolean flow() {
    if (index >= rows.size()) {
      logger.debug("Ending");
      readStream.end();
      return false;
    }
    
    synchronized (lock) {
      if (readStream.demand == 0) {
        logger.debug("Paused");
        return false;
      }
    }
    logger.debug("Sending row {}", index);      
    readStream.handle(rows.get(index++));
    return true;
  }
  
  private class SortWriteStream implements WriteStream<DataRow> {
    
    private Handler<Throwable> exceptionHandler;
    private Handler<Void> drainHandler;
    private boolean ended;

    public boolean isEnded() {
      return ended;
    }

    @Override
    public WriteStream<DataRow> exceptionHandler(Handler<Throwable> hndlr) {
      this.exceptionHandler = hndlr;
      return this;
    }

    @Override
    public Future<Void> write(DataRow data) {
      types = data.types();
      rows.add(data);
      size += data.bytesSize();
      return Future.succeededFuture();
    }

    @Override
    public void write(DataRow data, Handler<AsyncResult<Void>> hndlr) {
      Future<Void> result = write(data);
      hndlr.handle(result);
    }

    @Override
    public void end(Handler<AsyncResult<Void>> hndlr) {
      boolean sortAndFlow;
      synchronized (lock) {
        sortAndFlow = !ended;
        ended = true;
      }
      if (sortAndFlow) {
        logger.debug("Got {} rows ({} bytes) to sort: {}", rows.size(), size, rows);
        rows.sort(DataRowComparator.createChain(types, definition.getFields()));

        logger.debug("Sorted {} rows ({} bytes): {}", rows.size(), size, rows);
        boolean flowing = true;
        while (flowing) {
          flowing = flow();
        }
      }
    }

    @Override
    public WriteStream<DataRow> setWriteQueueMaxSize(int i) {
      return this;
    }

    @Override
    public boolean writeQueueFull() {
      return false;
    }

    @Override
    public WriteStream<DataRow> drainHandler(Handler<Void> hndlr) {
      this.drainHandler = hndlr;
      return this;
    }
    
  }
  
  private class SortReadStream implements DataRowStream<DataRow> {

    private Handler<Throwable> exceptionHandler;
    private Handler<DataRow> rowHandler;
    private Handler<Void> endHandler;
    private long demand;
    
    public void handle(DataRow row) {
      
      Handler<DataRow> handler ;
      synchronized (lock) {
        if (demand < Long.MAX_VALUE) {
          --demand;
        }
        handler = rowHandler;
      }
      handler.handle(row);
    }
    
    public void end() {
      endHandler.handle(null);
    }
    
    @Override
    public List<ColumnDescriptor> getColumnDescriptors() {
      return types == null ? Collections.emptyList() : types.getColumnDescriptors();
    }
    
    @Override
    public RowStream<DataRow> exceptionHandler(Handler<Throwable> hndlr) {
      this.exceptionHandler = hndlr;
      return this;
    }

    @Override
    public RowStream<DataRow> handler(Handler<DataRow> hndlr) {
      this.rowHandler = hndlr;
      return this;
    }
    
    @Override
    public RowStream<DataRow> pause() {
      synchronized (lock) {
        this.demand = 0;
      }
      return this;
    }

    @Override
    public RowStream<DataRow> resume() {
      synchronized (lock) {
        this.demand = Integer.MAX_VALUE;
      }
      return this;
    }

    @Override
    public RowStream<DataRow> endHandler(Handler<Void> hndlr) {
      this.endHandler = hndlr;
      return this;
    }

    @Override
    public RowStream<DataRow> fetch(long amount) {
      if (amount < 0L) {
        throw new IllegalArgumentException();
      }
      synchronized (lock) {
        demand += amount;
        if (demand < 0L) {
          demand = Long.MAX_VALUE;
        }
      }
      if (writeStream.isEnded()) {
        boolean flowing = true;
        while (flowing) {
          flowing = flow();
        }
      }
      return this;
    }

    @Override
    public Future<Void> close() {
      return Future.succeededFuture();
    }

    @Override
    public void close(Handler<AsyncResult<Void>> hndlr) {
      hndlr.handle(close());
    }
    
  }
  
}
