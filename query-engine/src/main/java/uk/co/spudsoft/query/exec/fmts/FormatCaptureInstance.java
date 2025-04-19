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
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.FormatInstance;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;

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
  
  private ReadStreamWithTypes streamWithTypes;

  /**
   * Constructor.
   */
  public FormatCaptureInstance() {
  }

  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline, ReadStreamWithTypes input) {
    this.streamWithTypes = input;
    return Future.succeededFuture();
  }

  /**
   * Return the internal {@link ReadStreamWithTypes} that was captured by {@link #initialize(uk.co.spudsoft.query.exec.PipelineExecutor, uk.co.spudsoft.query.exec.PipelineInstance, uk.co.spudsoft.query.exec.ReadStreamWithTypes)}.
   * @return the internal {@link ReadStreamWithTypes} that was captured by {@link #initialize(uk.co.spudsoft.query.exec.PipelineExecutor, uk.co.spudsoft.query.exec.PipelineInstance, uk.co.spudsoft.query.exec.ReadStreamWithTypes)}.
   */
  public ReadStreamWithTypes getReadStream() {
    return streamWithTypes;
  }
  
}
