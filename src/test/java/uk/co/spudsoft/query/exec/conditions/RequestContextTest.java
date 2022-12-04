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
import io.vertx.core.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.co.spudsoft.query.exec.conditions.ConditionInstanceTest.params;


/**
 *
 * @author jtalbut
 */
public class RequestContextTest {
  
  @Test
  public void testAttemptBase64Decode() {
    assertEquals("Hello", RequestContext.attemptBase64Decode("SGVsbG8="));
    assertEquals("SGVsG8=", RequestContext.attemptBase64Decode("SGVsG8="));
  }

  @Test
  public void testExtractRemoteIp() {
    assertNull(RequestContext.extractRemoteIp(null));

    HttpServerRequest request = mock(HttpServerRequest.class);
    when(request.getHeader("X-Cluster-Client-IP")).thenReturn("111.122.133.144");
    assertEquals(new IPAddressString("111.122.133.144"), RequestContext.extractRemoteIp(request));
    
    request = mock(HttpServerRequest.class);
    when(request.getHeader("X-Forwarded-For")).thenReturn("111.122.133.144");
    assertEquals(new IPAddressString("111.122.133.144"), RequestContext.extractRemoteIp(request));

    request = mock(HttpServerRequest.class);
    when(request.getHeader("X-Forwarded-For")).thenReturn("111.122.133.144, 122.133.144.155");
    assertEquals(new IPAddressString("111.122.133.144"), RequestContext.extractRemoteIp(request));

    request = mock(HttpServerRequest.class);
    when(request.remoteAddress()).thenReturn(SocketAddress.inetSocketAddress(80, "111.122.133.144"));
    assertEquals(new IPAddressString("111.122.133.144"), RequestContext.extractRemoteIp(request));

    request = mock(HttpServerRequest.class);
    assertNull(RequestContext.extractRemoteIp(request));
  }
  
  @Test
  public void testExtractHost() {
    assertNull(RequestContext.extractHost(null));

    HttpServerRequest request = mock(HttpServerRequest.class);
    when(request.getHeader("X-Forwarded-Host")).thenReturn("bob");
    assertEquals("bob", RequestContext.extractHost(request));

    request = mock(HttpServerRequest.class);
    when(request.host()).thenReturn("bob:1234");
    assertEquals("bob", RequestContext.extractHost(request));    
  }

  private static final String OPENID = Base64.getEncoder().encodeToString("{\"jti\":\"a28849b9-3624-42c3-aaad-21c5f80ffc55\",\"exp\":1653142100,\"nbf\":0,\"iat\":1653142040,\"iss\":\"http://ca.localtest.me\",\"aud\":\"security-admin-console\",\"sub\":\"af78202f-b54a-439d-913c-0bbe99ba6bf8\",\"typ\":\"Bearer\",\"azp\":\"QE2\",\"scope\":\"openid profile email qe2\",\"email_verified\":false,\"name\":\"Bob Fred\",\"preferred_username\":\"bob.fred\",\"given_name\":\"Bob\",\"family_name\":\"Fred\",\"email\":\"bob@localtest.me\",\"groups\":[\"group1\",\"group2\",\"group3\"]}".getBytes(StandardCharsets.UTF_8));
  
  @Test
  public void testGetUser() {
    HttpServerRequest req = mock(HttpServerRequest.class);
    when(req.getHeader("X-Forwarded-For")).thenReturn("111.122.133.144");
    when(req.getHeader("X-OpenID-Introspection")).thenReturn(OPENID);
    when(req.params()).thenReturn(params("http://bob/fred?param1=value1&param2=value2&param1=value3"));
    
    RequestContextBuilder rcb = new RequestContextBuilder(null, null, null, "X-OpenID-Introspection", "aud");
    RequestContext ctx = rcb.buildRequestContext(req).result();

    assertEquals("bob.fred", ctx.getJwt().getClaim("preferred_username"));
    assertEquals("{\"clientIp\":\"111.122.133.144\", \"arguments\":{\"param1\":\"value1\", \"param1\":\"value3\", \"param2\":\"value2\"}, \"iss\":\"http://ca.localtest.me\", \"sub\":\"af78202f-b54a-439d-913c-0bbe99ba6bf8\"}", ctx.toString());
    assertEquals("value1", ctx.getArguments().get("param1"));
    assertEquals("value2", ctx.getArguments().get("param2"));
    assertEquals(Arrays.asList("value1", "value3"), ctx.getArguments().getAll("param1"));
  }
  
  @Test
  public void textGetAud() {
    HttpServerRequest req = mock(HttpServerRequest.class);
    when(req.getHeader("X-Forwarded-For")).thenReturn("111.122.133.144");
    when(req.getHeader("X-OpenID-Introspection")).thenReturn(OPENID);
    when(req.params()).thenReturn(params("http://bob/fred?param1=value1&param2=value2&param1=value3"));
    
    RequestContextBuilder rcb = new RequestContextBuilder(null, null, null, "X-OpenID-Introspection", "aud");
    RequestContext ctx = rcb.buildRequestContext(req).result();

    assertEquals("security-admin-console", ctx.getJwt().getClaim("aud"));
    assertEquals(Arrays.asList("security-admin-console"), ctx.getJwt().getAudience());
  }

  @Test
  public void testGetClientIp() {
    HttpServerRequest request = mock(HttpServerRequest.class);
    when(request.getHeader("X-Forwarded-For")).thenReturn("111.122.133.144");
    RequestContext ctx = new RequestContext(request, null);
    assertEquals(new IPAddressString("111.122.133.144"), ctx.getClientIp());

    assertEquals("{\"clientIp\":\"111.122.133.144\"}", ctx.toString());
  }

  @Test
  public void testGetArguments() {
    HttpServerRequest request = mock(HttpServerRequest.class);
    when(request.getHeader("X-Forwarded-For")).thenReturn("111.122.133.144");
    RequestContext ctx = new RequestContext(request, null);
    assertEquals(new IPAddressString("111.122.133.144"), ctx.getClientIp());

    assertEquals("{\"clientIp\":\"111.122.133.144\"}", ctx.toString());
  }
  
  @Test
  public void testClientIpIsInV4() {
    HttpServerRequest request = mock(HttpServerRequest.class);
    when(request.getHeader("X-Cluster-Client-IP")).thenReturn("111.122.133.144");
    RequestContext requestContext = new RequestContext(request, null);
    assertEquals(new IPAddressString("111.122.133.144"), requestContext.getClientIp());
    assertTrue(requestContext.clientIpIsIn("111.122.133.144"));
    assertFalse(requestContext.clientIpIsIn("111.122.133.145"));
    assertFalse(requestContext.clientIpIsIn("111.122.133.143/31"));
    assertTrue(requestContext.clientIpIsIn("111.122.133.145/31"));
    assertTrue(requestContext.clientIpIsIn("111.122.133.144/32"));
    assertFalse(requestContext.clientIpIsIn("111.122.133.144/58"));
    assertTrue(requestContext.clientIpIsIn("0.0.0.0/0"));
    assertFalse(requestContext.clientIpIsIn("xxxxxxxxxxx"));
  }
  
  @Test
  public void testClientIpIsInV6() {
    HttpServerRequest request = mock(HttpServerRequest.class);
    when(request.getHeader("X-Cluster-Client-IP")).thenReturn("fe80::14e0:18c7:e093:f8dd%18");
    RequestContext requestContext = new RequestContext(request, null);
    assertEquals(new IPAddressString("fe80::14e0:18c7:e093:f8dd%18"), requestContext.getClientIp());
    assertTrue(requestContext.clientIpIsIn("fe80::14e0:18c7:e093:f8dd%18"));
    assertTrue(requestContext.clientIpIsIn("fe80::14e0:18c7:e093:f8dd/128"));
    assertTrue(requestContext.clientIpIsIn("fe80::14e0:18c7:e093:f8dc/127"));
    assertFalse(requestContext.clientIpIsIn("fe80::14e0:18c7:e093:f8dc/456"));
    assertTrue(requestContext.clientIpIsIn("0::0/0"));
  }

  @Test
  public void testGetName() {
    HttpServerRequest req = mock(HttpServerRequest.class);
    when(req.getHeader("X-Forwarded-For")).thenReturn("111.122.133.144");
    when(req.getHeader("X-OpenID-Introspection")).thenReturn(OPENID);
    when(req.params()).thenReturn(params("http://bob/fred?param1=value1&param2=value2&param1=value3"));
    
    RequestContextBuilder rcb = new RequestContextBuilder(null, null, null, "X-OpenID-Introspection", "aud");
    RequestContext ctx = rcb.buildRequestContext(req).result();

    assertEquals("Bob Fred", ctx.getNameFromJwt());
  }

  private static final String OPENID_GIVENNAME_FAMILYNAME = Base64.getEncoder().encodeToString("{\"jti\":\"a28849b9-3624-42c3-aaad-21c5f80ffc55\",\"exp\":1653142100,\"nbf\":0,\"iat\":1653142040,\"iss\":\"http://ca.localtest.me\",\"aud\":\"security-admin-console\",\"sub\":\"af78202f-b54a-439d-913c-0bbe99ba6bf8\",\"typ\":\"Bearer\",\"azp\":\"QE2\",\"scope\":\"openid profile email qe2\",\"email_verified\":false,\"preferred_username\":\"bob.fred\",\"given_name\":\"Bob\",\"family_name\":\"Fred\",\"email\":\"bob@localtest.me\",\"groups\":[\"group1\",\"group2\",\"group3\"]}".getBytes(StandardCharsets.UTF_8));
  
  @Test
  public void testGetNameGivenNameAndFamilyName() {
    HttpServerRequest req = mock(HttpServerRequest.class);
    when(req.getHeader("X-Forwarded-For")).thenReturn("111.122.133.144");
    when(req.getHeader("X-OpenID-Introspection")).thenReturn(OPENID_GIVENNAME_FAMILYNAME);
    when(req.params()).thenReturn(params("http://bob/fred?param1=value1&param2=value2&param1=value3"));
    
    RequestContextBuilder rcb = new RequestContextBuilder(null, null, null, "X-OpenID-Introspection", "aud");
    RequestContext ctx = rcb.buildRequestContext(req).result();

    assertEquals("Bob Fred", ctx.getNameFromJwt());
  }

  private static final String OPENID_GIVENNAME = Base64.getEncoder().encodeToString("{\"jti\":\"a28849b9-3624-42c3-aaad-21c5f80ffc55\",\"exp\":1653142100,\"nbf\":0,\"iat\":1653142040,\"iss\":\"http://ca.localtest.me\",\"aud\":\"security-admin-console\",\"sub\":\"af78202f-b54a-439d-913c-0bbe99ba6bf8\",\"typ\":\"Bearer\",\"azp\":\"QE2\",\"scope\":\"openid profile email qe2\",\"email_verified\":false,\"preferred_username\":\"bob.fred\",\"given_name\":\"Bob\",\"email\":\"bob@localtest.me\",\"groups\":[\"group1\",\"group2\",\"group3\"]}".getBytes(StandardCharsets.UTF_8));
  
  @Test
  public void testGetNameGivenName() {
    HttpServerRequest req = mock(HttpServerRequest.class);
    when(req.getHeader("X-Forwarded-For")).thenReturn("111.122.133.144");
    when(req.getHeader("X-OpenID-Introspection")).thenReturn(OPENID_GIVENNAME);
    when(req.params()).thenReturn(params("http://bob/fred?param1=value1&param2=value2&param1=value3"));
    
    RequestContextBuilder rcb = new RequestContextBuilder(null, null, null, "X-OpenID-Introspection", "aud");
    RequestContext ctx = rcb.buildRequestContext(req).result();

    assertEquals("Bob", ctx.getNameFromJwt());
  }

  private static final String OPENID_FAMILYNAME = Base64.getEncoder().encodeToString("{\"jti\":\"a28849b9-3624-42c3-aaad-21c5f80ffc55\",\"exp\":1653142100,\"nbf\":0,\"iat\":1653142040,\"iss\":\"http://ca.localtest.me\",\"aud\":\"security-admin-console\",\"sub\":\"af78202f-b54a-439d-913c-0bbe99ba6bf8\",\"typ\":\"Bearer\",\"azp\":\"QE2\",\"scope\":\"openid profile email qe2\",\"email_verified\":false,\"preferred_username\":\"bob.fred\",\"family_name\":\"Fred\",\"email\":\"bob@localtest.me\",\"groups\":[\"group1\",\"group2\",\"group3\"]}".getBytes(StandardCharsets.UTF_8));
  
  @Test
  public void testGetNameFamilyName() {
    HttpServerRequest req = mock(HttpServerRequest.class);
    when(req.getHeader("X-Forwarded-For")).thenReturn("111.122.133.144");
    when(req.getHeader("X-OpenID-Introspection")).thenReturn(OPENID_FAMILYNAME);
    when(req.params()).thenReturn(params("http://bob/fred?param1=value1&param2=value2&param1=value3"));
    
    RequestContextBuilder rcb = new RequestContextBuilder(null, null, null, "X-OpenID-Introspection", "aud");
    RequestContext ctx = rcb.buildRequestContext(req).result();

    assertEquals("Fred", ctx.getNameFromJwt());
  }

  private static final String OPENID_PREFERREDUSERNAME = Base64.getEncoder().encodeToString("{\"jti\":\"a28849b9-3624-42c3-aaad-21c5f80ffc55\",\"exp\":1653142100,\"nbf\":0,\"iat\":1653142040,\"iss\":\"http://ca.localtest.me\",\"aud\":\"security-admin-console\",\"sub\":\"af78202f-b54a-439d-913c-0bbe99ba6bf8\",\"typ\":\"Bearer\",\"azp\":\"QE2\",\"scope\":\"openid profile email qe2\",\"email_verified\":false,\"preferred_username\":\"bob.fred\",\"email\":\"bob@localtest.me\",\"groups\":[\"group1\",\"group2\",\"group3\"]}".getBytes(StandardCharsets.UTF_8));
  
  @Test
  public void testGetNamePreferredUsername() {
    HttpServerRequest req = mock(HttpServerRequest.class);
    when(req.getHeader("X-Forwarded-For")).thenReturn("111.122.133.144");
    when(req.getHeader("X-OpenID-Introspection")).thenReturn(OPENID_PREFERREDUSERNAME);
    when(req.params()).thenReturn(params("http://bob/fred?param1=value1&param2=value2&param1=value3"));
    
    RequestContextBuilder rcb = new RequestContextBuilder(null, null, null, "X-OpenID-Introspection", "aud");
    RequestContext ctx = rcb.buildRequestContext(req).result();

    assertEquals("bob.fred", ctx.getNameFromJwt());
  }
  
  
}
