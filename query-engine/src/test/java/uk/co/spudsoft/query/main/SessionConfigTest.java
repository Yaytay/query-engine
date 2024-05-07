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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author jtalbut
 */
public class SessionConfigTest {
  
  @Test
  public void testIsRequireSession() {
    SessionConfig sessionConfig = new SessionConfig();
    assertFalse(sessionConfig.isRequireSession());
    sessionConfig.setRequireSession(true);
    assertTrue(sessionConfig.isRequireSession());
  }

  @Test
  public void testGetStateLength() {
    SessionConfig sessionConfig = new SessionConfig();
    assertEquals(120, sessionConfig.getStateLength());
    sessionConfig.setStateLength(37);
    assertEquals(37, sessionConfig.getStateLength());
  }

  @Test
  public void testGetCodeVerifierLength() {
    SessionConfig sessionConfig = new SessionConfig();
    assertEquals(120, sessionConfig.getCodeVerifierLength());
    sessionConfig.setCodeVerifierLength(37);
    assertEquals(37, sessionConfig.getCodeVerifierLength());
  }

  @Test
  public void testGetNonceLength() {
    SessionConfig sessionConfig = new SessionConfig();
    assertEquals(120, sessionConfig.getNonceLength());
    sessionConfig.setNonceLength(37);
    assertEquals(37, sessionConfig.getNonceLength());
  }

}
