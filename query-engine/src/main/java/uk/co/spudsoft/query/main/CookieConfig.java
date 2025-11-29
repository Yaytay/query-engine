/*
 * Copyright (C) 2024 jtalbut
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
import io.vertx.core.http.CookieSameSite;
import java.util.regex.Pattern;

/**
 * Configuration of the cookie used by the session handler after login.
 * <P>
 * The cookie value is a randomly generated key pointing to the JWT generated during login (which is stored in the DB and cached in memory).
 * 
 * @author jtalbut
 */
public class CookieConfig {
  
  private String name = "QueryEngineSession";
  
  private Boolean secure;
  private Boolean httpOnly;
  private String domain;
  private String path;
  private CookieSameSite sameSite;

  private static final Pattern VALID_NAME = Pattern.compile("^[-a-zA-Z0-9+._]+$");
  
  /**
   * Constructor.
   */
  public CookieConfig() {
  }

  /**
   * Constructor taking in minimal configuration.
   * <P>
   * The cookie name must be alphanumeric (ASCII) characters or any of the characters in "+-._".
   * <P>
   * This is more restrictive than browser specifications for cookies, but it should be adequate.
   * 
   * @param name the name of the cookie.
   */
  public CookieConfig(String name) {
    this.name = name;
  }

  /**
   * The name of the cookie.
   * <P>
   * The name must be alphanumeric characters or any of the characters in "+-._".
   * <P>
   * This is more restrictive than browser specifications for cookies, but it should be adequate.
   * 
   * @return name of the cookie.
   */
  public String getName() {
    return name;
  }

  /**
   * The name of the cookie.
   * <P>
   * The name must be alphanumeric characters or any of the characters in "+-._".
   * <P>
   * This is more restrictive than browser specifications for cookies, but it should be adequate.
   * 
   * @param name the name of the cookie.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * The "secure" flag for the cookie.
   * <P>
   * Optional, if not set the secure flag will be set if the request is recognised as coming over HTTPS.
   * @return the "secure" flag for the cookie.
   */
  public Boolean isSecure() {
    return secure;
  }

  /**
   * The "secure" flag for the cookie.
   * <P>
   * Optional, if not set the secure flag will be set if the request is recognised as coming over HTTPS.
   * @param secure the "secure" flag for the cookie.
   */
  public void setSecure(Boolean secure) {
    this.secure = secure;
  }

  /**
   * The httpOnly flag for the cookie.
   * <P>
   * Optional, if not set the flag will be set to false.
   * @return httpOnly flag for the cookie.
   */
  public Boolean isHttpOnly() {
    return httpOnly;
  }

  /**
   * The httpOnly flag for the cookie.
   * <P>
   * Optional, if not set the flag will be set to false.
   * @param httpOnly httpOnly flag for the cookie.
   */
  public void setHttpOnly(Boolean httpOnly) {
    this.httpOnly = httpOnly;
  }

  /**
   * The domain to be specified in the cookie.
   * <P>
   * Optional, if not set the domain used in the request will be set in the cookie.
   * @return the domain to be specified in the cookie.
   */
  public String getDomain() {
    return domain;
  }

  /**
   * The domain to be specified in the cookie.
   * <P>
   * Optional, if not set the domain used in the request will be set in the cookie.
   * @param domain the domain to be specified in the cookie.
   */
  public void setDomain(String domain) {
    this.domain = domain;
  }

  /**
   * The path to be specified in the cookie.
   * <P>
   * Optional, if not set the path in the cookie will be set to "/".
   * @return the path to be specified in the cookie.
   */
  public String getPath() {
    return path;
  }

  /**
   * The path to be specified in the cookie.
   * <P>
   * Optional, if not set the path in the cookie will be set to "/".
   * @param path the path to be specified in the cookie.
   */
  public void setPath(String path) {
    this.path = path;
  }

  /**
   * The SameSite value to set in the cookie.
   * <P>
   * Optional, if not set same-site will not be set in the cookie.
   * @return the SameSite value to set in the cookie.
   */
  public CookieSameSite getSameSite() {
    return sameSite;
  }

  /**
   * The SameSite value to set in the cookie.
   * <P>
   * Optional, if not set same-site will not be set in the cookie.
   * @param sameSite the SameSite value to set in the cookie.
   */
  public void setSameSite(CookieSameSite sameSite) {
    this.sameSite = sameSite;
  }
  
  /**
   * Validate the configured values.
   * @param path Path to this configuration value.
   * @throws IllegalArgumentException if the configuration is not valid.
   */
  public void validate(String path) throws IllegalArgumentException {
    if (Strings.isNullOrEmpty(name)) {
      throw new IllegalArgumentException(path + ".name not set");
    }
    if (!VALID_NAME.matcher(name).matches()) {
      throw new IllegalArgumentException(path + ".name is invalid, must match " + VALID_NAME.pattern() + ", was \"" + name + "\"");
    }
  }
  
}
