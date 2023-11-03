/*
 * Copyright (C) 2023 njt
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

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.conditions.RequestContextBuilder;
import uk.co.spudsoft.query.main.ExceptionToString;
import uk.co.spudsoft.query.main.SessionConfig;

/**
 *
 * @author njt
 */
public class AuthHandler implements Handler<RoutingContext> {
  
  private static final Logger logger = LoggerFactory.getLogger(AuthHandler.class);
  
  private static final String BEARER = "Bearer ";
  
  private final Vertx vertx;
  private final MeterRegistry meterRegistry;
  private final RequestContextBuilder requestContextBuilder;
  private final SessionConfig config;
  private final boolean outputAllErrorMessages;

  public AuthHandler(Vertx vertx, MeterRegistry meterRegistry, RequestContextBuilder requestContextBuilder, SessionConfig config, boolean outputAllErrorMessages) {
    this.vertx = vertx;
    this.meterRegistry = meterRegistry;
    this.requestContextBuilder = requestContextBuilder;
    this.config = config;
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
              logger.warn("Request failed: ", ex);

              int statusCode = 500;
              String message = "Failed";

              if (ex instanceof ServiceException serviceException) {
                statusCode = serviceException.getStatusCode();
                message = serviceException.getMessage();
              } else if (ex instanceof IllegalArgumentException) {
                statusCode = 400;
                message = ex.getMessage();
              }

              if (outputAllErrorMessages) {
                message = ExceptionToString.convert(ex, "\n\t");
              }

              event
                      .response()
                      .setStatusCode(statusCode)
                      .end(message)
                      ;
            })
            ;
  }

}
