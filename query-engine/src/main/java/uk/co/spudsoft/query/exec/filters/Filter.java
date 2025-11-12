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
import io.vertx.core.Vertx;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.context.RequestContext;

/**
 * Definition of a Filter.
 *
 * Filters are classes for converting from command line arguments to {@link uk.co.spudsoft.query.exec.ProcessorInstance}s.
 *
 * The createProcessor method will only be called if the command line argument matches the key, with the value of the command line argument
 * being passed in.
 * Filters are created in the order they appear in the query string.
 *
 * @author jtalbut
 */
public interface Filter {

  /**
   * Get the key for this filter, that would be the argument name used in a query string.
   * @return the key for this filter, that would be the argument name used in a query string.
   */
  String getKey();

  /**
   * Create the processor given the argument set on the query string.
   * @param vertx the Vert.x instance.
   * @param requestContext the request context.
   * @param meterRegistry MeterRegistry for production of metrics.
   * @param argument the value of the query string parameter, that must be parsed into the configuration for this {@link ProcessorInstance}.
   * @param name the generated name of the processor to be used in logging and tracking.
   * @return a newly created {@link ProcessorInstance} of the appropriate type.
   */
  ProcessorInstance createProcessor(Vertx vertx, RequestContext requestContext, MeterRegistry meterRegistry, String argument, String name);

}
