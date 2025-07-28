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
package uk.co.spudsoft.query.web;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple router for setting default security headers on HTTP response if they are not already set.
 * 
 * @author njt
 */
public class SecurityHeadersRouter implements Handler<RoutingContext> {
  
  private static final Logger logger = LoggerFactory.getLogger(SecurityHeadersRouter.class);

  private static final String STRICT_TRANSPORT_SECURITY_HEADER = "Strict-Transport-Security";
  private static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";
  private static final String X_FRAME_OPTIONS = "X-Frame-Options";
  private static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
  private static final String REFERRER_POLICY = "Referrer-Policy";
  private static final String PERMISSIONS_POLICY = "Permissions-Policy";
  
  private static final String DEFAULT_X_FRAME_OPTIONS = "DENY";
  private static final String DEFAULT_REFERRER_POLICY = "same-origin";
  private static final String DEFAULT_PERMISSIONS_POLICY = "accelerometer=()"
          + ", autoplay=(), camera=(), clipboard-read=(), clipboard-write=(), display-capture=(), document-domain=()"
          + ", encrypted-media=(), fullscreen=(), geolocation=(), gyroscope=(), microphone=(), magnetometer=(), midi=()"
          + ", payment=(), picture-in-picture=(), publickey-credentials-get=(), screen-wake-lock=(), serial=(), usb=()"
          + ", web-share=(), xr-spatial-tracking=()";
  
  private final String csp;
  private final String xFrameOptions;
  private final String referrerPolicy;
  private final String permissionsPolicy;  

  private static final Set<String> VALID_REFERRER_POLICIES = ImmutableSet.<String>builder()
          .add("no-referrer")
          .add("no-referrer-when-downgrade")
          .add("origin")
          .add("origin-when-cross-origin")
          .add("same-origin")
          .add("strict-origin")
          .add("strict-origin-when-cross-origin")
          .add("unsafe-url")
          .build();
  
  /**
   * Constructor.
   * 
   * @param logoUrls Set of URLs to add to the img-src content security policy.
   * @param inlineStyleHashes Set of hashes to add to the style-src content security policy.
   * @param contentEndpoints Additional endpoints to add to the connect-src policy.
   * @param xFrameOptions Value to use for the X-Frame-Options header.
   * @param referrerPolicy Value to use for the Referrer-Policy header.
   * @param permissionsPolicy Value to use for the Permissions-Policy header.
   */
  public SecurityHeadersRouter(List<String> logoUrls, List<String> inlineStyleHashes, List<String> contentEndpoints, String xFrameOptions, String referrerPolicy, String permissionsPolicy) {
    StringBuilder builder = new StringBuilder();
    builder.append("default-src 'self'; img-src 'self'");
    
    if (logoUrls != null) {
      for (String url : logoUrls) {
        try {
          URI uri = URI.create(url);
          if (!Strings.isNullOrEmpty(uri.getHost())) {
            builder.append(" ").append(uri.getHost());
          }
        } catch (Throwable ex) {
          logger.warn("Unable to parse logo URL \"{}\" as a URI: ", url, ex);
        }
      }
    }
    builder.append("; style-src 'self'");
    if (inlineStyleHashes != null && !inlineStyleHashes.isEmpty()) {
      builder.append(" unsafe-hashes");
      for (String hash : inlineStyleHashes) {
        builder.append(" ").append(hash);
      }
    }
    builder.append("; connect-src 'self'");
    if (contentEndpoints != null && !contentEndpoints.isEmpty()) {
      for (String contentEndpoint : contentEndpoints) {
        builder.append(" ").append(contentEndpoint);
      }
    }
    this.csp = builder.toString();
    
    if (!Strings.isNullOrEmpty(xFrameOptions)) {
      xFrameOptions = xFrameOptions.toUpperCase(Locale.ROOT);
      if (!"DENY".equals(xFrameOptions) && !"SAMEORIGIN".equals(xFrameOptions)) {
        logger.warn("Attempt to set X-Frame-Options header to invalid \"{}\" ignored", xFrameOptions);
        xFrameOptions = null;
      }
    }
    if (Strings.isNullOrEmpty(xFrameOptions)) {
      xFrameOptions = DEFAULT_X_FRAME_OPTIONS;
    }
    this.xFrameOptions = xFrameOptions;
    
    if (!Strings.isNullOrEmpty(referrerPolicy)) {
      referrerPolicy = referrerPolicy.toLowerCase(Locale.ROOT);
      if (!VALID_REFERRER_POLICIES.contains(referrerPolicy)) {
        logger.warn("Attempt to set Referrer-Policy header to invalid \"{}\" ignored", referrerPolicy);
        referrerPolicy = null;
      }
    }
    if (Strings.isNullOrEmpty(referrerPolicy)) {
      referrerPolicy = DEFAULT_REFERRER_POLICY;
    }
    this.referrerPolicy = referrerPolicy;
    
    if (!Strings.isNullOrEmpty(permissionsPolicy)) {
      String[] directives = permissionsPolicy.split(",");
      for (String directive : directives) {
          if (!directive.trim().matches("[a-zA-Z0-9-]+\\s*=\\s*\\(([^\\p{Cntrl}\\x80-\\xFF]*)\\)")) {
              permissionsPolicy = null;
              break;
          }
      }
    }
    if (Strings.isNullOrEmpty(permissionsPolicy)) {
      permissionsPolicy = DEFAULT_PERMISSIONS_POLICY;
    }
    this.permissionsPolicy = permissionsPolicy;
  }
   
  static boolean wasHttps(HttpServerRequest request) {
    MultiMap headers = request.headers();
    String proto = headers == null ? null : headers.get("X-Forwarded-Proto");
    if (Strings.isNullOrEmpty(proto) || (!"https".equals(proto) && !"http".equals(proto))) {
      proto = request.scheme();
    }
    return "https".equals(proto);
  }
  
  @Override
  public void handle(RoutingContext routingContext) {
    
    routingContext.addHeadersEndHandler(v -> {
      HttpServerRequest request = routingContext.request();
      HttpServerResponse response = routingContext.response();
      MultiMap headers = response.headers();

      boolean wasHttps = wasHttps(request);

      if (wasHttps && !headers.contains(STRICT_TRANSPORT_SECURITY_HEADER)) {
        headers.add(STRICT_TRANSPORT_SECURITY_HEADER, "max-age=63072000");      
      }

      if (!headers.contains(CONTENT_SECURITY_POLICY)) {      
        headers.add(CONTENT_SECURITY_POLICY, csp);
      }

      if (!Strings.isNullOrEmpty(xFrameOptions) && !headers.contains(X_FRAME_OPTIONS)) {
        headers.add(X_FRAME_OPTIONS, xFrameOptions);
      }

      if (!headers.contains(X_CONTENT_TYPE_OPTIONS)) {
        headers.add(X_CONTENT_TYPE_OPTIONS, "nosniff");
      }

      if (!headers.contains(REFERRER_POLICY)) {
        headers.add(REFERRER_POLICY, referrerPolicy);
      }

      if (!headers.contains(PERMISSIONS_POLICY)) {
        headers.add(PERMISSIONS_POLICY, permissionsPolicy);
      }
    });

    routingContext.next();
  }
}
