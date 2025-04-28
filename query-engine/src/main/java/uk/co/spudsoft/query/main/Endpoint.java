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

/**
 * Represents an endpoint that includes a URL and associated credentials for accessing a service.
 * @author jtalbut
 */
public class Endpoint {

  private String url;
  private Credentials credentials;

  /**
   * Constructs a new instance of the Endpoint class.
   */
  public Endpoint() {
  }

  /**
   * Full fat constructor, primarily for unit tests.
   * @param url The URL of the endpoint.
   * @param credentials The credentials associated with the endpoint.
   */
  public Endpoint(String url, Credentials credentials) {
    this.url = url;
    this.credentials = credentials;
  }
  
  /**
   * Retrieves the URL of the endpoint.
   *
   * @return the URL as a string
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets the URL of the endpoint.
   *
   * @param url the*/
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * Retrieves the credentials associated with the endpoint.
   *
   * @return the credentials as a {@code Credentials} object
   */
  public Credentials getCredentials() {
    return credentials;
  }

  /**
   * Sets the credentials associated with the endpoint.
   *
   * @param credentials the {@code Credentials} object representing the authentication details
   */
  public void setCredentials(Credentials credentials) {
    this.credentials = credentials;
  }



}
