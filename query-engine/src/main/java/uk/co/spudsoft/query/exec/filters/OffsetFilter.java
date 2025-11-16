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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.ProcessorOffset;
import uk.co.spudsoft.query.exec.Auditor;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.logging.Log;

/**
 * Filter for converting _offset command line arguments into {@link uk.co.spudsoft.query.exec.procs.filters.ProcessorOffsetInstance}s.
 *
 * The value of the argument should be a positive integer, that will be the number of rows skipped in the output.
 * If at all possible offset instructions should be implemented in the query, rather than using this filter.
 *
 * @author jtalbut
 */
public class OffsetFilter implements Filter {

  private static final Logger logger = LoggerFactory.getLogger(OffsetFilter.class);

  /**
   * Constructor.
   */
  public OffsetFilter() {
  }

  @Override
  public String getKey() {
    return "_offset";
  }

  @Override
  public ProcessorInstance createProcessor(Vertx vertx, MeterRegistry meterRegistry, Auditor auditor, PipelineContext pipelineContext, String argument, String name) throws IllegalArgumentException {
    int value;
    try {
      value = Integer.parseInt(argument);
    } catch (Throwable ex) {
      Log.decorate(logger.atWarn(), pipelineContext).log("Failed to convert argument to _offset filter (\"{}\") to integer: ", argument, ex);
      throw new IllegalArgumentException("Invalid argument to _offset filter, should be an integer");
    }
    ProcessorOffset definition = ProcessorOffset.builder().offset(value).build();
    return definition.createInstance(vertx, meterRegistry, auditor, pipelineContext, name);
  }

}
