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
package uk.co.spudsoft.query.exec.context;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import inet.ipaddr.IPAddressString;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.reactiverse.contextual.logging.ContextualData;
import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.net.HostAndPort;
import io.vertx.ext.web.RoutingContext;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.jwtvalidatorvertx.Jwt;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;
import uk.co.spudsoft.query.web.OriginalUrl;


/**
 * Contextual information about the request used to provide this data to {@link uk.co.spudsoft.query.defn.Condition}s and script processors.
 *
 * @author jtalbut
 */
public final class RequestContext {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(RequestContext.class);
  
  private static final String REQUEST_ID = "requestId";
  
  private static final String REQUEST_CONTEXT = RequestContext.class.getName();
  
  private static final Base64.Decoder B64 = Base64.getDecoder();

  private final long startTime;

  private final String requestId;

  private final String url;

  private final URI uri;

  private final String host;

  private final String path;

  private final String runId;

  private final MultiMap params;

  private ImmutableMap<String, Object> arguments;

  private final MultiMap headers;

  private final ImmutableSet<Cookie> cookies;

  private final IPAddressString clientIp;

  private Jwt jwt;

  private long headersSentTime;

  private long rowsWritten;

  private final ImmutableMap<String, String> environment;
  
  /**
   * Constructor.
   *
   * @param environment Additional data to make available.
   * @param request HttpServerRequest to extract the context from.
   *
   */
  public RequestContext(Map<String, String> environment, HttpServerRequest request) {
    this.environment = ImmutableCollectionTools.copy(environment);
    this.startTime = System.currentTimeMillis();
    this.requestId = generateRequestId();
    this.url = OriginalUrl.get(request);
    this.uri = parseURI(this.url);
    this.clientIp = extractRemoteIp(request);
    this.host = extractHost(request);
    this.path = request.path();
    this.params = request.params();
    this.headers = request.headers();
    this.cookies = ImmutableSet.copyOf(request.cookies());
    this.runId = this.params == null ? null : this.params.get("_runid");

    ContextualData.put(REQUEST_ID, this.requestId);

    logger.debug("Created {} RequestContext@{} from HttpServerRequest", requestId, System.identityHashCode(this));
  }

  /**
   * Manual constructor for use in test cases.
   * @param environment Additional data to make available.
   * @param requestId an ID that is unique to this request.
   * @param url The absolute URL of the request.
   * @param host The value to use for the host.
   * @param path The path from the URL.
   * @param params Arguments that should have been extracted from the request.
   * @param headers Headers that should have been extracted from the request.
   * @param cookies Cookies that should have been extracted from the request.
   * @param clientIp Client IP address that should have been extracted from the request.
   * @param jwt JWT that should have been extracted from the request.
   */
  public RequestContext(Map<String, String> environment, String requestId, String url, String host, String path, MultiMap params, MultiMap headers, Set<Cookie> cookies, IPAddressString clientIp, Jwt jwt) {
    this.environment = ImmutableCollectionTools.copy(environment);
    this.startTime = System.currentTimeMillis();
    this.requestId = requestId;
    this.url = url;
    this.uri = parseURI(url);
    this.host = host;
    this.path = path;
    this.params = params;
    this.arguments = multiMapToMap(params);
    this.headers = headers == null ? HeadersMultiMap.httpHeaders() : headers;
    this.cookies = ImmutableSet.copyOf(cookies == null ? Collections.emptySet() : cookies);
    this.clientIp = clientIp;
    this.jwt = jwt;
    this.runId = params == null ? null : params.get("_runid");
    logger.trace("Created {} RequestContext@{} from values", requestId, System.identityHashCode(this));
  }
  
  private static URI parseURI(String url) {
    if (url == null) {
      return null;
    } else {      
      return URI.create(url);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private ImmutableMap<String, Object> multiMapToMap(MultiMap params) {
    Map<String, Object> result = new HashMap<>();
    if (params != null) {
      params.forEach((k, v) -> {
        if (result.containsKey(k)) {
          Object current = result.get(k);
          if (current instanceof List l) {
            l.add(v);
          } else {
            List<Object> l = new ArrayList<>();
            l.add(current);
            l.add(v);
            result.put(k, l);
          }
        } else {
          result.put(k, v);
        }
      });
    }
    return ImmutableCollectionTools.copy(result);
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

  /**
   * true if the context includes a JWT.
   * @return true if the context includes a JWT.
   */
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
   * This will be either built from OpenTelemetry/Zipkin trace/span IDs or a random GUID.
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
   * Set the JWT found in (or created for) the request context.
   * @param jwt the JWT found in (or created for) the request context.
   */
  public void setJwt(Jwt jwt) {
    this.jwt = jwt;
  }

  /**
   * Get the absolute URL used for the request.
   * @return Get the absolute URL used for the request.
   */
  public String getUrl() {
    return url;
  }

  /**
   * Get the parsed absolute URL used for the request.
   * @return Get the parsed absolute URL used for the request.
   */
  public URI getUri() {
    return uri;
  }

  /**
   * The RunID is a caller allocated value that can be used to track requests.
   * Every RunID must be unique and it is up to the caller to guarantee that.
   * @return the RunID, a caller allocated value that can be used to track requests.
   */
  public String getRunID() {
    return runId;
  }
  
  /**
   * Generate a requestId.
   * 
   * The request ID is usually generated from the OpenTelemetry trace/span IDs, falling back to a random UUID.
   * 
   * @return the request ID.
   */
  private static String generateRequestId() {
    String requestId = null;
    Span currentSpan = Span.current();
    if (currentSpan != null) {
      SpanContext spanContext = currentSpan.getSpanContext();
      if (spanContext.isValid()) {
        requestId = spanContext.getTraceId() + ":" + spanContext.getSpanId();
      }
    }
    if (requestId == null) {
      requestId = UUID.randomUUID().toString();
    }
    return requestId;
  }

  
  /**
   * Store the requestContext into the Vert.x RoutingContext.
   * @param routingContext the Vert.x routing context.
   */
  public void storeInRoutingContext(RoutingContext routingContext) {
    routingContext.put(REQUEST_CONTEXT, this);
  }
  
  /**
   * Retrieve the requestContext from the Vert.x RoutingContext.
   * @param routingContext the Vert.x routing context.
   * @return the request context.
   */
  public static RequestContext retrieveRequestContext(RoutingContext routingContext) {
    return routingContext.get(REQUEST_CONTEXT);
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
   * Get the original parameters that were provided on the query string.
   * @return the original parameters that were provided on the query string.
   */
  public MultiMap getParams() {
    return params;
  }

  /**
   * Get the result of processing the arguments that were defined on the pipeline with the values provided in the query string.
   * <P>
   * The values here are set in {@link uk.co.spudsoft.query.exec.PipelineExecutor#prepareArguments(uk.co.spudsoft.query.exec.conditions.RequestContext, java.util.List, io.vertx.core.MultiMap)}
   * until that has been called the values in this map will all be Strings (or Lists or Strings) reflecting the query string.
   *
   * @return the result of processing the arguments that were defined on the pipeline with the values provided in the query string.
   */
  public Map<String, Object> getArguments() {
    return arguments;
  }

  /**
   * Set the result of processing the arguments that were defined on the pipeline with the values provided in the query string.
   * <P>
   * The values here are set in {@link uk.co.spudsoft.query.exec.PipelineExecutor#prepareArguments(uk.co.spudsoft.query.exec.conditions.RequestContext, java.util.List, io.vertx.core.MultiMap)}
   * until that has been called the values in this map will all be Strings (or Lists or Strings) reflecting the query string.
   *
   * @param arguments the result of processing the arguments that were defined on the pipeline with the values provided in the query string.
   */
  public void setArguments(Map<String, Object> arguments) {
    this.arguments = ImmutableCollectionTools.copy(arguments);
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
  
  /**
   * Get the environment data of the context.
   * @return the environment data of the context.
   */
  public Map<String, String> getEnv() {
    return environment;
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
      appendMap(response, arguments);
    } else if (params != null) {
      if (!response.isEmpty()) {
        response.append(", ");
      }
      response.append("\"params\":");
      appendMultiMap(response, params);
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

  /**
   * Get the audience ('aud' claim) from the JWT, or null if there is no JWT.
   * @return the audience ('aud' claim) from the JWT, or null if there is no JWT.
   */
  public List<String> getAudience() {
    if (jwt != null) {
      return jwt.getAudience();
    }
    return null;
  }
  /**
   * Get the audience ('aud' claim) from the JWT, or null if there is no JWT.
   * @return the audience ('aud' claim) from the JWT, or null if there is no JWT.
   */
  public List<String> getAud() {
    return getAudience();
  }

  /**
   * Get the issuer ('iss' claim) from the JWT, or null if there is no JWT.
   * @return the issuer ('iss' claim) from the JWT, or null if there is no JWT.
   */
  public String getIssuer() {
    if (jwt != null) {
      return jwt.getIssuer();
    }
    return null;
  }

  /**
   * Get the issuer ('iss' claim) from the JWT, or null if there is no JWT.
   * @return the issuer ('iss' claim) from the JWT, or null if there is no JWT.
   */
  public String getIss() {
    return getIssuer();
  }

  /**
   * Get the subject ('sub' claim) from the JWT, or null if there is no JWT.
   * @return the subject ('sub' claim) from the JWT, or null if there is no JWT.
   */
  public String getSubject() {
    if (jwt != null) {
      return jwt.getSubject();
    }
    return null;
  }

  /**
   * Get the subject ('sub' claim) from the JWT, or null if there is no JWT.
   * @return the subject ('sub' claim) from the JWT, or null if there is no JWT.
   */
  public String getSub() {
    return getSubject();
  }

  /**
   * Get the groups ('groups' claim) from the JWT, or null if there is no JWT.
   * @return the groups ('groups' claim) from the JWT, or null if there is no JWT.
   */
  public List<String> getGroups() {
    if (jwt != null) {
      return jwt.getGroups();
    }
    return null;
  }

  /**
   * Checks if the current user belongs to at least one of the specified groups.
   * <P>
   * Each listed group will be treated as a regular expression.
   * 
   * @param requirements the group names to check against, provided as a variable number of arguments
   * @return true if the user is part of at least one of the specified groups, false otherwise
   */
  public boolean isInGroup(String... requirements) {
    if (jwt != null) {
      List<String> groups = jwt.getGroups();
      if (groups == null) {
        return false;
      }
      for (String requirement : requirements) {
        if (Strings.isNullOrEmpty(requirement)) {
          continue ;
        }
        Pattern pat = null;
        try {
          pat = Pattern.compile(requirement);
        } catch (Throwable ex) {
          logger.warn("Unable to trest required group \"{}\" as a regular expression: ", requirement, ex.getMessage());
        }
        for (String group : groups) {
          if (pat != null) {
            if (pat.matcher(group).matches()) {
              return true;
            }
          } else {
            if (requirement.equals(group)) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  /**
   * Get the roles ('roles' claim) from the JWT, or null if there is no JWT.
   * @return the roles ('roles' claim) from the JWT, or null if there is no JWT.
   */
  public List<String> getRoles() {
    if (jwt != null) {
      return jwt.getRoles();
    }
    return null;
  }

  /**
   * Append a Map to a StringBuilder as JSON.
   * @param builder the StringBuilder that is used to build up the JSON.
   *
   * @param map the map that is to be written to the build.
   */
  private static void appendMap(StringBuilder builder, Map<String, Object> map) {
    boolean started = false;
    builder.append("{");
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      if (started) {
        builder.append(", ");
      }
      started = true;
      builder.append('"').append(entry.getKey()).append("\":");
      appendValue(builder, entry.getValue());
    }
    builder.append("}");
  }

  /**
   * Append a value to a StringBuilder to build up a JSON structure.
   * <P>
   * The value is either a List or a single value.
   *
   * @param builder the StringBuilder that is used to build up the JSON.
   * @param map the map that is to be written to the build.
   */
  @SuppressWarnings("rawtypes")
  private static void appendValue(StringBuilder builder, Object value) {
    if (value instanceof List list) {
      builder.append("[");
      boolean started = false;
      for (Object entry : list) {
        if (started) {
          builder.append(", ");
        }
        started = true;
        appendValue(builder, entry);
      }
      builder.append("]");
    } else {
      if (value instanceof Boolean bool) {
        builder.append(bool);
      } else if (value instanceof Number num) {
        builder.append(num);
      } else {
        builder.append("\"").append(value).append("\"");
      }
    }
  }

  /**
   * Append a MuiltiMap to a StringBuilder as JSON.
   * @param builder the StringBuilder that is used to build up the JSON.
   *
   * @param map the map that is to be written to the build.
   */
  private static void appendMultiMap(StringBuilder builder, MultiMap map) {
    boolean started = false;
    builder.append("{");
    for (String key : map.names()) {
      if (started) {
        builder.append(", ");
      }
      started = true;
      List<String> values = map.getAll(key);
      builder.append('"').append(key).append("\":");
      if (values.size() == 1) {
        appendValue(builder, values.get(0));
      } else {
        appendValue(builder, values);
      }
    }
    builder.append("}");
  }

  /**
   * Get the username (either the 'preferred_username' claim or the 'sub' claim) from the JWT, or null if there is no JWT.
   * @return the username (either the 'preferred_username' claim or the 'sub' claim) from the JWT, or null if there is no JWT.
   */
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

  /**
   * Get the users human name (either the 'name' claim, a combination of the 'given_name' and 'family_name' claims, the 'preferred_username' claim, or the 'sub' claim) from the JWT, or null if there is no JWT.
   * @return the users human name (either the 'name' claim, a combination of the 'given_name' and 'family_name' claims, the 'preferred_username' claim, or the 'sub' claim) from the JWT, or null if there is no JWT.
   */
  public String getName() {
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
