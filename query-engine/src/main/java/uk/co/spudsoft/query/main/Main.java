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
import io.micrometer.prometheus.PrometheusMeterRegistry;
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
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.micrometer.impl.PrometheusScrapingHandlerImpl;
import io.vertx.tracing.opentelemetry.OpenTelemetryOptions;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
import uk.co.spudsoft.params4j.ConfigurationProperty;
import uk.co.spudsoft.params4j.FileType;
import uk.co.spudsoft.params4j.Params4J;
import uk.co.spudsoft.query.exec.Auditor;
import uk.co.spudsoft.query.exec.AuditorMemoryImpl;
import uk.co.spudsoft.query.exec.AuditorPersistenceImpl;
import uk.co.spudsoft.query.exec.FilterFactory;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineExecutorImpl;
import uk.co.spudsoft.query.exec.conditions.RequestContextBuilder;
import uk.co.spudsoft.query.exec.filters.LimitFilter;
import uk.co.spudsoft.query.exec.filters.MapFilter;
import uk.co.spudsoft.query.exec.filters.OffsetFilter;
import uk.co.spudsoft.query.exec.filters.QueryFilter;
import uk.co.spudsoft.query.exec.filters.SortFilter;
import uk.co.spudsoft.query.exec.procs.sort.ProcessorSortInstance;
import uk.co.spudsoft.query.json.ObjectMapperConfiguration;
import uk.co.spudsoft.query.logging.VertxMDCSpanProcessor;
import static uk.co.spudsoft.query.main.TracingSampler.alwaysOff;
import static uk.co.spudsoft.query.main.TracingSampler.alwaysOn;
import static uk.co.spudsoft.query.main.TracingSampler.parent;
import static uk.co.spudsoft.query.main.TracingSampler.ratio;
import uk.co.spudsoft.query.main.sample.SampleDataLoader;
import uk.co.spudsoft.query.main.sample.SampleDataLoaderMsSQL;
import uk.co.spudsoft.query.main.sample.SampleDataLoaderMySQL;
import uk.co.spudsoft.query.main.sample.SampleDataLoaderPostgreSQL;
import uk.co.spudsoft.query.pipeline.PipelineDefnLoader;
import uk.co.spudsoft.query.web.LoginDao;
import uk.co.spudsoft.query.web.LoginDaoMemoryImpl;
import uk.co.spudsoft.query.web.LoginDaoPersistenceImpl;
import uk.co.spudsoft.query.web.RequestContextHandler;
import uk.co.spudsoft.query.web.LoginRouter;
import uk.co.spudsoft.query.web.QueryRouter;
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
  
  private HealthCheckHandler healthCheckHandler;
  private HealthCheckHandler upCheckHandler;
  private final AtomicBoolean up = new AtomicBoolean();
  
  private OpenIdDiscoveryHandler openIdDiscoveryHandler;
  private JwtValidator jwtValidator;

  
  private int port;

  /**
   * Constructor.
   */
  public Main() {
  }
  
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

  protected Vertx getVertx() {
    return vertx;
  }

  protected DirCache getDirCache() {
    return dirCache;
  }

  protected PipelineDefnLoader getDefnLoader() {
    return defnLoader;
  }
  
  /**
   * Main method.
   * @param args Command line arguments that should have the same form as properties with the query-engine prefix, no dashes are required.
   *   
   */
  public static void main(String[] args) {
    Main main = new Main();
    main.innerMain(args, System.out).onComplete(ar -> main.mainCompletion(ar));
  }
  
  protected void shutdown(int statusCode) {
    shutdown();
    System.exit(statusCode);                
  }
  
  public void shutdown() {
    Vertx v = this.vertx;
    if (v != null) {
      v.close();
    }
  }
  
  /**
   * Method to allow test code to call main with no risk of System.exit being called.
   * @param args Command line arguments.
   * @param stdout PrintStream that would be System.out for a non-test call.
   * @return The status code that would have been returned if this was a real command line execution.
   * @throws ExecutionException If the main function throws an exception.
   * @throws InterruptedException If the thread is interrupted whilst waiting for the main function to complete.
   */
  public int testMain(String[] args, PrintStream stdout) throws ExecutionException, InterruptedException {    
    CompletableFuture<Integer> future = new CompletableFuture<>();
    innerMain(args, stdout)
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

  public int getPort() {
    return port;
  }
      
  @SuppressFBWarnings(value = {"PATH_TRAVERSAL_IN", "POTENTIAL_XML_INJECTION"}, justification = "False positive, the dirs at this stage cannot be specified by the user")
  protected Future<Integer> innerMain(String[] args, PrintStream stdout) {
        
    Params4J<Parameters> p4j = Params4J.<Parameters>factory()
            .withConstructor(() -> new Parameters())
            .withDirGatherer(new File(getBaseConfigDir()), FileType.Yaml)
            .withSecretsGatherer(new File(getBaseConfigDir() + "/conf.d").toPath(), 0, 0, 0, StandardCharsets.UTF_8)
            .withEnvironmentVariablesGatherer(NAME, false)
            .withSystemPropertiesGatherer(NAME)
            .withCommandLineArgumentsGatherer(args, "--")
            .create();
    
    logger.trace("Args: {}", (Object) args);
    
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
        return Future.succeededFuture(1);
      }
    }
        
    Parameters params = p4j.gatherParameters();
    LoggingConfiguration.configureLogback((LoggerContext) LoggerFactory.getILoggerFactory(), params.getLogging());

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
      return Future.succeededFuture(1);
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
      return Future.succeededFuture(1);
    }
    
    ProcessorSortInstance.setMemoryLimit(params.getProcessors().getInMemorySortLimitBytes());
    ProcessorSortInstance.setTempDir(params.getProcessors().getTempDir());
    
    this.port = params.getHttpServerOptions().getPort();
    
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
    OpenTelemetry openTelemetry = buildOpenTelemetry(params.getTracing());
    if (openTelemetry != null) {
      vertxOptions.setTracingOptions(new OpenTelemetryOptions(openTelemetry));
    }
    vertx = Vertx.vertx(vertxOptions);
    meterRegistry = (PrometheusMeterRegistry) BackendRegistries.getDefaultNow();
    
    LoginDao loginDao;
    if (params.getPersistence().getDataSource() != null 
            && !Strings.isNullOrEmpty(params.getPersistence().getDataSource().getUrl())) {
      auditor = new AuditorPersistenceImpl(vertx, meterRegistry, params.getPersistence());
      loginDao = new LoginDaoPersistenceImpl(vertx, meterRegistry, params.getPersistence(), params.getSession().getPurgeDelay());
    } else {
      logger.info("Persistence is not configured, using in-memory tracking of last {} runs", AuditorMemoryImpl.SIZE);
      auditor = new AuditorMemoryImpl();
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
      dirCache = DirCache.cache(baseConfigFile.toPath(), Duration.of(params.getFileStabilisationDelaySeconds(), ChronoUnit.SECONDS), Pattern.compile("\\..*"));
      defnLoader = new PipelineDefnLoader(meterRegistry, vertx, params.getPipelineCache(), dirCache);
    } catch (Throwable ex) {
      logger.error("Unable to config pipeline loader: ", ex);
      return Future.succeededFuture(2);
    }

    UpCheckHandler upCheck = new UpCheckHandler(up);
    healthCheckHandler = HealthCheckHandler.create(vertx);
    healthCheckHandler.register("Up", upCheck);
    healthCheckHandler.register("Auditor", auditor::healthCheck);
    upCheckHandler = HealthCheckHandler.create(vertx);
    upCheckHandler.register("Up", upCheck);
    
    Router router = Router.router(vertx);
    Router mgmtRouter = Router.router(vertx);
    
    ManagementRoute.deployStandardMgmtEndpoints(mgmtRouter, router, params.getManagementEndpoints(), new AtomicReference<>(params));
    if (ManagementRoute.mgmtEndpointPermitted(params.getManagementEndpoints(), "up")) {
      mgmtRouter.get("/up").handler(upCheckHandler).setName("Up");
    }
    if (ManagementRoute.mgmtEndpointPermitted(params.getManagementEndpoints(), "health")) {
      mgmtRouter.get("/health").handler(healthCheckHandler).setName("Health");
    }
    if (ManagementRoute.mgmtEndpointPermitted(params.getManagementEndpoints(), "prometheus")) {
      mgmtRouter.get("/prometheus").handler(new PrometheusScrapingHandlerImpl()).setName("Prometheus");
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
    
    RequestContextBuilder rcb;
    try {
      rcb = createRequestContextBuilder(params, loginDao);
    } catch (Throwable ex) {
      logger.error("Failed to create request context builder: ", ex);
      return Future.succeededFuture(-2);
    }
    
    FilterFactory filterFactory = createFilterFactory();
    
    List<Object> controllers = new ArrayList<>();
    boolean requireSession = params.getSession().isRequireSession();
    controllers.add(new InfoHandler(defnLoader, outputAllErrorMessages(), requireSession));
    controllers.add(new DocHandler(outputAllErrorMessages(), requireSession));
    controllers.add(new FormIoHandler(defnLoader, filterFactory, outputAllErrorMessages(), requireSession));
    controllers.add(new AuthConfigHandler(params.getSession().getOauth()));
    controllers.add(new SessionHandler(outputAllErrorMessages(), requireSession));
    
    controllers.add(new HistoryHandler(auditor, outputAllErrorMessages()));
    addExtraControllers(params, controllers);
    List<Object> providers = Arrays.asList(
        new JacksonJsonProvider(PipelineDefnLoader.JSON_OBJECT_MAPPER, JacksonJsonProvider.BASIC_ANNOTATIONS)
    );

    OpenAPIConfiguration openApiConfig = createOpenapiConfiguration(controllers);
    OpenApiHandler openApiHandler = new OpenApiHandler(this, openApiConfig, "/api");
    ModelConverters.getInstance(true).addConverter(new OpenApiModelConverter());
    
    PipelineExecutor pipelineExecutor = new PipelineExecutorImpl(filterFactory, params.getSecrets());
    vertx.fileSystem().mkdirs(params.getOutputCacheDir());
    router.route(QueryRouter.PATH_ROOT + "/*").handler(new QueryRouter(vertx, auditor, rcb, defnLoader, pipelineExecutor, params.getOutputCacheDir(), outputAllErrorMessages()));
    
    ManagementRoute.createAndDeploy(vertx
            , router, params.getHttpServerOptions()
            , params.getManagementEndpointPort()
            , corsHandler
            , mgmtRouter
            , params.getManagementEndpointUrl()
    );

    router.get("/api").handler(rc -> {
      rc.response().setStatusCode(301).putHeader("Location", "/openapi").end();
    });
    RequestContextHandler rch = new RequestContextHandler(vertx, rcb, outputAllErrorMessages());
    router.route("/api/*").handler(rch);
    router.route("/api/*").handler(new JaxRsHandler(vertx, meterRegistry, "/api", controllers, providers));
    router.route("/ui/*").handler(UiRouter.create(vertx, "/ui", "/www", "/www/index.html"));
    router.getWithRegex("/openapi\\..*").blockingHandler(openApiHandler);
    router.get("/openapi").handler(openApiHandler.getUiHandler());
    if (!params.getSession().getOauth().isEmpty()) {
      LoginRouter loginRouter = LoginRouter.create(vertx, loginDao, openIdDiscoveryHandler, jwtValidator, rcb, params.getSession(), params.getJwt().getRequiredAudiences(), outputAllErrorMessages(), params.getSession().getSessionCookie());
      router.get("/login").handler(loginRouter);
      router.get("/login/return").handler(loginRouter);
      router.get("/login/logout").handler(loginRouter);
    }
    router.route("/").handler(rc -> {
      rc.response().setStatusCode(307);
      rc.redirect("/ui/");
    });

    return httpServer
            .requestHandler(router)
            .listen()            
            .compose(svr -> {
              port = svr.actualPort();
              if (!params.getSampleDataLoads().isEmpty()) {
                return performSampleDataLoads(params.getSampleDataLoads().iterator());
              } else {
                return Future.succeededFuture();
              }
            })
            .compose(v -> {
              logger.info("Started on port {}", port);
              if (params.isExitOnRun()) {
                return Future.succeededFuture(1);
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
            )
    );
    return filterFactory;
  }

  private Future<Void> performSampleDataLoads(Iterator<DataSourceConfig> iter) {
    if (iter.hasNext()) {
      DataSourceConfig source = iter.next();
      String url = source.getUrl();
      SampleDataLoader loader;
      if (Strings.isNullOrEmpty(url)) {
        logger.warn("No URL configured for sample data loader {}", source);
        return performSampleDataLoads(iter);
      } else if (url.startsWith("mysql")) {
        loader = new SampleDataLoaderMySQL();
      } else if (url.startsWith("sqlserver")) {
        loader = new SampleDataLoaderMsSQL();
      } else if (url.startsWith("postgresql")) {
        loader = new SampleDataLoaderPostgreSQL();
      } else {
        logger.warn("No sample data loader found for {}", url);
        return performSampleDataLoads(iter);
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
              .compose(v -> performSampleDataLoads(iter));
    } else {
      return Future.succeededFuture();
    }
  }

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
        extractSampleFile(baseConfigFile, "samples/sub1/sub2/DynamicEndpointPipelineIT.yaml", sampleDataLoads);
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
      logger.warn("Failed to copy sample {}: ", ex);
    }
  }
  

  protected RequestContextBuilder createRequestContextBuilder(Parameters params, LoginDao loginDao) {    
    JwtValidationConfig jwtConfig = params.getJwt();
    IssuerAcceptabilityHandler iah = IssuerAcceptabilityHandler.create(jwtConfig.getAcceptableIssuerRegexes()
                    , jwtConfig.getAcceptableIssuersFile()
                    , jwtConfig.getFilePollPeriodDuration()
            );
    
    WebClient webClient = WebClient.create(vertx);
    if (!jwtConfig.getJwksEndpoints().isEmpty()) {
      openIdDiscoveryHandler = JsonWebKeySetOpenIdDiscoveryHandler.create(webClient, iah, jwtConfig.getDefaultJwksCacheDuration());
      jwtValidator = JwtValidator.createStatic(webClient, jwtConfig.getJwksEndpoints(), jwtConfig.getDefaultJwksCacheDuration(), iah);
    } else {
      JsonWebKeySetOpenIdDiscoveryHandler jsonWebKeySetHandler = JsonWebKeySetOpenIdDiscoveryHandler.create(webClient, iah, jwtConfig.getDefaultJwksCacheDuration());
      openIdDiscoveryHandler = jsonWebKeySetHandler;
      jwtValidator = JwtValidator.create(jsonWebKeySetHandler, iah);
    }
    
    RequestContextBuilder rcb = new RequestContextBuilder(WebClient.create(vertx)
            , jwtValidator
            , openIdDiscoveryHandler
            , loginDao
            , params.isEnableBasicAuth()
            , params.isEnableBearerAuth()
            , params.getOpenIdIntrospectionHeaderName()
            , jwtConfig.getJwksEndpoints() == null || jwtConfig.getJwksEndpoints().isEmpty()
            , jwtConfig.getIssuerHostPath()
            , jwtConfig.getRequiredAudiences()
            , params.getSession().getSessionCookie() != null ? params.getSession().getSessionCookie().getName() : null
    );
    return rcb;
  }
  
  private OpenAPIConfiguration createOpenapiConfiguration(List<Object> resources) {
    return new SwaggerConfiguration()
            .resourceClasses(Stream.concat(resources.stream(), Stream.of(this)).map(r -> r.getClass().getCanonicalName())
                    .collect(Collectors.toSet()))
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

    ResourceBuilder resourceBuilder = Resource.getDefault().toBuilder()
            .put("service.name", config.getServiceName())
            .put("service.version", Version.MAVEN_PROJECT_VERSION)
            ;
    SdkTracerProvider sdkTracerProvider = null;
    
    if (config.getProtocol() != TracingProtocol.none && !Strings.isNullOrEmpty(config.getUrl())) {
      SpanExporter spanExporter = switch (config.getProtocol()) {
        case none -> null;
        // case otlpgrpc -> OtlpGrpcSpanExporter.builder().setEndpoint(config.getUrl()).build();
        case otlphttp -> OtlpHttpSpanExporter.builder()
                .setEndpoint(config.getUrl())
                .build();
        case zipkin -> ZipkinSpanExporter.builder()
                .setSender((BytesMessageSender) URLConnectionSender.create(config.getUrl()))
                .setEndpoint(config.getUrl())
                .build();
      };
      Sampler sampler = switch (config.getSampler()) {
        case alwaysOff ->
          Sampler.alwaysOff();
        case alwaysOn ->
          Sampler.alwaysOn();
        case parent ->
          Sampler.parentBased(switch (config.getSampler()) {
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
      
      sdkTracerProvider = SdkTracerProvider.builder()
              .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
              .addSpanProcessor(new VertxMDCSpanProcessor())
              .setResource(resourceBuilder.build())
              .setSampler(sampler)
              .build();
    }
    TextMapPropagator propagator = switch (config.getPropagator()) {
      case b3 -> B3Propagator.injectingSingleHeader();
      case b3multi -> B3Propagator.injectingMultiHeaders();
      case w3c -> W3CTraceContextPropagator.getInstance();
    };
    OpenTelemetrySdkBuilder openTelemetryBuilder = OpenTelemetrySdk.builder()
            .setTracerProvider(sdkTracerProvider)
            .setPropagators(ContextPropagators.create(propagator))
            ;
    
    return openTelemetryBuilder.buildAndRegisterGlobal();
  }

  protected boolean outputAllErrorMessages() {
    return false;
  }
  
  protected void addExtraControllers(Parameters params, List<Object> controllers) {
  }
}
