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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;

/**
 *
 * @author njt
 */
public class SessionConfig {
  
  private boolean requireSession;
  private int stateLength = 256;
  private int codeVerifierLength = 256;
  private int nonceLength = 256;
  
  private Map<String, AuthEndpoint> oauth;

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

  public int getStateLength() {
    return stateLength;
  }

  public void setStateLength(int stateLength) {
    this.stateLength = stateLength;
  }

  public int getCodeVerifierLength() {
    return codeVerifierLength;
  }

  public void setCodeVerifierLength(int codeVerifierLength) {
    this.codeVerifierLength = codeVerifierLength;
  }

  public int getNonceLength() {
    return nonceLength;
  }

  public void setNonceLength(int nonceLength) {
    this.nonceLength = nonceLength;
  }
  
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public Map<String, AuthEndpoint> getOauth() {
    return oauth;
  }

  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public void setOauth(Map<String, AuthEndpoint> oauth) {
    this.oauth = oauth;
  }  
}
