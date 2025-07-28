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

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.ProcessorLimit;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.SourceNameTracker;

/**
 * Filter for converting _limit command line arguments into {@link uk.co.spudsoft.query.exec.procs.filters.ProcessorLimitInstance}s.
 * 
 * The value of the argument should be a positive integer, that will be the maximum number of rows returned.
 * If at all possible limit instructions should be implemented in the query, rather than using this filter.
 * 
 * @author jtalbut
 */
public class LimitFilter implements Filter {

  private static final Logger logger = LoggerFactory.getLogger(LimitFilter.class);

  /**
   * Constructor.
   */
  public LimitFilter() {
  }
  
  @Override
  public String getKey() {
    return "_limit";
  }

  @Override
  public ProcessorInstance createProcessor(Vertx vertx, SourceNameTracker sourceNameTracker, Context context, MeterRegistry meterRegistry, String argument, String name) throws IllegalArgumentException {
    int value;
    try {
      value = Integer.parseInt(argument);
    } catch (Throwable ex) {
      logger.warn("Failed to convert argument to _limit filter (\"{}\") to integer: ", argument, ex);
      throw new IllegalArgumentException("Invalid argument to _limit filter, should be an integer");
    }
    ProcessorLimit definition = ProcessorLimit.builder().name(name).limit(value).build();
    return definition.createInstance(vertx, sourceNameTracker, context, meterRegistry, name);
  }
  
}
