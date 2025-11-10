/*
 * Copyright (C) 2025 jtalbut
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
package uk.co.spudsoft.query.web;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import java.util.Map;
import uk.co.spudsoft.query.defn.Format;
import uk.co.spudsoft.query.defn.Pipeline;
import uk.co.spudsoft.query.exec.ArgumentInstance;
import uk.co.spudsoft.query.exec.context.RequestContext;

/**
 * Parameters for the PipelineRunningVerticle.
 * 
 * All parameters must be thread safe, or otherwise have guarantees that they won't be modified on other threads.
 * In particular the WriteStream must be one appropriate to this Context.
 * 
 * @author jtalbut
 */
public class PipelineRunningTask {
  
  /**
   * The request context for this HTTP request.
   */
  public final RequestContext requestContext;

  /**
   * The definition of the pipeline being run.
   */
  public final Pipeline pipeline;

  /**
   * The output format to use.
   */
  public final Format chosenFormat;

  /**
   * Query string parameters taken from the HTTP request.
   */
  public final MultiMap queryStringParams;

  /**
   * Processed arguments for the pipeline.
   */
  public final Map<String, ArgumentInstance> arguments;

  /**
   * The WriteStream that will be written to, in usual usage this must be a {@link BufferingContextAwareWriteStream}.
   */
  public final WriteStream<Buffer> responseStream;

  /**
   * Constructor. 
   * @param requestContext The request context for this HTTP request.
   * @param pipeline The definition of the pipeline being run.
   * @param chosenFormat The output format to use.
   * @param queryStringParams Query string parameters taken from the HTTP request.
   * @param arguments Processed arguments for the pipeline.
   * @param responseStream The WriteStream that will be written to, in usual usage this must be a {@link BufferingContextAwareWriteStream}.
   */
  public PipelineRunningTask(RequestContext requestContext, Pipeline pipeline, Format chosenFormat, MultiMap queryStringParams, Map<String, ArgumentInstance> arguments, WriteStream<Buffer> responseStream) {
    this.requestContext = requestContext;
    this.pipeline = pipeline;
    this.chosenFormat = chosenFormat;
    this.queryStringParams = queryStringParams;
    this.arguments = arguments;
    this.responseStream = responseStream;
  }

}
