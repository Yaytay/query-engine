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
package uk.co.spudsoft.query.main;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;


/**
 *
 * @author njt
 */
public class JwtValidationConfigIT {
  
  @Test
  public void testGetIssuer() {
    JwtValidationConfig instance = new JwtValidationConfig();
    assertNull(instance.getIssuer());
    instance.setIssuer("issuer");
    assertEquals("issuer", instance.getIssuer());
  }

  @Test
  public void testGetIssuerHostPath() {
    JwtValidationConfig instance = new JwtValidationConfig();
    assertNull(instance.getIssuerHostPath());
    instance.setIssuerHostPath("issuerHostPath");
    assertEquals("issuerHostPath", instance.getIssuerHostPath());
  }

  @Test
  public void testGetAcceptableIssuersFile() {
    JwtValidationConfig instance = new JwtValidationConfig();
    assertNull(instance.getAcceptableIssuersFile());
    instance.setAcceptableIssuersFile("path");
    assertEquals("path", instance.getAcceptableIssuersFile());
  }

  @Test
  public void testGetFilePollPeriodDuration() {
    JwtValidationConfig instance = new JwtValidationConfig();
    assertEquals(Duration.of(2, ChronoUnit.MINUTES), instance.getFilePollPeriodDuration());
    instance.setFilePollPeriodDuration(Duration.ofDays(2));
    assertEquals(Duration.ofDays(2), instance.getFilePollPeriodDuration());
  }

  @Test
  public void testGetDefaultJwksCacheDuration() {
    JwtValidationConfig instance = new JwtValidationConfig();
    assertEquals(Duration.of(1, ChronoUnit.MINUTES), instance.getDefaultJwksCacheDuration());
    instance.setDefaultJwksCacheDuration(Duration.ofDays(3));
    assertEquals(Duration.ofDays(3), instance.getDefaultJwksCacheDuration());
  }
  
}
