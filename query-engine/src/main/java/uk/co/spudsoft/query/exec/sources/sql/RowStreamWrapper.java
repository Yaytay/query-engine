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
package uk.co.spudsoft.query.exec.sources.sql;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.streams.ReadStream;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;
import io.vertx.sqlclient.desc.ColumnDescriptor;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.logging.Log;


/**
 * {@link io.vertx.core.streams.ReadStream}&lt;{@link uk.co.spudsoft.query.exec.DataRow}&gt; that works with {@link MetadataRowStreamImpl} 
 * to be able to report metadata after initialization even when no rows are returned.
 * 
 * @author jtalbut
 */
public final class RowStreamWrapper implements ReadStream<DataRow> {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(RowStreamWrapper.class);
  
  private final MetadataRowStreamImpl rowStream;
  private final SqlConnection connection;
  private final Transaction transaction;
  private final Types types;
  private final Log log;

  private Handler<Throwable> exceptionHandler;
  private Handler<DataRow> handler;
  
  private long rowCount;
  
  private final Promise<Void> readyPromise = Promise.promise();
  
  /**
   * Constructor.
   * 
   * @param pipelineContext The context in which this {@link SourcePipeline} is being run.
   * @param connection The connection to the data source.
   * @param transaction The database transaction.
   * @param rowStream The output row stream.
   * @param columnTypeOverrides Manually overridden types for columns.
   */
  public RowStreamWrapper(PipelineContext pipelineContext, SqlConnection connection, Transaction transaction, MetadataRowStreamImpl rowStream, Map<String, DataType> columnTypeOverrides) {
    this.connection = connection;
    this.transaction = transaction;
    this.rowStream = rowStream;
    this.types = new Types();
    this.log = new Log(logger, pipelineContext);
    
    rowStream.coloumnDescriptorHandler(columnDescriptors -> {
      for (ColumnDescriptor cd : columnDescriptors) {
        log.trace().log("Field {} is of JDBC type {} (aka {})", cd.name(), cd.jdbcType(), cd.typeName());
        if (columnTypeOverrides != null && columnTypeOverrides.containsKey(cd.name())) {
          types.putIfAbsent(cd.name(), columnTypeOverrides.get(cd.name()));
        } else {
          types.putIfAbsent(cd.name(), DataType.fromJdbcType(pipelineContext, cd.jdbcType()));
        }
      }
      log.debug().log("Got types: {}", types);
      readyPromise.complete();
    });
    rowStream.pause();
    rowStream.exceptionHandler(ex -> {
      log.error().log("Exception in RowStream after {} rows: ", rowCount, ex);
      readyPromise.tryFail(ex);
      Handler<Throwable> capturedExceptionHandler;
      synchronized (this) {
        capturedExceptionHandler = exceptionHandler;
      }
      if (capturedExceptionHandler != null) {
        capturedExceptionHandler.handle(ex);
      }
    });
    rowStream.handler(row -> {
      try {
        ++rowCount;
        DataRow dataRow = sqlRowToDataRow(pipelineContext, row);
        if (rowCount % 1000 == 0) {
          log.trace().log("{} Received {} rows", this, rowCount);
        }
        handler.handle(dataRow);
      } catch (Throwable ex) {
        log.warn().log("Exception processing row (with types {}): ", types, ex);
        if (exceptionHandler != null) {
          exceptionHandler.handle(ex);
        }
      }
    });
  }
 
  /**
   * Get the types in the {@link DataRow} objects in the output stream.
   * <P>
   * This method must only be called after the Future return by {@link #ready()} has succeeded.
   * 
   * @return the types in the {@link DataRow} objects in the output stream.
   */
  public Types getTypes() {
    return types;
  }
  
  /**
   * Return a Future that will be completed when the initial query has run (and the types are known).
   * @return a Future that will be completed when the initial query has run (and the types are known).
   */
  public Future<Void> ready() {
    return readyPromise.future();
  }
  
  @Override
  public RowStreamWrapper exceptionHandler(Handler<Throwable> handler) {
    this.exceptionHandler = handler;
    return this;            
  }
  
  @Override
  public RowStreamWrapper handler(Handler<DataRow> handler) {
    log.trace().log("handler({})", handler);
    this.handler = handler;
    if (handler == null) {
      // When rowStream.handler is called with a non-null handler it will always handle the currently in-memory rows
      // So must not pass in a 'debug' handler here.
      rowStream.handler(null);
    }
    return this;
  }
  
  private DataRow sqlRowToDataRow(PipelineContext pipelineContext, Row row) {
    DataRow result = DataRow.create(types);
    int size = row.size();
    for (int col = 0; col < size; col++) {
      String name = row.getColumnName(col);
      Object value = row.getValue(col);
      DataType type = types.get(name);
      try {
        Comparable<?> typedValue = type.cast(pipelineContext, value);
        result.put(name, typedValue);
      } catch (Exception ex) {
        log.warn().log("Unable to convert {} to {}: ", value, type, ex);
      }
    }
    return result;
  }
  
  @Override
  public RowStreamWrapper pause() {
    log.trace().log("{} paused", this);
    rowStream.pause();
    return this;
  }

  @Override
  public RowStreamWrapper resume() {
    log.trace().log("{} resumed", this);
    rowStream.resume();
    return this;
  }

  @Override
  public RowStreamWrapper fetch(long amount) {
    rowStream.fetch(amount);
    return this;
  }

  @Override
  public RowStreamWrapper endHandler(Handler<Void> endHandler) {
    rowStream.endHandler(ehv -> {
      if (rowCount > 0) {
        log.trace().log("Finished row stream after handling {} rows", rowCount);
      } else {
        log.trace().log("Finished row stream without handling any rows");
      }
      rowStream.close()
              .compose(v -> {
                return transaction.commit();
              })
              .compose(v -> {
                if (connection != null) {
                  log.info().log("Closing connection");
                  return connection.close();
                } else {
                  return Future.succeededFuture();
                }
              })
              .onComplete(ar -> {
                log.info().log("Closed connection");
                if (!ar.succeeded()) {
                  log.warn().log("Transaction failed: ", ar.cause());
                }
                endHandler.handle(null);                
              });
    });
    return this;
  }

  /**
   * Close the underlying {@link ReadStream}.
   * @return A Future that will be completed when the underlying {@link ReadStream} has been closed.
   */
  public Future<Void> close() {
    return rowStream.close();
  }

}
