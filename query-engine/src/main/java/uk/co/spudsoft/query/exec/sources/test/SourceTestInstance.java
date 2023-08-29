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
package uk.co.spudsoft.query.exec.sources.test;

import com.google.common.base.Strings;
import io.vertx.core.Context;
import io.vertx.core.Future;
import uk.co.spudsoft.query.defn.SourceTest;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.exec.DataRowStream;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.sources.AbstractSource;

/**
 *
 * @author jtalbut
 */
public class SourceTestInstance extends AbstractSource {

  private final int rowCount;
  private final Types types;
  private final BlockingReadStream<DataRow> stream;

  public SourceTestInstance(Context context, SourceTest definition, String defaultName) {
    super(Strings.isNullOrEmpty(definition.getName()) ? defaultName : definition.getName());
    this.rowCount = definition.getRowCount();
    this.types = new Types();
    this.types.putIfAbsent("value", DataType.Integer);
    if (!Strings.isNullOrEmpty(getName())) {
      types.putIfAbsent("name", DataType.String);
    }
    this.stream = new BlockingReadStream<>(context, rowCount, types);
  }    
 
  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline) {
    stream.pause();
    try {
      for (int i = 0; i < rowCount; ++i) {
        DataRow data = DataRow.create(types);
        data.put("value", i);
        if (!Strings.isNullOrEmpty(getName())) {
          data.put("name", getName());
        }
        stream.add(data);
      }
      stream.end();
    } catch (Throwable ex) {
      return Future.failedFuture(ex);
    }
    return Future.succeededFuture();
  }

  @Override
  public DataRowStream<DataRow> getReadStream() {
    return stream;
  }
}

