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
 *
 * A FormatInstance is created from a Format (based on FormatType).
 * 
 * @author jtalbut
 */
public interface FormatInstance extends Writable {
  
  /**
   * Take whatever steps are necessary to start the output.
   * 
   * At the time of the call the arguments in the {@link PipelineInstance} will have been set and can be used in the evaluation of any "template" values in the definition.
   * 
   * @param executor The executor that can be used by the class to initialize (and run) any sub pipelines.
   * @param pipeline Definition of the pipeline, primarily for access to arguments and sourceEndpoints.
   * @param input The ReadStream that is the input to the processor.
   * @return a Future that will be completed when the initialization is complete.
   */
  Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline, ReadStreamWithTypes input);
  
}
