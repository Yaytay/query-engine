/*
 * Copyright (C) 2023 jtalbut
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
import io.vertx.core.Promise;
import io.vertx.core.streams.ReadStream;
import io.vertx.sqlclient.Cursor;
import io.vertx.sqlclient.PreparedStatement;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.RowStream;
import io.vertx.sqlclient.Tuple;
import io.vertx.sqlclient.desc.ColumnDescriptor;
import io.vertx.sqlclient.impl.RowStreamInternal;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.context.RequestContext;

/**
 * Capture the metadata returned by a SQL statement even when there are no rows returned.
 *
 * @author jtalbut
 */
public class MetadataRowStreamImpl implements RowStreamInternal, Handler<AsyncResult<RowSet<Row>>>, ReadStream<Row> {

  private static final Logger logger = LoggerFactory.getLogger(MetadataRowStreamImpl.class);

  private final PreparedStatement ps;
  private final RequestContext requestContext;
  private final Context context;
  private final int fetch;
  private final Tuple params;

  private Handler<Void> endHandler;
  private Handler<Row> rowHandler;
  private Handler<Throwable> exceptionHandler;
  private Handler<List<ColumnDescriptor>> columnDescriptorHandler;
  private long demand;
  private boolean emitting;
  private Cursor cursor;
  private boolean readInProgress;
  private Iterator<Row> result;

  private long rowSetCount = 0;

  /**
   * Constructor.
   * @param ps The {@link PreparedStatement} to be executed.
   * @param requestContext The request context, for logging and tracking.
   * @param context The Vert.x context to use for asynchronous operations.
   * @param fetch The number of rows to fetch.
   * @param params Parameters to pass to the {@link PreparedStatement}.
   */
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public MetadataRowStreamImpl(PreparedStatement ps, RequestContext requestContext, Context context, int fetch, Tuple params) {
    this.ps = ps;
    this.requestContext = requestContext;
    this.context = context;
    this.fetch = fetch;
    this.params = params;
    this.demand = Long.MAX_VALUE;
  }

  @Override
  public synchronized Cursor cursor() {
    return cursor;
  }

  @Override
  public synchronized RowStream<Row> exceptionHandler(Handler<Throwable> handler) {
    exceptionHandler = handler;
    return this;
  }

  /**
   * Set the handler that will be notified when the {@link ColumnDescriptor} details are known.
   * @param handler the handler that will be notified when the {@link ColumnDescriptor} details are known.
   * @return this, so that this method may be used in a fluent manner.
   */
  public RowStream<Row> coloumnDescriptorHandler(Handler<List<ColumnDescriptor>> handler) {
    synchronized (this) {
      columnDescriptorHandler = handler;
    }
    return this;
  }

  @Override
  public RowStream<Row> handler(Handler<Row> handler) {
    Cursor c;
    synchronized (this) {
      if (handler != null) {
        if (cursor == null) {
          rowHandler = handler;
          c = cursor = ps.cursor(params);
          logger.trace("Cursor created");
          if (readInProgress) {
            return this;
          }
          readInProgress = true;
        } else {
          throw new UnsupportedOperationException("Handle me gracefully");
        }
      } else {
        rowHandler = null;
        if (cursor != null) {
          logger.trace("no handler, so closing cursor");
          cursor.close();
          readInProgress = false;
          cursor = null;
          result = null; // Will stop the current emission if any
        }
        return this;
      }
    }
    c.read(fetch).andThen(this);
    return this;
  }

  @Override
  public synchronized RowStream<Row> pause() {
    demand = 0L;
    return this;
  }

  @Override
  public RowStream<Row> fetch(long amount) {
    if (amount < 0L) {
      throw new IllegalArgumentException("Invalid fetch amount " + amount);
    }
    logger.trace("Fetch called with {}", amount);
    synchronized (this) {
      demand += amount;
      if (demand < 0L) {
        demand = Long.MAX_VALUE;
      }
    }
    checkPending();
    return this;
  }

  @Override
  public RowStream<Row> resume() {
    return fetch(Long.MAX_VALUE);
  }

  @Override
  public synchronized RowStream<Row> endHandler(Handler<Void> handler) {
    endHandler = handler;
    return this;
  }

  @Override
  public void handle(AsyncResult<RowSet<Row>> ar) {
    if (ar.failed()) {
      logger.warn("Failed to get RowSet {}: ", ++rowSetCount, ar.cause());
      Handler<Throwable> handler;
      synchronized (this) {
        readInProgress = false;
        cursor = null;
        result = null;
        handler = exceptionHandler;
      }
      if (handler != null) {
        handler.handle(ar.cause());
      }
    } else {
      RowSet<Row> rowSet = ar.result();
      if (rowSetCount % 100 == 0) {
        logger.debug("Got RowSet {}", ++rowSetCount);
      } else {
        logger.trace("Got RowSet {}", ++rowSetCount);
      }
      Handler<List<ColumnDescriptor>> colDescHandler;
      synchronized (this) {
        readInProgress = false;
        colDescHandler = columnDescriptorHandler;
        if (columnDescriptorHandler != null) {
          columnDescriptorHandler = null;
        }
        RowIterator<Row> it = rowSet.iterator();
        if (it.hasNext()) {
          result = it;
        }
      }
      if (colDescHandler != null) {
        colDescHandler.handle(rowSet.columnDescriptors());
      }
      checkPending();
    }
  }

  @Override
  public Future<Void> close() {
    logger.trace("close()");
    Cursor c;
    synchronized (this) {
      c = cursor;
      cursor = null;
    }
    if (c != null) {
      return c.close();
    } else {
      Promise<Void> promise = Promise.promise();
      context.runOnContext(v -> {
        promise.complete();
      });
      return promise.future();
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void checkPending() {
    logger.trace("checkPending");
    synchronized (this) {
      if (emitting) {
        logger.trace("already emitting");
        return;
      }
      emitting = true;
    }
    while (true) {
      synchronized (this) {
        if (demand == 0L) {
          emitting = false;
          logger.trace("0 demand");
          break;
        }
        Handler handler;
        Object event;
        if (result != null) {
          handler = rowHandler;
          event = result.next();
          if (demand != Long.MAX_VALUE) {
            demand--;
          }
          if (!result.hasNext()) {
            logger.trace("result does not have next");
            result = null;
          }
        } else {
          logger.trace("no result");
          emitting = false;
          if (readInProgress) {
            // logger.trace("readInProgress");
            break;
          } else {
            if (cursor == null) {
              logger.trace("cursor not set");
              break;
            } else if (cursor.hasMore()) {
              logger.trace("cursor has more, reading another {} rows", fetch);
              readInProgress = true;
              cursor.read(fetch).andThen(this);
              break;
            } else {
              logger.trace("cursor does not have more, closing");
              cursor.close();
              cursor = null;
              handler = endHandler;
              event = null;
            }
          }
        }
        if (handler != null) {
          handler.handle(event);
        }
      }
    }
  }
}
