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

import io.vertx.core.Future;

/**
 * An Instance of a {@link uk.co.spudsoft.query.defn.Processor}.
 * 
 * Usually created by called {@link uk.co.spudsoft.query.defn.Processor#createInstance(io.vertx.core.Vertx, uk.co.spudsoft.query.exec.SourceNameTracker, io.vertx.core.Context)}.
 * 
 * @author jtalbut
 */
public interface ProcessorInstance {
    
  /**
   * Return an ID for this processor, unique within the pipeline.
   * @return an ID for this processor, unique within the pipeline. 
   */
  String getId();
  
  /**
   * Take whatever steps are necessary to start reading or writing the streams.
   * 
   * At the time of the call the arguments in the {@link PipelineInstance} will have been set and can be used in the evaluation of any "template" values in the definition.
   * 
   * @param executor The executor that can be used by the class to initialize (and run) any sub pipelines.
   * @param pipeline Definition of the pipeline, primarily for access to arguments and sourceEndpoints.
   * @param parentSource The name of the Source feeding this processor.
   * @param processorIndex The index of this processor.
   * @param input The ReadStream that is the input to the processor.
   * @return a Future that will be completed when the initialization is complete.
   */
  Future<ReadStreamWithTypes> initialize(PipelineExecutor executor, PipelineInstance pipeline, String parentSource, int processorIndex, ReadStreamWithTypes input);
  
}
