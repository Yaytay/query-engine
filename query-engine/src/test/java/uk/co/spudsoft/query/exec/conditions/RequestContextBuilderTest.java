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
import uk.co.spudsoft.query.main.Endpoint;

/**
 *
 * @author jtalbut
 */
public class RequestContextBuilderTest {

  @Test
  public void testEnsureNonBlankStartsWith() {
    assertEquals("", RequestContextBuilder.ensureNonBlankStartsWith(null, "/"));
    assertEquals("", RequestContextBuilder.ensureNonBlankStartsWith("", "/"));
    assertEquals("/bob", RequestContextBuilder.ensureNonBlankStartsWith("bob", "/"));
    assertEquals("/fred", RequestContextBuilder.ensureNonBlankStartsWith("/fred", "/"));
  }
  
  @Test
  public void testBuildRequestContextAllNulls() {
    RequestContextBuilder builder = new RequestContextBuilder(null, null, null, null, null, true, null, false, null, null, null, null);
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

    when(request.authority()).thenReturn(null);
    when(request.scheme()).thenReturn("https");
    assertEquals("https://", RequestContextBuilder.baseRequestUrl(request));
  }

  @Test
  void testFindAuthEndpoint_NoIdpMapWithDefaultIdp() {
    // Create mock BasicAuthConfig
    BasicAuthConfig basicAuthConfig = mock(BasicAuthConfig.class);

    // Configure mock to return null/empty IdpMap and a valid default IdP
    when(basicAuthConfig.getIdpMap()).thenReturn(null);
    when(basicAuthConfig.getDefaultIdp()).thenReturn(new Endpoint("https://default-idp.example.com", null));

    // Call the method with any domain (should use default IdP)
    Endpoint result = RequestContextBuilder.findAuthEndpoint(basicAuthConfig, "example.com", "http://example.com", false);

    // Verify result
    assertEquals("https://default-idp.example.com", result.getUrl());
  }

  @Test
  void testFindAuthEndpoint_EmptyIdpMapWithDefaultIdp() {
    // Create mock BasicAuthConfig
    BasicAuthConfig basicAuthConfig = mock(BasicAuthConfig.class);

    // Configure mock to return empty IdpMap and a valid default IdP
    when(basicAuthConfig.getIdpMap()).thenReturn(new HashMap<>());
    when(basicAuthConfig.getDefaultIdp()).thenReturn(new Endpoint("https://default-idp.example.com", null));

    // Call the method with any domain (should use default IdP)
    Endpoint result = RequestContextBuilder.findAuthEndpoint(basicAuthConfig, "example.com", "http://example.com", false);

    // Verify result
    assertEquals("https://default-idp.example.com", result.getUrl());
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
      RequestContextBuilder.findAuthEndpoint(basicAuthConfig, "example.com", "http://example.com", false);
    });

    // Verify exception message
    assertEquals("No default IdP configured", exception.getMessage());
  }

  @Test
  void testFindAuthEndpoint_EmptyDomainWithDefaultIdp() {
    // Create mock BasicAuthConfig
    BasicAuthConfig basicAuthConfig = mock(BasicAuthConfig.class);

    // Create a non-empty IdpMap
    Map<String, Endpoint> idpMap = new HashMap<>();
    idpMap.put("example.com", new Endpoint("https://example-idp.com", null));

    // Configure mock to return IdpMap and default IdP
    when(basicAuthConfig.getIdpMap()).thenReturn(idpMap);
    when(basicAuthConfig.getDefaultIdp()).thenReturn(new Endpoint("https://default-idp.example.com", null));

    // Call the method with empty domain (should use default IdP)
    Endpoint result = RequestContextBuilder.findAuthEndpoint(basicAuthConfig, "", "http://example.com", false);

    // Verify result
    assertEquals("https://default-idp.example.com", result.getUrl());
  }

  @Test
  void testFindAuthEndpoint_NullDomainWithDefaultIdp() {
    // Create mock BasicAuthConfig
    BasicAuthConfig basicAuthConfig = mock(BasicAuthConfig.class);

    // Create a non-empty IdpMap
    Map<String, Endpoint> idpMap = new HashMap<>();
    idpMap.put("example.com", new Endpoint("https://example-idp.com", null));

    // Configure mock to return IdpMap and default IdP
    when(basicAuthConfig.getIdpMap()).thenReturn(idpMap);
    when(basicAuthConfig.getDefaultIdp()).thenReturn(new Endpoint("https://default-idp.example.com", null));

    // Call the method with null domain (should use default IdP)
    Endpoint result = RequestContextBuilder.findAuthEndpoint(basicAuthConfig, null, "http://example.com", false);

    // Verify result
    assertEquals("https://default-idp.example.com", result.getUrl());
  }

  @Test
  void testFindAuthEndpoint_EmptyDomainNoDefaultIdp() {
    // Create mock BasicAuthConfig
    BasicAuthConfig basicAuthConfig = mock(BasicAuthConfig.class);

    // Create a non-empty IdpMap
    Map<String, Endpoint> idpMap = new HashMap<>();
    idpMap.put("example.com", new Endpoint("https://example-idp.com", null));

    // Configure mock to return IdpMap but no default IdP
    when(basicAuthConfig.getIdpMap()).thenReturn(idpMap);
    when(basicAuthConfig.getDefaultIdp()).thenReturn(null);

    // Call the method with empty domain - should throw IllegalStateException
    IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
      RequestContextBuilder.findAuthEndpoint(basicAuthConfig, "", "http://example.com", false);
    });

    // Verify exception message
    assertEquals("No default IdP configured for no domain", exception.getMessage());
  }

  @Test
  void testFindAuthEndpoint_MappedDomain() {
    // Create mock BasicAuthConfig
    BasicAuthConfig basicAuthConfig = mock(BasicAuthConfig.class);

    // Create a non-empty IdpMap
    Map<String, Endpoint> idpMap = new HashMap<>();
    idpMap.put("example.com", new Endpoint("https://example-idp.com", null));
    idpMap.put("other.com", new Endpoint("https://other-idp.com", null));

    // Configure mock to return IdpMap
    when(basicAuthConfig.getIdpMap()).thenReturn(idpMap);

    // Call the method with a domain that exists in the map
    Endpoint result = RequestContextBuilder.findAuthEndpoint(basicAuthConfig, "example.com", "http://example.com", false);

    // Verify result
    assertEquals("https://example-idp.com", result.getUrl());
  }

  @Test
  void testFindAuthEndpoint_UnmappedDomainWithDefaultIdp() {
    // Create mock BasicAuthConfig
    BasicAuthConfig basicAuthConfig = mock(BasicAuthConfig.class);

    // Create a non-empty IdpMap
    Map<String, Endpoint> idpMap = new HashMap<>();
    idpMap.put("example.com", new Endpoint("https://example-idp.com", null));

    // Configure mock to return IdpMap and default IdP
    when(basicAuthConfig.getIdpMap()).thenReturn(idpMap);
    when(basicAuthConfig.getDefaultIdp()).thenReturn(new Endpoint("https://default-idp.example.com", null));

    // Call the method with domain not in the map (should use default IdP)
    Endpoint result = RequestContextBuilder.findAuthEndpoint(basicAuthConfig, "unmapped.com", "http://example.com", false);

    // Verify result
    assertEquals("https://default-idp.example.com", result.getUrl());
  }

  @Test
  void testFindAuthEndpoint_UnmappedDomainNoDefaultIdp() {
    // Create mock BasicAuthConfig
    BasicAuthConfig basicAuthConfig = mock(BasicAuthConfig.class);

    // Create a non-empty IdpMap
    Map<String, Endpoint> idpMap = new HashMap<>();
    idpMap.put("example.com", new Endpoint("https://example-idp.com", null));

    // Configure mock to return IdpMap but no default IdP
    when(basicAuthConfig.getIdpMap()).thenReturn(idpMap);
    when(basicAuthConfig.getDefaultIdp()).thenReturn(null);

    // Call the method with domain not in the map - should throw IllegalStateException
    IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
      RequestContextBuilder.findAuthEndpoint(basicAuthConfig, "unmapped.com", "http://example.com", false);
    });

    // Verify exception message
    assertEquals("No default IdP configured and no mapped IdP configured", exception.getMessage());
  }

  @Test
  void testFindAuthEndpoint_DomainMappedToEmptyEndpoint() {
    // Create mock BasicAuthConfig
    BasicAuthConfig basicAuthConfig = mock(BasicAuthConfig.class);

    // Create a non-empty IdpMap with one domain mapped to empty string
    Map<String, Endpoint> idpMap = new HashMap<>();
    idpMap.put("example.com", new Endpoint("", null));

    // Configure mock to return IdpMap and default IdP
    when(basicAuthConfig.getIdpMap()).thenReturn(idpMap);
    when(basicAuthConfig.getDefaultIdp()).thenReturn(new Endpoint("https://default-idp.example.com", null));

    // Call the method with domain mapped to empty string (should use default IdP)
    Endpoint result = RequestContextBuilder.findAuthEndpoint(basicAuthConfig, "example.com", "http://example.com", false);

    // Verify result
    assertEquals("https://default-idp.example.com", result.getUrl());
  }

  @Test
  void testFindAuthEndpoint_AuthPathAllSlashes() {
    // Create mock BasicAuthConfig
    BasicAuthConfig basicAuthConfig = mock(BasicAuthConfig.class);

    // Configure mock to return IdpMap and default IdP
    when(basicAuthConfig.getAuthorizationPath()).thenReturn("/auth/token");

    // Call the method with domain mapped to empty string (should use default IdP)
    Endpoint result = RequestContextBuilder.findAuthEndpoint(basicAuthConfig, "example.com", "https://example.com/", false);

    // Verify result
    assertEquals("https://example.com/auth/token", result.getUrl());
  }

  @Test
  void testFindAuthEndpoint_AuthPathSlashOnUrl() {
    // Create mock BasicAuthConfig
    BasicAuthConfig basicAuthConfig = mock(BasicAuthConfig.class);

    // Configure mock to return IdpMap and default IdP
    when(basicAuthConfig.getAuthorizationPath()).thenReturn("auth/token");

    // Call the method with domain mapped to empty string (should use default IdP)
    Endpoint result = RequestContextBuilder.findAuthEndpoint(basicAuthConfig, "example.com", "https://example.com/", false);

    // Verify result
    assertEquals("https://example.com/auth/token", result.getUrl());
  }

  @Test
  void testFindAuthEndpoint_AuthPathNoSlashes() {
    // Create mock BasicAuthConfig
    BasicAuthConfig basicAuthConfig = mock(BasicAuthConfig.class);

    // Configure mock to return IdpMap and default IdP
    when(basicAuthConfig.getAuthorizationPath()).thenReturn("auth/token");

    // Call the method with domain mapped to empty string (should use default IdP)
    Endpoint result = RequestContextBuilder.findAuthEndpoint(basicAuthConfig, "example.com", "https://example.com", false);

    // Verify result
    assertEquals("https://example.com/auth/token", result.getUrl());
  }


  @Test
  void testFindAuthEndpoint_AuthPathSlashOnPath() {
    // Create mock BasicAuthConfig
    BasicAuthConfig basicAuthConfig = mock(BasicAuthConfig.class);

    // Configure mock to return IdpMap and default IdP
    when(basicAuthConfig.getAuthorizationPath()).thenReturn("/auth/token");

    // Call the method with domain mapped to empty string (should use default IdP)
    Endpoint result = RequestContextBuilder.findAuthEndpoint(basicAuthConfig, "example.com", "https://example.com", false);

    // Verify result
    assertEquals("https://example.com/auth/token", result.getUrl());
  }

  @Test
  void testFindAuthEndpoint_NullConfig() {
    // Call the method with null config - should throw NullPointerException
    assertThrows(NullPointerException.class, () -> {
      RequestContextBuilder.findAuthEndpoint(null, "example.com", "http://example.com", false);
    });
  }
}
