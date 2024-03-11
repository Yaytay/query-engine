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

import io.vertx.core.Future;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.FormatInstance;

/**
 *
 * A Format that captures all the data and makes it available.
 * 
 * This is of no use to end-user pipelines because it provides no way to access the data
 * , but is used by the DynamicEndpoint processing.
 * 
 * @author jtalbut
 */
public class FormatCaptureInstance implements FormatInstance {
  
  private ReadStream<DataRow> stream;

  public FormatCaptureInstance() {
  }

  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline, ReadStream<DataRow> input) {
    this.stream = input;
    return Future.succeededFuture();
  }

  public ReadStream<DataRow> getReadStream() {
    return stream;
  }

  @Override
  public WriteStream<DataRow> getWriteStream() {
    throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
  }
  
}
