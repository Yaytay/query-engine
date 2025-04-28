/*
 * Copyright (C) 2025 jtalbut
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
import java.util.HashMap;

import java.util.Map;

/**
 * Configuration for handling basic authentication with support for customizable IdPs
 * and grant types.
 * <P>
 * Allows for the specification of a default IdP, domain-to-IdP mappings, and defines
 * the grant type to be used while processing basic auth requests.
 * <P>
 * If neither type of IdP is specified OpenID Discovery will be used to attempt to find
 * and authorization endpoint on the host used to make the original request (adding the path from
 * jwt.issuerHostPath).
 *
 */
public class BasicAuthConfig {

  private BasicAuthGrantType grantType = BasicAuthGrantType.clientCredentials;

  private Credentials discoveryEndpointCredentials;

  private Endpoint defaultIdp;

  private Map<String, Endpoint> idpMap = new HashMap<>();

  /**
   * Default constructor for the BasicAuthConfig class.
   */
  public BasicAuthConfig() {
  }

  /**
   * Set the type of grant to use when processing a basic auth request.
   * @return the type of grant to use when processing a basic auth request.
   */
  public BasicAuthGrantType getGrantType() {
    return grantType;
  }

  /**
   * Get the type of grant to use when processing a basic auth request.
   * @param grantType the type of grant to use when processing a basic auth request.
   */
  public void setGrantType(BasicAuthGrantType grantType) {
    this.grantType = grantType;
  }

  /**
   * Retrieves the credentials used for connecting to the authorization endpoint when OpenID Discovery is used.
   * 
   * This only needs to be set when OpenID Discovery is used with {@link BasicAuthGrantType#resourceOwnerPasswordCredentials}.
   *
   * @return the {@link Credentials} instance containing the username and password for the discovery endpoint.
   */
  public Credentials getDiscoveryEndpointCredentials() {
    return discoveryEndpointCredentials;
  }

  /**
   * Sets the credentials used for connecting to the authorization endpoint when OpenID Discovery is used.
   *
   * This only needs to be set when OpenID Discovery is used with {@link BasicAuthGrantType#resourceOwnerPasswordCredentials}.
   *
   * @param discoveryEndpointCredentials the {@link Credentials} instance containing the username and password
   *                                     for the discovery endpoint.
   */
  public void setDiscoveryEndpointCredentials(Credentials discoveryEndpointCredentials) {
    this.discoveryEndpointCredentials = discoveryEndpointCredentials;
  }

  /**
   * Get the IdP to use when no "domain" is specified in the username.
   * <P>
   * Note that this IdP will also be used for all requests if the IdMap is empty.
   * <P>
   * This should be the full URL to which the grant request will be POSTed.
   * @return the IdP to use when no "domain" is specified in the username (or when the IdpMap is empty).
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public Endpoint getDefaultIdp() {
    return defaultIdp;
  }

  /**
   * Set the IdP to use when no "domain" is specified in the username.
   * <P>
   * Note that this IdP will also be used for all requests if the IdMap is empty.
   * <P>
   * This should be the full URL to which the grant request will be POSTed.
   * @param defaultIdp the IdP to use when no "domain" is specified in the username (or when the IdpMap is empty).
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public void setDefaultIdp(Endpoint defaultIdp) {
    this.defaultIdp = defaultIdp;
  }

  /**
   * Get a map of "domains" to IdP URLs that will be consulted if the username in the request is of the form "user@domain".
   * <P>
   * The domain specified in the username is used as the key to this map, but serves no other purpose and does not have to be recognised by the IdP itself (the domain will be removed from the username when the request to the IdP is made).
   * <P>
   * Each value in the map should be the full URL to which the grant request will be POSTed.
   * <P>
   * If the request user contains a domain that is not found in the map the request will fail.
   *
   * @return a map of "domains" to IdP URLs that will be consulted if the username in the request is of the form "user@domain".
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public Map<String, Endpoint> getIdpMap() {
    return idpMap;
  }

  /**
   * Set a map of "domains" to IdP URLs that will be consulted if the username in the request is of the form "user@domain".
   * <P>
   * The domain specified in the username is used as the key to this map, but serves no other purpose and does not have to be recognised by the IdP itself (the domain will be removed from the username when the request to the IdP is made).
   *
   * @param idpMap a map of "domains" to IdP URLs that will be consulted if the username in the request is of the form "user@domain".
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public void setIdpMap(Map<String, Endpoint> idpMap) {
    this.idpMap = ImmutableCollectionTools.copy(idpMap);
  }

  /**
   * Validate the provided parameters.
   *
   * @param path The configuration path to this item, for reporting.
   * @throws IllegalArgumentException if anything in the parameters is invalid.
   */
  public void validate(String path) throws IllegalArgumentException {
    if (grantType == null) {
      throw new IllegalArgumentException(path + ".grantType is null");
    }
  }

}
