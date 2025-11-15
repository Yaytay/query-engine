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

import com.google.common.collect.ImmutableMap;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.logging.Log;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;

/**
 * Simple Vertx HTTP logger.
 *
 * @author jtalbut
 */
public class LoggingRouter implements Handler<RoutingContext> {

  private static final Logger logger = LoggerFactory.getLogger(LoggingRouter.class);

  private final ImmutableMap<String, String> requestContextEnvironment;

  /**
   * Default constructor.
   *
   * @param requestContextEnvironment The additional data that is made available via the request object.
   */
  public LoggingRouter(Map<String, String> requestContextEnvironment) {
    this.requestContextEnvironment = ImmutableCollectionTools.copy(requestContextEnvironment);
  }

  @Override
  public void handle(RoutingContext routingContext) {
    long start = System.currentTimeMillis();

    HttpServerRequest request = routingContext.request();
    RequestContext requestContext = new RequestContext(requestContextEnvironment, request);
    requestContext.storeInRoutingContext(routingContext);
    PipelineContext pipelineContext = new PipelineContext(null, requestContext);

    Span span = Span.current();
    logger.debug("Span.current: traceId={}, spanId={}, sampled={}, remote={}, recording={}",
        span.getSpanContext().getTraceId(),
        span.getSpanContext().getSpanId(),
        span.getSpanContext().isSampled(),
        span.getSpanContext().isRemote(),
        span.isRecording());
    
    Log.decorate(logger.atInfo(), pipelineContext)
            .log("Request: {} {}", routingContext.request().method(), routingContext.request().uri());

    routingContext.addHeadersEndHandler(v -> {
      long end = System.currentTimeMillis();
      Log.decorate(logger.atInfo(), pipelineContext)
            .log("Headers end: {} {}s", routingContext.response().getStatusCode(), (end - start) / 1000.0);
    });
    routingContext.addBodyEndHandler(v -> {
      long end = System.currentTimeMillis();
      Log.decorate(logger.atInfo(), pipelineContext)
            .log("Body end: {} {} {}s", routingContext.response().getStatusCode(), routingContext.response().bytesWritten(), (end - start) / 1000.0);
    });
    routingContext.addEndHandler(v -> {
      long end = System.currentTimeMillis();
      Log.decorate(logger.atInfo(), pipelineContext)
            .log("Complete: {} {} {}s", routingContext.response().getStatusCode(), routingContext.response().bytesWritten(), (end - start) / 1000.0);
    });
    routingContext.next();
  }
}
