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
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.web.ServiceException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import uk.co.spudsoft.jwtvalidatorvertx.Jwt;
import uk.co.spudsoft.query.exec.context.RequestContext;

/**
 * Unit tests for HandlerAuthHelper.
 *
 * @author jtalbut
 */
public class HandlerAuthHelperTest {

  private RoutingContext createMockRoutingContextWithRequestContext(RequestContext requestContext) {
    RoutingContext routingContext = mock(RoutingContext.class);
  
    when(routingContext.get(RequestContext.class.getName())).thenReturn(requestContext);

    return routingContext;
  }

  @Test
  public void testGetRequestContext_NotRequired_WithNullContext() throws Exception {
    RoutingContext routingContext = createMockRoutingContextWithRequestContext(null);

    RequestContext result = HandlerAuthHelper.getRequestContext(routingContext, false);
    assertNull(result);
  }

  @Test
  public void testGetRequestContext_NotRequired_WithUnauthenticatedContext() throws Exception {
    RequestContext unauthenticatedContext = new RequestContext(
            null, null, null, null, null, null, null, null, null, null
    );
    RoutingContext routingContext = createMockRoutingContextWithRequestContext(unauthenticatedContext);

    RequestContext result = HandlerAuthHelper.getRequestContext(routingContext, false);
    assertEquals(unauthenticatedContext, result);
  }

  @Test
  public void testGetRequestContext_NotRequired_WithAuthenticatedContext() throws Exception {
    RequestContext authenticatedContext = new RequestContext(
            null, null, null, null, null, null, null, null, null,
            new Jwt(null, null, null, null)
    );
    RoutingContext routingContext = createMockRoutingContextWithRequestContext(authenticatedContext);

    RequestContext result = HandlerAuthHelper.getRequestContext(routingContext, false);
    assertEquals(authenticatedContext, result);
  }

  @Test
  public void testGetRequestContext_Required_WithNullContext() {
    RoutingContext routingContext = createMockRoutingContextWithRequestContext(null);

    ServiceException exception = assertThrows(ServiceException.class,
            () -> HandlerAuthHelper.getRequestContext(routingContext, true));
    assertEquals(401, exception.getStatusCode());
    assertEquals("Unauthorized", exception.getMessage());
  }

  @Test
  public void testGetRequestContext_Required_WithUnauthenticatedContext() {
    RequestContext unauthenticatedContext = new RequestContext(
            null, null, null, null, null, null, null, null, null, null
    );
    RoutingContext routingContext = createMockRoutingContextWithRequestContext(unauthenticatedContext);

    ServiceException exception = assertThrows(ServiceException.class,
            () -> HandlerAuthHelper.getRequestContext(routingContext, true));
    assertEquals(401, exception.getStatusCode());
    assertEquals("Unauthorized", exception.getMessage());
  }

  @Test
  public void testGetRequestContext_Required_WithAuthenticatedContext() throws Exception {
    RequestContext authenticatedContext = new RequestContext(
            null, null, null, null, null, null, null, null, null,
            new Jwt(null, null, null, null)
    );
    RoutingContext routingContext = createMockRoutingContextWithRequestContext(authenticatedContext);

    RequestContext result = HandlerAuthHelper.getRequestContext(routingContext, true);
    assertEquals(authenticatedContext, result);
  }
}
