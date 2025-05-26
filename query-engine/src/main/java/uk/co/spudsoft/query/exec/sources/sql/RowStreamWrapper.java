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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.streams.ReadStream;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;
import io.vertx.sqlclient.desc.ColumnDescriptor;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.Types;


/**
 * {@link io.vertx.core.streams.ReadStream}&lt;{@link uk.co.spudsoft.query.exec.DataRow}&gt; that works with {@link MetadataRowStreamImpl} to be able to report metadata after initialization even when no rows are returned.
 * 
 * @author jtalbut
 */
public final class RowStreamWrapper implements ReadStream<DataRow> {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(RowStreamWrapper.class);
  
  private final SourceNameTracker sourceNameTracker;
  private final MetadataRowStreamImpl rowStream;
  private final SqlConnection connection;
  private final Transaction transaction;
  private final Types types;

  private Handler<Throwable> exceptionHandler;
  private Handler<DataRow> handler;
  private boolean handledRows;
  
  private final Promise<Void> readyPromise = Promise.promise();
  
  /**
   * Constructor.
   * 
   * @param sourceNameTracker The object used to identify this source in the Vert.x context for logging purposes.
   * @param connection The connection to the data source.
   * @param transaction The database transaction.
   * @param rowStream The output row stream.
   * @param columnTypeOverrides Manually overridden types for columns.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Be aware that the point of sourceNameTracker is to modify the context")
  public RowStreamWrapper(SourceNameTracker sourceNameTracker, SqlConnection connection, Transaction transaction, MetadataRowStreamImpl rowStream, Map<String, DataType> columnTypeOverrides) {
    this.sourceNameTracker = sourceNameTracker;
    this.connection = connection;
    this.transaction = transaction;
    this.rowStream = rowStream;
    this.types = new Types();
    rowStream.coloumnDescriptorHandler(columnDescriptors -> {
      for (ColumnDescriptor cd : columnDescriptors) {
        logger.trace("Field {} is of JDBC type {} (aka {})", cd.name(), cd.jdbcType(), cd.typeName());
        if (columnTypeOverrides != null && columnTypeOverrides.containsKey(cd.name())) {
          types.putIfAbsent(cd.name(), columnTypeOverrides.get(cd.name()));
        } else {
          types.putIfAbsent(cd.name(), DataType.fromJdbcType(cd.jdbcType()));
        }
      }
      logger.debug("Got types: {}", types);
      readyPromise.complete();
    });
    rowStream.pause();
    rowStream.exceptionHandler(ex -> {
      sourceNameTracker.addNameToContextLocalData();
      logger.warn("Exception in RowStream: ", ex);
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
        handledRows = true;
        Context context = Vertx.currentContext();
        logger.trace("RowStream context: {}", context);
        DataRow dataRow = sqlRowToDataRow(row);
        logger.trace("{} Received row: {}", this, dataRow);
        handler.handle(dataRow);
      } catch (Throwable ex) {
        logger.warn("Exception processing row (with types {}): ", types, ex);
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
    sourceNameTracker.addNameToContextLocalData();
    logger.trace("handler({})", handler);
    this.handler = handler;
    if (handler == null) {
      // When rowStream.handler is called with a non-null handler it will always handle the currently in-memory rows
      // So must not pass in a 'debug' handler here.
      rowStream.handler(null);
    }
    return this;
  }
  
  private DataRow sqlRowToDataRow(Row row) {
    DataRow result = DataRow.create(types);
    int size = row.size();
    for (int col = 0; col < size; col++) {
      String name = row.getColumnName(col);
      Object value = row.getValue(col);
      DataType type = types.get(name);
      try {
        Comparable<?> typedValue = type.cast(value);
        result.put(name, typedValue);
      } catch (Exception ex) {
        logger.warn("Unable to convert {} to {}: ", value, type, ex);
      }
    }
    return result;
  }
  
  @Override
  public RowStreamWrapper pause() {
    sourceNameTracker.addNameToContextLocalData();
    logger.trace("{} paused", this);
    rowStream.pause();
    return this;
  }

  @Override
  public RowStreamWrapper resume() {
    sourceNameTracker.addNameToContextLocalData();
    logger.trace("{} resumed", this);
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
      sourceNameTracker.addNameToContextLocalData();
      if (handledRows) {
        logger.trace("Finished row stream after handling some rows");
      } else {
        logger.trace("Finished row stream without handling any rows");
      }
      rowStream.close()
              .compose(v -> {
                return transaction.commit();
              })
              .compose(v -> {
                sourceNameTracker.addNameToContextLocalData();
                if (connection != null) {
                  logger.info("Closing connection");
                  return connection.close();
                } else {
                  return Future.succeededFuture();
                }
              })
              .onComplete(ar -> {
                sourceNameTracker.addNameToContextLocalData();
                logger.info("Closed connection");
                if (!ar.succeeded()) {
                  logger.warn("Transaction failed: ", ar.cause());
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
