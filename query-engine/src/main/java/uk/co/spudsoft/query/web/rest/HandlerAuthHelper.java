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
package uk.co.spudsoft.query.web.rest;

import io.vertx.core.Context;
import jakarta.ws.rs.core.Response;
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.web.RequestContextHandler;
import uk.co.spudsoft.query.web.ServiceException;

/**
 * Helper class for extracting a {@link uk.co.spudsoft.query.exec.conditions.RequestContext} from a Vertx {@link io.vertx.core.Context}.
 * 
 * @author jtalbut
 */
public class HandlerAuthHelper {

  private HandlerAuthHelper() {}

  /**
   * Extract a {@link uk.co.spudsoft.query.exec.conditions.RequestContext} from a Vertx {@link io.vertx.core.Context}.
   * @param context The Vertx Context.
   * @param required If true, the method will throw a ServiceException if the request does not have an authenticated request context.
   * @return a valid {@link uk.co.spudsoft.query.exec.conditions.RequestContext}.
   * @throws ServiceException if the request does not have an authenticated request context.
   */
  public static RequestContext getRequestContext(Context context, boolean required) throws ServiceException {
    RequestContext requestContext = RequestContextHandler.getRequestContext(context);
    if (required) {
      if (requestContext == null) {
        throw new ServiceException(Response.Status.UNAUTHORIZED.getStatusCode(), "");
      } else if (!requestContext.isAuthenticated()) {
        throw new ServiceException(Response.Status.UNAUTHORIZED.getStatusCode(), "");
      }
    }
    return requestContext;
  }
  
  
}
