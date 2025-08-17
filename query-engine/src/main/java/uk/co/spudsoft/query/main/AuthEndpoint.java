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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import uk.co.spudsoft.jwtvalidatorvertx.DiscoveryData;

/**
 * Configuration data for OAuth authentication.
 * <P>
 * OAuth is only intended to be configured for the user interface when accessed directly, which is not the intended standard production deployment
 * (where it is expected that the host will seamlessly provide a suitable JWT).
 * <P>
 * OAuth can be useful in design mode where there is no host platform but JWTs are required for testing conditions.
 * <P>
 * Each of the AuthEndpoints configured is presented as an option to the user when they attempt to login via the UI.
 * Login may be optional (controlled by {@link SessionConfig#requireSession}) - if login is optional the user must explicitly choose to login.
 * <P>
 * The login mechanism is purely concerned with the generation of the JWT - the validation is still goverened by the {@link JwtValidationConfig}.
 * <P>
 * The following properties are mandatory:
 * <ul>
 * <li>name
 * <li>issuer
 * </ul>
 * <P>
 * Authentication will follow the standard OAuth 2.0 authorization code grant, with or without PKCE and nonce.
 * 
 * @author jtalbut
 */
public class AuthEndpoint {
  
  private String logoUrl;
  private ClientCredentials credentials;
  private String scope;
  private boolean pkce = true;
  private boolean nonce = true;
  private String authorizationEndpoint;
  private String tokenEndpoint;
  private String endSessionEndpoint;
  private String revocationEndpoint;

  private String issuer;
  
  @JsonIgnore
  private LocalDateTime invalidDate;

  /**
   * Constructor.
   */
  public AuthEndpoint() {
  }

  /**
   * Copy constructor.
   * @param other Another prepared AuthEndpoint instance.
   */
  public AuthEndpoint(AuthEndpoint other) {
    this.logoUrl = other.logoUrl;
    this.credentials = other.credentials;
    this.scope = other.scope;
    this.issuer = other.issuer;
    this.authorizationEndpoint = other.authorizationEndpoint;
    this.tokenEndpoint = other.tokenEndpoint;
    this.invalidDate = other.invalidDate;
  }
  
  /**
   * Modify this AuthEndpoint according to the data found at an <a href="https://openid.net/specs/openid-connect-discovery-1_0.html">OpenID Connect Discovery</a> endpoint.
   * 
   * @param discoveryData Open ID Connector Discovery data downloaded.
   */
  public void updateFromOpenIdConfiguration(DiscoveryData discoveryData) {
    String tempTokenEndpoint = discoveryData.getTokenEndpoint();
    String tempAuthorizationEndpoint = discoveryData.getAuthorizationEndpoint();
    if (tempTokenEndpoint != null) {
      this.tokenEndpoint = tempTokenEndpoint;
    }
    if (tempAuthorizationEndpoint != null) {
      this.authorizationEndpoint = tempAuthorizationEndpoint;
    }
    this.invalidDate = LocalDateTime.now(ZoneOffset.UTC).plusDays(1);
  }
  
  /**
   * Validate the configuration.
   * @param path the configuration path to this set of properties.
   */
  public void validate(String path) {
    if (Strings.isNullOrEmpty(issuer)) {
      if (Strings.isNullOrEmpty(authorizationEndpoint)) {
        throw new IllegalArgumentException(path + ".issuer and " + path + ".authorizationEndpoint not configured");
      }
    }
    if (credentials == null) {
      throw new IllegalArgumentException(path + ".credentials not configured");
    }
  }
  
  /**
   * The URL to use to get a icon to display in the list of authentication endpoints in the login UI.
   * <P>
   * This is optional - if not provided no icon will be displayed.
   * @return URL to use to get a icon to display in the list of authentication endpoints in the login UI.
   */
  public String getLogoUrl() {
    return logoUrl;
  }

  /**
   * The URL to use to get a icon to display in the list of authentication endpoints in the login UI.
   * <P>
   * This is optional - if not provided no icon will be displayed.
   * @param logoUrl URL to use to get a icon to display in the list of authentication endpoints in the login UI.
   */
  public void setLogoUrl(String logoUrl) {
    this.logoUrl = logoUrl;
  }

  /**
   * The issuer that JWTs created by this login will use.
   * <P>
   * The issuer provided must still be valid according to the {@link JwtValidationConfig}, this is just used to identify the issuer 
   * because the user is given a choice of login providers and the issuer cannot be extracted from the JWT.
   * 
   * @return the issuer that JWTs created by this login will use.
   */
  public String getIssuer() {
    return issuer;
  }

  /**
   * The issuer that JWTs created by this login will use.
   * <P>
   * The issuer provided must still be valid according to the {@link JwtValidationConfig}, this is just used to identify the issuer 
   * because the user is given a choice of login providers and the issuer cannot be extracted from the JWT.
   * 
   * @param issuer the issuer that JWTs created by this login will use.
   */
  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  /**
   * The OAuth authorization endpoint.
   * @return the OAuth authorization endpoint.
   */
  public String getAuthorizationEndpoint() {
    return authorizationEndpoint;
  }
  
  /**
   * The OAuth authorization endpoint.
   * @param authorizationEndpoint the OAuth authorization endpoint.
   */
  public void setAuthorizationEndpoint(String authorizationEndpoint) {
    this.authorizationEndpoint = authorizationEndpoint;
  }

  /**
   * The OAuth token endpoint.
   * @return the OAuth token endpoint.
   */
  public String getTokenEndpoint() {
    return tokenEndpoint;
  }

  /**
   * The OAuth token endpoint.
   * @param tokenEndpoint the OAuth token endpoint.
   */
  public void setTokenEndpoint(String tokenEndpoint) {
    this.tokenEndpoint = tokenEndpoint;
  }

  /**
   * The endpoint to use for frontchannel logout.
   * <P>
   * The appropriate value is returned as "end_session_endpoint" in OpenID Discovery data.
   * <P>
   * When used, a request to the logout endpoint will result in a redirect to this URL with a query string parameter of 
   * post_logout_redirect_uri set to the Query Engine start page.
   * <P>
   * This endpoint will not be used at all if backchannel logout works (via {@link #getRevocationEndpoint()}.
   * @return the endpoint to use for frontchannel logout.
   */
  public String getEndSessionEndpoint() {
    return endSessionEndpoint;
  }

  /**
   * The endpoint to use for frontchannel logout.
   * <P>
   * The appropriate value is returned as "end_session_endpoint" in OpenID Discovery data.
   * <P>
   * When used, a request to the logout endpoint will result in a redirect to this URL with a query string parameter of 
   * post_logout_redirect_uri set to the Query Engine start page.
   * <P>
   * This endpoint will not be used at all if backchannel logout works (via {@link #getRevocationEndpoint()}.
   * @param endSessionEndpoint the endpoint to use for frontchannel logout. 
   */
  public void setEndSessionEndpoint(String endSessionEndpoint) {
    this.endSessionEndpoint = endSessionEndpoint;
  }

  /**
   * The endpoint to use for backchannel logout.
   * <P>
   * The appropriate value is returned as "revocation_endpoint" in OpenID Discovery data.
   * <P>
   * When used, a request to the logout endpoint will make a request from the back end to this endpoint passing in id_token,
   * post_logout_redirect_uri and client_id.
   * <P>
   * This endpoint will not be used at all if no id_token is received from the authorisation endpoint during login.
   * Some OpenID providers require the "openid" scope to be specified in order for them to generate an id_token.
   * @return the endpoint to use for backchannel logout.
   */
  public String getRevocationEndpoint() {
    return revocationEndpoint;
  }

  /**
   * The endpoint to use for backchannel logout.
   * <P>
   * The appropriate value is returned as "revocation_endpoint" in OpenID Discovery data.
   * <P>
   * When used, a request to the logout endpoint will make a request from the back end to this endpoint passing in id_token,
   * post_logout_redirect_uri and client_id.
   * <P>
   * This endpoint will not be used at all if no id_token is received from the authorisation endpoint during login.
   * Some OpenID providers require the "openid" scope to be specified in order for them to generate an id_token.
   * @param revocationEndpoint the endpoint to use for backchannel logout.
   */
  public void setRevocationEndpoint(String revocationEndpoint) {
    this.revocationEndpoint = revocationEndpoint;
  }
  
  
  /**
   * The client credentials to use when communicating with the OAuth endpoint.
   * @return the client credentials to use when communicating with the OAuth endpoint.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public ClientCredentials getCredentials() {
    return credentials;
  }

  /**
   * The client credentials to use when communicating with the OAuth endpoint.
   * @param credentials the client credentials to use when communicating with the OAuth endpoint.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public void setCredentials(ClientCredentials credentials) {
    this.credentials = credentials;
  }

  /**
   * The scope(s) to specify in the OAuth request.
   * @return the scope to specify in the OAuth request.
   */
  public String getScope() {
    return scope;
  }

  /**
   * The scope(s) to specify in the OAuth request.
   * @param scope the scope to specify in the OAuth request.
   */
  public void setScope(String scope) {
    this.scope = scope;
  }

  /**
   * When true the OAuth request will use PKCE.
   * @return true if the OAuth request will use PKCE.
   */
  public boolean isPkce() {
    return pkce;
  }

  /**
   * True if the OAuth request will use PKCE.
   * @param pkce true if the OAuth request will use PKCE.
   */
  public void setPkce(boolean pkce) {
    this.pkce = pkce;
  }

  /**
   * When true the OAuth request will include (and validate) a nonce value.
   * @return true if the OAuth request will include (and validate) a nonce value.
   */
  public boolean isNonce() {
    return nonce;
  }

  /**
   * True if the OAuth request will include (and validate) a nonce value.
   * @param nonce true if the OAuth request will include (and validate) a nonce value.
   */
  public void setNonce(boolean nonce) {
    this.nonce = nonce;
  }

  /**
   * The date at which this endpoint should be refreshed via <a href="https://openid.net/specs/openid-connect-discovery-1_0.html">OpenID Connect Discovery</a>.
   * <P>
   * This is not intended to be used for configuration, it is an internal value for implementation use.
   * @return the date at which this endpoint should be refreshed via OpenID Connect Discovery.
   */
  @JsonIgnore
  public LocalDateTime getInvalidDate() {
    return invalidDate;
  }

  /**
   * The date at which this endpoint should be refreshed via <a href="https://openid.net/specs/openid-connect-discovery-1_0.html">OpenID Connect Discovery</a>.
   * <P>
   * This is not intended to be used for configuration, it is an internal value for implementation use.
   * 
   * @param invalidDate the date at which this endpoint should be refreshed via OpenID Connect Discovery.
   */
  @JsonIgnore
  public void setInvalidDate(LocalDateTime invalidDate) {
    this.invalidDate = invalidDate;
  }
  
}
