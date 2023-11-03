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

import java.util.List;

/**
 *
 * @author njt
 */
public class SessionConfig {
  
  private boolean requireSession;
  
  private List<AuthEndpoint> oauth;

  /**
   * Get the flag to indicate that a session is required for all REST API calls.
   * @return the flag to indicate that a session is required for all REST API calls.
   */
  public boolean isRequireSession() {
    return requireSession;
  }

  /**
   * Set the flag to indicate that a session is required for all REST API calls.
   * @param requireSession the flag to indicate that a session is required for all REST API calls.
   */
  public void setRequireSession(boolean requireSession) {
    this.requireSession = requireSession;
  }

  public List<AuthEndpoint> getOauth() {
    return oauth;
  }

  public void setOauth(List<AuthEndpoint> oauth) {
    this.oauth = oauth;
  }

}
