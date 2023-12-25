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
package uk.co.spudsoft.query.exec.conditions;

import brave.propagation.TraceContext;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import inet.ipaddr.IPAddressString;
import io.vertx.core.Context;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.net.HostAndPort;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.jwtvalidatorvertx.Jwt;
import static uk.co.spudsoft.query.logging.VertxZipkinLogbackConverter.ACTIVE_SPAN;


/**
 *
 * @author jtalbut
 */
public class RequestContext {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(RequestContext.class);

  private static final Base64.Decoder B64 = Base64.getDecoder();

  private final long startTime;
  
  private final String requestId;
  
  private final String url;
  
  private final String host;
  
  private final String path;
  
  private final MultiMap arguments;
  
  private final MultiMap headers;
  
  private final ImmutableSet<Cookie> cookies;

  public final IPAddressString clientIp;
  
  public final Jwt jwt;
  
  private long headersSentTime;
  
  private long rowsWritten;
  
  /**
   * Constructor.
   *
   * @param request HttpServerRequest to extract the context from.
   * @param jwt JsonWebToken extracted from the request.
   * 
   */
  public RequestContext(HttpServerRequest request, Jwt jwt) {
    this.startTime = System.currentTimeMillis();
    this.requestId = extractRequestId();
    this.url = request.absoluteURI();
    this.clientIp = extractRemoteIp(request);
    this.host = extractHost(request);
    this.path = request.path();
    this.arguments = request.params();
    this.headers = request.headers();
    this.cookies = ImmutableSet.copyOf(request.cookies());
    this.jwt = jwt;
  }

  /**
   * Manual constructor for use in test cases.
   * @param requestId an ID that is unique to this request.
   * @param url The absolute URL of the request.
   * @param host The value to use for the host.
   * @param path The path from the URL.
   * @param arguments Arguments that should have been extracted from the request.
   * @param headers Headers that should have been extracted from the request.
   * @param cookies Cookies that should have been extracted from the request.
   * @param clientIp Client IP address that should have been extracted from the request.
   * @param jwt JWT that should have been extracted from the request.
   */
  public RequestContext(String requestId, String url, String host, String path, MultiMap arguments, MultiMap headers, Set<Cookie> cookies, IPAddressString clientIp, Jwt jwt) {
    this.startTime = System.currentTimeMillis();
    this.requestId = requestId;
    this.url = url;
    this.host = host;
    this.path = path;
    this.arguments = arguments == null ? new HeadersMultiMap() : arguments;
    this.headers = headers == null ? new HeadersMultiMap() : headers;
    this.cookies = ImmutableSet.copyOf(cookies == null ? Collections.emptySet() : cookies);
    this.clientIp = clientIp;
    this.jwt = jwt;
  }
  
  /**
   * Perform a UTF8 base64 decode of the value, returning the original value if any error occurs.
   * @param value The value.
   * @return a UTF8 base64 decode of the value, returning the original value if any error occurs.
   */
  public static String attemptBase64Decode(String value) {
    try {
      return new String(B64.decode(value.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    } catch (IllegalArgumentException ex) {
      return value;
    }
  }
  
  public boolean isAuthenticated() {
    return jwt != null;
  }

  /**
   * Get the time (in millis since epoch) of the request.
   * @return the time (in millis since epoch) of the request.
   */
  public long getStartTime() {
    return startTime;
  }

  /**
   * Get the time (in millis since epoch) at which headers were completely sent.
   * @return the time (in millis since epoch) at which headers were completely sent.
   */
  public long getHeadersSentTime() {
    return headersSentTime;
  }

  /**
   * Set the time (in millis since epoch) at which headers were completely sent.
   * @param headersSentTime the time (in millis since epoch) at which headers were completely sent.
   */
  public void setHeadersSentTime(long headersSentTime) {
    this.headersSentTime = headersSentTime;
  }

  /**
   * Get the number of rows written.
   * This is dependent upon the FormatInstance writing the value back to the request context correctly.
   * @return the number of rows written.
   */
  public long getRowsWritten() {
    return rowsWritten;
  }

  /**
   * Set the number of rows written.
   * This is dependent upon the FormatInstance writing the value back to the request context correctly.
   * @param rowsWritten the number of rows written.
   */
  public void setRowsWritten(long rowsWritten) {
    this.rowsWritten = rowsWritten;
  }

  
  /**
   * Get an ID that is unique to this request.
   * This will be either built from Zipkin trace/span IDs or a random GUID.
   * @return an ID that is unique to this request.
   */
  public String getRequestId() {
    return requestId;
  }

  /**
   * Get the JWT found in (or created for) the request context.
   * @return the JWT found in (or created for) the request context.
   */
  public Jwt getJwt() {
    return jwt;
  }

  /**
   * Get the absolute URL used for the request.
   * @return Get the absolute URL used for the request.
   */
  public String getUrl() {
    return url;
  }
  
  /**
   * Either use the zipkin span ID (combined with the trace ID if they are not the same) or a random UUID as a unique ID for this request.
   * @return either the zipkin span ID (combined with the trace ID if they are not the same) or a random UUID as a unique ID for this request.
   */
  public static String extractRequestId() {
    Context context = Vertx.currentContext();
    if (context != null) {
      Object value = context.getLocal(ACTIVE_SPAN);
      if (value instanceof zipkin2.Span span) {
        if (span.traceId().equals(span.id())) {
          return span.id();
        } else {
          return span.traceId() + "/" + span.id();
        }
      } else if (value instanceof brave.Span span) {
        TraceContext traceContext = span.context();
        if (traceContext.traceIdString().equals(traceContext.spanIdString())) {
          return traceContext.spanIdString();
        } else {
          return traceContext.traceIdString() + "/" + traceContext.spanIdString();
        }
      }
    }
    return UUID.randomUUID().toString();    
  }
  
  /**
   * Get the Host of the original request from an HttpServerRequest.
   * @param request the HttpServerRequest.
   * @return the value of either the X-Forwarded-Host or Host header.
   */
  public static String extractHost(HttpServerRequest request) {
    if (request == null) {
      return null;
    }
    String source = "X-Forwarded-Host";
    String host = request.getHeader("X-Forwarded-Host");
    if (host == null) {
      source = "Host";
      HostAndPort hap = request.authority();
      if (hap != null) {
        host = hap.host();
      }
    }
    if (host == null) {
      logger.trace("No Host found in request");
      return null;
    }
    int colonPos = host.indexOf(":");
    if (colonPos > 0) {
      String original = host;
      host = original.substring(0, colonPos);
      logger.trace("Host of {} extracted from {} found from {}", host, original, source);
    } else {
      logger.trace("Host of {} found from {}", host, source);
    }
    return host;
  }
  
  /**
   * Get the remote IP address from an HttpServerRequest.
   * @param request the HttpServerRequest.
   * @return the remote IP address from an HttpServerRequest.
   */
  public static IPAddressString extractRemoteIp(HttpServerRequest request) {
    if (request == null) {
      return null;
    }
    String source = "X-Cluster-Client-IP";
    String ipAddress = request.getHeader("X-Cluster-Client-IP");
    if (ipAddress == null) {
      source = "X-Forwarded-For";
      ipAddress = request.getHeader("X-Forwarded-For");
    }
    if (ipAddress == null) {
      source = "getRemoteAddr()";
      ipAddress = request.remoteAddress() == null ? null : request.remoteAddress().hostAddress();
    }
    if (ipAddress == null) {
      logger.trace("No IP Address found in request");
      return null;
    }
    int commaPos = ipAddress.indexOf(",");
    if (commaPos > 0) {
      String original = ipAddress;
      ipAddress = original.substring(0, commaPos);
      logger.trace("IP address of {} extracted from {} found from {}", ipAddress, original, source);
    } else {
      logger.trace("IP address of {} found from {}", ipAddress, source);
    }
    
    return new IPAddressString(ipAddress);
  }

  /**
   * Get the client IP address that has been extracted from the request context.
   * @return the client IP address that has been extracted from the request context.
   */
  public IPAddressString getClientIp() {
    return clientIp;
  }  
  
  /**
   * Check whether the client IP is in any of the given ranges.
   * @param subnets Any number of IPv4 or IPv6 addresses in the form "X.X.X.X" or subnets in the form "X.X.X.X/M".
   * @return true if any of the passed in address/subnets/prefixes match the client IP.
   */
  public boolean clientIpIsIn(String... subnets) {
    for (String subnet : subnets) {
      try {
        IPAddressString subnetString = new IPAddressString(subnet);
        if (subnetString.isPrefixed()) {
          if (subnetString.prefixContains(clientIp)) {
            return true;
          }
        } else {
          if (subnetString.equals(clientIp)) {
            return true;
          }
        }
      } catch (Exception ex) {
        logger.warn("Failed to parse {} as a subnet: ", subnet, ex);
      }
    }
    return false;
  }

  /**
   * Get the arguments that have been extracted from the request context.
   * @return the arguments that have been extracted from the request context.
   */
  public MultiMap getArguments() {
    return arguments;
  }

  /**
   * Get the headers that have been extracted from the request context.
   * @return the headers that have been extracted from the request context.
   */
  public MultiMap getHeaders() {
    return headers;
  }

  /**
   * Get the host header that has been extracted from the request context.
   * @return the host header that has been extracted from the request context.
   */
  public String getHost() {
    return host;
  }
  
  /**
   * Get the path part of the URL.
   * @return the path part of the URL.
   */
  public String getPath() {
    return path;
  }
  
  /**
   * Get the cookies from the request.
   * @return the cookies from the request.
   */
  public Set<Cookie> getCookies() {
    return cookies;
  }
  
  @Override
  public String toString() {
    StringBuilder response = new StringBuilder();
    if (url != null) {
      if (!response.isEmpty()) {
        response.append(", ");
      }
      response.append("\"url\":\"").append(url).append('"');
    }
    if (clientIp != null) {
      if (!response.isEmpty()) {
        response.append(", ");
      }
      response.append("\"clientIp\":\"").append(clientIp).append('"');
    }
    if (host != null) {
      if (!response.isEmpty()) {
        response.append(", ");
      }
      response.append("\"host\":\"").append(host).append('"');
    }
    if (arguments != null) {
      if (!response.isEmpty()) {
        response.append(", ");
      }
      response.append("\"arguments\":");
      appendMultiMap(response, arguments);
    }
    if (headers != null) {
      if (!response.isEmpty()) {
        response.append(", ");
      }
      response.append("\"headers\":");
      appendMultiMap(response, headers);
    }
    if (jwt != null) {
      String iss = jwt.getIssuer();
      if (!Strings.isNullOrEmpty(iss)) {
        if (!response.isEmpty()) {
          response.append(", ");
        }
        response.append("\"iss\":\"").append(iss).append('"');
      }
      
      String sub = jwt.getSubject();
      if (!Strings.isNullOrEmpty(sub)) {
        if (!response.isEmpty()) {
          response.append(", ");
        }
        response.append("\"sub\":\"").append(sub).append('"');
      }
    }
    response.insert(0, "{");
    response.append("}");
    return response.toString();
  }
  
  public String getIssuer() {
    if (jwt != null) {
      return jwt.getIssuer();
    } 
    return null;
  }
  
  public String getSubject() {
    if (jwt != null) {
      return jwt.getSubject();
    } 
    return null;
  }
  
  public List<String> getGroups() {
    if (jwt != null) {
      return jwt.getGroups();
    } 
    return null;
  }
  
  public void appendMultiMap(StringBuilder builder, MultiMap map) {
    boolean started = false;
    builder.append("{");
    for (Map.Entry<String, String> entry : arguments.entries()) {
      if (started) {
        builder.append(", ");          
      }
      started = true;
      builder.append('"').append(entry.getKey()).append("\":\"").append(entry.getValue()).append('"');
    }
    builder.append("}");
  }
  
  public String getUsername() {
    if (jwt == null) {
      return null;
    }
    Object node = jwt.getClaim("preferred_username");
    if (node instanceof String s) {
      return s;
    }    
    node = jwt.getClaim("sub");
    if (node instanceof String s) {
      return s;
    }
    
    return null;
  }
  
  public String getNameFromJwt() {
    if (jwt == null) {
      return null;
    }
    Object node = jwt.getClaim("name");
    if (node instanceof String s) {
      return s;
    }
    String givenName = null;
    Object nodeGiven = jwt.getClaim("given_name");
    if (nodeGiven instanceof String s) {
      givenName = s;
    }
    String familyName = null;
    Object nodeFamily = jwt.getClaim("family_name");
    if (nodeFamily instanceof String s) {
      familyName = s;
    }
    if (givenName != null || familyName != null) {
      if (givenName == null) {
        return familyName;
      } else if (familyName == null) {
        return givenName;
      } else {
        return givenName + " " + familyName;
      }
    }    
    node = jwt.getClaim("preferred_username");
    if (node instanceof String s) {
      return s;
    }    
    node = jwt.getClaim("sub");
    if (node instanceof String s) {
      return s;
    }
    
    return null;
  }
  
}
