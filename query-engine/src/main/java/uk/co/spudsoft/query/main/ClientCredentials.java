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
 *
 * @author jtalbut
 */
public class ClientCredentials {
  
  /**
   * The id.
   */
  private String id;
  /**
   * The secret.
   */
  private String secret;

  /**
   * The id.
   * @param id the id.
   * @return id.
   */
  public ClientCredentials setId(String id) {
    this.id = id;
    return this;
  }

  /**
   * The secret.
   * @param secret The secret.
   * @return The secret.
   */
  public ClientCredentials setSecret(String secret) {
    this.secret = secret;
    return this;
  }

  public ClientCredentials() {
  }
    
  /**
   * Constructor.
   * @param id The id to use, if any.
   * @param secret The secret to use, if any.
   */
  public ClientCredentials(String id, String secret) {
    this.id = id;
    this.secret = secret;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    if (!Strings.isNullOrEmpty(id)) {
      sb.append("id=").append(id);
    }
    sb.append("}");
    return sb.toString();
  }
  
  /**
   * The id.
   * @return The id.
   */
  public String getId() {
    return id;
  }

  /**
   * The secret.
   * @return the secret.
   */
  @JsonSerialize(using = SecretsSerializer.class)
  public String getSecret() {
    return secret;
  }

}
