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
package uk.co.spudsoft.query.exec.fmts;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.streams.WriteStream;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.DataRow;

/**
 *
 * @author jtalbut
 */
public class CapturingWriteStream implements WriteStream<DataRow> {
  
  private static final Logger logger = LoggerFactory.getLogger(CapturingWriteStream.class);
  
  private final List<DataRow> list = new ArrayList<>();
  private final Handler<List<DataRow>> terminate;

  public CapturingWriteStream(Handler<List<DataRow>> terminate) {
    this.terminate = terminate;
  }

  @Override
  public WriteStream<DataRow> exceptionHandler(Handler<Throwable> handler) {
    return this;
  }

  @Override
  public Future<Void> write(DataRow data) {
    logger.debug("write({})", data);
    list.add(data);
    return Future.succeededFuture();
  }

  @Override
  public void write(DataRow data, Handler<AsyncResult<Void>> handler) {
    write(data).onComplete(handler);
  }

  @Override
  public void end(Handler<AsyncResult<Void>> handler) {
    logger.debug("end({})", handler);
    terminate.handle(list);
    handler.handle(Future.succeededFuture());
  }

  @Override
  public WriteStream<DataRow> setWriteQueueMaxSize(int maxSize) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public boolean writeQueueFull() {
    return false;
  }

  @Override
  public WriteStream<DataRow> drainHandler(Handler<Void> handler) {
    throw new UnsupportedOperationException("Not supported.");
  }
  
  
  
}
