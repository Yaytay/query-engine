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
package uk.co.spudsoft.query.exec.context;

/**
 * Context data for the running of a SourcePipeline within a request.
 * @author jtalbut
 */
public class PipelineContext {
  
  private final String pipe;
  private final RequestContext requestContext;

  /**
   * Constructor.
   * 
   * @param pipe The name of the current {@link uk.co.spudsoft.query.defn.SourcePipeline}.
   *          This will either be $ (for the root) or the name of the field of the processor that defines this pipeline.
   * @param requestContext The context of the HTTP request for running the entire {@link uk.co.spudsoft.query.defn.Pipeline}.
   */
  public PipelineContext(String pipe, RequestContext requestContext) {
    this.pipe = pipe;
    this.requestContext = requestContext;
  }

  /**
   * Get the name of the current {@link uk.co.spudsoft.query.defn.SourcePipeline}.
   * This will either be $ (for the root) or the name of the field of the processor that defines this pipeline.
   * @return the name of the current {@link uk.co.spudsoft.query.defn.SourcePipeline}.
   */
  public String getPipe() {
    return pipe;
  }

  /**
   * Get the context of the HTTP request for running the entire {@link uk.co.spudsoft.query.defn.Pipeline}.
   * @return the context of the HTTP request for running the entire {@link uk.co.spudsoft.query.defn.Pipeline}.
   */
  public RequestContext getRequestContext() {
    return requestContext;
  }
  
}
