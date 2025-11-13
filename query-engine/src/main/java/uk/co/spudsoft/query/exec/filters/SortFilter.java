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
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.ProcessorSort;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.logging.Log;

/**
 * Filter for converting _sort command line arguments into {@link uk.co.spudsoft.query.exec.procs.sort.ProcessorSortInstance}s.
 * <p>
 * The value of the argument should be a space delimited list of field names to sort by.
 * A field name may be preceded by '-' to sort in descending order.
 * <p>
 * Sort is inherently slow because it has to break the streaming of the data - all rows must be collated before they can be sorted.
 * There is a configurable limit to the amount of sortable data that can be stored in memory (see @{link uk.co.spudsoft.query.main.ProcessorConfig#setInMemorySortLimitBytes})
 * , once that limit has been exceeded rows will be streamed to disc, making the process slow still.
 * <p>
 * Only sort with a Processor/Filter when there is no other option.
 *
 * @author jtalbut
 *
 * @author jtalbut
 */
public class SortFilter implements Filter {

  private static final Logger logger = LoggerFactory.getLogger(SortFilter.class);
  
  /**
   * Constructor.
   */
  public SortFilter() {
  }

  @Override
  public String getKey() {
    return "_sort";
  }

  @Override
  public ProcessorInstance createProcessor(Vertx vertx, PipelineContext pipelineContext, MeterRegistry meterRegistry, String argument, String name) {
    List<String> fields = SpaceParser.parse(argument);
    if (fields.isEmpty()) {
      Log.decorate(logger.atWarn(), pipelineContext).log("Invalid argument to _sort filter: no fields found");
      throw new IllegalArgumentException("Invalid argument to _sort filter, should be a space delimited list of fields");
    } else {
      ProcessorSort definition = ProcessorSort.builder().fields(fields).build();
      return definition.createInstance(vertx, pipelineContext, meterRegistry, name);
    }
  }

}
