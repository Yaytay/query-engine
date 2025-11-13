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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import uk.co.spudsoft.query.logging.LogbackOptions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.co.spudsoft.params4j.JavadocCapture;

/**
 * Top level configuration options for Query Engine.
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
   * Temporary directory to use.
   * Defaults to the value of System.getProperty("java.io.tmpdir").
   */
  private String tempDir = System.getProperty("java.io.tmpdir");

  /**
   * The HttpServerOptions that will be used when creating the HTTP server.
   */
  private HttpServerOptions httpServerOptions = new HttpServerOptions().setPort(0);

  /**
   * The default Allowed-Origin.
   */
  private List<String> corsAllowedOrigins = new ArrayList<>();

  /**
   * The configuration to use for distributed tracing.
   * If the base URL is not set tracing will be disabled.
   */
  private TracingConfig tracing = new TracingConfig();

  /**
   * if true the process will end rather than waiting for requests.
   */
  private boolean exitOnRun = false;

  /**
   * The path to the root of the configuration files.
   */
  private String baseConfigPath = "/var/query-engine";

  /**
   * Persistence is used for both audit and state management of logins.
   * Persistence is optional, without it there will be no audit and login state
   * will be scoped to the current process.
   */
  private Persistence persistence = new Persistence();

  /**
   * Configuration of the JWT validator.
   */
  private JwtValidationConfig jwt = new JwtValidationConfig();

  /**
   * Configuration of the pipeline cache.
   * This is the in-memory cache of parsed pipelines.
   */
  private CacheConfig pipelineCache = new CacheConfig();

  /**
   * By default requests to / redirect to /openapi and display the OpenAPI docs.
   * This is not much use in a path hijack situation, so allow for the provision of an alternative.
   */
  private String rootRedirectUrl;
  
  /**
   * If true, the path /login/forcejwt can be PUT to create a session based on the the JWT in the message body.
   * This should be secure even in a production environment (because the caller must be still be able to create an acceptable JWT)
   * , but for the sake of safety it defaults to being disabled.
   */
  private boolean enableForceJwt = false;


  /**
   * The directory to contain cached output.
   * This is the on-disc caching of stream output, controlled by the cacheDuration value in individual pipelines.
   *
   * The cache key is based on:
   * <UL>
   * <LI>The full request URL.
   * <LI>Headers:
   * <UL>
   * <LI>Accept
   * <LI>Accept-Encoding
   * </UL>
   * <LI>Token fields:
   * <UL>
   * <LI>aud
   * <LI>iss
   * <LI>sub
   * <LI>groups
   * <LI>roles
   * </UL>
   * </UL>
   *
   * Note that the fileHash must also match, but isn't built into the key (should usually match because of the use of the inclusion of full URL).
   *
   * Note that the default value for the outputCacheDir is probably a bad choice for anything other than the simplest setups.
   */
  private String outputCacheDir = System.getProperty("java.io.tmpdir");

  
  /**
   * The amount of data to cache before writing to the output stream.
   * 
   * Pipeline are run in parallel in Verticles, but the actual write to the network has to occur from the HttpServer Context.
   * 
   * If the context hop from the Verticle to the HttpServer context happens too often it will be a drain on CPU cycles
   * , but making it too large will cause the response to stutter.
   * 
   * Recommended Buffer Sizes 
   * <table class="striped">
   *   <caption>Table of recommended buffer sizes</caption>
   *   <thead>
   *    <tr>
   *      <th scope="col">Buffer Size</th>
   *      <th scope="col">When to Use It</th>
   *      <th scope="col">Notes</th>
   *    </tr>
   *   </thead>
   *   <tbody>
   *     <tr>
   *       <td>8KB</td>
   *       <td>Low-latency, chatty streams</td>
   *       <td>Matches TCP segment size on many systems</td>
   *     </tr>
   *     <tr>
   *       <td>16KB</td>
   *       <td>Balanced throughput and responsiveness</td>
   *       <td>Good default for HTTP/1.1</td>
   *     </tr>
   *     <tr>
   *       <td>32KB</td>
   *       <td>High-throughput, large payloads</td>
   *       <td>Reduces write calls, but increases latency</td>
   *     </tr>
   *     <tr>
   *       <td>64KB</td>
   *       <td>Bulk transfer (e.g. file streaming)</td>
   *       <td>Only if memory pressure is low </td>
   *     </tr>
   *   </tbody>
   * </table>
   * 
   */
  private int writeStreamBufferSize = 32 * 1024;
  
  /**
   * Configuration of specific processors.
   */
  private ProcessorConfig processors = new ProcessorConfig();

  /**
   * The Query Engine maintains an internal model of the files under the baseConfigPath.
   * <p>
   * When files under the baseConfigPath change this model gets updated.
   * <p>
   * There are two mechanisms by which this internal model can be maintained: file notifications and polling.
   * File notifications is the better approach, but it does not work on some filesystems (in particular not on networked file system).
   * <p>
   * With file notifications, notification are fired as soon as a file change starts, so
   * a delay has to be put in to avoid reading the changes whilst they are still be written.
   * <p>
   * If this value is less than zero file notifications will be disabled.
   *
   */
  private int fileStabilisationDelaySeconds = 2;

  /**
   * The Query Engine maintains an internal model of the files under the baseConfigPath.
   * <p>
   * When files under the baseConfigPath change this model gets updated.
   * <p>
   * There are two mechanisms by which this internal model can be maintained: file notifications and polling.
   * Polling is less efficient and slower than file notifications, but will work in all circumstances.
   * <p>
   * If this value is less than or equal to zero polling will be disabled.
   *
   */
  private int filePollPeriodSeconds = 0;

  /**
   * Credentials that can be used in Source definitions.
   * <p>
   * Externalising credentials is much more secure - the credentials do not need to be committed to the query definition repository
   * and developers do not need access to live credentials.
   */
  private Map<String, ProtectedCredentials> secrets = new HashMap<>();

  /**
   * Configuration of the handling of requests using basic authentication for data requests.
   */
  private BasicAuthConfig basicAuth;

  /**
   * If set to false any bearer auth header will be ignored.
   */
  private boolean enableBearerAuth = true;

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
   * The URL to the minified OpenAPI Explorer JS that is to be used for displaying The API documentation.
   * <p>
   * The default value is (something like) "https://unpkg.com/openapi-explorer@2.2.733/dist/browser/openapi-explorer.min.js".
   * <p>
   * It is not usually necessary to set this.
   */
  private String openApiExplorerUrl;

  /**
   * Configuration of the session management for the UI and REST API.
   */
  private SessionConfig session = new SessionConfig();

  /**
   * Path to alternative documentation to make available.
   * <p>
   * Documentation for Query Engine is built in, but documents how to deploy Query Engine and configure its security.
   * In deployments aimed at clients it may be inappropriate to display this information.
   * <p>
   * This configuration allows for the built in documentation to be replaced with an alternative set aimed at your clients.
   * <p>
   * The alternativeDocumentation should be a directory containing a hierarchy of HTML files (and supporting resources).
   * The entire hierarchy will be read on startup and then served by the DocHandler - any changes to the contents of the directory will be ignored.
   * <p>
   * Soft links will not be followed.
   * <p>
   * The path must be valid (Query Engine will not start if it is not), but may be set to "/dev/null" in which case no documentation will be served at all.
   */
  private String alternativeDocumentation;

  /**
   * Additional data that is made available via the request object.
   * <p>
   * The {@link uk.co.spudsoft.query.exec.conditions.RequestContext} is made available in both
   * {@link uk.co.spudsoft.query.exec.conditions.Condition}s and various templates
   * (such as {@link uk.co.spudsoft.query.defn.SourceSql#queryTemplate} and {@link uk.co.spudsoft.query.defn.Endpoint#urlTemplate}).
   * By default this context contains information specific to the request, and very little information about
   * the service it is running in.
   * <P>
   * The entire requestContextEnvironment is also available (with no change)
   * using {@link uk.co.spudsoft.query.exec.conditions.RequestContext#getEnv}, providing a way to
   * add additional environmental information to the context.
   */
  private Map<String, String> requestContextEnvironment = new HashMap<>();

  /**
   * Configuration of security headers.
   * <P>
   * The following response headers can be configured:
   * <ul>
   * <li>X-Frame-Options - Controls whether the page can be displayed in a frame</li>
   * <li>Referrer-Policy - Controls how much referrer information should be included with requests</li>
   * <li>Permissions-Policy - Controls which features and APIs can be used in the browser</li>
   * </ul>
   * <P>
   * All of these values are optional, the default values are secure, but may be too restrictive in some environments.
   */
  private SecurityHeadersConfig securityHeaders;

  /**
   * Constructor.
   */
  public Parameters() {
  }

  /**
   * Get the options for configuring logback.
   *
   * @return the options for configuring logback.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public LogbackOptions getLogging() {
    return logging;
  }

  /**
   * The options for configuring logback.
   *
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
   *
   * These values do not usually need to be altered.
   * @return the VertxOptions that will be used when creating the Vertx instance.
   */
  public VertxOptions getVertxOptions() {
    return vertxOptions;
  }

  /**
   * The temporary directory to use.
   * Defaults to the value of System.getProperty("java.io.tmpdir").
   * @return the temporary directory to use.
   */
  public String getTempDir() {
    return tempDir;
  }

  /**
   * The temporary directory to use.
   * Defaults to the value of System.getProperty("java.io.tmpdir").
   * @param tempDir the temporary directory to use.
   */
  public void setTempDir(String tempDir) {
    this.tempDir = tempDir;
  }

  /**
   * Get the HttpServerOptions that will be used when creating the HTTP server.
   *
   * The {@link io.vertx.core.http.HttpServerOptions#setMaxHeaderSize(int)} method should be particularly useful when running behind a proxy that passes large JSON headers.
   * @return the HttpServerOptions that will be used when creating the HTTP server.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public HttpServerOptions getHttpServerOptions() {
    return httpServerOptions;
  }

  /**
   * Get the configuration to use for distributed tracing.
   *
   * @return the configuration to use for distributed tracing.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public TracingConfig getTracing() {
    return tracing;
  }

  /**
   * Get whether the process will end rather than waiting for requests
   *
   * This is useful for things such as JIT compilers or CDS preparation.
   * @return the exitOnRun value.
   */
  public boolean isExitOnRun() {
    return exitOnRun;
  }

  /**
   * Get the path to the root of the configuration files.
   *
   * @return the path to the root of the configuration files.
   */
  public String getBaseConfigPath() {
    return baseConfigPath;
  }

  /**
   * Get the seconds to wait after being notified or a file change to allow all file writes to complete.
   *
   * @return the seconds to wait after being notified or a file change to allow all file writes to complete.
   */
  public int getFileStabilisationDelaySeconds() {
    return fileStabilisationDelaySeconds;
  }

  /**
   * The seconds to wait after being notified or a file change to allow all file writes to complete.
   *
   * @param fileStabilisationDelaySeconds the seconds to wait after being notified or a file change to allow all file writes to complete.
   * @return this, so that the method may be called in a fluent manner.
   */
  public Parameters setFileStabilisationDelaySeconds(int fileStabilisationDelaySeconds) {
    this.fileStabilisationDelaySeconds = fileStabilisationDelaySeconds;
    return this;
  }

  /**
   * Get the delay between initiating scans for query config changes.
   *
   * @return the delay between initiating scans for query config changes.
   */
  public int getFilePollPeriodSeconds() {
    return filePollPeriodSeconds;
  }

  /**
   * The delay between initiating scans for query config changes.
   *
   * @param filePollPeriodSeconds the delay between initiating scans for query config changes.
   * @return this, so that the method may be called in a fluent manner.
   */
  public Parameters setFilePollPeriodSeconds(int filePollPeriodSeconds) {
    this.filePollPeriodSeconds = filePollPeriodSeconds;
    return this;
  }

  /**
   * Get the persistence configuration.
   * Persistence is used for both audit and state management of logins.
   * Persistence is optional, without it there will be no audit and login state
   * will be scoped to the current process.
   * @return the persistence configuration
   */
  public Persistence getPersistence() {
    return persistence;
  }


  /**
   * Get the configuration of the pipeline cache.
   * @return Configuration of the pipeline cache.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
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
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
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
   * The configuration to use for distributed tracing.
   *
   * @param tracing the configuration to use for distributed tracing.
   * @return this, so that the method may be called in a fluent manner.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public Parameters setTracing(TracingConfig tracing) {
    this.tracing = tracing;
    return this;
  }

  /**
   * Whether the process will end rather than waiting for requests.
   *
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
   *
   * @param baseConfigPath the path to the root of the configuration files.
   * @return this, so that the method may be called in a fluent manner.
   */
  public Parameters setBaseConfigPath(String baseConfigPath) {
    this.baseConfigPath = baseConfigPath;
    return this;
  }

  /**
   * Set the persistence configuration.
   * Persistence is used for both audit and state management of logins.
   * Persistence is optional, without it there will be no audit and login state
   * will be scoped to the current process.
   * @param persistence the persistence configuration
   * @return this, so that the method may be called in a fluent manner.
   */
  public Parameters setPersistence(Persistence persistence) {
    this.persistence = persistence;
    return this;
  }

  /**
   * The configuration of the pipeline cache.
   *
   * @param pipelineCache the configuration of the pipeline cache.
   * @return this, so that the method may be called in a fluent manner.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public Parameters setPipelineCache(CacheConfig pipelineCache) {
    this.pipelineCache = pipelineCache;
    return this;
  }

  /**
   * Get the Allowed-Origin-Regex to use for CORS.
   *
   * @return the Allowed-Origin-Regex to use for CORS.
   */
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public List<String> getCorsAllowedOrigins() {
    return corsAllowedOrigins;
  }

  /**
   * The Allowed-Origin-Regex to use for CORS.
   *
   * @param corsAllowedOrigins the Allowed-Origin valuess to use for CORS.
   * @return this, so that the method may be called in a fluent manner.
   */
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public Parameters setCorsAllowedOrigins(List<String> corsAllowedOrigins) {
    this.corsAllowedOrigins = corsAllowedOrigins;
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
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
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
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public Parameters setSecrets(Map<String, ProtectedCredentials> secrets) {
    this.secrets = secrets;
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
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public JwtValidationConfig getJwt() {
    return jwt;
  }

  /**
   * The configuration of the JWT validator.
   * @param jwt the configuration of the JWT validator.
   * @return this, so that the method may be called in a fluent manner.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public Parameters setJwt(JwtValidationConfig jwt) {
    this.jwt = jwt;
    return this;
  }

  /**
   * Get the management endpoints (all under /manage) that should be enabled.
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
   * @return managementEndpoints the management endpoints (all under /manage) that should be enabled.
   * @see Parameters#setManagementEndpointPort
   *
   */
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

  /**
   * Get the port that the /manage endpoints should listen on.
   * <p>
   * In order to help secure the management endpoints they can be run on a secondary port.
   * <p>
   * If the managementEndpointPort is set to null (the default) the management endpoints will listen on the same port as the API.
   * This is the least secure option (but most convenient for the UI).
   * <p>
   * It the managementEndpointPort is negative the entire management endpoint setup will be disabled.
   *
   * @return the port that the /manage endpoints should listen on.
   */
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
   *
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
   *
   * @return the authentication configuration of the UI and REST API.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public SessionConfig getSession() {
    return session;
  }

  /**
   * Set the authentication configuration of the UI and REST API.
   *
   * @param session the authentication configuration of the UI and REST API.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public void setSession(SessionConfig session) {
    this.session = session;
  }

  /**
   * Get the configuration for individual processors.
   * <p>
   * Some processors have specific configuration options that are not appropriate for configuration in pipeline definitions, they are  controlled here.
   *
   * @return the configuration for individual processors.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public ProcessorConfig getProcessors() {
    return processors;
  }

  /**
   * Set the configuration for individual processors.
   * <p>
   * Some processors have specific configuration options that are not appropriate for configuration in pipeline definitions, they are  controlled here.
   *
   * @param processors set the configuration for individual processors.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public void setProcessors(ProcessorConfig processors) {
    this.processors = processors;
  }

  /**
   * Get the directory to contain cached output.
   * <p>
   * This is the on-disc caching of stream output, controlled by the cacheDuration value in individual pipelines.
   * <p>
   * The cache key is based on:
   * <UL>
   * <LI>The full request URL.
   * <LI>Headers:
   * <UL>
   * <LI>Accept
   * <LI>Accept-Encoding
   * </UL>
   * <LI>Token fields:
   * <UL>
   * <LI>aud
   * <LI>iss
   * <LI>sub
   * <LI>groups
   * <LI>roles
   * </UL>
   * </UL>
   *
   * Note that the fileHash must also match, but isn't built into the key (should usually match because of the use of the inclusion of full URL).
   * <p>
   * Note that the default value for the outputCacheDir is probably a bad choice for anything other than the simplest setups.
   *
   * @return the directory to contain cached output.
   */
  public String getOutputCacheDir() {
    return outputCacheDir;
  }

  /**
   * Set the directory to contain cached output.
   * <p>
   * This is the on-disc caching of stream output, controlled by the cacheDuration value in individual pipelines.
   * <p>
   * The cache key is based on:
   * <UL>
   * <LI>The full request URL.
   * <LI>Headers:
   * <UL>
   * <LI>Accept
   * <LI>Accept-Encoding
   * </UL>
   * <LI>Token fields:
   * <UL>
   * <LI>aud
   * <LI>iss
   * <LI>sub
   * <LI>groups
   * <LI>roles
   * </UL>
   * </UL>
   *
   * Note that the fileHash must also match, but isn't built into the key (should usually match because of the use of the inclusion of full URL).
   * <p>
   * Note that the default value for the outputCacheDir is probably a bad choice for anything other than the simplest setups.
   *
   * @param outputCacheDir the directory to contain cached output.
   */
  public void setOutputCacheDir(String outputCacheDir) {
    this.outputCacheDir = outputCacheDir.endsWith("/") || outputCacheDir.endsWith("\\") ? outputCacheDir : outputCacheDir + File.separator;
  }

  /**
   * Get the WriteStream buffer size.
   * 
   * The amount of data to cache before writing to the output stream.
   * <p>
   * Pipeline are run in parallel in Verticles, but the actual write to the network has to occur from the HttpServer Context.
   * <p>
   * If the context hop from the Verticle to the HttpServer context happens too often it will be a drain on CPU cycles
   * , but making it too large will cause the response to stutter.
   * <p>
   * Recommended Buffer Sizes 
   * <table border="1">
   *   <caption>Table of recommended buffer sizes</caption>
   *   <tr>
   *     <th>Buffer Size</th><th>When to Use It</th><th>Notes</th>
   *   </tr>
   *   <tr>
   *     <td>8KB</td><td>Low-latency, chatty streams Balanced throughput and responsiveness</td><td>Matches TCP segment size on many systems</td>
   *   </tr>
   *   <tr>
   *     <td>16KB</td><td>Balanced throughput and responsiveness</td><td>Good default for HTTP/1.1</td>
   *   </tr>
   *   <tr>
   *     <td>32KB</td><td>High-throughput, large payloads</td><td>Reduces write calls, but increases latency</td>
   *   </tr>
   *   <tr>
   *     <td>64KB</td><td>Bulk transfer (e.g. file streaming)</td><td>Only if memory pressure is low </td>
   *   </tr>
   * </table>
   * 
   * @return the WriteStream buffer size.
   */
  public int getWriteStreamBufferSize() {
    return writeStreamBufferSize;
  }

  /**
   * Set the WriteStream buffer size.
   * 
   * The amount of data to cache before writing to the output stream.
   * <p>
   * Pipeline are run in parallel in Verticles, but the actual write to the network has to occur from the HttpServer Context.
   * <p>
   * If the context hop from the Verticle to the HttpServer context happens too often it will be a drain on CPU cycles
   * , but making it too large will cause the response to stutter.
   * <p>
   * Recommended Buffer Sizes 
   * <table border="1">
   *   <caption>Table of recommended buffer sizes</caption>
   *   <tr>
   *     <th>Buffer Size</th><th>When to Use It</th><th>Notes</th>
   *   </tr>
   *   <tr>
   *     <td>8KB</td><td>Low-latency, chatty streams Balanced throughput and responsiveness</td><td>Matches TCP segment size on many systems</td>
   *   </tr>
   *   <tr>
   *     <td>16KB</td><td>Balanced throughput and responsiveness</td><td>Good default for HTTP/1.1</td>
   *   </tr>
   *   <tr>
   *     <td>32KB</td><td>High-throughput, large payloads</td><td>Reduces write calls, but increases latency</td>
   *   </tr>
   *   <tr>
   *     <td>64KB</td><td>Bulk transfer (e.g. file streaming)</td><td>Only if memory pressure is low </td>
   *   </tr>
   * </table>
   * 
   * @param writeStreamBufferSize the WriteStream buffer size.
   */
  public void setWriteStreamBufferSize(int writeStreamBufferSize) {
    this.writeStreamBufferSize = writeStreamBufferSize;
  }


  /**
   * Get the configuration of the handling of requests using basic authentication for data requests.
   * <P>
   * Note that when the IdP to use for validating the credentials is determined by the OpenID Discovery the
   * path from jwt.issuerHostPath will be appended to the host used to make the request.
   *
   * @return the configuration of the handling of requests using basic authentication for data requests.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public BasicAuthConfig getBasicAuth() {
    return basicAuth;
  }

  /**
   * Set the configuration of the handling of requests using basic authentication for data requests.
   * <P>
   * Note that when the IdP to use for validating the credentials is determined by the OpenID Discovery the
   * path from jwt.issuerHostPath will be appended to the host used to make the request.
   *
   * @param basicAuth the configuration of the handling of requests using basic authentication for data requests.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public void setBasicAuth(BasicAuthConfig basicAuth) {
    this.basicAuth = basicAuth;
  }

  /**
   * If set to false any basic auth header will be ignored.
   *
   * @return false if any basic auth header will be ignored.
   */
  public boolean isEnableBearerAuth() {
    return enableBearerAuth;
  }

  /**
   * If set to false any basic auth header will be ignored.
   *
   * @param enableBearerAuth false if any basic auth header will be ignored.
   */
  public void setEnableBearerAuth(boolean enableBearerAuth) {
    this.enableBearerAuth = enableBearerAuth;
  }

  /**
   * Get the URL to the minified OpenAPI Explorer JS that is to be used for displaying The API documentation.
   * The default value is (something like) "https://unpkg.com/openapi-explorer@2.2.733/dist/browser/openapi-explorer.min.js".
   * <p>
   * It is not usually necessary to set this.
   * @return the URL to the minified OpenAPI Explorer JS that is to be used for displaying The API documentation.
   */
  public String getOpenApiExplorerUrl() {
    return openApiExplorerUrl;
  }

  /**
   * Get the URL to the minified OpenAPI Explorer JS that is to be used for displaying The API documentation.
   * The default value is (something like) "https://unpkg.com/openapi-explorer@2.2.733/dist/browser/openapi-explorer.min.js".
   * <p>
   * It is not usually necessary to set this.
   *
   * @param openApiExplorerUrl the URL to the minified OpenAPI Explorer JS that is to be used for displaying The API documentation.
   */
  public void setOpenApiExplorerUrl(String openApiExplorerUrl) {
    this.openApiExplorerUrl = openApiExplorerUrl;
  }

  /**
   * Get the path to alternative documentation to make available.
   * <p>
   * Documentation for Query Engine is built in, but documents how to deploy Query Engine and configure its security.
   * In deployments aimed at clients it may be inappropriate to display this information.
   * <p>
   * This configuration allows for the built in documentation to be replaced with an alternative set aimed at your clients.
   * <p>
   * The alternativeDocumentation should be a directory containing a hierarchy of HTML files (and supporting resources).
   * The entire hierarchy will be read on startup and then served by the DocHandler - any changes to the contents of the directory will be ignored.
   * <p>
   * Soft links will not be followed.
   * <p>
   * The path must be valid (Query Engine will not start if it is not), but may be set to "/dev/null" in which case no documentation will be served at all.
   * @return the path to alternative documentation to make available.
   */
  public String getAlternativeDocumentation() {
    return alternativeDocumentation;
  }

  /**
   * Set the path to alternative documentation to make available.
   * <p>
   * Documentation for Query Engine is built in, but documents how to deploy Query Engine and configure its security.
   * In deployments aimed at clients it may be inappropriate to display this information.
   * <p>
   * This configuration allows for the built in documentation to be replaced with an alternative set aimed at your clients.
   * <p>
   * The alternativeDocumentation should be a directory containing a hierarchy of HTML files (and supporting resources).
   * The entire hierarchy will be read on startup and then served by the DocHandler - any changes to the contents of the directory will be ignored.
   * <p>
   * Soft links will not be followed.
   * <p>
   * The path must be valid (Query Engine will not start if it is not), but may be set to "/dev/null" in which case no documentation will be served at all.
   * @param alternativeDocumentation the path to alternative documentation to make available.
   */
  public void setAlternativeDocumentation(String alternativeDocumentation) {
    this.alternativeDocumentation = alternativeDocumentation;
  }

  /**
   * The additional data that is made available via the request object.
   * <p>
   * The {@link uk.co.spudsoft.query.exec.context.RequestContext} is made available in both
   * {@link uk.co.spudsoft.query.exec.conditions.ConditionInstance}s and various templates
   * (such as {@link uk.co.spudsoft.query.defn.SourceSql#queryTemplate} and {@link uk.co.spudsoft.query.defn.Endpoint#urlTemplate}).
   * By default this context contains information specific to the request, and very little information about
   * the service it is running in.
   * <P>
   * The entire requestContextEnvironment is also available (with no change)
   * using {@link uk.co.spudsoft.query.exec.context.RequestContext#getEnv}, providing a way to
   * add additional environmental information to the context.
   * @return the additional data that is made available via the request object.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public Map<String, String> getRequestContextEnvironment() {
    return requestContextEnvironment;
  }

  /**
   * The additional data that is made available via the request object.
   * <p>
   * The {@link uk.co.spudsoft.query.exec.context.RequestContext} is made available in both
   * {@link uk.co.spudsoft.query.exec.conditions.ConditionInstance}s and various templates
   * (such as {@link uk.co.spudsoft.query.defn.SourceSql#queryTemplate} and {@link uk.co.spudsoft.query.defn.Endpoint#urlTemplate}).
   * By default this context contains information specific to the request, and very little information about
   * the service it is running in.
   * <P>
   * The entire requestContextEnvironment is also available (with no change)
   * using {@link uk.co.spudsoft.query.exec.context.RequestContext#getEnv}, providing a way to
   * add additional environmental information to the context.
   * @param requestContextEnvironment the additional data that is made available via the request object.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Configuration parameter, should not be changed after being initialized by Jackson")
  public void setRequestContextEnvironment(Map<String, String> requestContextEnvironment) {
    this.requestContextEnvironment = requestContextEnvironment;
  }

  /**
   * Get the URL to redirect requests to / to.
   *
   * By default requests to / redirect to /openapi and display the OpenAPI docs.
   * This is not much use in a path hijack situation, so allow for the provision of an alternative.
   *
   * @return the URL to redirect requests to / to.
   */
  public String getRootRedirectUrl() {
    return rootRedirectUrl;
  }

  /**
   * Set the URL to redirect requests to / to.
   *
   * By default requests to / redirect to /openapi and display the OpenAPI docs.
   * This is not much use in a path hijack situation, so allow for the provision of an alternative.
   *
   * @param rootRedirectUrl the URL to redirect requests to / to.
   */
  public void setRootRedirectUrl(String rootRedirectUrl) {
    this.rootRedirectUrl = rootRedirectUrl;
  }

  /**
   * Get the security headers configuration.
   * <P>
   * The following response headers can be configured:
   * <ul>
   * <li>X-Frame-Options - Controls whether the page can be displayed in a frame</li>
   * <li>Referrer-Policy - Controls how much referrer information should be included with requests</li>
   * <li>Permissions-Policy - Controls which features and APIs can be used in the browser</li>
   * </ul>
   * <P>
   * All of these values are optional, the default values are secure, but may be too restrictive in some environments.
   *
   * @return the security headers configuration.
   */
  public SecurityHeadersConfig getSecurityHeaders() {
    return securityHeaders;
  }

  /**
   * Set the security headers configuration.
   * <P>
   * The following response headers can be configured:
   * <ul>
   * <li>X-Frame-Options - Controls whether the page can be displayed in a frame</li>
   * <li>Referrer-Policy - Controls how much referrer information should be included with requests</li>
   * <li>Permissions-Policy - Controls which features and APIs can be used in the browser</li>
   * </ul>
   * <P>
   * All of these values are optional, the default values are secure, but may be too restrictive in some environments.
   *
   * @param securityHeaders the security headers configuration.
   */
  public void setSecurityHeaders(SecurityHeadersConfig securityHeaders) {
    this.securityHeaders = securityHeaders;
  }

  /**
   * Get the enableForceJwt parameter.
   *
   * If true, the path /login/forcejwt can be PUT to create a session based on the the JWT in the message body.
   * This should be secure even in a production environment (because the caller must be still be able to create an acceptable JWT)
   * , but for the sake of safety it defaults to being disabled.
   * 
   * @return the enableForceJwt parameter.
   */
  public boolean isEnableForceJwt() {
    return enableForceJwt;
  }

  /**
   * Set the enableForceJwt parameter.
   *
   * If true, the path /login/forcejwt can be PUT to create a session based on the the JWT in the message body.
   * This should be secure even in a production environment (because the caller must be still be able to create an acceptable JWT)
   * , but for the sake of safety it defaults to being disabled.
   * 
   * @param enableForceJwt the enableForceJwt parameter.
   */
  public void setEnableForceJwt(boolean enableForceJwt) {
    this.enableForceJwt = enableForceJwt;
  }

  
  
  /**
   * Validate the provided parameters.
   *
   * @throws IllegalArgumentException if anything in the parameters is invalid.
   */
  @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "User has specified the path to use for alternative documentation")
  public void validate() throws IllegalArgumentException {
    if (logging != null) {
      logging.validate("logging");
    }
    if (tracing != null) {
      tracing.validate("tracing");
    }
    if (jwt != null) {
      jwt.validate("jwt");
    }
    if (session != null) {
      if (session.getOauth() != null && !session.getOauth().isEmpty()) {
        if (jwt == null) {
          throw new IllegalArgumentException("Sessions are configured with oauth without any acceptable jwt configuration.");
        } else {
          if (jwt.getJwksEndpoints() == null || jwt.getJwksEndpoints().isEmpty()) {
            throw new IllegalArgumentException("Sessions are configured with oauth without known JWKS endpoints being configured, please set jwt.jwksEndpoints.");
          }
        }
      }
      session.validate("session");
    }
    if (pipelineCache != null) {
      pipelineCache.validate("pipelineCache");
    }
    if (persistence != null) {
      persistence.validate("persistence");
    }
    if (basicAuth != null) {
      basicAuth.validate("basicAuth");
    }
    if (!Strings.isNullOrEmpty(alternativeDocumentation)) {
      if (!"/dev/null".equals(alternativeDocumentation)) {
        File altDocFile = new File(alternativeDocumentation);
        if (!altDocFile.isDirectory()) {
          throw new IllegalArgumentException("The alternativeDocumentation value does not point to a directory.");
        }
      }
    }
    if (writeStreamBufferSize < 1024) {
      throw new IllegalArgumentException("The writeStreamBufferSize cannot be less than 1024.");
    }
  }

}

