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
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

/**
 *
 * @author njt
 */
public class SecurityHeadersConfigTest {
  
  @Test
  public void testGetXFrameOptions() {
    SecurityHeadersConfig instance = new SecurityHeadersConfig();
    assertNull(instance.getXFrameOptions());
    instance.setXFrameOptions("SAMEORIGIN");
    assertEquals("SAMEORIGIN", instance.getXFrameOptions());
  }

  @Test
  public void testGetReferrerPolicy() {
    SecurityHeadersConfig instance = new SecurityHeadersConfig();
    assertNull(instance.getReferrerPolicy());
    instance.setReferrerPolicy("no-referrer");
    assertEquals("no-referrer", instance.getReferrerPolicy());
  }

  @Test
  public void testGetPermissionsPolicy() {
    SecurityHeadersConfig instance = new SecurityHeadersConfig();
    assertNull(instance.getPermissionsPolicy());
    instance.setPermissionsPolicy("camera=()");
    assertEquals("camera=()", instance.getPermissionsPolicy());
  }

}
