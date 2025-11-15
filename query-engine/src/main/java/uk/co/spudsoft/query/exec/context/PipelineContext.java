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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

/**
 * Context data for the running of a SourcePipeline within a request.
 * @author jtalbut
 */
public class PipelineContext {
  
  private final String pipe;
  private final RequestContext requestContext;
  private final Span span;

  /**
   * Constructor.
   * 
   * @param pipe The name of the current {@link uk.co.spudsoft.query.defn.SourcePipeline}.
   *          This will either be $ (for the root) or the name of the field of the processor that defines this pipeline.
   * @param requestContext The context of the HTTP request for running the entire {@link uk.co.spudsoft.query.defn.Pipeline}.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The RequestContext may be modified in a few very specific ways during a pipeline run")
  public PipelineContext(String pipe, RequestContext requestContext) {
    this.pipe = pipe;
    this.requestContext = requestContext;
    if (requestContext == null || requestContext.getSpan() == null) {
      this.span = null;
    } else {
      Tracer tracer = GlobalOpenTelemetry.getTracer("query-engine:pipeline");
      Context parentContext = Context.current().with(requestContext.getSpan());
      this.span = tracer.spanBuilder(pipe)
              .setParent(parentContext)
              .startSpan();
    }
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
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "The RequestContext may be modified in a few very specific ways during a pipeline run")
  public RequestContext getRequestContext() {
    return requestContext;
  }

  /**
   * Get the Span for this  {@link uk.co.spudsoft.query.defn.SourcePipeline}.
   * @return the Span for this  {@link uk.co.spudsoft.query.defn.SourcePipeline}.
   */
  public Span getSpan() {
    return span;
  }
  
  /**
   * Create a new PipelineContext for a child {@link uk.co.spudsoft.query.defn.SourcePipeline}.
   * 
   * There is no connection between the parent and child contexts.
   * It would be possible to create a tree of PipelineContexts within a given RequestContext
   * , but I have seen no use for such a thing so far.
   * 
   * @param childPipe The name of the {@link uk.co.spudsoft.query.defn.SourcePipeline}.
   * @return a new PipelineContext for a child {@link uk.co.spudsoft.query.defn.SourcePipeline}.
   */
  public PipelineContext child(String childPipe) {
    return new PipelineContext(childPipe, requestContext);
  }
  
}
