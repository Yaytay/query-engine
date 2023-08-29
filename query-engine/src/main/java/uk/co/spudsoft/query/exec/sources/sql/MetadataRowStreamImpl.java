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
import io.vertx.core.impl.ContextInternal;
import io.vertx.sqlclient.Cursor;
import io.vertx.sqlclient.PreparedStatement;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.RowStream;
import io.vertx.sqlclient.Tuple;
import io.vertx.sqlclient.desc.ColumnDescriptor;
import io.vertx.sqlclient.impl.RowStreamInternal;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import uk.co.spudsoft.query.exec.DataRowStream;

/**
 *
 * @author njt
 */
public class MetadataRowStreamImpl implements RowStreamInternal, Handler<AsyncResult<RowSet<Row>>>, DataRowStream<Row> {

  private final PreparedStatement ps;
  private final ContextInternal context;
  private final int fetch;
  private final Tuple params;

  private Handler<Void> endHandler;
  private Handler<Row> rowHandler;
  private Handler<Throwable> exceptionHandler;
  private long demand;
  private boolean emitting;
  private Cursor cursor;
  private boolean readInProgress;
  private List<ColumnDescriptor> columnDescriptors;
  private Iterator<Row> result;

  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "It is expected that these members may change externally")
  public MetadataRowStreamImpl(PreparedStatement ps, Context context, int fetch, Tuple params) {
    this.ps = ps;
    this.context = (ContextInternal) context;
    this.fetch = fetch;
    this.params = params;
    this.demand = Long.MAX_VALUE;
  }

  @Override
  public synchronized List<ColumnDescriptor> getColumnDescriptors() {
    return Collections.unmodifiableList(columnDescriptors);
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

  @Override
  public RowStream<Row> handler(Handler<Row> handler) {
    Cursor c;
    synchronized (this) {
      if (handler != null) {
        if (cursor == null) {
          rowHandler = handler;
          c = cursor = ps.cursor(params);
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
          cursor.close();
          readInProgress = false;
          cursor = null;
          result = null; // Will stop the current emission if any
        }
        return this;
      }
    }
    c.read(fetch, this);
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
      RowSet<Row> rowset = ar.result();
      synchronized (this) {
        readInProgress = false;
        RowIterator<Row> it = rowset.iterator();
        this.columnDescriptors = rowset.columnDescriptors();
        if (it.hasNext()) {
          result = it;
        }
      }
      checkPending();
    }
  }

  @Override
  public Future<Void> close() {
    Cursor c;
    synchronized (this) {
      c = cursor;
      cursor = null;
    }
    if (c != null) {
      return c.close();
    } else {
      return context.succeededFuture();
    }
  }

  @Override
  public void close(Handler<AsyncResult<Void>> completionHandler) {
    Future<Void> fut = close();
    if (completionHandler != null) {
      fut.onComplete(completionHandler);
    }
  }
  
  @SuppressWarnings({"unchecked", "rawtypes"})
  private void checkPending() {
    synchronized (MetadataRowStreamImpl.this) {
      if (emitting) {
        return;
      }
      emitting = true;
    }
    while (true) {
      synchronized (MetadataRowStreamImpl.this) {
        if (demand == 0L) {
          emitting = false;
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
            result = null;
          }
        } else {
          emitting = false;
          if (readInProgress) {
            break;
          } else {
            if (cursor == null) {
              break;
            } else if (cursor.hasMore()) {
              readInProgress = true;
              cursor.read(fetch, this);
              break;
            } else {
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
