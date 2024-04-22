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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * JWT validation configuration.
 * 
 * The JWT validator can be configured with either a dynamic or a static configuration.
 * <p>
 * With a dynamic configuration the issuer is derived from the Host (or X-Forwarded-Host) header (with a fixed suffix appended) and OpenID Discovery is used to determine the JWKS endpoint to use.
 * Key IDs (kid) only have to be unique for each issuer.
 * <p>
 * With a static configuration the issuer is not derived and any key from any of the configured JWKS endpoints may be used.
 * Key IDs (kid) only have to be unique across all JWKS endpoints.
 * <p>
 * 
 * @author njt
 */
public class JwtValidationConfig {
  
  
  
  /**
   * Path to be appended to the Host to derive the issuer.
   * <p>
   * Usually an issuer has an empty path, being just https://host[:port]/ however it is perfectly valid for an issuer
   * to have a path on the condition that, when ".well-known/openid-configuration" is appended to it it results in a valid URL
   * to the OpenID configuration for that issuer.
   * <p>
   * This value is used to signify that the issuer should be derived from the header.
   */
  private String issuerHostPath;
  
  /**
   * JWKS endpoints.
   * <P>
   * If any values are set here they will be the only endpoints used for downloading JSON Web Keys.
   * If this value is empty the issuer will be determined (typically from the Host header, see {@link #issuer} and {@link #issuerHostPath}), tested for acceptability, and used to perform OpenID Discovery.
   * <P>
   * In a SAAS deployment the appropriate setting depends on whether the clients share a single JWKS.
   * If the JWKS is shared the URL for it should be provided here, if there is a separate pool of keys for each client then this setting should be left empty and OpenID Discovery will be used for each issuer.
   * <P>
   * Regardless of how the JWKS endpoints are found, the acceptable issuers must be configured as tightly as possible.
   */
  private List<String> jwksEndpoints = new ArrayList<>();
  
  /**
   * Path to a file that may contain acceptable issuers to validate token issuers.
   * <p>
   * This is a core security control and must be set as tightly as possible.
   * <p>
   * The file itself may be updated in a running system, but the path to the file is immutable.
   * <p>
   * An issuer is considered acceptable if it matches an entry in this file OR an acceptableIssuerRegex.
   * <p>
   * For the security of the system every URL in this file must be an https endpoint.
   */
  private String acceptableIssuersFile;

  /**
   * Time between checks of the file, and also the time that the file must stabilise before it is re-read.
   * <p>
   * Thus the delay between a change to the file being made and being picked up will be 
   * between filePollPeriodMs and 2 * filePollPeriodMs.
   * <p>
   * Checks of the file are based entirely on the last-modified timestamp - if the file is on a filesystem that
   * does not support the last-modified timestamp changes will never be picked up.
   */
  private Duration filePollPeriodDuration = Duration.ofMinutes(2);
  
  /**
   * The list of regular expressions that are used to define acceptable token issuers.
   * <p>
   * This is a core security control and must be set as tightly as possible.
   * <p>
   * An issuer is considered acceptable if it matches one of these regular expressions, OR it matches an entry in the acceptableIssuersFile.
   * <p>
   * For the security of the system only https endpoints should match these regular expressions.
   */
  private List<String> acceptableIssuerRegexes = new ArrayList<>();

  /**
   * The set of audience values, any one of which must be included in any token for the query engine to accept it.
   * The token validation requires a non-empty intersection of the required audiences with the provided audiences.
   */
  private List<String> requiredAudiences = new ArrayList<>(Arrays.asList("query-engine"));
  
  /**
   * The default period to cache JWKS data for.
   * <p>
   * This is expected to be overridden by cache-control/max-age headers on the JWKS response, so the default value is usually reasonable.
   */
  private Duration defaultJwksCacheDuration = Duration.ofMinutes(1);

  /**
   * Get the path to be appended to the Host to derive the issuer.
   * <p>
   * Usually an issuer has an empty path, being just https://host[:port]/ however it is perfectly valid for an issuer
   * to have a path as long as, when ".well-known/openid-configuration" is appended to it it results in a valid URL
   * to the OpenID configuration for that issuer.
   * <p>
   * This value is <em>not</em> used to signify that the issuer should be derived from the header, that indication is driven entirely 
   * by the {@link jwksEndpoints} value.
   * @return the path to be appended to the Host to derive the issuer.
   */
  public String getIssuerHostPath() {
    return issuerHostPath;
  }

  /**
   * Set the path to be appended to the Host to derive the issuer.
   * <p>
   * Usually an issuer has an empty path, being just https://host[:port]/ however it is perfectly valid for an issuer
   * to have a path as long as, when ".well-known/openid-configuration" is appended to it it results in a valid URL
   * to the OpenID configuration for that issuer.
   * <p>
   * This value is <em>not</em> used to signify that the issuer should be derived from the header, that indication is driven entirely 
   * by the {@link jwksEndpoints} value.
   * @param issuerHostPath the path to be appended to the Host to derive the issuer. 
   * @return this, so that the method may be called in a fluent manner.
   */
  public JwtValidationConfig setIssuerHostPath(String issuerHostPath) {
    this.issuerHostPath = issuerHostPath;
    return this;
  }

  /**
   * Get the set of audience values, any one of which must be included in any token for the query engine to accept it.
   * The token validation requires a non-empty intersection of the required audiences with the provided audiences.
   * @return the set of audience values, any one of which must be included in any token for the query engine to accept it.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public List<String> getRequiredAudiences() {
    return requiredAudiences;
  }

  /**
   * Set the set of audience values, any one of which must be included in any token for the query engine to accept it.
   * <p>
   * The token validation requires a non-empty intersection of the required audiences with the provided audiences.
   * @param requiredAudiences the set of audience values, any one of which must be included in any token for the query engine to accept it.
   * @return this, so that the method may be called in a fluent manner.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public JwtValidationConfig setRequiredAudiences(List<String> requiredAudiences) {
    this.requiredAudiences = requiredAudiences;
    return this;
  }
  
  /**
   * Get that path to the acceptable issuers file.
   * <p>
   * This is a core security control and must be set as tightly as possible.
   * <p>
   * The file itself may be updated in a running system, but the path to the file is immutable.
   * <p>
   * An issuer is considered acceptable if it matches an entry in this file OR an acceptableIssuerRegex.
   * @return the path to the acceptable issuers file.
   */
  public String getAcceptableIssuersFile() {
    return acceptableIssuersFile;
  }

  /**
   * Set the path to the acceptable issuers file.
   * <p>
   * This is a core security control and must be set as tightly as possible.
   * <p>
   * The file itself may be updated in a running system, but the path to the file is immutable.
   * <p>
   * An issuer is considered acceptable if it matches an entry in this file OR an acceptableIssuerRegex.
   * @param acceptableIssuersFile the path to the acceptable issuers file.
   * @return this, so that the method may be called in a fluent manner.
   */
  public JwtValidationConfig setAcceptableIssuersFile(String acceptableIssuersFile) {
    this.acceptableIssuersFile = acceptableIssuersFile;
    return this;
  }

  /**
   * Get the time between checks of the file (also the time that the file must stabilise before it is re-read).
   * <p>
   * Thus the delay between a change to the file being made and being picked up will be 
   * between filePollPeriodMs and 2 * filePollPeriodMs.
   * <p>
   * Checks of the file are based entirely on the last-modified timestamp - if the file is on a filesystem that
   * does not support the last-modified timestamp changes will never be picked up.
   * @return the time between checks of the file (also the time that the file must stabilise before it is re-read).
   */
  public Duration getFilePollPeriodDuration() {
    return filePollPeriodDuration;
  }

  /**
   * Get the time between checks of the file (also the time that the file must stabilise before it is re-read).
   * <p>
   * Thus the delay between a change to the file being made and being picked up will be 
   * between filePollPeriodMs and 2 * filePollPeriodMs.
   * <p>
   * Checks of the file are based entirely on the last-modified timestamp - if the file is on a filesystem that
   * does not support the last-modified timestamp changes will never be picked up.
   * @param filePollPeriodDuration the time between checks of the file (also the time that the file must stabilise before it is re-read).
   * @return this, so that the method may be called in a fluent manner.
   */
  public JwtValidationConfig setFilePollPeriodDuration(Duration filePollPeriodDuration) {
    this.filePollPeriodDuration = filePollPeriodDuration;
    return this;
  }

  /**
   * Get the list of regular expressions that are used to define acceptable token issuers.
   * <p>
   * This is a core security control and must be set as tightly as possible.
   * <p>
   * An issuer is considered acceptable if it matches one of these regular expressions, OR it matches an entry in the acceptableIssuersFile.
   * 
   * @return the list of regular expressions that are used to define acceptable token issuers.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public List<String> getAcceptableIssuerRegexes() {
    return acceptableIssuerRegexes;
  }

  /**
   * Set the list of regular expressions that are used to define acceptable token issuers.
   * <p>
   * This is a core security control and must be set as tightly as possible.
   * <p>
   * An issuer is considered acceptable if it matches one of these regular expressions, OR it matches an entry in the acceptableIssuersFile.
   * 
   * @param acceptableIssuerRegexes the list of regular expressions that are used to define acceptable token issuers.
   * @return this, so that the method may be called in a fluent manner.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public JwtValidationConfig setAcceptableIssuerRegexes(List<String> acceptableIssuerRegexes) {
    this.acceptableIssuerRegexes = acceptableIssuerRegexes;
    return this;
  }
  
  /**
   * Get the default period to cache JWKS data for.
   * <p>
   * This is expected to be overridden by cache-control/max-age headers on the JWKS response, so the default value is usually reasonable.
   * @return the default period to cache JWKS data for.
   */
  public Duration getDefaultJwksCacheDuration() {
    return defaultJwksCacheDuration;
  }

  /**
   * Set the default period to cache JWKS data for.
   * <p>
   * This is expected to be overridden by cache-control/max-age headers on the JWKS response, so the default value is usually reasonable.
   * @param defaultJwksCacheDuration the default period to cache JWKS data for.
   * @return this, so that the method may be called in a fluent manner.
   */
  public JwtValidationConfig setDefaultJwksCacheDuration(Duration defaultJwksCacheDuration) {
    this.defaultJwksCacheDuration = defaultJwksCacheDuration;
    return this;
  }

  /**
   * Set the explicitly configured endpoints that will be used to download JWK sets.
   * <P>
   * If any values are set here they will be the only endpoints used for downloading JSON Web Keys.
   * If this value is empty the issuer will be determined (typically from the Host header, see {@link #issuerHostPath}), tested for acceptability, and used to perform OpenID Discovery.
   * <P>
   * In a SAAS deployment the appropriate setting depends on whether the clients share a single JWKS.
   * If the JWKS is shared the URL for it should be provided here, if there is a separate pool of keys for each client then this setting should be left empty and OpenID Discovery will be used for each issuer.
   * <P>
   * Regardless of how the JWKS endpoints are found, the acceptable issuers must be configured as tightly as possible.
   * 
   * @return the explicitly configured endpoints that will be used to download JWK sets.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public List<String> getJwksEndpoints() {
    return jwksEndpoints;
  }

  /**
   * Get the explicitly configured endpoints that will be used to download JWK sets.
   * <P>
   * If any values are set here they will be the only endpoints used for downloading JSON Web Keys.
   * If this value is empty the issuer will be determined (typically from the Host header, see {@link #issuerHostPath}), tested for acceptability, and used to perform OpenID Discovery.
   * <P>
   * In a SAAS deployment the appropriate setting depends on whether the clients share a single JWKS.
   * If the JWKS is shared the URL for it should be provided here, if there is a separate pool of keys for each client then this setting should be left empty and OpenID Discovery will be used for each issuer.
   * <P>
   * Regardless of how the JWKS endpoints are found, the acceptable issuers must be configured as tightly as possible.
   * 
   * @param jwksEndpoints the explicitly configured endpoints that will be used to download JWK sets.
   * @return this, so that the method may be called in a fluent manner.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public JwtValidationConfig setJwksEndpoints(List<String> jwksEndpoints) {
    this.jwksEndpoints = jwksEndpoints;
    return this;
  }
  
  /**
   * Validate the provided parameters.
   * 
   * @param fieldName The name of the parent parameter, to be used in exception messages.
   * @throws IllegalArgumentException if anything in the parameters is invalid.
   */
  public void validate(String fieldName) throws IllegalArgumentException {
  }  
}
