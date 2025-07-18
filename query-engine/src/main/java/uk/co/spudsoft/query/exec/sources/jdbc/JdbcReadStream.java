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
package uk.co.spudsoft.query.exec.sources.jdbc;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Deque;
import java.util.TimeZone;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.Types;

/**
 * Processor for converting a JDBC {@link ResultSet} into a Vert.x {@link ReadStream}.
 * 
 * The logic will attempt to keep between fetchSize/2 and fetchSize rows buffered at all times (until the rows run out).
 * 
 * @author jtalbut
 */
public class JdbcReadStream implements ReadStream<DataRow> {
  
  private static final Logger logger = LoggerFactory.getLogger(JdbcReadStream.class);
  
  private final SourceNameTracker sourceNameTracker;
  private final Context context;
  private final Types types;
  private final Connection connection;
  private final ResultSet rs;
  private final int fetchSize;
  
  private final Deque<DataRow> items = new ArrayDeque<>();
  
  private final ReentrantLock queueLock = new ReentrantLock();
  private final Condition notFull = queueLock.newCondition();
  
  private Handler<Throwable> exceptionHandler;
  private Handler<DataRow> handler;
  private Handler<Void> endHandler;
  
  private long demand;
  private volatile boolean initialized;
  private volatile boolean emitting;
  private volatile boolean ended;
  private volatile boolean completed;
  
  /**
   * Constructor.
   * @param sourceNameTracker Name tracker to use for logging, must be called at each entry point.
   * @param context The Vert.x context to use for asynchronous method calls.
   * @param types The types that are consistent across each row.
   * @param connection The {@link Connection} that must be closed when the processing finishes.
   * @param rs The {@link ResultSet} to read from.
   * @param fetchSize The number of rows to buffer.
   */
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public JdbcReadStream(SourceNameTracker sourceNameTracker, Context context, Types types, Connection connection, ResultSet rs, int fetchSize) {
    this.sourceNameTracker = sourceNameTracker;
    this.context = context;
    this.types = types;
    this.connection = connection;
    this.rs = rs;
    this.fetchSize = fetchSize;
  }
  
  /**
   * Start processing the {@link ResultSet} in a new thread.
   * 
   * @param name The name to assign to the new thread.
   */
  public void start(String name) {
    Thread thread = new Thread(this::resultSetWalker, name);
    thread.start();
  }
  
  private void checkProcessing(String message) {
    if (initialized) {
      if (!emitting) {
        logger.trace(message);
        emitting = true;
        context.runOnContext(v -> process());
      }
    } else {
      logger.trace("Not initialized yet");
    }
  }
  
  private void resultSetWalker() {
    
    sourceNameTracker.addNameToContextLocalData();
    ResultSetMetaData rsmeta = null;
    int rowsSinceBlock = 0;
    try {
      while (rs.next()) {
        if (rsmeta == null) {
          rsmeta = rs.getMetaData();
        }
        DataRow row = dataRowFromResult(rsmeta, rs);
        logger.trace("resultSetWalker: Taking out queue lock for results walker");
        queueLock.lock();
        try {
          items.add(row);
          while (items.size() > fetchSize) {
            logger.trace("resultSetWalker: Waiting until notFull ({} > {})", items.size(), fetchSize);
            notFull.await();
            logger.trace("resultSetWalker: notFull ({} > {})", items.size(), fetchSize);
          }
          checkProcessing("resultSetWalker: starting process");
        } finally {
          logger.trace("resultSetWalker: Releasing queue lock from results walker");
          queueLock.unlock();
        }
      }
    } catch (Throwable ex) {
      logger.error("resultSetWalker: Failed to process resultset: ", ex);
    } finally {
      try {
        rs.close();
      } catch (Throwable ex) {
        logger.warn("resultSetWalker: Failed to close result set");
      }
      try {
        connection.close();
      } catch (Throwable ex) {
        logger.warn("resultSetWalker: Failed to close connection");
      }
    }
    logger.trace("resultSetWalker: Finished iterating rows");
    complete();
  }
  
  private DataRow dataRowFromResult(ResultSetMetaData rsmeta, ResultSet rs) throws SQLException {
    DataRow row = DataRow.create(types);
    for (int i = 0; i < rsmeta.getColumnCount(); ++i) {
      String name = rsmeta.getColumnLabel(i + 1);

      DataType type = types.get(name);
      Object value;
      if  (type == DataType.DateTime) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        value = rs.getTimestamp(i + 1, cal);
      } else {
        value = rs.getObject(i + 1);        
      }
      try {
        Comparable<?> typedValue = type.cast(value);
        row.put(name, typedValue);
      } catch (Throwable ex) {
        logger.warn("Failed to cast {} ({}) value ({}): ", name, i, value, ex);
      }
    }
    return row;
  }
    
  /**
   * Add an item to the queue of items to be sent (and emit it if appropriate).
   * @param item The item to add.
   * @return this, so that this method may be used in a fluent manner.
   */
  private JdbcReadStream add(DataRow item) {
    if (completed) {
      throw new IllegalStateException("Last item has already been sent");
    }
    queueLock.lock();
    try {
      items.add(item);
    } finally {
      queueLock.unlock();
    }
    checkProcessing("Item added");
    return this;
  }

  /**
   * Mark that no more items will be added.
   * @return this, so that this method may be used in a fluent manner.
   */
  private JdbcReadStream complete() {
    queueLock.lock();
    try {
      this.completed = true;
    } finally {
      queueLock.unlock();
    }
    if (initialized) {
      logger.trace("Completed with buffer of {}", items.size());
    }
    checkProcessing("Completed");
    return this;
  }
  
  private void process() {
    Handler<Void> endHandlerCaptured = null;
    sourceNameTracker.addNameToContextLocalData();
    logger.trace("Starting to process {} {} ({}) {} {}", ended, emitting, completed, demand, items.size());
    while (!ended && emitting) {
      Handler<Throwable> exceptionHandlerCaptured;
      Handler<DataRow> handlerCaptured = null;
      DataRow item = null;
      
      logger.trace("Taking out queue lock for stream processor");
      queueLock.lock();
      try {
        if (demand <= 0) {
          logger.trace("Stop emitting, demand ({}) < 0", demand);
          emitting = false;
          break ;
        } else if (demand < Long.MAX_VALUE) {
          --demand;
        }        
        exceptionHandlerCaptured = exceptionHandler;
        if (!items.isEmpty()) {
          item =  items.pop();
          handlerCaptured = handler;
          if (items.size() < fetchSize / 2) {
            logger.trace("Signalling notFull");
            notFull.signal();
          } else {
            logger.trace("Not signalling - buffer too full {} > {}", items.size(), fetchSize / 2);
          }
        } else {
          logger.trace("Stop emitting, no items");
          emitting = false;
          notFull.signal();
        }
      } finally {
        logger.trace("Releasing queue lock from stream processor");
        queueLock.unlock();
      }
      if (item != null && handlerCaptured != null) {
        try {
          logger.trace("Handling {}", item);
          handlerCaptured.handle(item);
        } catch (Throwable ex) {
          if (exceptionHandlerCaptured != null) {
            exceptionHandlerCaptured.handle(ex);
          } else {
            logger.warn("Exception handling item in QueueReadStream: ", ex);
          }
        }
      }
    }
    if (completed && !ended) {
      endHandlerCaptured = endHandler;
      if (endHandlerCaptured != null) {
        logger.trace("Calling endHandler"); 
        endHandlerCaptured.handle(null);        
      } else {
        logger.trace("No endHandler");
      }
    }
  }
  
  @Override
  public JdbcReadStream exceptionHandler(Handler<Throwable> handler) {
    queueLock.lock();
    try {
      this.exceptionHandler = handler;
    } finally {
      queueLock.unlock();
    }
    return this;
  }

  @Override
  public JdbcReadStream handler(Handler<DataRow> handler) {
    queueLock.lock();
    try {
      this.handler = handler;
    } finally {
      queueLock.unlock();
    }
    return this;
  }

  @Override
  public JdbcReadStream endHandler(Handler<Void> endHandler) {
    queueLock.lock();
    try {
      this.endHandler = endHandler;
    } finally {
      queueLock.unlock();
    }
    return this;
  }
  
  @Override
  public JdbcReadStream pause() {
    logger.trace("pause()");
    queueLock.lock();
    try {
      demand = 0;
    } finally {
      queueLock.unlock();
    }
    return this;
  }

  @Override
  public JdbcReadStream resume() {
    sourceNameTracker.addNameToContextLocalData();
    logger.trace("resume()");
    queueLock.lock();
    try {
      initialized = true;
      demand = Long.MAX_VALUE;
    } finally {
      queueLock.unlock();
    }
    checkProcessing("Resume");
    return this;
  }

  @Override
  public JdbcReadStream fetch(long amount) {
    if (amount < 0L) {
      throw new IllegalArgumentException("Negative fetch amount");
    }
    sourceNameTracker.addNameToContextLocalData();
    logger.trace("fetch({})", amount);
    queueLock.lock();
    try {
      initialized = true;
      demand += amount;
      if (demand < 0L) {
        demand = Long.MAX_VALUE;
      }
    } finally {
      queueLock.unlock();
    }
    checkProcessing("Fetch");
    return this;
  }
}
