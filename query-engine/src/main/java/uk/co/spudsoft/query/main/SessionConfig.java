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
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author jtalbut
 */
public class SessionConfig {
  
  private boolean requireSession;
  private Duration purgeDelay = Duration.of(1, ChronoUnit.HOURS);
  private int stateLength = 120;
  private int codeVerifierLength = 120;
  private int nonceLength = 120;
  private CookieConfig sessionCookie = new CookieConfig("qe-session");
  
  private Map<String, AuthEndpoint> oauth = new HashMap<>();

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

  public Duration getPurgeDelay() {
    return purgeDelay;
  }

  public void setPurgeDelay(Duration purgeDelay) {
    this.purgeDelay = purgeDelay;
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

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public CookieConfig getSessionCookie() {
    return sessionCookie;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setSessionCookie(CookieConfig sessionCookie) {
    this.sessionCookie = sessionCookie;
  }

  public void validate(String path) throws IllegalArgumentException {
    if (sessionCookie == null) {
      throw new IllegalArgumentException(path + ".sessionCookie not configured");
    }
    sessionCookie.validate(path +  ".sessionCookie");
    if (oauth != null) {
      for (Entry<String, AuthEndpoint> entry : oauth.entrySet()) {
        entry.getValue().validate(path + ".oauth." + entry.getKey());
      }
    }
    
  }
  
}
