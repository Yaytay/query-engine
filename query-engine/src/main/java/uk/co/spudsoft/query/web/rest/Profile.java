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
package uk.co.spudsoft.query.web.rest;

import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Information about the current user and environment.
 * @author jtalbut
 */
@Schema(description = """
                      Information about the current user and environment.
                      <P>
                      This information is pulled from the access token and is only available if present there.
                      """)
public class Profile {
  
  private final String username;
  private final String fullname;
  private final String version;
  private final String claims;
  
  

  /**
   * Constructor.
   * 
   * @param username The username from the token ({@link uk.co.spudsoft.query.exec.conditions.RequestContext#getUsername()}).
   * @param fullname The full name from the token ({@link uk.co.spudsoft.query.exec.conditions.RequestContext#getName()}).
   * @param version The version of the Query Engine ({@link uk.co.spudsoft.query.main.Version}).
   * @param claims The claims from the JWT token.
   */
  public Profile(String username, String fullname, String version, String claims) {
    this.username = username;
    this.fullname = fullname;
    this.version = version;
    this.claims = claims;
    
  }

  /**
   * The username from the token.
   * <p>
   * See {@link uk.co.spudsoft.query.exec.conditions.RequestContext#getUsername()}.
   * 
   * @return username from the token.
   */
  @Schema(description = """
                        The username from the token.
                        <P>
                        This is taken from the first of the following claims to have a value:
                        <UL>
                        <LI>preferred_username
                        <LI>sub
                        </UL>
                        """
          , maxLength = 1000
  )
  public String getUsername() {
    return username;
  }

  /**
   * The full name from the token.
   * <p>
   * See {@link uk.co.spudsoft.query.exec.conditions.RequestContext#getName()}.
   * 
   * @return full name from the token.
   */
  @Schema(description = """
                        The users full name from the token.
                        <P>
                        This is taken from the first of the following claims to have a value:
                        <UL>
                        <LI>name
                        <LI>given_name & family_name (either or both)
                        <LI>preferred_username
                        <LI>sub
                        </UL>
                        """
          , maxLength = 1000
  )
  public String getFullname() {
    return fullname;
  }

  /**
   * Get the version of the Query Engine.
   * @return the version of the Query Engine.
   */
  @Schema(description = """
                        The version of the Query Engine backend.
                        """
          , maxLength = 100
  )
  public String getVersion() {
    return version;
  }

  /**
   * Get the claims from the JWT representing the user.
   * @return the claims from the JWT representing the user.
   */
  @Schema(description = """
                        The claims from the JWT representing the user.
                        The type will be an object with string keys.
                        """
          , types = {"object"}
          , implementation = Object.class
          , additionalProperties = Schema.AdditionalPropertiesValue.TRUE
          , example = """
                      {
                        "sub": "user123",
                        "iss": "http://issuer-endpoint/token",
                        "exp": 1754310371,
                        "nbf": 1754306771,
                        "preferred_username": "john.doe",
                        "name": "John Doe",
                        "email": "john.doe@example.com",
                        "aud": "query-engine"
                      }
                      """
  )
  @JsonRawValue
  public String getClaims() {
    return claims;
  }
}
