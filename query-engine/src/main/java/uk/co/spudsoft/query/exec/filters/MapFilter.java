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
import uk.co.spudsoft.query.defn.ProcessorMap;
import uk.co.spudsoft.query.defn.ProcessorMapLabel;
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.exec.ProcessorInstance;

/**
 * Filter for converting _map command line arguments into {@link uk.co.spudsoft.query.exec.procs.filters.ProcessorMapInstance}s.
 *
 * The argument should be a space delimited list of relabels, each of which should be SourceLabel:NewLabel.
 * The new label cannot contain a colon or a space, if the new label is blank the field will be dropped - the source label may not be blank.
 *
 * @author jtalbut
 */
public class MapFilter implements Filter {

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
  public ProcessorInstance createProcessor(Vertx vertx, RequestContext requestContext, MeterRegistry meterRegistry, String argument, String name) {
    List<String> fields = SpaceParser.parse(argument);
    if (fields.isEmpty()) {
      throw new IllegalArgumentException("Invalid argument to _map filter, should be a space delimited list of relabels, each of which should be SourceLabel:NewLabel.  The new label cannot contain a colon or a space, if the new label is blank the field will be dropped - the source label may not be blank.");
    } else {
      List<ProcessorMapLabel> relabels = new ArrayList<>();
      for (String field : fields) {
        int idx = field.lastIndexOf(":");
        if (idx < 0) {
          throw new IllegalArgumentException("Invalid argument to _map filter, should be a space delimited list of relabels, each of which should be SourceLabel:NewLabel.  The new label cannot contain a colon or a space, if the new label is blank the field will be dropped - the source label may not be blank.");
        }
        String sourceLabel = field.substring(0, idx);
        String newLabel = field.substring(idx + 1);
        relabels.add(ProcessorMapLabel.builder().sourceLabel(sourceLabel).newLabel(newLabel).build());
      }

      ProcessorMap definition = ProcessorMap.builder().relabels(relabels).build();
      return definition.createInstance(vertx, requestContext, meterRegistry, name);
    }
  }

}
