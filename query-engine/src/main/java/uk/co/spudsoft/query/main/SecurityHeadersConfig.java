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

import uk.co.spudsoft.params4j.JavadocCapture;

/**
 * Configuration for security headers that will be added to HTTP responses.
 * <P>
 * This class allows administrators to configure three key security headers:
 * <ul>
 * <li>X-Frame-Options - Controls whether the page can be displayed in a frame</li>
 * <li>Referrer-Policy - Controls how much referrer information should be included with requests</li>
 * <li>Permissions-Policy - Controls which features and APIs can be used in the browser</li>
 * </ul>
 * All of these values are optional, the default values are secure, but may be too restrictive in some environments.
 *
 * @author jtalbut
 */
@JavadocCapture
public class SecurityHeadersConfig {

  private String xFrameOptions;
  private String referrerPolicy;
  private String permissionsPolicy;

  /**
   * Constructor.
   */
  public SecurityHeadersConfig() {
  }
  
  /**
   * Get the X-Frame-Options header value.
   * <P>
   * The X-Frame-Options HTTP response header can be used to indicate whether or not a browser
   * should be allowed to render a page in a &lt;frame&gt;, &lt;iframe&gt;, &lt;embed&gt; or &lt;object&gt;.
   * <P>
   * Common values include:
   * <ul>
   * <li>DENY - The page cannot be displayed in a frame, regardless of the site attempting to do so</li>
   * <li>SAMEORIGIN - The page can only be displayed in a frame on the same origin as the page itself</li>
   * </ul>
   *
   * @return the X-Frame-Options header value.
   */
  public String getXFrameOptions() {
    return xFrameOptions;
  }

  /**
   * Set the X-Frame-Options header value.
   * <P>
   * The X-Frame-Options HTTP response header can be used to indicate whether or not a browser
   * should be allowed to render a page in a &lt;frame&gt;, &lt;iframe&gt;, &lt;embed&gt; or &lt;object&gt;.
   * <P>
   * Common values include:
   * <ul>
   * <li>DENY - The page cannot be displayed in a frame, regardless of the site attempting to do so</li>
   * <li>SAMEORIGIN - The page can only be displayed in a frame on the same origin as the page itself</li>
   * </ul>
   *
   * @param xFrameOptions the X-Frame-Options header value.
   * @return this, so that this method may be used in a fluent manner.
   */
  public SecurityHeadersConfig setXFrameOptions(String xFrameOptions) {
    this.xFrameOptions = xFrameOptions;
    return this;
  }

  /**
   * Get the Referrer-Policy header value.
   * <P>
   * The Referrer-Policy HTTP header controls how much referrer information
   * (sent via the Referer header) should be included with requests.
   * <P>
   * Common values include:
   * <ul>
   * <li>no-referrer - The Referer header will be omitted entirely</li>
   * <li>no-referrer-when-downgrade - Send a full URL when performing a same-origin request, only send the origin when the protocol security level stays the same</li>
   * <li>origin - Only send the origin of the document as the referrer</li>
   * <li>origin-when-cross-origin - Send a full URL when performing a same-origin request, but only send the origin for cross-origin requests</li>
   * <li>same-origin - A referrer will be sent for same-site origins, but cross-origin requests will contain no referrer information</li>
   * <li>strict-origin - Only send the origin of the document as the referrer when the protocol security level stays the same</li>
   * <li>strict-origin-when-cross-origin - Send a full URL when performing a same-origin request, only send the origin when the protocol security level stays the same, and send no header for cross-origin requests to less secure destinations</li>
   * <li>unsafe-url - Send a full URL when performing a same-origin or cross-origin request</li>
   * </ul>
   *
   * @return the Referrer-Policy header value.
   */
  public String getReferrerPolicy() {
    return referrerPolicy;
  }

  /**
   * Set the Referrer-Policy header value.
   * <P>
   * The Referrer-Policy HTTP header controls how much referrer information
   * (sent via the Referer header) should be included with requests.
   * <P>
   * Common values include:
   * <ul>
   * <li>no-referrer - The Referer header will be omitted entirely</li>
   * <li>no-referrer-when-downgrade - Send a full URL when performing a same-origin request, only send the origin when the protocol security level stays the same</li>
   * <li>origin - Only send the origin of the document as the referrer</li>
   * <li>origin-when-cross-origin - Send a full URL when performing a same-origin request, but only send the origin for cross-origin requests</li>
   * <li>same-origin - A referrer will be sent for same-site origins, but cross-origin requests will contain no referrer information</li>
   * <li>strict-origin - Only send the origin of the document as the referrer when the protocol security level stays the same</li>
   * <li>strict-origin-when-cross-origin - Send a full URL when performing a same-origin request, only send the origin when the protocol security level stays the same, and send no header for cross-origin requests to less secure destinations</li>
   * <li>unsafe-url - Send a full URL when performing a same-origin or cross-origin request</li>
   * </ul>
   *
   * @param referrerPolicy the Referrer-Policy header value.
   * @return this, so that this method may be used in a fluent manner.
   */
  public SecurityHeadersConfig setReferrerPolicy(String referrerPolicy) {
    this.referrerPolicy = referrerPolicy;
    return this;
  }

  /**
   * Get the Permissions-Policy header value.
   * <P>
   * The Permissions-Policy header allows you to control which features and APIs
   * can be used in the browser. It replaces the deprecated Feature-Policy header.
   * <P>
   * The policy is specified as a list of directives, each controlling a specific feature.
   * For example: "geolocation=(), camera=(), microphone=()" would disable geolocation,
   * camera, and microphone access for all origins.
   *
   * @return the Permissions-Policy header value.
   */
  public String getPermissionsPolicy() {
    return permissionsPolicy;
  }

  /**
   * Set the Permissions-Policy header value.
   * <P>
   * The Permissions-Policy header allows you to control which features and APIs
   * can be used in the browser. It replaces the deprecated Feature-Policy header.
   * <P>
   * The policy is specified as a list of directives, each controlling a specific feature.
   * For example: "geolocation=(), camera=(), microphone=()" would disable geolocation,
   * camera, and microphone access for all origins.
   *
   *
   * @param permissionsPolicy the Permissions-Policy header value.
   * @return this, so that this method may be used in a fluent manner.
   */
  public SecurityHeadersConfig setPermissionsPolicy(String permissionsPolicy) {
    this.permissionsPolicy = permissionsPolicy;
    return this;
  }
}
