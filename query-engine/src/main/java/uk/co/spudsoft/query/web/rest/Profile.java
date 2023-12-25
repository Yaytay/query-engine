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

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Information about the current user and environment.
 * @author njt
 */
@Schema(description = """
                      Information about the current user and environment.
                      <P>
                      This information is pulled from the access token and is only available if present there.
                      """)
public class Profile {
  
  private String username;
  private String fullname;
  private String version;

  @Schema(description = """
                        The username from the token.
                        <P>
                        This is taken from the first of the following claims to have a value:
                        <UL>
                        <LI>preferred_username
                        <LI>sub
                        </UL>
                        """)
  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

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
                        """)
  public String getFullname() {
    return fullname;
  }

  public void setFullname(String fullname) {
    this.fullname = fullname;
  }

  @Schema(description = """
                        The version of the Query Engine backend.
                        """)
  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }
  
}
