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
import java.util.List;

/**
 *
 * @author njt
 */
public class JwtValidationConfig {
  
  /**
   * Path to a file that may contain acceptable issuers to validate token issuers.
   * This is a core security control and must be set as tightly as possible.
   * The file itself may be updated in a running system, but the path to the file is immutable.
   * An issuer is considered acceptable if it matches an entry in this file OR an acceptableIssuerRegex.
   */
  private String acceptableIssuersFile;

  /**
   * Time between checks of the file, and also the time that the file must stabilise before it is re-read.
   * Thus the delay between a change to the file being made and being picked up will be 
   * between filePollPeriodMs and 2 * filePollPeriodMs.
   * Checks of the file are based entirely on the last-modified timestamp - if the file is on a filesystem that
   * does not support the last-modified timestamp changes will never be picked up.
   */
  private Duration filePollPeriodDuration = Duration.ofMinutes(2);
  
  /**
   * The list of regular expressions that are used to define acceptable token issuers.
   * This is a core security control and must be set as tightly as possible.
   * An issuer is considered acceptable if it matches one of these regular expressions, OR it matches an entry in the acceptableIssuersFile.
   */
  private List<String> acceptableIssuerRegexes;

  /**
   * The audience value that must be included in any token for the query engine to accept it.
   */
  private String requiredAudience = "query-engine";
  
  /**
   * The default period to cache JWKS data for.
   * This is expected to be overridden by cache-control/max-age headers on the JWKS response, so the default value is usually reasonable.
   */
  private Duration defaultJwksCacheDuration = Duration.ofMinutes(1);

  /**
   * Get the audience value that must be included in any token for the query engine to accept it.
   * @return the audience value that must be included in any token for the query engine to accept it.
   */
  public String getRequiredAudience() {
    return requiredAudience;
  }

  /**
   * Set the audience value that must be included in any token for the query engine to accept it.
   * @param requiredAudience the audience value that must be included in any token for the query engine to accept it.
   * @return this, so that the method may be called in a fluent manner.
   */
  public JwtValidationConfig setRequiredAudience(String requiredAudience) {
    this.requiredAudience = requiredAudience;
    return this;
  }
  
  
  /**
   * Get that path to the acceptable issuers file.
   * This is a core security control and must be set as tightly as possible.
   * The file itself may be updated in a running system, but the path to the file is immutable.
   * An issuer is considered acceptable if it matches an entry in this file OR an acceptableIssuerRegex.
   * @return the path to the acceptable issuers file.
   */
  public String getAcceptableIssuersFile() {
    return acceptableIssuersFile;
  }

  /**
   * Set the path to the acceptable issuers file.
   * This is a core security control and must be set as tightly as possible.
   * The file itself may be updated in a running system, but the path to the file is immutable.
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
   * Thus the delay between a change to the file being made and being picked up will be 
   * between filePollPeriodMs and 2 * filePollPeriodMs.
   * Checks of the file are based entirely on the last-modified timestamp - if the file is on a filesystem that
   * does not support the last-modified timestamp changes will never be picked up.
   * @return the time between checks of the file (also the time that the file must stabilise before it is re-read).
   */
  public Duration getFilePollPeriodDuration() {
    return filePollPeriodDuration;
  }

  /**
   * Get the time between checks of the file (also the time that the file must stabilise before it is re-read).
   * Thus the delay between a change to the file being made and being picked up will be 
   * between filePollPeriodMs and 2 * filePollPeriodMs.
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
   * This is a core security control and must be set as tightly as possible.
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
   * This is a core security control and must be set as tightly as possible.
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
   * This is expected to be overridden by cache-control/max-age headers on the JWKS response, so the default value is usually reasonable.
   * @return the default period to cache JWKS data for.
   */
  public Duration getDefaultJwksCacheDuration() {
    return defaultJwksCacheDuration;
  }

  /**
   * Set the default period to cache JWKS data for.
   * This is expected to be overridden by cache-control/max-age headers on the JWKS response, so the default value is usually reasonable.
   * @param defaultJwksCacheDuration the default period to cache JWKS data for.
   * @return this, so that the method may be called in a fluent manner.
   */
  public JwtValidationConfig setDefaultJwksCacheDuration(Duration defaultJwksCacheDuration) {
    this.defaultJwksCacheDuration = defaultJwksCacheDuration;
    return this;
  }

}
