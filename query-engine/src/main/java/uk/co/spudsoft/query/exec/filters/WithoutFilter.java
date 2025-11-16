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
import uk.co.spudsoft.query.exec.Auditor;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.context.PipelineContext;

/**
 * Filter for converting _without command line arguments into {@link uk.co.spudsoft.query.exec.procs.filters.ProcessorMapInstance}s.
 *
 * The argument should be a space delimited list of fields to be removed.
 *
 * @author jtalbut
 */
public class WithoutFilter implements Filter {

  /**
   * Constructor.
   */
  public WithoutFilter() {
  }

  @Override
  public String getKey() {
    return "_without";
  }

  @Override
  public ProcessorInstance createProcessor(Vertx vertx, MeterRegistry meterRegistry, Auditor auditor, PipelineContext pipelineContext, String argument, String name) {
    List<ProcessorMapLabel> relabels = new ArrayList<>();

    String sourceLabel = argument;
    String newLabel = "";
    relabels.add(ProcessorMapLabel.builder().sourceLabel(sourceLabel).newLabel(newLabel).build());

    ProcessorMap definition = ProcessorMap.builder().relabels(relabels).build();
    return definition.createInstance(vertx, meterRegistry, auditor, pipelineContext, name);
  }

}
