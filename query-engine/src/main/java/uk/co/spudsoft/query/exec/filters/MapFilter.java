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
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.ProcessorMap;
import uk.co.spudsoft.query.defn.ProcessorMapLabel;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.logging.Log;

/**
 * Filter for converting _map command line arguments into {@link uk.co.spudsoft.query.exec.procs.filters.ProcessorMapInstance}s.
 *
 * The argument should be a space delimited list of relabels, each of which should be SourceLabel:NewLabel.
 * The new label cannot contain a colon or a space, if the new label is blank the field will be dropped - the source label may not be blank.
 *
 * @author jtalbut
 */
public class MapFilter implements Filter {

  private static final Logger logger = LoggerFactory.getLogger(MapFilter.class);
  
  /**
   * Constructor.
   */
  public MapFilter() {
  }

  @Override
  public String getKey() {
    return "_map";
  }

  @Override
  public ProcessorInstance createProcessor(Vertx vertx, PipelineContext pipelineContext, MeterRegistry meterRegistry, String argument, String name) {
    List<String> fields = SpaceParser.parse(argument);
    if (fields.isEmpty()) {
      Log.decorate(logger.atWarn(), pipelineContext).log("Invalid argument to _map filter, no fields found");
      throw new IllegalArgumentException("Invalid argument to _map filter, should be a space delimited list of relabels, each of which should be SourceLabel:NewLabel.  The new label cannot contain a colon or a space, if the new label is blank the field will be dropped - the source label may not be blank.");
    } else {
      List<ProcessorMapLabel> relabels = new ArrayList<>();
      for (int i = 0; i < fields.size(); ++i) {
        String field = fields.get(i);
        int idx = field.lastIndexOf(":");
        if (idx < 0) {
          Log.decorate(logger.atWarn(), pipelineContext).log("Invalid argument to _map filter, no colon found in field {}: {}", i, field);
          throw new IllegalArgumentException("Invalid argument to _map filter, should be a space delimited list of relabels, each of which should be SourceLabel:NewLabel.  The new label cannot contain a colon or a space, if the new label is blank the field will be dropped - the source label may not be blank.");
        }
        String sourceLabel = field.substring(0, idx);
        String newLabel = field.substring(idx + 1);
        relabels.add(ProcessorMapLabel.builder().sourceLabel(sourceLabel).newLabel(newLabel).build());
      }

      ProcessorMap definition = ProcessorMap.builder().relabels(relabels).build();
      return definition.createInstance(vertx, pipelineContext, meterRegistry, name);
    }
  }

}
