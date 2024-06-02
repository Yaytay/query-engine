/*
 * Copyright (C) 2024 jtalbut
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
package uk.co.spudsoft.query.main;

import io.vertx.core.http.CookieSameSite;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author jtalbut
 */
public class CookieConfigTest {
  
  @Test
  public void testGetName() {
    CookieConfig cookie = new CookieConfig();
    assertEquals("QueryEngineSession", cookie.getName());
    cookie.setName("fred");
    assertEquals("fred", cookie.getName());
    cookie = new CookieConfig("bob");
    assertEquals("bob", cookie.getName());
  }

  @Test
  public void testIsSecure() {
    CookieConfig cookie = new CookieConfig("bob");
    assertNull(cookie.isSecure());
    cookie.setSecure(Boolean.TRUE);
    assertTrue(cookie.isSecure());
  }

  @Test
  public void testIsHttpOnly() {
    CookieConfig cookie = new CookieConfig();
    assertNull(cookie.isHttpOnly());
    cookie.setHttpOnly(Boolean.TRUE);
    assertTrue(cookie.isHttpOnly());
  }

  @Test
  public void testGetDomain() {
    CookieConfig cookie = new CookieConfig("bob");
    assertNull(cookie.getDomain());
    cookie.setDomain("fred");
    assertEquals("fred", cookie.getDomain());
  }

  @Test
  public void testGetPath() {
    CookieConfig cookie = new CookieConfig("bob");
    assertNull(cookie.getPath());
    cookie.setPath("fred");
    assertEquals("fred", cookie.getPath());
  }

  @Test
  public void testGetSameSite() {
    CookieConfig cookie = new CookieConfig("bob");
    assertNull(cookie.getSameSite());
    cookie.setSameSite(CookieSameSite.LAX);
    assertEquals(CookieSameSite.LAX, cookie.getSameSite());
  }
 
  @Test
  public void testValidate() {
    CookieConfig cookie = new CookieConfig("bob");
    cookie.validate("cookie");
    
    cookie = new CookieConfig();
    cookie.validate("cookie");
    
    IllegalArgumentException ex;
    
    ex = assertThrows(IllegalArgumentException.class, () -> {
      new CookieConfig(null).validate("cookie");
    });
    assertEquals("cookie.name not set", ex.getMessage());
    
    ex = assertThrows(IllegalArgumentException.class, () -> {
      new CookieConfig("£4.50").validate("cookie");
    });
    assertEquals("cookie.name is invalid, must match ^[-a-zA-Z0-9!#$%&'*+.^_`|~]+$, was \"£4.50\"", ex.getMessage());
  }
  
}
