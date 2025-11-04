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
package uk.co.spudsoft.query.main;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.CheckResult;
import io.vertx.ext.healthchecks.HealthChecks;
import io.vertx.ext.web.RoutingContext;

/**
 * Simple Handler&lt;RoutingContext&gt; for presenting a HealthChecks object
 * via Vertx web.
 * 
 * @author njt
 */
public class HealthCheckHandler implements Handler<RoutingContext> {

  private final HealthChecks checks;

  /**
   * Constructor.
   * 
   * @param checks The {@link HealthChecks} instance to present via HTTP.
   * 
   */
  public HealthCheckHandler(HealthChecks checks) {
    this.checks = checks;
  }
  
  @Override
  public void handle(RoutingContext ctx) {
    
    HttpServerResponse response = ctx.response();
    
    HttpServerRequest request = ctx.request();
    
    if (request.method() == HttpMethod.GET) {
      
      checks.checkStatus()
              .onComplete(ar -> {
                if (ar.succeeded()) {
                  CheckResult result = ar.result();
                  JsonObject data = result.getData();
                  if (result.getUp()) {
                    response
                            .putHeader("Content-Type", "application/json")
                            .setStatusCode(200)
                            .end(data == null ? Buffer.buffer() : data.toBuffer());
                  } else {
                    response
                            .putHeader("Content-Type", "application/json")
                            .setStatusCode(503)
                            .end(data == null ? Buffer.buffer() : data.toBuffer());
                  }
                } else {
                    response
                            .putHeader("Content-Type", "application/json")
                            .setStatusCode(500)
                            .end(ar.cause().getMessage());
                }
              });
    } else {
      ctx.next();
    }
  }
  
}
