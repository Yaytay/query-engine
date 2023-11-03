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
import uk.co.spudsoft.query.web.ServiceException;

/**
 *
 * @author njt
 */
public class HandlerAuthHelper {

  private HandlerAuthHelper() {}


  public static RequestContext getRequestContext(Context context, boolean required) throws ServiceException {
    RequestContext requestContext = context.getLocal("req");
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
