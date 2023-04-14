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
package uk.co.spudsoft.query.defn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author jtalbut
 */
public class EndpointTest {
  
  @Test
  public void testValidate() {
    assertThrows(IllegalArgumentException.class, () -> {
      Endpoint.builder().password("shh").secret("secret").build().validate();
    });
    assertThrows(IllegalArgumentException.class, () -> {
      Endpoint.builder().username("bob").secret("secret").build().validate();
    });
  }
  
  @Test
  public void testGetType() {
    Endpoint instance = Endpoint.builder().type(EndpointType.SQL).build();
    assertEquals(EndpointType.SQL, instance.getType());
  }

  @Test
  public void testGetUrl() {
    Endpoint instance = Endpoint.builder().url("url").build();
    assertEquals("url", instance.getUrl());
  }

  @Test
  public void testGetUsername() {
    Endpoint instance = Endpoint.builder().username("username").build();
    assertEquals("username", instance.getUsername());
  }

  @Test
  public void testGetPassword() {
    Endpoint instance = Endpoint.builder().password("password").build();
    assertEquals("password", instance.getPassword());
  }

  @Test
  public void testGetSecret() {
    Endpoint instance = Endpoint.builder().secret("secret").build();
    assertEquals("secret", instance.getSecret());    
  }
}
