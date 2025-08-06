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
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.main.Authenticator;

/**
 * Vert.x {@link io.vertx.core.Handler}&lt;{@link io.vertx.ext.web.RoutingContext}&gt; for extracting the {@link uk.co.spudsoft.query.exec.context.RequestContext} and storing it in the {@link io.vertx.ext.web.RoutingContext}.
 * <p>
 * The RequestContextHandler is not a terminal handler, if the request context is extracted successfully the request will be passed on to the next handler in the chain.
 * 
 * @author jtalbut
 */
public class AuthenticationRouter implements Handler<RoutingContext> {
  
  private static final Logger logger = LoggerFactory.getLogger(AuthenticationRouter.class);
  private static final String KEY = "req";
  
  private final Vertx vertx;
  private final Authenticator authenticator;
  private final boolean outputAllErrorMessages;

  /**
   * Constructor.
   * @param vertx Vertx instance.
   * @param authenticator The builder that does the actual work.
   * @param outputAllErrorMessages In a production environment error messages should usually not leak information that may assist a bad actor, set this to true to return full details in error responses.
   */
  public AuthenticationRouter(Vertx vertx, Authenticator authenticator,  boolean outputAllErrorMessages) {
    this.vertx = vertx;
    this.authenticator = authenticator;
    this.outputAllErrorMessages = outputAllErrorMessages;
  }
    
  @Override
  public void handle(RoutingContext event) {
    authenticator
            .authenticate(event, RequestContext.retrieveRequestContext(event))
            .onSuccess(requestContext -> {
              Vertx.currentContext().put(KEY, requestContext);
              logger.debug("Context found for request to {}", event.request().absoluteURI());
              event.next();
            })
            .onFailure(ex -> {
              QueryRouter.internalError(ex, event, outputAllErrorMessages);
            });
  }
  
}
