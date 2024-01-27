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
package uk.co.spudsoft.query.exec;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import uk.co.spudsoft.query.exec.filters.Filter;

/**
 *
 * @author jtalbut
 */
public class FilterFactory {
  
  private final Map<String, Filter> filters;
  private final List<String> sortedKeys;

  public FilterFactory(List<Filter> filters) {
    ImmutableMap.Builder<String, Filter> builder = ImmutableMap.<String, Filter>builder();
    filters.forEach(f -> builder.put(f.getKey(), f));
    this.filters = builder.build();
    sortedKeys = ImmutableList.copyOf(this.filters.keySet().stream().sorted().collect(Collectors.toList()));
  }
  
  public List<String> getSortedKeys() {
    return sortedKeys;
  }
  
  public ProcessorInstance createFilter(Vertx vertx, SourceNameTracker sourceNameTracker, Context context, String arg, String value) {
    Filter filter = filters.get(arg);
    if (filter != null) {
      return filter.createProcessor(vertx, sourceNameTracker, context, value);
    } else {
      return null;
    }
  }
  
}
