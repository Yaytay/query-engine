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

import io.vertx.core.http.CookieSameSite;

/**
 *
 * @author jtalbut
 */
public class CookieConfig {
  
  private String name;
  private Boolean secure;
  private Boolean httpOnly;
  private String domain;
  private String path;
  private CookieSameSite sameSite;

  public CookieConfig() {
  }

  public CookieConfig(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Boolean isSecure() {
    return secure;
  }

  public void setSecure(Boolean secure) {
    this.secure = secure;
  }

  public Boolean isHttpOnly() {
    return httpOnly;
  }

  public void setHttpOnly(Boolean httpOnly) {
    this.httpOnly = httpOnly;
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public CookieSameSite getSameSite() {
    return sameSite;
  }

  public void setSameSite(CookieSameSite sameSite) {
    this.sameSite = sameSite;
  }
  
  
  
}
