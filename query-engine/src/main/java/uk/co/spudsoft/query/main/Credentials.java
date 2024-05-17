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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Strings;
import uk.co.spudsoft.params4j.SecretsSerializer;

/**
 * Configuration data for credentials used when communicating with services (typically databases).
 *
 * @author jtalbut
 */
public class Credentials {
  
  /**
   * The username.
   */
  private String username;
  /**
   * The password.
   */
  private String password;

  /**
   * The username.
   * @param username the username.
   * @return username.
   */
  public Credentials setUsername(String username) {
    this.username = username;
    return this;
  }

  /**
   * The password.
   * @param password The password.
   * @return The password.
   */
  public Credentials setPassword(String password) {
    this.password = password;
    return this;
  }

  /**
   * Constructor.
   */
  public Credentials() {
  }
    
  /**
   * Constructor.
   * @param username The username to use, if any.
   * @param password The password to use, if any.
   */
  public Credentials(String username, String password) {
    this.username = username;
    this.password = password;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    if (!Strings.isNullOrEmpty(username)) {
      sb.append("username=").append(username);
    }
    sb.append("}");
    return sb.toString();
  }
  
  /**
   * The username.
   * @return The username.
   */
  public String getUsername() {
    return username;
  }

  /**
   * The password.
   * @return the password.
   */
  @JsonSerialize(using = SecretsSerializer.class)
  public String getPassword() {
    return password;
  }

}
