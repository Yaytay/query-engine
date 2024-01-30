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
import java.util.ArrayList;
import java.util.List;
import uk.co.spudsoft.query.defn.ProcessorRelabel;
import uk.co.spudsoft.query.defn.ProcessorRelabelLabel;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.SourceNameTracker;

/**
 *
 * @author jtalbut
 */
public class RelabelFilter implements Filter {

  @Override
  public String getKey() {
    return "_relabel";
  }

  @Override
  public ProcessorInstance createProcessor(Vertx vertx, SourceNameTracker sourceNameTracker, Context context, String argument) {
    List<String> fields = SpaceParser.parse(argument);
    if (fields.isEmpty()) {
      throw new IllegalArgumentException("Invalid argument to _relabel filter, should be a space delimited list of relabels, each of which should be SourceLabel:NewLabel.  The new label cannot contain a colon and neith label can be zero characters in length.");
    } else {
      List<ProcessorRelabelLabel> relabels = new ArrayList<>();
      for (String field : fields) {
        int idx = field.lastIndexOf(":");
        if (idx < 0 || idx >= field.length()) {
          throw new IllegalArgumentException("Invalid argument to _relabel filter, should be a space delimited list of relabels, each of which should be SourceLabel:NewLabel.  The new label cannot contain a colon and neith label can be zero characters in length.");
        }
        String sourceLabel = field.substring(0, idx);
        String newLabel = field.substring(idx + 1);
        relabels.add(ProcessorRelabelLabel.builder().sourceLabel(sourceLabel).newLabel(newLabel).build());
      }
      
      ProcessorRelabel definition = ProcessorRelabel.builder().relabels(relabels).build();
      return definition.createInstance(vertx, sourceNameTracker, context);
    }
  }
  
}
