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
package uk.co.spudsoft.query.web.rest;

import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.core.Response;
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.web.ServiceException;

/**
 * Helper class for extracting a {@link uk.co.spudsoft.query.exec.context.RequestContext} from a Vert.x {@link io.vertx.ext.web.RoutingContext}.
 * 
 * @author jtalbut
 */
public class HandlerAuthHelper {

  private HandlerAuthHelper() {}

  /**
   * Extract a {@link uk.co.spudsoft.query.exec.context.RequestContext} from a Vert.x {@link io.vertx.ext.web.RoutingContext}.
   * @param routingContext The Vert.x {@link io.vertx.ext.web.RoutingContext}.
   * @param required If true, the method will throw a ServiceException if the request does not have an authenticated request context.
   * @return a valid {@link uk.co.spudsoft.query.exec.context.RequestContext}.
   * @throws ServiceException if the request does not have an authenticated request context.
   */
  public static RequestContext getRequestContext(RoutingContext routingContext, boolean required) throws ServiceException {
    RequestContext requestContext = RequestContext.retrieveRequestContext(routingContext);
    if (required) {
      if (requestContext == null || !requestContext.isAuthenticated()) {
        throw new ServiceException(Response.Status.UNAUTHORIZED.getStatusCode(), "Unauthorized");
      }
    }
    return requestContext;
  }
  
  
}
