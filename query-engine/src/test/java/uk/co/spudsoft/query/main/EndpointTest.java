/*
 * Copyright (C) 2025 njt
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author jtalbut
 */
public class EndpointTest {
  
  @Test
  public void testToString() {
    
    assertEquals("https://bob@test.com", new Endpoint("https://test.com", new Credentials("bob", "secret")).toString());
    assertEquals("http://bob@test.com", new Endpoint("http://test.com", new Credentials("bob", "secret")).toString());
    assertEquals("bob@file://test", new Endpoint("file://test", new Credentials("bob", "secret")).toString());
    assertEquals("https://test.com", new Endpoint("https://test.com", null).toString());
    assertEquals(null, new Endpoint(null, new Credentials("bob", "secret")).toString());
    
  }
  
}
