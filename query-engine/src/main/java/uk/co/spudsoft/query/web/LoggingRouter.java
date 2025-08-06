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
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.context.RequestContext;
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

    logger.info("Request: {} {}", routingContext.request().method(), routingContext.request().uri());

    routingContext.addHeadersEndHandler(v -> {
      long end = System.currentTimeMillis();
      logger.info("Headers end: {} {}s", routingContext.response().getStatusCode(), (end - start) / 1000.0);
    });
    routingContext.addBodyEndHandler(v -> {
      long end = System.currentTimeMillis();
      logger.info("Body end: {} {} {}s", routingContext.response().getStatusCode(), routingContext.response().bytesWritten(), (end - start) / 1000.0);
    });
    routingContext.addEndHandler(v -> {
      long end = System.currentTimeMillis();
      logger.info("Complete: {} {} {}s", routingContext.response().getStatusCode(), routingContext.response().bytesWritten(), (end - start) / 1000.0);
    });
    routingContext.next();
  }
}
