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

import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import uk.co.spudsoft.query.logging.LoggingConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxBuilder;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.cpu.CpuCoreSensor;
import io.vertx.core.json.Json;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.healthchecks.HealthChecks;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.micrometer.impl.PrometheusScrapingHandlerImpl;
import io.vertx.tracing.opentelemetry.OpenTelemetryTracingFactory;
import jakarta.ws.rs.core.Application;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import uk.co.spudsoft.dircache.DirCache;
import uk.co.spudsoft.jwtvalidatorvertx.IssuerAcceptabilityHandler;
import uk.co.spudsoft.jwtvalidatorvertx.JsonWebKeySetOpenIdDiscoveryHandler;
import uk.co.spudsoft.jwtvalidatorvertx.JwtValidator;
import uk.co.spudsoft.jwtvalidatorvertx.OpenIdDiscoveryHandler;
import uk.co.spudsoft.mgmt.ManagementRoute;
import uk.co.spudsoft.mgmt.ThreadDumpRoute;
import uk.co.spudsoft.params4j.ConfigurationProperty;
import uk.co.spudsoft.params4j.FileType;
import uk.co.spudsoft.params4j.Params4J;
import uk.co.spudsoft.query.exec.Auditor;
import uk.co.spudsoft.query.exec.AuditorMemoryImpl;
import uk.co.spudsoft.query.exec.AuditorPersistenceImpl;
import uk.co.spudsoft.query.exec.FilterFactory;
import uk.co.spudsoft.query.exec.JdbcHelper;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.filters.LimitFilter;
import uk.co.spudsoft.query.exec.filters.MapFilter;
import uk.co.spudsoft.query.exec.filters.OffsetFilter;
import uk.co.spudsoft.query.exec.filters.QueryFilter;
import uk.co.spudsoft.query.exec.filters.SortFilter;
import uk.co.spudsoft.query.exec.filters.WithoutFilter;
import uk.co.spudsoft.query.exec.procs.sort.ProcessorSortInstance;
import uk.co.spudsoft.query.json.ObjectMapperConfiguration;
import uk.co.spudsoft.query.logging.RequestCollatingAppender;
import static uk.co.spudsoft.query.main.TracingSampler.alwaysOff;
import static uk.co.spudsoft.query.main.TracingSampler.alwaysOn;
import static uk.co.spudsoft.query.main.TracingSampler.parent;
import static uk.co.spudsoft.query.main.TracingSampler.ratio;
import uk.co.spudsoft.query.main.sample.SampleDataLoader;
import uk.co.spudsoft.query.main.sample.SampleDataLoaderMsSQL;
import uk.co.spudsoft.query.main.sample.SampleDataLoaderMySQL;
import uk.co.spudsoft.query.main.sample.SampleDataLoaderPostgreSQL;
import uk.co.spudsoft.query.pipeline.PipelineDefnLoader;
import uk.co.spudsoft.query.web.LoggingRouter;
import uk.co.spudsoft.query.web.LoginDao;
import uk.co.spudsoft.query.web.LoginDaoMemoryImpl;
import uk.co.spudsoft.query.web.LoginDaoPersistenceImpl;
import uk.co.spudsoft.query.web.AuthenticationRouter;
import uk.co.spudsoft.query.web.LoginRouter;
import uk.co.spudsoft.query.web.QueryRouter;
import uk.co.spudsoft.query.web.SecurityHeadersRouter;
import uk.co.spudsoft.query.web.TableauWDCRouter;
import uk.co.spudsoft.query.web.UiRouter;
import uk.co.spudsoft.query.web.rest.AuthConfigHandler;
import uk.co.spudsoft.query.web.rest.DocHandler;
import uk.co.spudsoft.query.web.rest.FormIoHandler;
import uk.co.spudsoft.query.web.rest.HistoryHandler;
import uk.co.spudsoft.query.web.rest.InfoHandler;
import uk.co.spudsoft.query.web.rest.SessionHandler;
import uk.co.spudsoft.vertx.rest.JaxRsHandler;
import uk.co.spudsoft.vertx.rest.OpenApiHandler;
import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.urlconnection.URLConnectionSender;

/**
 * The main entry point for the Query Engine.
 *
 * The Query Engine uses dependency inversion as much as it can, but does not use any third party libraries for wiring everything together
 * (because wiring based on reflection is magic and slow).
 * So this class is mainly concerned with connecting all the components together.
 *
 * @author jtalbut
 */
@OpenAPIDefinition
public class Main extends Application {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  private static final String NAME = "query-engine";

  private MeterRegistry meterRegistry;
  private Vertx vertx;
  private HttpServer httpServer;

  private DirCache dirCache;
  private PipelineDefnLoader defnLoader;
  private Auditor auditor;

  private HealthChecks healthChecks;
  private HealthChecks upChecks;
  private final AtomicBoolean up = new AtomicBoolean();

  private OpenIdDiscoveryHandler openIdDiscoveryHandler;
  private JwtValidator jwtValidator;

  private static volatile OpenTelemetry openTelemetry;
  private static final Object OPEN_TELEMETRY_LOCK = new Object();

  private volatile int port;

  private JdbcHelper jdbcHelper;
  private Authenticator authenticator;


  /**
   * Constructor.
   */
  public Main() {
  }

  /**
   * Handler for completion of the Future returned by the {@link #innerMain(java.lang.String[], java.io.PrintStream, java.util.Map) } method.
   * @param result the {@link AsyncResult} found in the Future returned by {@link #innerMain(java.lang.String[], java.io.PrintStream, java.util.Map)}.
   */
  @ExcludeFromJacocoGenerated
  protected void mainCompletion(AsyncResult<Integer> result) {
    if (result.succeeded()) {
      int statusCode = result.result();
      if (statusCode > 0) {
        shutdown(statusCode);
      }
    } else {
      logger.error("Failed: ", result.cause());
      shutdown(-1);
    }
  }

  /**
   * Get the Vert.x instance.
   * @return the Vert.x instance.
   */
  protected Vertx getVertx() {
    return vertx;
  }

  /**
   * Get the running authenticator.
   * This is for tests only.
   * @return the running authenticator.
   */
  Authenticator getAuthenticator() {
    return authenticator;
  }
  
  /**
   * Get the {@link DirCache} for the {@link uk.co.spudsoft.query.defn.Pipeline} definitions.
   * @return the {@link DirCache} for the {@link uk.co.spudsoft.query.defn.Pipeline} definitions.
   */
  protected DirCache getDirCache() {
    return dirCache;
  }

  /**
   * Get the {@link PipelineDefnLoader}.
   * @return the {@link PipelineDefnLoader}.
   */
  protected PipelineDefnLoader getDefnLoader() {
    return defnLoader;
  }

  /**
   * Main method.
   * @param args Command line arguments that should have the same form as properties with the query-engine prefix, no dashes are required.
   *7
   */
  public static void main(String[] args) {
    Main main = new Main();
    main.innerMain(args, System.out, System.getenv()).onComplete(ar -> main.mainCompletion(ar));
    Thread shutdownHook = new Thread(() -> {
      logger.info("Shutdown hook called");
      if (logger.isDebugEnabled()) {
        try {
          logger.debug("Thread state: {}", ThreadDumpRoute.buildStackTraceJson());
        } catch (Throwable ex) {
        }
      }
      main.shutdown();
    });
    Runtime.getRuntime().addShutdownHook(shutdownHook);
  }

  /**
   * Shutdown the entire process.
   * <p>
   * Do not call this from tests.
   * @param statusCode The status code to return to the calling shell.
   */
  protected void shutdown(int statusCode) {
    shutdown();
    System.exit(statusCode);
  }

  /**
   * Shutdown Vert.x, but not the entire process.
   */
  public void shutdown() {
    Vertx localVertx = this.vertx;

    CountDownLatch latch = new CountDownLatch(1);

    logger.info("Performing graceful shutdown");
    Future<Void> httpServerCloseFuture = httpServer == null ? Future.succeededFuture() : httpServer.close();
    httpServerCloseFuture
            .compose(v -> {
              if (auditor == null) {
                return Future.succeededFuture();
              } else {
                return auditor.waitForOutstandingRequests(30000);
              }
            })
            .compose(v -> {
              if (jdbcHelper == null) {
                return Future.succeededFuture();
              } else {
                return jdbcHelper.shutdown(10000);
              }
            })
            .onComplete(ar -> {
              if (ar.failed()) {
                logger.error("Graceful shutdown failed: ", ar.cause());
              } else {
                logger.info("Graceful shutdown process completed successfully");
              }
              latch.countDown();
            });
    try {
      // Wait for shutdown to complete (with reasonable timeout)
      boolean completed = latch.await(60, TimeUnit.SECONDS);
      if (!completed) {
        logger.warn("Graceful shutdown timed out after 60 seconds");
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      logger.warn("Interrupted while waiting for graceful shutdown", ex);
    }

    if (localVertx != null) {
      localVertx.close();
    }
  }

  /**
   * Method to allow test code to call main with no risk of System.exit being called.
   * @param args Command line arguments.
   * @param stdout PrintStream that would be System.out for a non-test call.
   * @param env Map of environment variables (from {@link java.lang.System#getenv()} in a non-test environment).
   * @return The status code that would have been returned if this was a real command line execution.
   * @throws ExecutionException If the main function throws an exception.
   * @throws InterruptedException If the thread is interrupted whilst waiting for the main function to complete.
   */
  public int testMain(String[] args, PrintStream stdout, Map<String, String> env) throws ExecutionException, InterruptedException {
    CompletableFuture<Integer> future = new CompletableFuture<>();
    innerMain(args, stdout, env)
            .onSuccess(i -> {
              future.complete(i);
            })
            .onFailure(ex -> {
              future.completeExceptionally(ex);
            });
    int statusCode = future.get();
    if (statusCode > 0) {
      logger.warn("Should exit with code {}", statusCode);
    }
    return statusCode;
  }

  String getBaseConfigDir() {
    if (System.getProperty("os.name").toLowerCase().contains("win")) {
      return "target\\" + NAME;
    } else {
      return "/etc/" + NAME;
    }
  }

  /**
   * Get the port that is actually being used (which will be non-deterministic if httpServerOptions.port is zero (the default).
   * @return the port that is actually being used.
   */
  public int getPort() {
    return port;
  }

  /**
   * The actual, non-static, main method that sets up the Query Engine.
   * <p>
   * The static method {@link Main#main(java.lang.String[])} just creates a {@link Main} instance and calls this method on it.
   *
   * @param args The command line arguments.
   * @param stdout The output stream to use for messages.  Direct use of {@link System#out} is avoided for test reasons.
   * @param env Map of environment variables (from {@link java.lang.System#getenv()} in a non-test environment).
   * @return A Future that will be completed when everything is ready.  If the result of this Future is not zero the process will be shut down.
   */
  @SuppressFBWarnings(value = {"PATH_TRAVERSAL_IN", "POTENTIAL_XML_INJECTION"}, justification = "False positive, the dirs at this stage cannot be specified by the user")
  protected Future<Integer> innerMain(String[] args, PrintStream stdout, Map<String, String> env) {

    Params4J<Parameters> p4j = Params4J.<Parameters>factory()
            .withConstructor(() -> new Parameters())
            .withDirGatherer(new File(getBaseConfigDir()), FileType.Yaml)
            .withSecretsGatherer(new File(getBaseConfigDir() + "/conf.d").toPath(), 1000, 1000, 20, StandardCharsets.UTF_8)
            .withEnvironmentVariablesGatherer(NAME, false)
            .withSystemPropertiesGatherer(NAME)
            .withCommandLineArgumentsGatherer(args, "--")
            .create();

    for (String arg : args) {
      if ("-?".equals(arg) || "--help".equals(arg)) {
        StringBuilder usage = new StringBuilder();
        buildUsage(usage, p4j, true);
        stdout.println(usage.toString());
        return Future.succeededFuture(1);
      }
      if ("--helpenv".equals(arg)) {
        StringBuilder usage = new StringBuilder();
        List<ConfigurationProperty> propDocs = p4j.getDocumentation(
                new Parameters()
                , "--"
                , Arrays.asList(
                        Pattern.compile("java.lang.Boolean")
                )
                , Arrays.asList(
                        Pattern.compile(".*VertxOptions.*")
                        , Pattern.compile(".*HttpServerOptions.*")
                )
        );
        int maxNameLen = propDocs.stream().map(p -> p.name.length()).max(Integer::compare).get();

        usage.append("This is a list of environment variables understood by the query-engine.\n")
                .append("Call with --help for moire details of configuration\n")
                ;

        for (ConfigurationProperty prop : propDocs) {
          prop.appendEnv(usage, maxNameLen, "--", NAME, "\n");
        }
        stdout.println(usage.toString());
        return Future.succeededFuture(2);
      }
    }

    LoggingConfiguration.configureLogbackFromEnvironment((LoggerContext) LoggerFactory.getILoggerFactory(), env);
    Parameters params = p4j.gatherParameters();
    
    RequestCollatingAppender requestLoggingAppender = new RequestCollatingAppender();    
    LoggingConfiguration.configureLogback((LoggerContext) LoggerFactory.getILoggerFactory(), params.getLogging(), requestLoggingAppender);

    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
    java.util.logging.Logger.getLogger("").setLevel(Level.FINEST);

    ObjectMapperConfiguration.configureObjectMapper(DatabindCodec.mapper());

    try {
      logger.info("Params: {}", DatabindCodec.mapper().writeValueAsString(params));
    } catch (JsonProcessingException ex) {
      logger.error("Failed to convert params to json: ", ex);
    }

    try {
      params.validate();
    } catch (IllegalArgumentException ex) {
      StringBuilder usage = new StringBuilder();
      usage.append("Invalid parameter:\n");
      usage.append(ex.getMessage()).append("\n");
      buildUsage(usage, p4j, false);
      stdout.println(usage.toString());
      logger.error("Invalid parameter: {}", ex.getMessage());
      return Future.succeededFuture(3);
    } catch (Throwable ex) {
      StringBuilder usage = new StringBuilder();
      usage.append("Failed to validate parameters:\n");
      usage.append(ex.getClass().getName())
              .append("")
              .append(ex.getStackTrace()[0].getClassName())
              .append("@")
              .append(ex.getStackTrace()[0].getLineNumber())
              .append("\n");
      usage.append(ex.getMessage()).append("\n");
      buildUsage(usage, p4j, false);
      stdout.println(usage.toString());
      return Future.succeededFuture(4);
    }

    ProcessorSortInstance.setMemoryLimit(params.getProcessors().getInMemorySortLimitBytes());
    ProcessorSortInstance.setTempDir(params.getProcessors().getTempDir());

    VertxOptions vertxOptions = params.getVertxOptions();
    vertxOptions.setMetricsOptions(
            new MicrometerMetricsOptions()
                    .setJvmMetricsEnabled(true)
                    .setPrometheusOptions(
                            new VertxPrometheusOptions()
                                    .setEnabled(true)
                                    .setStartEmbeddedServer(false)
                    )
                    .setEnabled(true)
    );
    VertxBuilder vertxBuilder = Vertx.builder()
            .with(vertxOptions);

    openTelemetry = buildOpenTelemetry(params.getTracing());
    if (openTelemetry != null) {
      OpenTelemetryTracingFactory tracingFactory = new OpenTelemetryTracingFactory(openTelemetry);
      vertxBuilder = vertxBuilder.withTracer(tracingFactory);
    } else {
      logger.warn("OpenTelemetry NOT set");
    }

    vertx = vertxBuilder.build();
    meterRegistry = (PrometheusMeterRegistry) BackendRegistries.getDefaultNow();

    vertx.setPeriodic(Duration.ofHours(1).toMillis(), id -> {
      requestLoggingAppender.purgeOlderThanDefault();
    });
    
    LoginDao loginDao;
    OperatorsInstance operators = new OperatorsInstance(params.getOperators());
    if (params.getPersistence().getDataSource() != null
            && !Strings.isNullOrEmpty(params.getPersistence().getDataSource().getUrl())) {

      Credentials credentials = params.getPersistence().getDataSource().getUser();
      if (credentials == null) {
        credentials = params.getPersistence().getDataSource().getAdminUser();
      }
      this.jdbcHelper = new JdbcHelper(vertx, JdbcHelper.createDataSource(params.getPersistence().getDataSource(), credentials, meterRegistry));

      auditor = new AuditorPersistenceImpl(vertx, meterRegistry, params.getPersistence(), jdbcHelper, operators);
      loginDao = new LoginDaoPersistenceImpl(vertx, meterRegistry, params.getPersistence(), params.getSession().getPurgeDelay(), jdbcHelper);
    } else {
      logger.info("Persistence is not configured, using in-memory tracking of last {} runs", AuditorMemoryImpl.SIZE);
      auditor = new AuditorMemoryImpl(vertx, operators);
      loginDao = new LoginDaoMemoryImpl(params.getSession().getPurgeDelay());
    }
    try {
      auditor.prepare();
      logger.info("Persistence database prepared");
    } catch (Throwable ex) {
      logger.error("Failed to prepare audit database: ", ex);
      return Future.succeededFuture(-2);
    }

    try {
      loginDao.prepare();
    } catch (Throwable ex) {
      logger.error("Failed to prepare login data: ", ex);
      return Future.succeededFuture(-2);
    }

    httpServer = vertx.createHttpServer(params.getHttpServerOptions());
    try {
      File baseConfigFile = new File(params.getBaseConfigPath());
      prepareBaseConfigPath(baseConfigFile, params.getSampleDataLoads());
      dirCache = DirCache.cache(baseConfigFile.toPath()
              , params.getFileStabilisationDelaySeconds() >= 0 ? Duration.of(params.getFileStabilisationDelaySeconds(), ChronoUnit.SECONDS) : null
              , Pattern.compile("\\..*")
              , params.getFilePollPeriodSeconds() > 0 ? Duration.of(params.getFilePollPeriodSeconds(), ChronoUnit.SECONDS) : null
      );
      dirCache.setCallback(() -> {
        if (logger.isDebugEnabled()) {
          logger.debug("Known files changed to: {}", Json.encode(dirCache.getRoot()));
        }
      });
      defnLoader = new PipelineDefnLoader(meterRegistry, vertx, params.getPipelineCache(), dirCache);
    } catch (Throwable ex) {
      logger.error("Unable to config pipeline loader: ", ex);
      return Future.succeededFuture(7);
    }

    UpCheckHandler upCheck = new UpCheckHandler(up);
    healthChecks = HealthChecks.create(vertx);
    healthChecks.register("Up", upCheck);
    healthChecks.register("Auditor", auditor::healthCheck);
    upChecks = HealthChecks.create(vertx);
    upChecks.register("Up", upCheck);

    Router router = Router.router(vertx);
    Router mgmtRouter = Router.router(vertx);

    router.route().handler(new LoggingRouter(params.getRequestContextEnvironment()));
    List<String> logoUrls = new ArrayList<>();
    if (params.getSession() != null && params.getSession().getOauth() != null) {
      logoUrls = params.getSession().getOauth().values().stream().map(o -> o.getLogoUrl()).toList();
    }
    List<String> cspStyleSrcs = Arrays.asList(
              "'sha256-47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU='"
              , "https://cdn.form.io/"
    );
    List<String> cspContentSrcs = 
            Strings.isNullOrEmpty(params.getManagementEndpointUrl()) ? null : Arrays.asList(params.getManagementEndpointUrl());
    List<String> cspScriptSrcs = Arrays.asList("'unsafe-inline'", "'unsafe-eval'", "https://cdn.form.io/");
    List<String> cspManifestSrcs = Arrays.asList("'self'");
    SecurityHeadersConfig secHdrs = params.getSecurityHeaders();
    if (secHdrs == null) {
      router.route().handler(new SecurityHeadersRouter(logoUrls, cspStyleSrcs, cspContentSrcs, cspScriptSrcs, cspManifestSrcs, null, null, null));
    } else {
      router.route().handler(new SecurityHeadersRouter(logoUrls, cspStyleSrcs, cspContentSrcs, cspScriptSrcs, cspManifestSrcs
              , secHdrs.getXFrameOptions(), secHdrs.getReferrerPolicy(), secHdrs.getPermissionsPolicy()));
    }

    ManagementRoute.deployStandardMgmtEndpoints(mgmtRouter, router, params.getManagementEndpoints(), new AtomicReference<>(params));
    if (ManagementRoute.mgmtEndpointPermitted(params.getManagementEndpoints(), "up")) {
      mgmtRouter.get("/up").handler(new HealthCheckHandler(upChecks)).setName("Up");
    }
    if (ManagementRoute.mgmtEndpointPermitted(params.getManagementEndpoints(), "health")) {
      mgmtRouter.get("/health").handler(new HealthCheckHandler(healthChecks)).setName("Health");
    }
    if (ManagementRoute.mgmtEndpointPermitted(params.getManagementEndpoints(), "prometheus")) {
      mgmtRouter.get("/prometheus").handler(new PrometheusScrapingHandlerImpl()).setName("Prometheus");
    }
    if (ManagementRoute.mgmtEndpointPermitted(params.getManagementEndpoints(), "dircache")) {
      DirCacheManagementRoute.createAndDeploy(mgmtRouter, dirCache);
    }

    CorsHandler corsHandler = CorsHandler.create();
    if (!params.getCorsAllowedOrigins().isEmpty()) {
      corsHandler.addOrigins(params.getCorsAllowedOrigins());
    }
    corsHandler.allowedMethods(
            ImmutableSet
                    .<HttpMethod>builder()
                    .add(HttpMethod.GET)
                    .add(HttpMethod.PUT)
                    .add(HttpMethod.DELETE)
                    .add(HttpMethod.POST)
                    .build()
    );
    corsHandler.allowCredentials(true);
    router.route("/*").handler(corsHandler);

    try {
      this.authenticator = createAuthenticator(params, loginDao);
    } catch (Throwable ex) {
      logger.error("Failed to create request context builder: ", ex);
      return Future.succeededFuture(-2);
    }

    FilterFactory filterFactory = createFilterFactory();

    List<Object> controllers = new ArrayList<>();
    boolean requireSession = params.getSession().isRequireSession();
    controllers.add(new InfoHandler(defnLoader, outputAllErrorMessages(), requireSession));
    controllers.add(new DocHandler(params.getAlternativeDocumentation(), outputAllErrorMessages(), requireSession));
    controllers.add(new FormIoHandler(defnLoader, filterFactory, outputAllErrorMessages(), requireSession));
    controllers.add(new AuthConfigHandler(params.getSession().getOauth()));
    controllers.add(new SessionHandler(outputAllErrorMessages(), requireSession));

    controllers.add(new HistoryHandler(auditor, outputAllErrorMessages()));
    addExtraControllers(params, controllers);
    List<Object> providers = Arrays.asList(
        new JacksonJsonProvider(PipelineDefnLoader.JSON_OBJECT_MAPPER, JacksonJsonProvider.BASIC_ANNOTATIONS)
    );

    OpenAPIConfiguration openApiConfig = createOpenapiConfiguration(controllers, this, "QueryEngine");
    OpenApiHandler openApiHandler = new OpenApiHandler(this, openApiConfig, "/api", params.getOpenApiExplorerUrl());
    ModelConverters.getInstance(true).addConverter(new OpenApiModelConverter());

    PipelineExecutor pipelineExecutor = PipelineExecutor.create(meterRegistry, auditor, filterFactory, params.getSecrets());
    vertx.fileSystem().mkdirs(params.getOutputCacheDir());
    
    int pipelineVerticleInstances = params.getVertxOptions().getEventLoopPoolSize();
    if (pipelineVerticleInstances < 1) {
      pipelineVerticleInstances = 2 * CpuCoreSensor.availableProcessors();
    }
    
    QueryRouter queryRouter = new QueryRouter(vertx
            , meterRegistry
            , auditor
            , authenticator
            , defnLoader
            , pipelineExecutor
            , requestLoggingAppender
            , params.getOutputCacheDir()
            , params.getWriteStreamBufferSize()
            , params.getResponseWriteQueueMaxSize()
            , outputAllErrorMessages()
            , pipelineVerticleInstances
    );
    
    router.route(QueryRouter.PATH_ROOT + "/*").handler(queryRouter);

    ManagementRoute.createAndDeploy(vertx
            , router, params.getHttpServerOptions()
            , params.getManagementEndpointPort()
            , corsHandler
            , mgmtRouter
            , params.getManagementEndpointUrl()
    );

    router.get("/api").handler(rc -> {
      rc.response().setStatusCode(301).putHeader("Location", "../openapi").end();
    });
    AuthenticationRouter rch = new AuthenticationRouter(vertx, authenticator, outputAllErrorMessages());
    router.route("/api/*").handler(rch);
    router.route("/api/*").handler(new JaxRsHandler(vertx, meterRegistry, "/api", controllers, providers, true, false));
    router.route("/ui/*").handler(UiRouter.create(vertx, meterRegistry, "/ui", "/www", "/www/index.html"));
    router.route("/tableau*").handler(TableauWDCRouter.create(vertx));
    router.getWithRegex("/openapi\\..*").blockingHandler(openApiHandler);
    router.get("/openapi").handler(openApiHandler.getUiHandler());
    if (!params.getSession().getOauth().isEmpty() || params.isEnableForceJwt()) {
      LoginRouter loginRouter = LoginRouter.create(vertx, loginDao, openIdDiscoveryHandler, jwtValidator, authenticator, params.getSession()
              , params.getJwt().getRequiredAudiences(), outputAllErrorMessages(), params.isEnableForceJwt(), params.getSession().getSessionCookie());
      if (params.isEnableForceJwt()) {
        router.put("/login/forcejwt").handler(loginRouter);
      }
      if (!params.getSession().getOauth().isEmpty()) {
        router.get("/login").handler(loginRouter);
        router.get("/login/return").handler(loginRouter);
        router.get("/logout").handler(loginRouter);
      }
    }
    router.route("/").handler(rc -> {
      rc.response().setStatusCode(307);
      if (Strings.isNullOrEmpty(params.getRootRedirectUrl())) {
        rc.redirect("/ui/");
      } else {
        rc.redirect(params.getRootRedirectUrl());
      }
    });
    
    addExtraRoutes(params, router);

    return queryRouter.deploy()
            .compose(v -> {
              return httpServer
                      .requestHandler(router)
                      .listen();
            })
            .compose(svr -> {
              this.port = svr.actualPort();
              if (!params.getSampleDataLoads().isEmpty()) {
                return performSampleDataLoads(params.getTempDir(), params.getSampleDataLoads().iterator());
              } else {
                return Future.succeededFuture();
              }
            })
            .compose(v -> {
              logger.info("Started on port {}", port);
              if (params.isExitOnRun()) {
                return Future.succeededFuture(9);
              }
              return Future.succeededFuture(0);
            })
            .onSuccess(i -> up.set(true))
            ;

  }

  @SuppressFBWarnings("POTENTIAL_XML_INJECTION")
  void buildUsage(StringBuilder usage, Params4J<Parameters> p4j, boolean withProps) {
    List<ConfigurationProperty> propDocs = p4j.getDocumentation(
            new Parameters()
            , "--"
            , Arrays.asList(
                    Pattern.compile(".*Condition.*")
                    , Pattern.compile("java.lang.Boolean")
            )
            , Arrays.asList(
                    Pattern.compile(".*VertxOptions.*")
                    , Pattern.compile(".*HttpServerOptions.*")
            )
    );
    int maxNameLen = propDocs.stream().map(p -> p.name.length()).max(Integer::compare).get();
    usage.append("Usage: query-engine [PROPERTIES]\n")
            .append("The query-engine is intended to be used within a container, running outside of a container is only useful to output this usage\n")
            .append("Configuration may be set by (in priority order):\n")
            .append("  any yaml files in ").append(getBaseConfigDir()).append("\n")
            .append("  a file per property in a dir hierarchy matching the property structure starting at at ").append(getBaseConfigDir()).append("/conf.d\n")
            .append("  environment variables (call with --helpenv to see a list of the main environment variables)\n")
            .append("  system properties (-D").append(NAME).append(".baseConfigPath is equivalent to --baseConfigPath\n")
            .append("  command line arguments as listed below\n")
            .append("Properties may be:\n")
            .append("  simple values\n")
            .append("  maps (in which case '<xxx>' should be replaced with the key value\n")
            .append("  arrays (in which case '<n>' should be replaced with an integer (and they should start at zero and be contiguous!)\n")
            .append("  undocumented arrays (in which case '<***>' should be replaced with the path to a property in the appropriate class\n")
            .append("The full set of parameters is logged at INFO level on startup and can be used to determine the loaded configuration\n")
            .append("\n")
            .append("    --help").append(" ".repeat(maxNameLen + 1 - 6)).append("display this help text\n")
            .append("    --helpenv").append(" ".repeat(maxNameLen + 1 - 9)).append("display the environment variable form of this help\n")
            ;
    for (ConfigurationProperty prop : propDocs) {
      prop.appendUsage(usage, maxNameLen, "\n");
    }
  }

  FilterFactory createFilterFactory() {
    FilterFactory filterFactory = new FilterFactory(
            Arrays.asList(
                    new LimitFilter()
                    , new OffsetFilter()
                    , new QueryFilter()
                    , new MapFilter()
                    , new SortFilter()
                    , new WithoutFilter()
            )
    );
    return filterFactory;
  }

  private Future<Void> performSampleDataLoads(String tempDir, Iterator<DataSourceConfig> iter) {
    if (iter.hasNext()) {
      DataSourceConfig source = iter.next();
      String url = source.getUrl();
      SampleDataLoader loader;
      if (Strings.isNullOrEmpty(tempDir)) {
        tempDir = System.getProperty("java.io.tmpdir");
      }
      String basePath = (tempDir.endsWith("/") ? tempDir + "database-locks" : tempDir + File.separator + "database-locks");

      if (Strings.isNullOrEmpty(url)) {
        logger.warn("No URL configured for sample data loader {}", source);
        return performSampleDataLoads(basePath, iter);
      } else if (url.startsWith("mysql")) {
        loader = new SampleDataLoaderMySQL(basePath);
      } else if (url.startsWith("sqlserver")) {
        loader = new SampleDataLoaderMsSQL(basePath);
      } else if (url.startsWith("postgresql")) {
        loader = new SampleDataLoaderPostgreSQL(basePath);
      } else {
        logger.warn("No sample data loader found for {}", url);
        return performSampleDataLoads(basePath, iter);
      }
      logger.info("Using sample data loader {} to load to {}", loader.getName(), url);
      Credentials user = source.getAdminUser();
      if (user == null || (Strings.isNullOrEmpty(user.getUsername()) && Strings.isNullOrEmpty(user.getPassword()))) {
        user = source.getUser();
      }
      return loader.prepareTestDatabase(vertx, url, user.getUsername(), user.getPassword())
              .onSuccess(v -> {
                logger.info("Completed sample data load {}", loader.getName());
              })
              .recover(ex -> {
                logger.warn("Failed to prepare {} sample data: ", loader.getName(), ex);
                return Future.succeededFuture();
              })
              .compose(v -> performSampleDataLoads(basePath, iter));
    } else {
      return Future.succeededFuture();
    }
  }

  /**
   * Return a description of the type of thing that a file on disc is.
   * <p>
   * This is used solely to make log records more useful, see {@link #prepareBaseConfigPath(java.io.File, java.util.List)}.
   * @param file The file being described.
   * @return A simple string that is either "directory" or "file" or "does not exist".
   */
  public static String fileType(File file) {
    if (file.exists()) {
      if (file.isDirectory()) {
        return "directory";
      } else {
        return "file";
      }
    } else {
      return "does not exist";
    }
  }

  /**
   * Create the base config directory, and fill it with the sample files if it is currently empty.
   * <p>
   * If sample data loads have been requested, any sample pipeline definitions that reference localhost endpoints of that type will be updated with the URL from the sample data load.
   * @param baseConfigFile the base config directory.
   * @param sampleDataLoads details of the sample data loads that have been requested.
   */
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
  public static void prepareBaseConfigPath(File baseConfigFile, List<DataSourceConfig> sampleDataLoads) {
    if (!baseConfigFile.exists()) {
      logger.info("Creating base config dir at {}", baseConfigFile);
      baseConfigFile.mkdirs();
    }
    String[] children = baseConfigFile.list();
    if (children == null) {
      logger.warn("Base config dir ({}) is not a directory ({})", baseConfigFile, fileType(baseConfigFile));
    } else {
      if (children.length == 0) {
        logger.info("Creating sample configs");
        extractSampleFile(baseConfigFile, "samples/args/Args00.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/args/Args01.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/args/Args02.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/args/Args03.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/args/Args04.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/args/Args05.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/args/Args06.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/args/Args07.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/args/Args08.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/args/Args09.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/args/Args10.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/args/Args11.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/args/Args12.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/args/Args13.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/args/Args14.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/args/Args15.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/args/Args16.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/demo/FeatureRichExample.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/demo/LookupValues.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/sub1/sub2/AllDynamicIT.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/sub1/sub2/ConcurrentRulesIT.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/sub1/sub2/ConditionalArgument.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/sub1/sub2/DemoStatic.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/sub1/sub2/DynamicEndpointPipelineJdbcIT.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/sub1/sub2/DynamicEndpointPipelineSqlIT.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/sub1/sub2/EmptyDataIT.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/sub1/sub2/JsonToPipelineIT.json", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/sub1/sub2/LookupIT.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/sub1/sub2/SortableIT.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/sub1/sub2/TemplatedJsonToPipelineIT.json.vm", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/sub1/sub2/TemplatedYamlToPipelineIT.yaml.vm", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/sub1/sub2/TestData.yaml.vm", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/sub1/sub2/YamlToPipelineIT.yaml", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/sub1/sub2/permissions.jexl", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/sub1/permissions.jexl", sampleDataLoads);
        extractSampleFile(baseConfigFile, "samples/permissions.jexl", sampleDataLoads);
      } else {
        logger.info("Not creating sample configs because {} already contains {} files", baseConfigFile, children.length);
      }
    }
  }

  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
  private static void extractSampleFile(File baseConfigDir, String path, List<DataSourceConfig> sampleDataLoads) {
    try {
      File destFile = new File(baseConfigDir, path.substring(8));
      File destParent = destFile.getParentFile();

      if (!destParent.exists()) {
        destParent.mkdirs();
      }

      String fileContents;
      try (InputStream is = Main.class.getResourceAsStream("/" + path)) {
        fileContents = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      }

      if (sampleDataLoads != null) {
        for (DataSourceConfig sampleDataLoad : sampleDataLoads) {
          String destUrl = sampleDataLoad.getUrl();
          if (!Strings.isNullOrEmpty(destUrl)) {
            int idx = destUrl.indexOf(":");
            if (idx > 0) {
              String scheme = destUrl.substring(0, idx);
              fileContents = fileContents.replaceAll(scheme + "://localhost:[0-9]+/test", destUrl);

              int idx2 = destUrl.indexOf(":", idx + 1);
              if (idx2 > 0) {
                String urlWithoutPort = destUrl.substring(0, idx2) + ":";
                fileContents = fileContents.replaceAll(scheme + "://localhost:", urlWithoutPort);
              }
            }
          }
        }
      }

      try (OutputStream os = new FileOutputStream(destFile)) {
        os.write(fileContents.getBytes(StandardCharsets.UTF_8));
      }

    } catch (Throwable ex) {
      logger.warn("Failed to copy sample {}: ", path, ex);
    }
  }

  /**
   * Create the {@link Authenticator} that will be used for updating {@link uk.co.spudsoft.query.exec.context.RequestContext} instances.
   * @param params the {@link Parameters} for configuring the {@link Authenticator}.
   * @param loginDao this {@link LoginDao} passed to the {@link Authenticator}.
   * @return a newly created {@link Authenticator}.
   */
  protected Authenticator createAuthenticator(Parameters params, LoginDao loginDao) {
    JwtValidationConfig jwtConfig = params.getJwt();
    IssuerAcceptabilityHandler iah = IssuerAcceptabilityHandler.create(jwtConfig.getAcceptableIssuerRegexes()
                    , jwtConfig.getAcceptableIssuersFile()
                    , jwtConfig.getFilePollPeriodDuration()
            );

    WebClient webClient = WebClient.create(vertx, new WebClientOptions().setConnectTimeout(60000));
    if (!jwtConfig.getJwksEndpoints().isEmpty()) {
      openIdDiscoveryHandler = JsonWebKeySetOpenIdDiscoveryHandler.create(webClient, iah, jwtConfig.getDefaultJwksCacheDuration());
      jwtValidator = JwtValidator.createStatic(webClient, jwtConfig.getJwksEndpoints(), jwtConfig.getDefaultJwksCacheDuration(), iah);
    } else {
      JsonWebKeySetOpenIdDiscoveryHandler jsonWebKeySetHandler = JsonWebKeySetOpenIdDiscoveryHandler.create(webClient, iah, jwtConfig.getDefaultJwksCacheDuration());
      openIdDiscoveryHandler = jsonWebKeySetHandler;
      jwtValidator = JwtValidator.create(jsonWebKeySetHandler, iah);
    }
    jwtValidator.setRequireExp(jwtConfig.isRequireExp());
    jwtValidator.setRequireNbf(jwtConfig.isRequireNbf());
    if (jwtConfig.getPermittedTimeSkew() != null) {
      jwtValidator.setTimeLeeway(jwtConfig.getPermittedTimeSkew());
    }

    Authenticator auther = new Authenticator(WebClient.create(vertx, new WebClientOptions().setConnectTimeout(60000))
            , jwtValidator
            , openIdDiscoveryHandler
            , loginDao
            , meterRegistry
            , params.getBasicAuth()
            , params.isEnableBearerAuth()
            , params.getOpenIdIntrospectionHeaderName()
            , jwtConfig.getJwksEndpoints() == null || jwtConfig.getJwksEndpoints().isEmpty()
            , jwtConfig.getIssuerHostPath()
            , jwtConfig.getRequiredAudiences()
            , params.getSession().getSessionCookie() != null ? params.getSession().getSessionCookie().getName() : null
    );
    
    vertx.setPeriodic(Duration.ofHours(1).toMillis(), id -> {
      auther.purgeCache();
    });
    
    return auther;    
  }

  static OpenAPIConfiguration createOpenapiConfiguration(List<Object> resources, Object application, String openApiContextId) {
    return new SwaggerConfiguration()
            .resourceClasses(Stream.concat(resources.stream(), Stream.of(application)).map(r -> r.getClass().getCanonicalName())
                    .collect(Collectors.toSet()))
            .id(openApiContextId)
            .prettyPrint(true)
            .filterClass("uk.co.spudsoft.query.main.OpenApiFilterClass")
            .openAPI31(Boolean.TRUE)
            .openAPI(
                    new OpenAPI()
                            .info(
                                    new Info()
                                            .title(Version.MAVEN_PROJECT_NAME)
                                            .version(Version.MAVEN_PROJECT_VERSION)
                            )
            );
  }

  static OpenTelemetry buildOpenTelemetry(TracingConfig config) {

    // Check if GlobalOpenTelemetry is already set
    synchronized (OPEN_TELEMETRY_LOCK) {
      if (openTelemetry == null) {
        openTelemetry = createOpenTelemetry(config);
      }
    }
    return openTelemetry;
  }

  static OpenTelemetry createOpenTelemetry(TracingConfig config) {
    ResourceBuilder resourceBuilder = Resource.getDefault().toBuilder()
            .put("service.name", config.getServiceName())
            .put("service.version", Version.MAVEN_PROJECT_VERSION)
            ;

    Sampler sampler;
    SdkTracerProviderBuilder builder = SdkTracerProvider.builder()
            ;

    if (config.getProtocol() != TracingProtocol.none && !Strings.isNullOrEmpty(config.getUrl())) {
      SpanExporter spanExporter = spanExporter(config);
      if (spanExporter != null) {
        builder.addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build());
      }
      sampler = sampler(config);
    } else {
      sampler = Sampler.alwaysOff();
    }

    SdkTracerProvider sdkTracerProvider = builder
            .setResource(resourceBuilder.build())
            .setSampler(sampler)
            .build();

    TextMapPropagator propagator = switch (config.getPropagator()) {
      case b3 -> B3Propagator.injectingSingleHeader();
      case b3multi -> B3Propagator.injectingMultiHeaders();
      case w3c -> W3CTraceContextPropagator.getInstance();
    };
    OpenTelemetrySdkBuilder openTelemetryBuilder = OpenTelemetrySdk.builder()
            .setTracerProvider(sdkTracerProvider)
            .setPropagators(ContextPropagators.create(propagator))
            ;

    logger.debug("Building OpenTelemetry");
    try {
      return openTelemetryBuilder.buildAndRegisterGlobal();
    } catch (IllegalStateException ex) {
      logger.warn("Failed to build and registry OpenTelemetry: {}", ex.getMessage());
      return GlobalOpenTelemetry.get();
    }
  }

  static Sampler sampler(TracingConfig config) {
    Sampler sampler;
    sampler = switch (config.getSampler()) {
      case alwaysOff ->
            Sampler.alwaysOff();
      case alwaysOn ->
            Sampler.alwaysOn();
      case parent ->
            Sampler.parentBased(switch (config.getRootSampler()) {
              case alwaysOff ->
                      Sampler.alwaysOff();
              case alwaysOn ->
                      Sampler.alwaysOn();
              case parent ->
                      Sampler.alwaysOff();
              case ratio ->
                      Sampler.traceIdRatioBased(config.getSampleRatio());
            });
      case ratio ->
            Sampler.traceIdRatioBased(config.getSampleRatio());
    };
    return sampler;
  }

  static SpanExporter spanExporter(TracingConfig config) {
    if (Strings.isNullOrEmpty(config.getUrl())) {
      return null;
    }
    SpanExporter spanExporter = switch (config.getProtocol()) {
      case none -> null;
      case otlphttp -> OtlpHttpSpanExporter.builder()
              .setEndpoint(config.getUrl())
              .build();
      case zipkin -> ZipkinSpanExporter.builder()
              .setSender((BytesMessageSender) URLConnectionSender.create(config.getUrl()))
              .setEndpoint(config.getUrl())
              .build();
    };
    return spanExporter;
  }

  /**
   * Should error messages be returned in detail to callers?
   * <p>
   * For a production deployment the answer is always no.
   * In Design Mode it is useful to output all full details of errors because it should only be used in a trusted environment.
   *
   * @return false.
   */
  protected boolean outputAllErrorMessages() {
    return false;
  }

  /**
   * Allow subclasses to provide additional JAX-RS controllers.
   * <p>
   * This is used by Design Mode to provide write access to the pipeline definitions.
   *
   * @param params the Parameters object that may be required to configure the additional controllers.
   * @param controllers the {@link List} of JAX-RS controllers that will be appended to.
   */
  protected void addExtraControllers(Parameters params, List<Object> controllers) {
  }
  
  /**
   * Allow subclasses to provide additional Vertx-web routes
   * <p>
   * This is used by Design Mode to provide the test-auth endpoints.
   *
   * @param params the Parameters object that may be required to configure the additional controllers.
   * @param router the top level {@link Router} that may be modified.
   */
  protected void addExtraRoutes(Parameters params, Router router) {    
  }

}
