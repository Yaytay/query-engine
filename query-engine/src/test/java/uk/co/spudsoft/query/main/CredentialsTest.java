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
package uk.co.spudsoft.query.main;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

/**
 *
 * @author jtalbut
 */
public class CredentialsTest {
  
  @Test
  public void testToString() {
    Credentials creds = new Credentials().setUsername("user").setPassword("pass");
    assertThat(creds.toString(), not(containsString("pass")));
    creds = new Credentials().setPassword("pass");
    assertThat(creds.toString(), not(containsString("pass")));
  }

  @Test
  public void testGetUsername() {
    Credentials creds = new Credentials().setUsername("user").setPassword("pass");
    assertThat(creds.getUsername(), equalTo("user"));
  }

  @Test
  public void testGetPassword() {
    Credentials creds = new Credentials().setUsername("user").setPassword("pass");
    assertThat(creds.getPassword(), equalTo("pass"));
  }

}
