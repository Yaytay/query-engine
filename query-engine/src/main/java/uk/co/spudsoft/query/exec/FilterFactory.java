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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.filters.Filter;

/**
 * Factory for creating Processors from command line arguments.
 * @author jtalbut
 */
public class FilterFactory {
  
  private static final Logger logger = LoggerFactory.getLogger(FilterFactory.class);
  
  private final Map<String, Filter> filters;
  private final List<String> sortedKeys;

  /**
   * Constructor.
   * @param filters the filters that are to be handled.
   */
  public FilterFactory(List<Filter> filters) {
    ImmutableMap.Builder<String, Filter> builder = ImmutableMap.<String, Filter>builder();
    filters.forEach(f -> builder.put(f.getKey(), f));
    this.filters = builder.build();
    sortedKeys = ImmutableList.copyOf(this.filters.keySet().stream().sorted().collect(Collectors.toList()));
  }
  
  /**
   * Get the value of {@link Filter#getKey()} from each configured {@link Filter}, sorted alphabetically.
   * @return the value of {@link Filter#getKey()} from each configured {@link Filter}, sorted alphabetically.
   */
  public List<String> getSortedKeys() {
    return sortedKeys;
  }
  
  /**
   * Create the {@link ProcessorInstance} for the identified {@link Filter}.
   * @param vertx the Vert.x instance.
   * @param sourceNameTracker the name tracker used to record the name of this source at all entry points for logger purposes.
   * @param context the Vert.x context.
   * @param arg the query string parameter name (the key for the filter).
   * @param value the value of the query string parameter, that must be parsed into the configuration for this {@link ProcessorInstance}.
   * @param name the generated name of the processor to be used in logging and tracking
   * @return a newly created {@link ProcessorInstance} of the appropriate type.
   */
  public ProcessorInstance createFilter(Vertx vertx, SourceNameTracker sourceNameTracker, Context context, String arg, String value, String name) {
    Filter filter = filters.get(arg);
    if (filter != null) {
      logger.debug("Creating processor from {}={}", arg, value);
      return filter.createProcessor(vertx, sourceNameTracker, context, value, name);
    } else {
      return null;
    }
  }
  
}
