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
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.HostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Methods for regenerating the original URL called by a client.
 * 
 * For a service sitting behind reverse proxies it is non-trivial to obtain the original URL called by the client.
 * The information about the original URL is usually contained in a bunch of X-Forwarded headers added by the proxies.
 * This class provides a couple of helper methods for extracting those headers and using them to generate the request
 * URL that the client actually made.
 * 
 * @author jtalbut
 */
public class OriginalUrl {

  private static final Logger logger = LoggerFactory.getLogger(OriginalUrl.class);

  private OriginalUrl() {
  }

  /**
   * Generate the original URL called by the client from headers and/or details of the current request.
   * @param request The current request.
   * @return the original URL called by the client.
   */
  public static String get(HttpServerRequest request) {
    return get(request.scheme()
            , request.authority()
            , request.path()
            , request.query()
            , request.headers()
    );
  }

  /**
   * Generate the original URL called by the client from headers and/or details of the current request.
   * For standard usage, see the source for {@link #get(io.vertx.core.http.HttpServerRequest)}.
   * @param requestScheme The scheme/protocol of the request as seen by this service.
   * @param requestHostAndPort The host and port of the request as seen by this service.
   * @param requestPath The path to use for the generated URL, may be null.
   * @param requestQueryString The query string to use for the generated URL, may be null.
   * @param headers The headers seen by the request to this service, which may include X-Forwarded headers.
   * @return a generated URL that may be called by the client.
   */
  public static String get(String requestScheme
          , HostAndPort requestHostAndPort
          , String requestPath
          , String requestQueryString
          , MultiMap headers
  ) {
    if (Strings.isNullOrEmpty(requestScheme)) {
      throw new IllegalArgumentException("Scheme from current request must be specified");
    }
    if (requestHostAndPort == null) {
      throw new IllegalArgumentException("Host and port from current request must be specified");
    }
    String proto = requestScheme;
    if (headers != null) {
      proto = coalesce(
              headers.get("X-Forwarded-Proto")
              , headers.get("X-Forwarded-Scheme")
              , requestScheme
      );
    }

    HostAndPort hostAndPort = getHostAndPort(requestHostAndPort, headers);

    StringBuilder result = new StringBuilder();
    result.append(proto).append("://");
    result.append(hostAndPort.host());
    if (proto.equals("https")) {
      if (hostAndPort.port() != 443) {
        result.append(":").append(hostAndPort.port());
      }
    } else {
      if (hostAndPort.port() != 80) {
        result.append(":").append(hostAndPort.port());
      }
    }
    if (!Strings.isNullOrEmpty(requestPath)) {
      if (!requestPath.startsWith("/")) {
        result.append("/");
      }
      result.append(requestPath);
    }
    if (!Strings.isNullOrEmpty(requestQueryString)) {
      if (!requestQueryString.startsWith("?")) {
        result.append("?");
      }
      result.append(requestQueryString);
    }

    return result.toString();
  }

  private static String coalesce(String... args) {
    for (String arg : args) {
      if (!Strings.isNullOrEmpty(arg)) {
        return arg;
      }
    }
    return args[args.length - 1];
  }

  private static HostAndPort getHostAndPort(HostAndPort requestHostAndPort, MultiMap headers) {

    String host = null;
    int port = -1;
    String forwardedHost = headers == null ? null : headers.get("X-Forwarded-Host");
    if (forwardedHost != null) {
      int colonPos = forwardedHost.indexOf(":");
      if (colonPos < 0) {
        host = forwardedHost;
      } else {
        host = forwardedHost.substring(0, colonPos);
        String portString = forwardedHost.substring(colonPos + 1);
        try {
          port = Integer.parseInt(portString);
        } catch (IllegalArgumentException ex) {
          logger.error("X-Forwarded-Host value ({}) contains port value ({}) that failed to convert to integer: "
                  , forwardedHost, portString, ex);
        }
      }
    }
    if (host == null) {
      host = requestHostAndPort.host();
    }
    String forwardedPort = headers == null ? null : headers.get("X-Forwarded-Port");
    if (forwardedPort != null) {
      try {
        port = Integer.parseInt(forwardedPort);
      } catch (IllegalArgumentException ex) {
        logger.error("X-Forwarded-Port value ({}) failed to convert to integer: ", forwardedPort, ex);
      }
    }
    if (port < 0) {
      port = requestHostAndPort.port();
    }
    return HostAndPort.create(host, port);
  }

}
