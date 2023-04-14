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

import uk.co.spudsoft.query.logging.LogbackOptions;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import java.util.List;
import java.util.Map;
import uk.co.spudsoft.params4j.JavadocCapture;

/**
 *
 * @author jtalbut
 */
@JavadocCapture
public class Parameters {

  /**
   * The options that will be used to configure Logback.
   */
  private LogbackOptions logging = new LogbackOptions();
  
  /**
   * The VertxOptions that will be used when creating the Vertx instance.
   */
  private VertxOptions vertxOptions = new VertxOptions();
  
  /**
   * The HttpServerOptions that will be used when creating the HTTP server.
   */ 
  private HttpServerOptions httpServerOptions = new HttpServerOptions().setPort(0);
  
  /**
   * The default Allowed-Origin-Regex.
   * This allows browsers from any port on localhost to access the query engine (most convenient for UI development).
   * An alternative that is often convenient for internal use is:
   * "https?://(((.*\\.)internal.net)|(localhost))(:[^/]+)?/?"
   * This will all browsers on any subdomain of internal.net (on any port) to access the query engine.
   */
  private String corsAllowedOriginRegex = "https?://((localhost):[^/]+)?/?";    // "https?://(((.*\\.)groupgti.net)|(localhost))(:[^/]+)?/?"
  
  /**
   * The configuration to use for Zipkin tracing.
   * If the base URL is not set zipkin tracing will be disabled.
   */
  private ZipkinConfig zipkin = new ZipkinConfig();
  
  /**
   * if true the process will end rather than waiting for requests.
   */
  private boolean exitOnRun = false;
  
  /**
   * The path to the root of the configuration files.
   */
  private String baseConfigPath = "/var/query-engine";
  
  /**
   * Configuration of the audit of requests.
   */
  private Audit audit = new Audit();
  
  /**
   * Configuration of the pipeline cache.
   */
  private CacheConfig pipelineCache = new CacheConfig();
  
  /**
   * The Query Engine maintains an internal model of the files under the baseConfigPath.
   * When files under the baseConfigPath change this model gets updated.
   * A delay has to be put in to avoid reading the changes whilst they are still be written.
   */
  private int fileStabilisationDelaySeconds = 2;

  /**
   * Credentials that can be used in Source definitions.
   * Externalising credentials is much more secure - the credentials do not need to be committed to the query definition repository
   * and developers do not need access to live credentials.
   */
  private ImmutableMap<String, ProtectedCredentials> secrets = ImmutableMap.<String, ProtectedCredentials>builder().build();
    
  /**
   * The list of regular expressions that are used to define acceptable token issuers.
   * This is a core security control and must be set as tightly as possible.
   */
  private List<String> acceptableIssuerRegexes;

  /**
   * The default period to cache JWKS data for.
   * This is expected to be overridden by cache-control/max-age headers on the JWKS response, so the default value is usually reasonable.
   */
  private int defaultJwksCacheDurationSeconds = 60;

  /**
   * The name of the header that will contain the payload from a token as Json (that may be base64 encoded or not).
   * If this is used the query engine will not attempt to validate tokens itself, the header will be trusted implicitly.
   */
  private String openIdIntrospectionHeaderName;
  
  /**
   * The audience value that must be included in any token for the query engine to accept it.
   */
  private String audience = "query-engine";

  /**
   * Get the options for configuring logback.
   * @return the options for configuring logback.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public LogbackOptions getLogging() {
    return logging;
  }

  /**
   * Set the options for configuring logback.
   * @param logging the options for configuring logback.
   * @return this, so that the method may be called in a fluent manner.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public Parameters setLogging(LogbackOptions logging) {
    this.logging = logging;
    return this;
  }
  
  
  /**
   * The VertxOptions that will be used when creating the Vertx instance.
   * @return the VertxOptions that will be used when creating the Vertx instance.
   */
  public VertxOptions getVertxOptions() {
    return vertxOptions;
  }

  /**
   * The HttpServerOptions that will be used when creating the HTTP server.
   * The {@link io.vertx.core.http.HttpServerOptions#setMaxHeaderSize(int)} method should be particularly useful when running behind a proxy that passes large JSON headers.
   * @return the HttpServerOptions that will be used when creating the HTTP server.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public HttpServerOptions getHttpServerOptions() {
    return httpServerOptions;
  }

  /**
   * The configuration to use for Zipkin tracing.
   * @return the configuration to use for Zipkin tracing.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public ZipkinConfig getZipkin() {
    return zipkin;
  }

  /**
   * if true the process will end rather than waiting for requests
   * This is expected to be useful for things such as JIT compilers or CDS preparation.
   * @return the exitOnRun value.
   */
  public boolean isExitOnRun() {
    return exitOnRun;
  }

  /**
   * Path to the root of the configuration files.
   * @return the path to the root of the configuration files.
   */
  public String getBaseConfigPath() {
    return baseConfigPath;
  }

  /**
   * Get the seconds to wait after being notified or a file change to allow all file writes to complete.
   * @return the seconds to wait after being notified or a file change to allow all file writes to complete.
   */
  public int getFileStabilisationDelaySeconds() {
    return fileStabilisationDelaySeconds;
  }

  /**
   * Set the seconds to wait after being notified or a file change to allow all file writes to complete.
   * @param fileStabilisationDelaySeconds the seconds to wait after being notified or a file change to allow all file writes to complete.
   * @return this, so that the method may be called in a fluent manner.
   */
  public Parameters setFileStabilisationDelaySeconds(int fileStabilisationDelaySeconds) {
    this.fileStabilisationDelaySeconds = fileStabilisationDelaySeconds;
    return this;
  }
  
  /**
   * Configuration of the audit of requests.
   * @return configuration of the audit of requests.
   */
  public Audit getAudit() {
    return audit;
  }

  /**
   * Configuration of the pipeline cache.
   * @return Configuration of the pipeline cache.
   */
  public CacheConfig getPipelineCache() {
    return pipelineCache;
  }

  public Parameters setVertxOptions(VertxOptions vertxOptions) {
    this.vertxOptions = vertxOptions;
    return this;
  }

  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public Parameters setHttpServerOptions(HttpServerOptions httpServerOptions) {
    this.httpServerOptions = httpServerOptions;
    return this;
  }

  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public void setZipkin(ZipkinConfig zipkin) {
    this.zipkin = zipkin;
  }
  
  public Parameters setExitOnRun(boolean exitOnRun) {
    this.exitOnRun = exitOnRun;
    return this;
  }

  public Parameters setBaseConfigPath(String baseConfigPath) {
    this.baseConfigPath = baseConfigPath;
    return this;
  }

  public Parameters setAudit(Audit audit) {
    this.audit = audit;
    return this;
  }

  public Parameters setPipelineCache(CacheConfig pipelineCache) {
    this.pipelineCache = pipelineCache;
    return this;
  }

  /**
   * Get the Allowed-Origin-Regex to use for CORS.
   * @return the Allowed-Origin-Regex to use for CORS.
   */
  public String getCorsAllowedOriginRegex() {
    return corsAllowedOriginRegex;
  }

  /**
   * Set the Allowed-Origin-Regex to use for CORS.
   * @param corsAllowedOriginRegex the Allowed-Origin-Regex to use for CORS.
   */
  public void setCorsAllowedOriginRegex(String corsAllowedOriginRegex) {
    this.corsAllowedOriginRegex = corsAllowedOriginRegex;
  }

  /**
   * Get the externalized credentials map.
   * 
   * Credentials that can be used in Source definitions.
   * Externalising credentials is much more secure - the credentials do not need to be committed to the query definition repository
   * and developers do not need access to live credentials.
   * 
   * @return the externalized credentials map.
   */
  public Map<String, ProtectedCredentials> getSecrets() {
    return secrets;
  }

  /**
   * Set the externalized credentials map.
   * 
   * Credentials that can be used in Source definitions.
   * Externalising credentials is much more secure - the credentials do not need to be committed to the query definition repository
   * and developers do not need access to live credentials.
   * 
   * @param secrets the externalized credentials map.
   * @return this, so that the method may be called in a fluent manner.
   */
  public Parameters setSecrets(Map<String, ProtectedCredentials> secrets) {
    this.secrets = ImmutableMap.copyOf(secrets);
    return this;
  }

  /**
   * Get the list of regular expressions that are used to define acceptable token issuers.
   * This is a core security control and must be set as tightly as possible.
   * @return the list of regular expressions that are used to define acceptable token issuers.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public List<String> getAcceptableIssuerRegexes() {
    return acceptableIssuerRegexes;
  }

  /**
   * Set the list of regular expressions that are used to define acceptable token issuers.
   * This is a core security control and must be set as tightly as possible.
   * @param acceptableIssuerRegexes the list of regular expressions that are used to define acceptable token issuers.
   * @return this, so that the method may be called in a fluent manner.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public Parameters setAcceptableIssuerRegexes(List<String> acceptableIssuerRegexes) {
    this.acceptableIssuerRegexes = acceptableIssuerRegexes;
    return this;
  }

  /**
   * Get the default period to cache JWKS data for.
   * This is expected to be overridden by cache-control/max-age headers on the JWKS response, so the default value is usually reasonable.
   * @return the default period to cache JWKS data for.
   */
  public int getDefaultJwksCacheDurationSeconds() {
    return defaultJwksCacheDurationSeconds;
  }

  /**
   * Set the default period to cache JWKS data for.
   * This is expected to be overridden by cache-control/max-age headers on the JWKS response, so the default value is usually reasonable.
   * @param defaultJwksCacheDurationSeconds the default period to cache JWKS data for.
   * @return this, so that the method may be called in a fluent manner.
   */
  public Parameters setDefaultJwksCacheDurationSeconds(int defaultJwksCacheDurationSeconds) {
    this.defaultJwksCacheDurationSeconds = defaultJwksCacheDurationSeconds;
    return this;
  }

  /**
   * Get the name of the header that will contain the payload from a token as Json (that may be base64 encoded or not).
   * If this is used the query engine will not attempt to validate tokens itself, the header will be trusted implicitly.
   * @return the name of the header that will contain the payload from a token as Json (that may be base64 encoded or not).
   */
  public String getOpenIdIntrospectionHeaderName() {
    return openIdIntrospectionHeaderName;
  }

  /**
   * Set the name of the header that will contain the payload from a token as Json (that may be base64 encoded or not).
   * If this is used the query engine will not attempt to validate tokens itself, the header will be trusted implicitly.
   * @param openIdIntrospectionHeaderName the name of the header that will contain the payload from a token as Json (that may be base64 encoded or not).
   * @return this, so that the method may be called in a fluent manner.
   */
  public Parameters setOpenIdIntrospectionHeaderName(String openIdIntrospectionHeaderName) {
    this.openIdIntrospectionHeaderName = openIdIntrospectionHeaderName;
    return this;
  }

  /**
   * Get the audience value that must be included in any token for the query engine to accept it.
   * @return the audience value that must be included in any token for the query engine to accept it.
   */
  public String getAudience() {
    return audience;
  }

  /**
   * Set the audience value that must be included in any token for the query engine to accept it.
   * @param audience the audience value that must be included in any token for the query engine to accept it.
   * @return this, so that the method may be called in a fluent manner.
   */
  public Parameters setAudience(String audience) {
    this.audience = audience;
    return this;
  }
  
  
  
}

