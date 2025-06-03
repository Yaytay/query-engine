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
package uk.co.spudsoft.query.main;

import io.vertx.core.json.JsonObject;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.jwtvalidatorvertx.DiscoveryData;


/**
 *
 * @author jtalbut
 */
public class AuthEndpointTest {
  
  @Test
  public void testUpdateFromOpenIdConfiguration() {
    DiscoveryData dd = new DiscoveryData(new JsonObject());
    AuthEndpoint authEndpoint = new AuthEndpoint();
    authEndpoint.updateFromOpenIdConfiguration(dd);
    assertNull(authEndpoint.getAuthorizationEndpoint());
    assertNull(authEndpoint.getTokenEndpoint());
    assertThat(authEndpoint.getInvalidDate(), not(greaterThan(LocalDateTime.now(ZoneOffset.UTC).plusDays(1))));
    assertThat(authEndpoint.getInvalidDate(), greaterThan(LocalDateTime.now(ZoneOffset.UTC).plusDays(1).minusMinutes(1)));

    dd = new DiscoveryData(new JsonObject("{\"token_endpoint\": \"toke\", \"authorization_endpoint\": \"authe\"}"));
    authEndpoint.updateFromOpenIdConfiguration(dd);
    assertEquals("authe", authEndpoint.getAuthorizationEndpoint());
    assertEquals("toke", authEndpoint.getTokenEndpoint());
    assertThat(authEndpoint.getInvalidDate(), not(greaterThan(LocalDateTime.now(ZoneOffset.UTC).plusDays(1))));
    assertThat(authEndpoint.getInvalidDate(), greaterThan(LocalDateTime.now(ZoneOffset.UTC).plusDays(1).minusMinutes(1)));
            
    dd = new DiscoveryData(new JsonObject("{\"token_endpoint\": \"toke2\", \"authorization_endpoint\": \"authe2\"}"));
    authEndpoint.updateFromOpenIdConfiguration(dd);
    assertEquals("authe2", authEndpoint.getAuthorizationEndpoint());
    assertEquals("toke2", authEndpoint.getTokenEndpoint());
    assertThat(authEndpoint.getInvalidDate(), not(greaterThan(LocalDateTime.now(ZoneOffset.UTC).plusDays(1))));
    assertThat(authEndpoint.getInvalidDate(), greaterThan(LocalDateTime.now(ZoneOffset.UTC).plusDays(1).minusMinutes(1)));
            
    dd = new DiscoveryData(new JsonObject());
    authEndpoint.updateFromOpenIdConfiguration(dd);
    assertEquals("authe2", authEndpoint.getAuthorizationEndpoint());
    assertEquals("toke2", authEndpoint.getTokenEndpoint());
    assertThat(authEndpoint.getInvalidDate(), not(greaterThan(LocalDateTime.now(ZoneOffset.UTC).plusDays(1))));
    assertThat(authEndpoint.getInvalidDate(), greaterThan(LocalDateTime.now(ZoneOffset.UTC).plusDays(1).minusMinutes(1)));
  }
  
  @Test
  public void testValidate() throws IllegalArgumentException {
    AuthEndpoint authEndpoint = new AuthEndpoint();
    assertEquals("auth.issuer and auth.authorizationEndpoint not configured", assertThrows(IllegalArgumentException.class, () -> {
      authEndpoint.validate("auth");
    }).getMessage());
    authEndpoint.setIssuer("issuer");
    assertEquals("auth.credentials not configured", assertThrows(IllegalArgumentException.class, () -> {
      authEndpoint.validate("auth");
    }).getMessage());
    authEndpoint.setCredentials(new ClientCredentials("id", "secret"));
    authEndpoint.validate("auth");
  }

  @Test
  public void testGetLogoUrl() {
    AuthEndpoint authEndpoint = new AuthEndpoint();
    assertNull(authEndpoint.getLogoUrl());
    authEndpoint.setLogoUrl("logo url");
    assertEquals("logo url", authEndpoint.getLogoUrl());
  }

  @Test
  public void testGetIssuer() {
    AuthEndpoint authEndpoint = new AuthEndpoint();
    assertNull(authEndpoint.getIssuer());
    authEndpoint.setIssuer("issuer");
    assertEquals("issuer", authEndpoint.getIssuer());
  }

  @Test
  public void testGetAuthorizationEndpoint() {
    AuthEndpoint authEndpoint = new AuthEndpoint();
    assertNull(authEndpoint.getAuthorizationEndpoint());
    authEndpoint.setAuthorizationEndpoint("auth endpoint");
    assertEquals("auth endpoint", authEndpoint.getAuthorizationEndpoint());
  }

  @Test
  public void testGetTokenEndpoint() {
    AuthEndpoint authEndpoint = new AuthEndpoint();
    assertNull(authEndpoint.getTokenEndpoint());
    authEndpoint.setTokenEndpoint("token endpoint");
    assertEquals("token endpoint", authEndpoint.getTokenEndpoint());
  }

  @Test
  public void testGetScope() {
    AuthEndpoint authEndpoint = new AuthEndpoint();
    assertNull(authEndpoint.getScope());
    authEndpoint.setScope("scope");
    assertEquals("scope", authEndpoint.getScope());
  }

  @Test
  public void testIsPkce() {
    AuthEndpoint authEndpoint = new AuthEndpoint();
    assertTrue(authEndpoint.isPkce());
    authEndpoint.setPkce(false);
    assertFalse(authEndpoint.isPkce());
  }

  @Test
  public void testIsNonce() {
    AuthEndpoint authEndpoint = new AuthEndpoint();
    assertTrue(authEndpoint.isNonce());
    authEndpoint.setNonce(false);
    assertFalse(authEndpoint.isNonce());
  }

  @Test
  public void testGetInvalidDate() {
    AuthEndpoint authEndpoint = new AuthEndpoint();
    assertNull(authEndpoint.getInvalidDate());
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    authEndpoint.setInvalidDate(now);
    assertEquals(now, authEndpoint.getInvalidDate());
  }
  
}
