/*
 * Copyright (C) 2023 jtalbut
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

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.conditions.RequestContextBuilder;

/**
 *
 * @author njt
 */
public class RequestContextHandler implements Handler<RoutingContext> {
  
  private static final Logger logger = LoggerFactory.getLogger(RequestContextHandler.class);
  
  private static final String BEARER = "Bearer ";
  
  private final Vertx vertx;
  private final RequestContextBuilder requestContextBuilder;
  private final boolean outputAllErrorMessages;

  public RequestContextHandler(Vertx vertx, RequestContextBuilder requestContextBuilder,  boolean outputAllErrorMessages) {
    this.vertx = vertx;
    this.requestContextBuilder = requestContextBuilder;
    this.outputAllErrorMessages = outputAllErrorMessages;
  }
    
  @Override
  public void handle(RoutingContext event) {
    requestContextBuilder
            .buildRequestContext(event.request())
            .onSuccess(requestContext -> {
              Vertx.currentContext().putLocal("req", requestContext);
              
              event.next();
            })
            .onFailure(ex -> {
              QueryRouter.internalError(ex, event, outputAllErrorMessages);
            });
  }

}
