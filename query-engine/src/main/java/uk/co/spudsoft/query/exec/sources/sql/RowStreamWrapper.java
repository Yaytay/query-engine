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
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.PreparedStatement;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;
import io.vertx.sqlclient.desc.ColumnDescriptor;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.DataRowStream;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.Types;


/**
 *
 * @author jtalbut
 */
public class RowStreamWrapper implements DataRowStream<DataRow> {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(RowStreamWrapper.class);
  
  private final SourceNameTracker sourceNameTracker;
  private final DataRowStream<Row> rowStream;
  private final SqlConnection connection;
  private final Transaction transaction;
  private final PreparedStatement preparedStatement;
  private final Types types;

  private Handler<Throwable> exceptionHandler;
  private Handler<DataRow> handler;
  private boolean handledRows;
  
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Be aware that the point of sourceNameTracker is to modify the context")
  public RowStreamWrapper(SourceNameTracker sourceNameTracker, SqlConnection connection, Transaction transaction, PreparedStatement preparedStatement, MetadataRowStreamImpl rowStream) {
    this.sourceNameTracker = sourceNameTracker;
    this.connection = connection;
    this.transaction = transaction;
    this.preparedStatement = preparedStatement;
    this.rowStream = rowStream;
    this.types = new Types();
  }

  @Override
  public DataRowStream<DataRow> exceptionHandler(Handler<Throwable> handler) {
    this.exceptionHandler = handler;
    rowStream.exceptionHandler(ex -> {
      sourceNameTracker.addNameToContextLocalData(Vertx.currentContext());
      logger.warn("Exception in RowStream: ", ex);
      this.exceptionHandler.handle(ex);
    });
    return this;            
  }
  
  @Override
  public DataRowStream<DataRow> handler(Handler<DataRow> handler) {
    sourceNameTracker.addNameToContextLocalData(Vertx.currentContext());
    logger.trace("handler({})", handler);
    this.handler = handler;
    if (handler == null) {
      // When rowStream.handler is called with a non-null handler it will always handle the currently in-memory rows
      // So must not pass in a 'debug' handler here.
      rowStream.handler(null);
    } else {      
      rowStream.handler(row -> {
        try {
          handledRows = true;
          Context context = Vertx.currentContext();
          logger.trace("RowStream context: {}", context);
          DataRow dataRow = sqlRowToDataRow(row);
          logger.trace("{} Received row: {}", this, dataRow);
          handler.handle(dataRow);
        } catch (Throwable ex) {
          if (exceptionHandler != null) {
            exceptionHandler.handle(ex);
          }
        }
      });
    }
    return this;
  }
  
  private DataRow sqlRowToDataRow(Row row) {
    DataRow result = DataRow.create(types);
    int size = row.size();
    for (int col = 0; col < size; col++) {
      String name = row.getColumnName(col);
      Object value = row.getValue(col);
      Comparable<?> compValue = DataRow.convert(value);
      result.put(name, compValue);
    }
    return result;
  }
  
  @Override
  public DataRowStream<DataRow> pause() {
    sourceNameTracker.addNameToContextLocalData(Vertx.currentContext());
    logger.trace("{} paused", this);
    rowStream.pause();
    return this;
  }

  @Override
  public DataRowStream<DataRow> resume() {
    sourceNameTracker.addNameToContextLocalData(Vertx.currentContext());
    logger.trace("{} resumed", this);
    rowStream.resume();
    return this;
  }

  @Override
  public DataRowStream<DataRow> fetch(long amount) {
    rowStream.fetch(amount);
    return this;
  }

  @Override
  public DataRowStream<DataRow> endHandler(Handler<Void> endHandler) {    
    rowStream.endHandler(ehv -> {
      if (handledRows) {
        logger.info("Finished row stream after handling some rows");
      } else {
        logger.info("Finished row stream without handling any rows");
        Handler<DataRow> localHandler = this.handler;
        if (localHandler != null) {
          ensureTypesReflectRowStream();
          DataRow dataRow = DataRow.create(types);
          logger.trace("{} Passing on empty row: {}", this, dataRow);
          localHandler.handle(dataRow);
        }
      }
      rowStream.close()
              .compose(v -> {
                return transaction.commit();
              })
              .compose(v -> {
                sourceNameTracker.addNameToContextLocalData(Vertx.currentContext());
                if (connection != null) {
                  logger.info("Closing connection");
                  return connection.close();
                } else {
                  return Future.succeededFuture();
                }
              })
              .onComplete(ar -> {
                sourceNameTracker.addNameToContextLocalData(Vertx.currentContext());
                logger.info("Closed connection");
                if (!ar.succeeded()) {
                  logger.warn("Transaction failed: ", ar.cause());
                }
                endHandler.handle(null);                
              });
    });
    return this;
  }

  @Override
  public List<ColumnDescriptor> getColumnDescriptors() {
    ensureTypesReflectRowStream();
    
    return types.getColumnDescriptors();
  }

  private void ensureTypesReflectRowStream() throws IllegalStateException {
    if (types.isEmpty()) {
      for (ColumnDescriptor cd : rowStream.getColumnDescriptors()) {
        logger.info("Field {} is of JDBC type {} (aka {})", cd.name(), cd.jdbcType(), cd.typeName());
        types.putIfAbsent(cd.name(), DataType.fromJdbcType(cd.jdbcType()));
      }
    }
  }

  @Override
  public Future<Void> close() {
    return rowStream.close();
  }

  @Override
  public void close(Handler<AsyncResult<Void>> completionHandler) {
    rowStream.close(completionHandler);
  }
  
}
