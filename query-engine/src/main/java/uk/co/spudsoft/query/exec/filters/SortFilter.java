/*
 * Copyright (C) 2024 jtalbut
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
package uk.co.spudsoft.query.exec.filters;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import java.util.List;
import uk.co.spudsoft.query.defn.ProcessorSort;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.SourceNameTracker;

/**
 *
 * @author jtalbut
 */
public class SortFilter implements Filter {

  @Override
  public String getKey() {
    return "_sort";
  }

  @Override
  public ProcessorInstance createProcessor(Vertx vertx, SourceNameTracker sourceNameTracker, Context context, String argument) {
    List<String> fields = SpaceParser.parse(argument);
    if (fields.isEmpty()) {
      throw new IllegalArgumentException("Invalid argument to _sort filter, should be a space delimited list of fields");
    } else {
      ProcessorSort definition = ProcessorSort.builder().fields(fields).build();
      return definition.createInstance(vertx, sourceNameTracker, context);
    }
  }
  
}
