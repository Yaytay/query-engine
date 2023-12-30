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
 * Configuration data for the display of authentication selection.
 * 
 * @author njt
 */
@Schema(description = """
                      <P>
                      Configuration data for the display of authentication selection.
                      </P>
                      """)
public class AuthConfig {
  
  private final String name;
  private final String logo;

  /**
   * 
   * @param name The name to use in the list of authentication providers.
   * @param logo The URL to the logo to use in the list of authentication providers.
   */
  public AuthConfig(String name, String logo) {
    this.name = name;
    this.logo = logo;
  }

  
  /**
   * Get the name to use in the list of authentication providers.
   * @return the name to use in the list of authentication providers.
   */
  @Schema(description = """
                        <P>
                        The name to use in the list of authentication providers.
                        </P>
                        """
          , minLength = 1
          , maxLength = 100
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public String getName() {
    return name;
  }

  /**
   * Set the URL to the logo to use in the list of authentication providers.
   * @return the URL to the logo to use in the list of authentication providers.
   */
  @Schema(description = """
                        <P>
                        The URL to the logo to use in the list of authentication providers.
                        </P>
                        """
          , minLength = 1
          , maxLength = 1000
          , format = "uri"
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public String getLogo() {
    return logo;
  }
  
}
