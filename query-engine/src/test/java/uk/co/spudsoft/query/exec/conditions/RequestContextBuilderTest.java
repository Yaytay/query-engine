/*
 * Copyright (C) 2022 jtalbut
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
package uk.co.spudsoft.query.exec.conditions;

import inet.ipaddr.IPAddressString;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.impl.HostAndPortImpl;
import io.vertx.core.net.impl.SocketAddressImpl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author jtalbut
 */
public class RequestContextBuilderTest {
  
  @Test
  public void testBuildRequestContextAllNulls() {
    RequestContextBuilder builder = new RequestContextBuilder(null, null, null, null, false, null, null);
    HttpServerRequest request = mock(HttpServerRequest.class);
    when(request.remoteAddress()).thenReturn(new SocketAddressImpl(0, "1.2.3.4"));
    RequestContext context = builder.buildRequestContext(request).result();
    assertNotNull(context);
    assertEquals(new IPAddressString("1.2.3.4"), context.getClientIp());
  }
  
  @Test
  public void testHap() {
    HttpServerRequest request = mock(HttpServerRequest.class);
    when(request.authority()).thenReturn(new HostAndPortImpl("bob", 1234));
    when(request.scheme()).thenReturn("http");
    assertEquals("http://bob:1234", RequestContextBuilder.baseRequestUrl(request));

    when(request.authority()).thenReturn(new HostAndPortImpl("bob", 80));
    when(request.scheme()).thenReturn("http");
    assertEquals("http://bob", RequestContextBuilder.baseRequestUrl(request));

    when(request.authority()).thenReturn(new HostAndPortImpl("bob", 1234));
    when(request.scheme()).thenReturn("https");
    assertEquals("https://bob:1234", RequestContextBuilder.baseRequestUrl(request));

    when(request.authority()).thenReturn(new HostAndPortImpl("bob", 443));
    when(request.scheme()).thenReturn("https");
    assertEquals("https://bob", RequestContextBuilder.baseRequestUrl(request));
  }
  
}
