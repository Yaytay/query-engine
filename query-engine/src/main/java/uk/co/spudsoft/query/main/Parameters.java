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

import com.google.common.collect.ImmutableList;
import uk.co.spudsoft.query.logging.LogbackOptions;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import java.util.ArrayList;
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
   * Configuration of the JWT validator.
   */
  private JwtValidationConfig jwt = new JwtValidationConfig();
  
  /**
   * Configuration of the pipeline cache.
   */
  private CacheConfig pipelineCache = new CacheConfig();
  
  /**
   * The Query Engine maintains an internal model of the files under the baseConfigPath.
   * <p>
   * When files under the baseConfigPath change this model gets updated.
   * A delay has to be put in to avoid reading the changes whilst they are still be written.
   */
  private int fileStabilisationDelaySeconds = 2;

  /**
   * Credentials that can be used in Source definitions.
   * <p>
   * Externalising credentials is much more secure - the credentials do not need to be committed to the query definition repository
   * and developers do not need access to live credentials.
   */
  private ImmutableMap<String, ProtectedCredentials> secrets = ImmutableMap.<String, ProtectedCredentials>builder().build();
    
  /**
   * The name of the header that will contain the payload from a token as Json (that may be base64 encoded or not).
   * <p>
   * If this is used the query engine will not attempt to validate tokens itself, the header will be trusted implicitly.
   */
  private String openIdIntrospectionHeaderName;
    
  /**
   * The query engine is provided with some example queries that will be deployed to the baseConfigPath one startup if the directory is empty.
   * <p>
   * These sample queries depend upon the target databases being accessible at known locations with known credentials,
   * it is recommended that the provided query-engine-compose.yml file be used set up the database servers within Docker.
   * An attempt will be made to load each data source configured here with the sample data.
   * If loadSampleData is true, and the targets databases can be accessed, then will be loaded with the sample data on startup.
   * <p>
   * The sample data is loaded using three SQL scripts (one per database engine) and it is perfectly acceptable to run those queries manually 
   * instead of using loadSampleData.
   * <p>
   * Note that the URLs here must be vertx sql URLs, not JDBC URLs, for example:
   * <ul>
   * <li>mysql://localhost:2001/test
   * <li>sqlserver://localhost:2002/test
   * <li>postgresql://localhost:2003/test
   * </ul>
   * The leading component of the URL (the scheme) will be used to determine which script to run.
   */
  private List<DataSourceConfig> sampleDataLoads = new ArrayList<>();
  
  /**
   * The management endpoints (all under /manage) that should be enabled.
   * <p>
   * Some of the management endpoints provide internal information and should absolutely not be accessible to end-users.
   * This can either be achieved by configuring the ingress appropriately, or by disabling the endpoints.
   * <p>
   * If no endpoints are specified then all endpoints will be enabled.
   * Whilst this does mean that it is not possible to disable all management endpoints, the "up" endpoint should always be enabled so this should not be a problem.
   * Also, if you really want to you can set a single invalid value for the list of management endpoints, which will result in none of them being enabled (invalid values are silently ignored).
   * <p>
   * The complete list of management endpoints can be seen by making a request to /manage on a running query engine.
   * The list below is a subset of some of them:
   * <ul>
   * <li>up
   * A simple health endpoint that reports when the service is up (suitable for use by a Kubernetes readiness/startup probe).
   * <li>health
   * A more complete health endpoint.
   * <li>prometheus
   * System metrics in Prometheus format.
   * <li>parameters
   * Dumps the full set of configuration parameters.
   * <li>envvars
   * Dumps all environment variables.
   * <li>sysprops
   * Dumps all system properties.
   * <li>accesslog
   * Reports the past few requests to the system.
   * <li>inflight
   * Reports all requests made to the system that have not yet completed.
   * <li>threads
   * Dump stack traces from all threads.
   * <li>heapdump
   * Download a heap dump.
   * </ul>
   * <p>
   * Unless you are sure that you have secured your /manage endpoint adequately it is strongly recommended that production systems only
   * enable the up; health and prometheus endpoints.
   * 
   * @see Parameters#managementEndpointPort
   */
  private List<String> managementEndpoints = new ArrayList<>();
  
  /**
   * The port that the should be used for the management endpoints.
   * <p>
   * Set to null (the default) to use the main port.
   * Set negative to disable the management endpoints completely (not recommended, as they include health check and metrics).
   */
  private Integer managementEndpointPort;
  
  /**
   * The URL that clients should be using to access the management endpoints.
   * <p>
   * If set (and managementEndpointPort is positive), requests to /manage will return a JSON object containing a single "location" value with this URL.
   * An HTTP redirect would be more appropriate, but causes issues with client UI behaviour.
   * <p>
   * Aimed at use cases where a different ingress is required for accessing the management endpoints.
   * The replacement ingress should not usually be accessible to end users.
   * <p>
   * If managementEndpointPort does not have a positive value any setting of managementEndpointUrl will be ignored.
   * <p>
   * The value provided must be the full URL to the /manage path.
   * <p>
   * If not set, and managementEndpointPort is positive, users will have no way to discover the management endpoint URL (which may be the intention).
   * 
   * @param managementEndpointUrl the URL that clients should be using to access the management endpoints.
   */
  private String managementEndpointUrl;
  
  /**
   * Configuration of the session management for the UI and REST API.
   */
  private SessionConfig session;

  /**
   * Get the options for configuring logback.
   * <p>
   * @return the options for configuring logback.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public LogbackOptions getLogging() {
    return logging;
  }

  /**
   * The options for configuring logback.
   * <p>
   * @param logging the options for configuring logback.
   * @return this, so that the method may be called in a fluent manner.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public Parameters setLogging(LogbackOptions logging) {
    this.logging = logging;
    return this;
  }
  
  
  /**
   * Get the VertxOptions that will be used when creating the Vertx instance.
   * <p>
   * These values do not usually need to be altered.
   * @return the VertxOptions that will be used when creating the Vertx instance.
   */
  public VertxOptions getVertxOptions() {
    return vertxOptions;
  }

  /**
   * Get the HttpServerOptions that will be used when creating the HTTP server.
   * <p>
   * The {@link io.vertx.core.http.HttpServerOptions#setMaxHeaderSize(int)} method should be particularly useful when running behind a proxy that passes large JSON headers.
   * @return the HttpServerOptions that will be used when creating the HTTP server.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public HttpServerOptions getHttpServerOptions() {
    return httpServerOptions;
  }

  /**
   * Get the configuration to use for Zipkin tracing.
   * <p>
   * @return the configuration to use for Zipkin tracing.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public ZipkinConfig getZipkin() {
    return zipkin;
  }

  /**
   * Get whether the process will end rather than waiting for requests
   * <p>
   * This is useful for things such as JIT compilers or CDS preparation.
   * @return the exitOnRun value.
   */
  public boolean isExitOnRun() {
    return exitOnRun;
  }

  /**
   * Get the path to the root of the configuration files.
   * <p>
   * @return the path to the root of the configuration files.
   */
  public String getBaseConfigPath() {
    return baseConfigPath;
  }

  /**
   * Get the seconds to wait after being notified or a file change to allow all file writes to complete.
   * <p>
   * @return the seconds to wait after being notified or a file change to allow all file writes to complete.
   */
  public int getFileStabilisationDelaySeconds() {
    return fileStabilisationDelaySeconds;
  }

  /**
   * The seconds to wait after being notified or a file change to allow all file writes to complete.
   * <p>
   * @param fileStabilisationDelaySeconds the seconds to wait after being notified or a file change to allow all file writes to complete.
   * @return this, so that the method may be called in a fluent manner.
   */
  public Parameters setFileStabilisationDelaySeconds(int fileStabilisationDelaySeconds) {
    this.fileStabilisationDelaySeconds = fileStabilisationDelaySeconds;
    return this;
  }
  
  /**
   * Get the configuration of the audit of requests.
   * @return configuration of the audit of requests.
   */
  public Audit getAudit() {
    return audit;
  }

  /**
   * Get the configuration of the pipeline cache.
   * @return Configuration of the pipeline cache.
   */
  public CacheConfig getPipelineCache() {
    return pipelineCache;
  }

  /**
   * The VertxOptions that will be used when creating the Vertx instance.
   * <p>
   * These values do not usually need to be altered.
   * @param vertxOptions The general Vert.x configuration.
   * @return this, so that the method may be called in a fluent manner.
   */
  public Parameters setVertxOptions(VertxOptions vertxOptions) {
    this.vertxOptions = vertxOptions;
    return this;
  }

  /**
   * The HttpServerOptions that will be used when creating the HTTP server.
   * <p>
   * The {@link io.vertx.core.http.HttpServerOptions#setMaxHeaderSize(int)} method should be particularly useful when running behind a proxy that passes large JSON headers.
   * @param httpServerOptions the HttpServerOptions that will be used when creating the HTTP server.
   * @return this, so that the method may be called in a fluent manner.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public Parameters setHttpServerOptions(HttpServerOptions httpServerOptions) {
    this.httpServerOptions = httpServerOptions;
    return this;
  }

  /**
   * The configuration to use for Zipkin tracing.
   * <p>
   * @param zipkin the configuration to use for Zipkin tracing.
   * @return this, so that the method may be called in a fluent manner.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public Parameters setZipkin(ZipkinConfig zipkin) {
    this.zipkin = zipkin;
    return this;
  }
  
  /**
   * Whether the process will end rather than waiting for requests.
   * <p>
   * This is useful for things such as JIT compilers or CDS preparation.
   * @param exitOnRun if true process will end rather than waiting for requests.
   * @return this, so that the method may be called in a fluent manner.
   */
  public Parameters setExitOnRun(boolean exitOnRun) {
    this.exitOnRun = exitOnRun;
    return this;
  }

  /**
   * The path to the root of the configuration files.
   * <p>
   * @param baseConfigPath the path to the root of the configuration files.
   * @return this, so that the method may be called in a fluent manner.
   */
  public Parameters setBaseConfigPath(String baseConfigPath) {
    this.baseConfigPath = baseConfigPath;
    return this;
  }

  /**
   * The configuration of the audit of requests.
   * <p>
   * @param audit the configuration of the audit of requests.
   * @return this, so that the method may be called in a fluent manner.
   */
  public Parameters setAudit(Audit audit) {
    this.audit = audit;
    return this;
  }

  /**
   * The configuration of the pipeline cache.
   * <p>
   * @param pipelineCache the configuration of the pipeline cache.
   * @return this, so that the method may be called in a fluent manner.
   */
  public Parameters setPipelineCache(CacheConfig pipelineCache) {
    this.pipelineCache = pipelineCache;
    return this;
  }

  /**
   * Get the Allowed-Origin-Regex to use for CORS.
   * <p>
   * @return the Allowed-Origin-Regex to use for CORS.
   */
  public String getCorsAllowedOriginRegex() {
    return corsAllowedOriginRegex;
  }

  /**
   * The Allowed-Origin-Regex to use for CORS.
   * <p>
   * @param corsAllowedOriginRegex the Allowed-Origin-Regex to use for CORS.
   * @return this, so that the method may be called in a fluent manner.
   */
  public Parameters setCorsAllowedOriginRegex(String corsAllowedOriginRegex) {
    this.corsAllowedOriginRegex = corsAllowedOriginRegex;
    return this;
  }

  /**
   * Get the externalized credentials map.
   * <p>
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
   * The externalized credentials map.
   * <p>
   * Credentials that can be used in Source definitions.
   * Externalising credentials is much more secure - the credentials do not need to be committed to the query definition repository
   * and developers do not need access to live credentials.
   * <p>
   * Kubernetes secrets can be mounted as volumes directly into the externalized credentials map, keeping them out of all configuration.
   * 
   * @param secrets the externalized credentials map.
   * @return this, so that the method may be called in a fluent manner.
   */
  public Parameters setSecrets(Map<String, ProtectedCredentials> secrets) {
    this.secrets = ImmutableMap.copyOf(secrets);
    return this;
  }

  /**
   * Get the name of the header that will contain the payload from a token as Json (that may be base64 encoded or not).
   * <p>
   * If this is used the query engine will not attempt to validate tokens itself, the header will be trusted implicitly.
   * @return the name of the header that will contain the payload from a token as Json (that may be base64 encoded or not).
   */
  public String getOpenIdIntrospectionHeaderName() {
    return openIdIntrospectionHeaderName;
  }

  /**
   * The name of the header that will contain the payload from a token as Json (that may be base64 encoded or not).
   * <p>
   * If this is used the query engine will not attempt to validate tokens itself, the header will be trusted implicitly.
   * This clearly has security implications and should only be used in a secure environment.
   * @param openIdIntrospectionHeaderName the name of the header that will contain the payload from a token as Json (that may be base64 encoded or not).
   * @return this, so that the method may be called in a fluent manner.
   */
  public Parameters setOpenIdIntrospectionHeaderName(String openIdIntrospectionHeaderName) {
    this.openIdIntrospectionHeaderName = openIdIntrospectionHeaderName;
    return this;
  }

  /**
   * Get data sources to use attempt to initialize with the sample data.
   * <p>
   * The query engine is provided with some example queries that will be deployed to the baseConfigPath one startup if the directory is empty.
   * These sample queries depend upon the target databases being accessible at known locations with known credentials,
   * it is recommended that the provided query-engine-compose.yml file be used set up the database servers within Docker.
   * An attempt will be made to load each data source configured here with the sample data.
   * If loadSampleData is true, and the targets databases can be accessed, then will be loaded with the sample data on startup.
   * <p>
   * The sample data is loaded using three SQL scripts (one per database engine) and it is perfectly acceptable to run those queries manually 
   * instead of using loadSampleData.
   * 
   * Note that the URLs here must be vertx sql URLs, not JDBC URLs, for example:
   * <ul>
   * <li>mysql://localhost:2001/test
   * <li>sqlserver://localhost:2002/test
   * <li>postgresql://localhost:2003/test
   * </ul>
   * The leading component of the URL (the scheme) will be used to determine which script to run.
   * 
   * Only the URL and adminUser values from the DataSourceConfig are used.
   *
   * This is unlikely to be useful unless the example compose file is used to start the Query Engine and the different database engines.
   * 
   * @return data sources to use attempt to initialize with the sample data.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public List<DataSourceConfig> getSampleDataLoads() {
    return sampleDataLoads;
  }

  /**
   * The data sources to use attempt to initialize with the sample data.
   * <p>
   * The query engine is provided with some example queries that will be deployed to the baseConfigPath on startup if the directory is empty.
   * These sample queries depend upon the target databases being accessible at known locations with known credentials,
   * it is recommended that the provided query-engine-compose.yml file be used set up the database servers within Docker.
   * An attempt will be made to load each data source configured here with the sample data.
   * If loadSampleData is true, and the targets databases can be accessed, then will be loaded with the sample data on startup.
   * <p>
   * The sample data is loaded using three SQL scripts (one per database engine) and it is perfectly acceptable to run those queries manually 
   * instead of using loadSampleData.
   * <p>
   * Note that the URLs here must be vertx sql URLs, not JDBC URLs, for example:
   * <ul>
   * <li>mysql://localhost:2001/test
   * <li>sqlserver://localhost:2002/test
   * <li>postgresql://localhost:2003/test
   * </ul>
   * The leading component of the URL (the scheme) will be used to determine which script to run.
   * <p>
   * This is unlikely to be useful unless the example compose file is used to start the Query Engine and the different database engines.
   * 
   * @param sampleDataLoads data sources to use attempt to initialize with the sample data.
   * @return this, so that the method may be called in a fluent manner.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public Parameters setSampleDataLoads(List<DataSourceConfig> sampleDataLoads) {
    this.sampleDataLoads = ImmutableList.copyOf(sampleDataLoads);
    return this;
  }

  /**
   * Get the configuration of the JWT validator.
   * @return the configuration of the JWT validator.
   */
  public JwtValidationConfig getJwt() {
    return jwt;
  }

  /**
   * The configuration of the JWT validator.
   * @param jwt the configuration of the JWT validator.
   * @return this, so that the method may be called in a fluent manner.
   */
  public Parameters setJwt(JwtValidationConfig jwt) {
    this.jwt = jwt;
    return this;
  }

  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public List<String> getManagementEndpoints() {
    return managementEndpoints;
  }

  /**
   * Set the management endpoints (all under /manage) that should be enabled.
   * <p>
   * Some of the management endpoints provide internal information and should absolutely not be accessible to end-users.
   * This can either be achieved by configuring the ingress appropriately, or by disabling the endpoints.
   * <p>
   * If no endpoints are specified then all endpoints will be enabled.
   * Whilst this does mean that it is not possible to disable all management endpoints, the "up" endpoint should always be enabled so this should not be a problem.
   * Also, if you really want to you can set a single invalid value for the list of management endpoints, which will result in none of them being enabled (invalid values are silently ignored).
   * <p>
   * The complete list of management endpoints can be seen by making a request to /manage on a running query engine.
   * The list below is a subset of some of them:
   * <ul>
   * <li>up
   * A simple health endpoint that reports when the service is up (suitable for use by a Kubernetes readiness/startup probe).
   * <li>health
   * A more complete health endpoint.
   * <li>prometheus
   * System metrics in Prometheus format.
   * <li>parameters
   * Dumps the full set of configuration parameters.
   * <li>envvars
   * Dumps all environment variables.
   * <li>sysprops
   * Dumps all system properties.
   * <li>accesslog
   * Reports the past few requests to the system.
   * <li>inflight
   * Reports all requests made to the system that have not yet completed.
   * <li>threads
   * Dump stack traces from all threads.
   * <li>heapdump
   * Download a heap dump.
   * </ul>
   * <p>
   * Unless you are sure that you have secured your /manage endpoint adequately it is strongly recommended that production systems only
   * enable the up; health and prometheus endpoints.
   * 
   * @param managementEndpoints the management endpoints (all under /manage) that should be enabled.
   * @see Parameters#setManagementEndpointPort
   * 
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public void setManagementEndpoints(List<String> managementEndpoints) {
    this.managementEndpoints = ImmutableList.copyOf(managementEndpoints);
  }

  public Integer getManagementEndpointPort() {
    return managementEndpointPort;
  }

  /**
   * Set the port that the /manage endpoints should listen on.
   * <p>
   * In order to help secure the management endpoints they can be run on a secondary port.
   * <p>
   * If the managementEndpointPort is set to null (the default) the management endpoints will listen on the same port as the API.
   * This is the least secure option (but most convenient for the UI).
   * <p>
   * It the managementEndpointPort is negative the entire management endpoint setup will be disabled.
   * <p>
   * @param managementEndpointPort The port to listen on for the management endpoints.
   * @see Parameters#setManagementEndpoints
   */
  public void setManagementEndpointPort(Integer managementEndpointPort) {
    this.managementEndpointPort = managementEndpointPort;
  }

  /**
   * Get the URL that clients should be using to access the management endpoints.
   * <p>
   * If set (and managementEndpointPort is positive), requests to /manage will return a JSON object containing a single "location" value with this URL.
   * An HTTP redirect would be more appropriate, but causes issues with client UI behaviour.
   * <p>
   * Aimed at use cases where a different ingress is required for accessing the management endpoints.
   * The replacement ingress should not usually be accessible to end users.
   * <p>
   * If managementEndpointPort does not have a positive value any setting of managementEndpointUrl will be ignored.
   * <p>
   * The value provided must be the full URL to the /manage path.
   * <p>
   * If not set, and managementEndpointPort is positive, users will have no way to discover the management endpoint URL (which may be the intention).
   * 
   * @return the URL that clients should be using to access the management endpoints.
   */
  public String getManagementEndpointUrl() {
    return managementEndpointUrl;
  }

  /**
   * Set the URL that clients should be using to access the management endpoints.
   * <p>
   * If set (and managementEndpointPort is positive), requests to /manage will return a JSON object containing a single "location" value with this URL.
   * An HTTP redirect would be more appropriate, but causes issues with client UI behaviour.
   * <p>
   * Aimed at use cases where a different ingress is required for accessing the management endpoints.
   * The replacement ingress should not usually be accessible to end users.
   * <p>
   * If managementEndpointPort does not have a positive value any setting of managementEndpointUrl will be ignored.
   * <p>
   * The value provided must be the full URL to the /manage path.
   * <p>
   * If not set, and managementEndpointPort is positive, users will have no way to discover the management endpoint URL (which may be the intention).
   * 
   * @param managementEndpointUrl the URL that clients should be using to access the management endpoints.
   */
  public void setManagementEndpointUrl(String managementEndpointUrl) {
    this.managementEndpointUrl = managementEndpointUrl;
  }

  /**
   * Get the authentication configuration of the UI and REST API.
   * @return the authentication configuration of the UI and REST API.
   */
  public SessionConfig getSession() {
    return session;
  }

  /**
   * Set the authentication configuration of the UI and REST API.
   * @param session the authentication configuration of the UI and REST API.
   */
  public void setSession(SessionConfig session) {
    this.session = session;
  }

  
}

