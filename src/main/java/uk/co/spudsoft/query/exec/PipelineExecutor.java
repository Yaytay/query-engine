/*
 * Copyright (C) 2022 jtalbut
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

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import java.util.List;
import java.util.Map;
import uk.co.spudsoft.query.defn.Argument;
import uk.co.spudsoft.query.defn.Pipeline;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.main.ProtectedCredentials;
import uk.co.spudsoft.query.defn.Format;

/**
 *
 * @author jtalbut
 */
public interface PipelineExecutor extends SharedMap {

  ProtectedCredentials getSecret(String name);
  
  Map<String, ArgumentInstance> prepareArguments(List<Argument> definitions, MultiMap valuesMap);
  
  Future<Pipeline> validatePipeline(Pipeline definition);
  
  List<ProcessorInstance> createProcessors(Vertx vertx, SourceNameTracker sourceNameTracker, Context context, SourcePipeline definition);
  
  List<PreProcessorInstance> createPreProcessors(Vertx vertx, Context context, Pipeline definition);

  Future<Void> initializePipeline(PipelineInstance pipeline);
  
  Format getFormat(List<Format> formats, FormatRequest requested);

}
