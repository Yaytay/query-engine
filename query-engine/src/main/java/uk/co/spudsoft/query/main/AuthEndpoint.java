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

import com.google.common.base.Strings;
import java.time.LocalDateTime;
import uk.co.spudsoft.jwtvalidatorvertx.DiscoveryData;

/**
 *
 * @author njt
 */
public class AuthEndpoint {
  
  private String name;
  private String logoUrl;
  private ClientCredentials credentials;
  private String scope;
  private boolean pkce = true;
  private boolean nonce = true;
  private String authorizationEndpoint;
  private String tokenEndpoint;

  private String issuer;
  private LocalDateTime invalidDate;

  public AuthEndpoint() {
  }

  public AuthEndpoint(AuthEndpoint other) {
    this.name = other.name;
    this.logoUrl = other.logoUrl;
    this.credentials = other.credentials;
    this.scope = other.scope;
    this.issuer = other.issuer;
    this.authorizationEndpoint = other.authorizationEndpoint;
    this.tokenEndpoint = other.tokenEndpoint;
    this.invalidDate = other.invalidDate;
  }
  
  public void updateFromOpenIdConfiguration(DiscoveryData discoveryData) {
    String tempTokenEndpoint = discoveryData.getTokenEndpoint();
    String tempAuthorizationEndpoint = discoveryData.getAuthorizationEndpoint();
    if (tempTokenEndpoint != null) {
      this.tokenEndpoint = tempTokenEndpoint;
    }
    if (tempAuthorizationEndpoint != null) {
      this.authorizationEndpoint = tempAuthorizationEndpoint;
    }
    this.invalidDate = LocalDateTime.now().plusDays(1);
  }
  
  public void validate() {
    if (Strings.isNullOrEmpty(name)) {
      throw new IllegalArgumentException("AuthEndpoint configured with no name");
    }
    if (Strings.isNullOrEmpty(issuer)) {
      throw new IllegalArgumentException("AuthEndpoint \"" + name + "\" configured with no issuer");
    }
  }
  
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLogoUrl() {
    return logoUrl;
  }

  public void setLogoUrl(String logoUrl) {
    this.logoUrl = logoUrl;
  }

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  public String getAuthorizationEndpoint() {
    return authorizationEndpoint;
  }

  public void setAuthorizationEndpoint(String authorizationEndpoint) {
    this.authorizationEndpoint = authorizationEndpoint;
  }

  public String getTokenEndpoint() {
    return tokenEndpoint;
  }

  public void setTokenEndpoint(String tokenEndpoint) {
    this.tokenEndpoint = tokenEndpoint;
  }

  public ClientCredentials getCredentials() {
    return credentials;
  }

  public void setCredentials(ClientCredentials credentials) {
    this.credentials = credentials;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public boolean isPkce() {
    return pkce;
  }

  public void setPkce(boolean pkce) {
    this.pkce = pkce;
  }

  public boolean isNonce() {
    return nonce;
  }

  public void setNonce(boolean nonce) {
    this.nonce = nonce;
  }

  public LocalDateTime getInvalidDate() {
    return invalidDate;
  }

  public void setInvalidDate(LocalDateTime invalidDate) {
    this.invalidDate = invalidDate;
  }
  
}
