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
import io.vertx.core.net.impl.HostAndPortImpl;
import io.vertx.core.net.impl.SocketAddressImpl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import io.vertx.core.http.HttpServerRequest;
import org.junit.jupiter.api.Test;

import uk.co.spudsoft.query.main.BasicAuthConfig;

import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author jtalbut
 */
public class RequestContextBuilderTest {

  @Test
  public void testBuildRequestContextAllNulls() {
    RequestContextBuilder builder = new RequestContextBuilder(null, null, null, null, null, true, null, false, null, null, null);
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

  @Test
  void testFindAuthEndpoint_NoIdpMapWithDefaultIdp() {
    // Create mock BasicAuthConfig
    BasicAuthConfig basicAuthConfig = mock(BasicAuthConfig.class);

    // Configure mock to return null/empty IdpMap and a valid default IdP
    when(basicAuthConfig.getIdpMap()).thenReturn(null);
    when(basicAuthConfig.getDefaultIdp()).thenReturn("https://default-idp.example.com");

    // Call the method with any domain (should use default IdP)
    String result = RequestContextBuilder.findAuthEndpoint(basicAuthConfig, "example.com");

    // Verify result
    assertEquals("https://default-idp.example.com", result);
  }

  @Test
  void testFindAuthEndpoint_EmptyIdpMapWithDefaultIdp() {
    // Create mock BasicAuthConfig
    BasicAuthConfig basicAuthConfig = mock(BasicAuthConfig.class);

    // Configure mock to return empty IdpMap and a valid default IdP
    when(basicAuthConfig.getIdpMap()).thenReturn(new HashMap<>());
    when(basicAuthConfig.getDefaultIdp()).thenReturn("https://default-idp.example.com");

    // Call the method with any domain (should use default IdP)
    String result = RequestContextBuilder.findAuthEndpoint(basicAuthConfig, "example.com");

    // Verify result
    assertEquals("https://default-idp.example.com", result);
  }

  @Test
  void testFindAuthEndpoint_NoIdpMapNoDefaultIdp() {
    // Create mock BasicAuthConfig
    BasicAuthConfig basicAuthConfig = mock(BasicAuthConfig.class);

    // Configure mock to return null IdpMap and no default IdP
    when(basicAuthConfig.getIdpMap()).thenReturn(null);
    when(basicAuthConfig.getDefaultIdp()).thenReturn(null);

    // Call the method - should throw IllegalStateException
    IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
      RequestContextBuilder.findAuthEndpoint(basicAuthConfig, "example.com");
    });

    // Verify exception message
    assertEquals("No default IdP configured", exception.getMessage());
  }

  @Test
  void testFindAuthEndpoint_EmptyDomainWithDefaultIdp() {
    // Create mock BasicAuthConfig
    BasicAuthConfig basicAuthConfig = mock(BasicAuthConfig.class);

    // Create a non-empty IdpMap
    Map<String, String> idpMap = new HashMap<>();
    idpMap.put("example.com", "https://example-idp.com");

    // Configure mock to return IdpMap and default IdP
    when(basicAuthConfig.getIdpMap()).thenReturn(idpMap);
    when(basicAuthConfig.getDefaultIdp()).thenReturn("https://default-idp.example.com");

    // Call the method with empty domain (should use default IdP)
    String result = RequestContextBuilder.findAuthEndpoint(basicAuthConfig, "");

    // Verify result
    assertEquals("https://default-idp.example.com", result);
  }

  @Test
  void testFindAuthEndpoint_NullDomainWithDefaultIdp() {
    // Create mock BasicAuthConfig
    BasicAuthConfig basicAuthConfig = mock(BasicAuthConfig.class);

    // Create a non-empty IdpMap
    Map<String, String> idpMap = new HashMap<>();
    idpMap.put("example.com", "https://example-idp.com");

    // Configure mock to return IdpMap and default IdP
    when(basicAuthConfig.getIdpMap()).thenReturn(idpMap);
    when(basicAuthConfig.getDefaultIdp()).thenReturn("https://default-idp.example.com");

    // Call the method with null domain (should use default IdP)
    String result = RequestContextBuilder.findAuthEndpoint(basicAuthConfig, null);

    // Verify result
    assertEquals("https://default-idp.example.com", result);
  }

  @Test
  void testFindAuthEndpoint_EmptyDomainNoDefaultIdp() {
    // Create mock BasicAuthConfig
    BasicAuthConfig basicAuthConfig = mock(BasicAuthConfig.class);

    // Create a non-empty IdpMap
    Map<String, String> idpMap = new HashMap<>();
    idpMap.put("example.com", "https://example-idp.com");

    // Configure mock to return IdpMap but no default IdP
    when(basicAuthConfig.getIdpMap()).thenReturn(idpMap);
    when(basicAuthConfig.getDefaultIdp()).thenReturn(null);

    // Call the method with empty domain - should throw IllegalStateException
    IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
      RequestContextBuilder.findAuthEndpoint(basicAuthConfig, "");
    });

    // Verify exception message
    assertEquals("No default IdP configured for no domain", exception.getMessage());
  }

  @Test
  void testFindAuthEndpoint_MappedDomain() {
    // Create mock BasicAuthConfig
    BasicAuthConfig basicAuthConfig = mock(BasicAuthConfig.class);

    // Create a non-empty IdpMap
    Map<String, String> idpMap = new HashMap<>();
    idpMap.put("example.com", "https://example-idp.com");
    idpMap.put("other.com", "https://other-idp.com");

    // Configure mock to return IdpMap
    when(basicAuthConfig.getIdpMap()).thenReturn(idpMap);

    // Call the method with a domain that exists in the map
    String result = RequestContextBuilder.findAuthEndpoint(basicAuthConfig, "example.com");

    // Verify result
    assertEquals("https://example-idp.com", result);
  }

  @Test
  void testFindAuthEndpoint_UnmappedDomainWithDefaultIdp() {
    // Create mock BasicAuthConfig
    BasicAuthConfig basicAuthConfig = mock(BasicAuthConfig.class);

    // Create a non-empty IdpMap
    Map<String, String> idpMap = new HashMap<>();
    idpMap.put("example.com", "https://example-idp.com");

    // Configure mock to return IdpMap and default IdP
    when(basicAuthConfig.getIdpMap()).thenReturn(idpMap);
    when(basicAuthConfig.getDefaultIdp()).thenReturn("https://default-idp.example.com");

    // Call the method with domain not in the map (should use default IdP)
    String result = RequestContextBuilder.findAuthEndpoint(basicAuthConfig, "unmapped.com");

    // Verify result
    assertEquals("https://default-idp.example.com", result);
  }

  @Test
  void testFindAuthEndpoint_UnmappedDomainNoDefaultIdp() {
    // Create mock BasicAuthConfig
    BasicAuthConfig basicAuthConfig = mock(BasicAuthConfig.class);

    // Create a non-empty IdpMap
    Map<String, String> idpMap = new HashMap<>();
    idpMap.put("example.com", "https://example-idp.com");

    // Configure mock to return IdpMap but no default IdP
    when(basicAuthConfig.getIdpMap()).thenReturn(idpMap);
    when(basicAuthConfig.getDefaultIdp()).thenReturn(null);

    // Call the method with domain not in the map - should throw IllegalStateException
    IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
      RequestContextBuilder.findAuthEndpoint(basicAuthConfig, "unmapped.com");
    });

    // Verify exception message
    assertEquals("No default IdP configured and no mapped IdP configured", exception.getMessage());
  }

  @Test
  void testFindAuthEndpoint_DomainMappedToEmptyEndpoint() {
    // Create mock BasicAuthConfig
    BasicAuthConfig basicAuthConfig = mock(BasicAuthConfig.class);

    // Create a non-empty IdpMap with one domain mapped to empty string
    Map<String, String> idpMap = new HashMap<>();
    idpMap.put("example.com", "");

    // Configure mock to return IdpMap and default IdP
    when(basicAuthConfig.getIdpMap()).thenReturn(idpMap);
    when(basicAuthConfig.getDefaultIdp()).thenReturn("https://default-idp.example.com");

    // Call the method with domain mapped to empty string (should use default IdP)
    String result = RequestContextBuilder.findAuthEndpoint(basicAuthConfig, "example.com");

    // Verify result
    assertEquals("https://default-idp.example.com", result);
  }

  @Test
  void testFindAuthEndpoint_NullConfig() {
    // Call the method with null config - should throw NullPointerException
    assertThrows(NullPointerException.class, () -> {
      RequestContextBuilder.findAuthEndpoint(null, "example.com");
    });
  }
}
