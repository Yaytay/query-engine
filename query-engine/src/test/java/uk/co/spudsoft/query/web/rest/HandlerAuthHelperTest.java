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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import uk.co.spudsoft.jwtvalidatorvertx.Jwt;
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.web.ServiceException;

/**
 *
 * @author jtalbut
 */
public class HandlerAuthHelperTest {
  
  @Test
  public void testGetRequestContext() throws Exception {
    
    Context vertxContext = mock(Context.class);
    
    when(vertxContext.getLocal("req")).thenReturn(null);
    assertNull(HandlerAuthHelper.getRequestContext(vertxContext, false));

    RequestContext emptyRequestContext = new RequestContext(null, null, null, null, null, null, null, null, null, null);
    when(vertxContext.getLocal("req")).thenReturn(emptyRequestContext);
    assertEquals(emptyRequestContext, HandlerAuthHelper.getRequestContext(vertxContext, false));
    
    when(vertxContext.getLocal("req")).thenReturn(null);
    assertThrows(ServiceException.class, () -> { HandlerAuthHelper.getRequestContext(vertxContext, true); });
    
    when(vertxContext.getLocal("req")).thenReturn(emptyRequestContext);
    assertThrows(ServiceException.class, () -> { HandlerAuthHelper.getRequestContext(vertxContext, true); });
    
    RequestContext fullRequestContext = new RequestContext(null, null, null, null, null, null, null, null, null, new Jwt(null, null, null, null));
    when(vertxContext.getLocal("req")).thenReturn(fullRequestContext);
    assertEquals(fullRequestContext, HandlerAuthHelper.getRequestContext(vertxContext, true));
    
  }
  
}
