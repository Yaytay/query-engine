/*
 * Copyright (C) 2023 njt
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

import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.HostAndPort;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import uk.co.spudsoft.query.main.AuthEndpoint;
import uk.co.spudsoft.query.main.CookieConfig;

/**
 *
 * @author njt
 */
public class LoginRouterTest {
  
  @Test
  public void testRedirectUri() {
    HttpServerRequest request = mock(HttpServerRequest.class);
    when(request.getHeader("X-Forwarded-Proto")).thenReturn("http");
    when(request.getHeader("X-Forwarded-Host")).thenReturn("host");
    when(request.authority()).thenReturn(HostAndPort.create("hap", 80));
    assertEquals("http://host/login/return", LoginRouter.redirectUri(request));
    
    when(request.getHeader("X-Forwarded-Port")).thenReturn("456");
    when(request.getHeader("X-Forwarded-Proto")).thenReturn("http");
    when(request.getHeader("X-Forwarded-Host")).thenReturn(null);
    when(request.authority()).thenReturn(HostAndPort.create("hap", 123));
    assertEquals("http://hap:456/login/return", LoginRouter.redirectUri(request));
    
    when(request.getHeader("X-Forwarded-Port")).thenReturn(null);
    when(request.getHeader("X-Forwarded-Proto")).thenReturn("https");
    when(request.getHeader("X-Forwarded-Host")).thenReturn(null);
    when(request.authority()).thenReturn(HostAndPort.create("hap", 443));
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
  }

  @Test
  public void testHandle() {
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
  
}
