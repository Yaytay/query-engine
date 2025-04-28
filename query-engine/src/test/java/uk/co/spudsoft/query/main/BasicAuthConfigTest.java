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
package uk.co.spudsoft.query.main;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for BasicAuthConfig.
 */
public class BasicAuthConfigTest {

  /**
   * Test default constructor and initial values.
   */
  @Test
  public void testDefaultConstructor() {
    BasicAuthConfig config = new BasicAuthConfig();
    assertEquals(BasicAuthGrantType.clientCredentials, config.getGrantType());
    assertNull(config.getDefaultIdp());
    assertTrue(config.getIdpMap().isEmpty());
  }

  /**
   * Test setters and getters.
   */
  @Test
  public void testSettersAndGetters() {
    BasicAuthConfig config = new BasicAuthConfig();

    // Test grant type
    config.setGrantType(BasicAuthGrantType.resourceOwnerPasswordCredentials);
    assertEquals(BasicAuthGrantType.resourceOwnerPasswordCredentials, config.getGrantType());

    // Test default IdP
    String defaultIdp = "https://default.auth.example.com/token";
    config.setDefaultIdp(new Endpoint(defaultIdp, null));
    assertEquals(defaultIdp, config.getDefaultIdp().getUrl());

    // Test IdP map
    ImmutableMap<String, Endpoint> idpMap = ImmutableMap.of(
      "domain1.com", new Endpoint("https://idp1.example.com/token", null),
      "domain2.com", new Endpoint("https://idp2.example.com/token", null)
    );
    config.setIdpMap(idpMap);
    assertEquals(idpMap, config.getIdpMap());
    assertEquals("https://idp1.example.com/token", config.getIdpMap().get("domain1.com").getUrl());
    assertEquals("https://idp2.example.com/token", config.getIdpMap().get("domain2.com").getUrl());
  }

  /**
   * Test with null values.
   */
  @Test
  public void testNullValues() {
    BasicAuthConfig config = new BasicAuthConfig();

    // Setting grant type to null should not throw exception
    config.setGrantType(null);
    assertNull(config.getGrantType());

    // Setting defaultIdp to null should not throw exception
    config.setDefaultIdp(null);
    assertNull(config.getDefaultIdp());

    // Setting idpMap to null should not throw exception
    // Note: Implementation might prevent this by setting empty map instead
    config.setIdpMap(null);
  }

  /**
   * Test with empty values.
   */
  @Test
  public void testEmptyValues() {
    BasicAuthConfig config = new BasicAuthConfig();

    // Test with empty default IdP
    config.setDefaultIdp(new Endpoint("", null));
    assertEquals("", config.getDefaultIdp().getUrl());

    // Test with empty IdP map
    ImmutableMap<String, Endpoint> emptyMap = ImmutableMap.of();
    config.setIdpMap(emptyMap);
    assertTrue(config.getIdpMap().isEmpty());
  }
  
  @Test
  public void testValidate() {
    BasicAuthConfig config = new BasicAuthConfig();
    assertEquals(BasicAuthGrantType.clientCredentials, config.getGrantType());
    config.validate("basicAuth");
    
    assertEquals("basicAuth.grantType is null", assertThrows(IllegalArgumentException.class, () -> {
      BasicAuthConfig config2 = new BasicAuthConfig();
      config2.setGrantType(null);
      config2.validate("basicAuth");
    }).getMessage());
    
  }
}
