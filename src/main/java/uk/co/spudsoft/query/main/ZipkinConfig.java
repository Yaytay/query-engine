/*
 * Copyright (C) 2022 jtalbut
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
 *
 * @author jtalbut
 */
public class ZipkinConfig {
  
  /**
   * The service name to use in zipkin spans.
   */
  private String serviceName;
  
  /**
   * The base URL to report zipkin spans to.
   */
  private String baseUrl;

  /**
   * Get the service name to use in zipkin spans.
   * 
   * @return the service name to use in zipkin spans.
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * Set the service name to use in zipkin spans.
   * @param serviceName the service name to use in zipkin spans.
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * Get the base URL to report zipkin spans to.
   * @return the base URL to report zipkin spans to.
   */
  public String getBaseUrl() {
    return baseUrl;
  }

  /**
   * Set the base URL to report zipkin spans to.
   * @param baseUrl the base URL to report zipkin spans to.
   */
  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }
  
}
