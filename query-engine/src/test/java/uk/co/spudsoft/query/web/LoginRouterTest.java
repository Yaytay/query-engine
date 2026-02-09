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
package uk.co.spudsoft.query.web;

import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.HostAndPort;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.main.AuthEndpoint;
import uk.co.spudsoft.query.main.CookieConfig;

/**
 *
 * @author jtalbut
 */
public class LoginRouterTest {

  private static final Logger logger = LoggerFactory.getLogger(LoginRouterTest.class);

  @Test
  public void testRedirectUri() {
    HttpServerRequest request = mock(HttpServerRequest.class);
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add("X-Forwarded-Proto", "http");
    headers.add("X-Forwarded-Host", "host");
    when(request.authority()).thenReturn(HostAndPort.create("hap", 80));
    when(request.scheme()).thenReturn("http");
    when(request.headers()).thenReturn(headers);
    assertEquals("http://host/login/return", LoginRouter.redirectUri(request));

    headers = MultiMap.caseInsensitiveMultiMap();
    headers.add("X-Forwarded-Port", "456");
    headers.add("X-Forwarded-Proto", "http");
    when(request.authority()).thenReturn(HostAndPort.create("hap", 123));
    when(request.scheme()).thenReturn("http");
    when(request.headers()).thenReturn(headers);
    assertEquals("http://hap:456/login/return", LoginRouter.redirectUri(request));

    headers = MultiMap.caseInsensitiveMultiMap();
    headers.add("X-Forwarded-Proto", "https");
    when(request.authority()).thenReturn(HostAndPort.create("hap", 443));
    when(request.scheme()).thenReturn("http");
    when(request.headers()).thenReturn(headers);
    assertEquals("https://hap/login/return", LoginRouter.redirectUri(request));
  }

  @Test
  public void testShouldDiscover() {
    AuthEndpoint authEndpoint = new AuthEndpoint();
    assertFalse(LoginRouter.shouldDiscover(authEndpoint));
    authEndpoint.setIssuer("issuer");
    assertTrue(LoginRouter.shouldDiscover(authEndpoint));
    authEndpoint.setAuthorizationEndpoint("auth");
    assertTrue(LoginRouter.shouldDiscover(authEndpoint));
    authEndpoint.setTokenEndpoint("token");
    assertTrue(LoginRouter.shouldDiscover(authEndpoint));
    authEndpoint.setInvalidDate(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
    assertTrue(LoginRouter.shouldDiscover(authEndpoint));
    authEndpoint.setInvalidDate(LocalDateTime.now(ZoneOffset.UTC).plusSeconds(10));
    assertFalse(LoginRouter.shouldDiscover(authEndpoint));
  }

  @Test
  public void testCreateCodeChallenge() {
    for (int i = 1; i < 200; ++i) {
      String codeVerifier = LoginRouter.randomString(i);
      String challenge = LoginRouter.createCodeChallenge(codeVerifier);
      logger.debug("{}: {} => {}", i, codeVerifier.length(), challenge.length());
    }
    assertEquals(assertThrows(IllegalArgumentException.class, () -> {
      LoginRouter.randomString(0);
    }).getMessage(), "Length must be positive");

  }

  @Test
  public void testCreateCookie() {
    CookieConfig nameOnly = new CookieConfig("fred");
    Cookie cookie = LoginRouter.createCookie(nameOnly, 10, true, "domain", "value");
    assertEquals("fred", cookie.getName());
    assertEquals("/", cookie.getPath());
    assertEquals(10L, cookie.getMaxAge());
    assertEquals(false, cookie.isHttpOnly());
    assertEquals(true, cookie.isSecure());
    assertEquals("domain", cookie.getDomain());
    assertEquals(null, cookie.getSameSite());
    assertEquals("value", cookie.getValue());

    CookieConfig path = new CookieConfig("fred");
    path.setPath("/bob");
    cookie = LoginRouter.createCookie(path, 11, false, "domain", "value");
    assertEquals("fred", cookie.getName());
    assertEquals("/bob", cookie.getPath());
    assertEquals(11L, cookie.getMaxAge());
    assertEquals(false, cookie.isHttpOnly());
    assertEquals(false, cookie.isSecure());
    assertEquals("domain", cookie.getDomain());
    assertEquals(null, cookie.getSameSite());
    assertEquals("value", cookie.getValue());

    CookieConfig secure = new CookieConfig("fred");
    secure.setSecure(Boolean.FALSE);
    cookie = LoginRouter.createCookie(secure, 10, true, "domain", "value");
    assertEquals("fred", cookie.getName());
    assertEquals("/", cookie.getPath());
    assertEquals(10L, cookie.getMaxAge());
    assertEquals(false, cookie.isHttpOnly());
    assertEquals(false, cookie.isSecure());
    assertEquals("domain", cookie.getDomain());
    assertEquals(null, cookie.getSameSite());
    assertEquals("value", cookie.getValue());

    CookieConfig http = new CookieConfig("fred");
    http.setHttpOnly(Boolean.TRUE);
    cookie = LoginRouter.createCookie(http, 10, true, "domain", "value");
    assertEquals("fred", cookie.getName());
    assertEquals("/", cookie.getPath());
    assertEquals(10L, cookie.getMaxAge());
    assertEquals(true, cookie.isHttpOnly());
    assertEquals(true, cookie.isSecure());
    assertEquals("domain", cookie.getDomain());
    assertEquals(null, cookie.getSameSite());
    assertEquals("value", cookie.getValue());

    CookieConfig domain = new CookieConfig("fred");
    domain.setDomain("other");
    cookie = LoginRouter.createCookie(domain, 10, true, "domain", "value");
    assertEquals("fred", cookie.getName());
    assertEquals("/", cookie.getPath());
    assertEquals(10L, cookie.getMaxAge());
    assertEquals(false, cookie.isHttpOnly());
    assertEquals(true, cookie.isSecure());
    assertEquals("other", cookie.getDomain());
    assertEquals(null, cookie.getSameSite());
    assertEquals("value", cookie.getValue());

    CookieConfig site = new CookieConfig("fred");
    site.setSameSite(CookieSameSite.STRICT);
    cookie = LoginRouter.createCookie(site, 10, true, "domain", "value");
    assertEquals("fred", cookie.getName());
    assertEquals("/", cookie.getPath());
    assertEquals(10L, cookie.getMaxAge());
    assertEquals(false, cookie.isHttpOnly());
    assertEquals(true, cookie.isSecure());
    assertEquals("domain", cookie.getDomain());
    assertEquals(CookieSameSite.STRICT, cookie.getSameSite());
    assertEquals("value", cookie.getValue());

  }

  @Test
  public void testNoOAuth() {
    assertThat(LoginRouter.deepCopyAuthEndpoints(null), notNullValue());
  }

  @Test
  void testDomain_withForwardedHost() {
    HttpServerRequest request = mock(HttpServerRequest.class);
    when(request.getHeader("X-Forwarded-Host")).thenReturn("forwarded.example.com");

    String result = LoginRouter.domain(request);

    assertEquals("forwarded.example.com", result);
    verify(request, never()).authority(); // Should not call authority when header exists
  }

  @Test
  void testDomain_withoutForwardedHost() {
    HttpServerRequest request = mock(HttpServerRequest.class);
    HostAndPort authority = mock(HostAndPort.class);
    when(request.getHeader("X-Forwarded-Host")).thenReturn(null);
    when(request.authority()).thenReturn(authority);
    when(authority.host()).thenReturn("direct.example.com");

    String result = LoginRouter.domain(request);

    assertEquals("direct.example.com", result);
  }

  @Test
  void testDomain_withEmptyForwardedHost() {
    HttpServerRequest request = mock(HttpServerRequest.class);
    HostAndPort authority = mock(HostAndPort.class);
    when(request.getHeader("X-Forwarded-Host")).thenReturn("");
    when(request.authority()).thenReturn(authority);
    when(authority.host()).thenReturn("direct.example.com");

    String result = LoginRouter.domain(request);

    assertEquals("direct.example.com", result);
  }

  @Test
  void testWasTls_withHttpsForwardedProto() {
    HttpServerRequest request = mock(HttpServerRequest.class);
    when(request.getHeader("X-Forwarded-Proto")).thenReturn("https");

    boolean result = LoginRouter.wasTls(request);

    assertTrue(result);
    verify(request, never()).isSSL(); // Should not call isSSL when header exists
  }

  @Test
  void testWasTls_withHttpForwardedProto() {
    HttpServerRequest request = mock(HttpServerRequest.class);
    when(request.getHeader("X-Forwarded-Proto")).thenReturn("http");

    boolean result = LoginRouter.wasTls(request);

    assertFalse(result);
    verify(request, never()).isSSL();
  }

  @Test
  void testWasTls_withoutForwardedProto_sslTrue() {
    HttpServerRequest request = mock(HttpServerRequest.class);
    when(request.getHeader("X-Forwarded-Proto")).thenReturn(null);
    when(request.isSSL()).thenReturn(true);

    boolean result = LoginRouter.wasTls(request);

    assertTrue(result);
  }

  @Test
  void testWasTls_withoutForwardedProto_sslFalse() {
    HttpServerRequest request = mock(HttpServerRequest.class);
    when(request.getHeader("X-Forwarded-Proto")).thenReturn(null);
    when(request.isSSL()).thenReturn(false);

    boolean result = LoginRouter.wasTls(request);

    assertFalse(result);
  }

  @Test
  void testWasTls_withEmptyForwardedProto() {
    HttpServerRequest request = mock(HttpServerRequest.class);
    when(request.getHeader("X-Forwarded-Proto")).thenReturn("");
    when(request.isSSL()).thenReturn(true);

    boolean result = LoginRouter.wasTls(request);

    assertTrue(result);
  }

  // ---------------------------------------------------------------------------
  // Additional coverage for LoginRouter utilities
  // ---------------------------------------------------------------------------

  @Test
  void testLoggedOutUri_usesForwardedHeaders() {
    HttpServerRequest request = mock(HttpServerRequest.class);
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add("X-Forwarded-Proto", "https");
    headers.add("X-Forwarded-Host", "example.com");
    when(request.authority()).thenReturn(HostAndPort.create("internal-host", 8080));
    when(request.scheme()).thenReturn("http");
    when(request.headers()).thenReturn(headers);

    assertEquals("https://example.com/", LoginRouter.loggedOutUri(request));
  }

  @Test
  void testLoggedOutUri_usesForwardedPort() {
    HttpServerRequest request = mock(HttpServerRequest.class);
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add("X-Forwarded-Proto", "http");
    headers.add("X-Forwarded-Port", "1234");
    when(request.authority()).thenReturn(HostAndPort.create("hap", 80));
    when(request.scheme()).thenReturn("http");
    when(request.headers()).thenReturn(headers);

    assertEquals("http://hap:1234/", LoginRouter.loggedOutUri(request));
  }

  @Test
  void testDeepCopyAuthEndpoints_createsNewObjects() {
    AuthEndpoint original = new AuthEndpoint();
    original.setIssuer("issuer");
    original.setAuthorizationEndpoint("auth");
    original.setTokenEndpoint("token");

    Map<String, AuthEndpoint> input = new HashMap<>();
    input.put("p1", original);

    var copied = LoginRouter.deepCopyAuthEndpoints(input);

    assertEquals(1, copied.size());
    assertTrue(copied.containsKey("p1"));
    assertNotSame(original, copied.get("p1"));

    // Sanity-check a couple of fields were copied (and not nulled)
    assertEquals("issuer", copied.get("p1").getIssuer());
    assertEquals("auth", copied.get("p1").getAuthorizationEndpoint());
    assertEquals("token", copied.get("p1").getTokenEndpoint());
  }

  @Test
  void testRandomString_exactLength_andUrlSafeAlphabet() {
    for (int len = 1; len < 256; len += 7) {
      String s = LoginRouter.randomString(len);
      assertEquals(len, s.length());

      // URL-safe Base64 without padding should not contain '+', '/', '='
      assertFalse(s.contains("+"), "randomString should be URL-safe Base64");
      assertFalse(s.contains("/"), "randomString should be URL-safe Base64");
      assertFalse(s.contains("="), "randomString should be unpadded");
    }
  }

  @Test
  void testCreateRandomSessionId_length100() {
    String id = LoginRouter.createRandomSessionId();
    assertEquals(100, id.length());
  }

  @Test
  void testCreateCodeChallenge_knownVector() {
    // PKCE S256 for "abc" (SHA-256 then base64url without padding)
    // SHA-256("abc") = ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad
    // base64url = ungWv48Bz-pBQUDeXa4iI7ADYaOWF3qctBD_YfIAFa0
    assertEquals("ungWv48Bz-pBQUDeXa4iI7ADYaOWF3qctBD_YfIAFa0", LoginRouter.createCodeChallenge("abc"));
  }

  @Test
  void testGetFastestSecureRandom_returnsWorkingInstance() {
    SecureRandom sr = LoginRouter.getFastestSecureRandom();
    assertThat(sr, notNullValue());

    byte[] bytes = new byte[32];
    sr.nextBytes(bytes);

    // Extremely weak, but good enough to prove bytes were produced and code path executed
    boolean anyNonZero = false;
    for (byte b : bytes) {
      if (b != 0) {
        anyNonZero = true;
        break;
      }
    }
    assertTrue(anyNonZero);
  }
}
