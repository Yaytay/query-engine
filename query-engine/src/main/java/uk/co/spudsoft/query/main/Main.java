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

import brave.Tracing;
import brave.http.HttpServerParser;
import brave.http.HttpTracing;
import brave.sampler.Sampler;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import uk.co.spudsoft.query.logging.LoggingConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.core.tracing.TracingOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.micrometer.impl.PrometheusScrapingHandlerImpl;
import io.vertx.tracing.zipkin.ZipkinTracingOptions;
import jakarta.ws.rs.core.Application;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import uk.co.spudsoft.dircache.DirCache;
import uk.co.spudsoft.jwtvalidatorvertx.IssuerAcceptabilityHandler;
import uk.co.spudsoft.jwtvalidatorvertx.JwtValidatorVertx;
import uk.co.spudsoft.jwtvalidatorvertx.OpenIdDiscoveryHandler;
import uk.co.spudsoft.jwtvalidatorvertx.impl.JWKSOpenIdDiscoveryHandlerImpl;
import uk.co.spudsoft.mgmt.HeapDumpRoute;
import uk.co.spudsoft.mgmt.InFlightRoute;
import uk.co.spudsoft.mgmt.LogbackMgmtRoute;
import uk.co.spudsoft.mgmt.ManagementRoute;
import uk.co.spudsoft.mgmt.ThreadDumpRoute;
import uk.co.spudsoft.params4j.ConfigurationProperty;
import uk.co.spudsoft.params4j.FileType;
import uk.co.spudsoft.params4j.Params4J;
import uk.co.spudsoft.query.exec.AuditorImpl;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineExecutorImpl;
import uk.co.spudsoft.query.exec.conditions.RequestContextBuilder;
import uk.co.spudsoft.query.json.TracingOptionsMixin;
import uk.co.spudsoft.query.main.sample.SampleDataLoader;
import uk.co.spudsoft.query.main.sample.SampleDataLoaderMsSQL;
import uk.co.spudsoft.query.main.sample.SampleDataLoaderMySQL;
import uk.co.spudsoft.query.main.sample.SampleDataLoaderPostgreSQL;
import uk.co.spudsoft.query.pipeline.PipelineDefnLoader;
import uk.co.spudsoft.query.web.QueryRouter;
import uk.co.spudsoft.query.web.UiRouter;
import uk.co.spudsoft.query.web.rest.DocHandler;
import uk.co.spudsoft.query.web.rest.InfoHandler;
import uk.co.spudspoft.vertx.rest.JaxRsHandler;
import uk.co.spudspoft.vertx.rest.OpenApiHandler;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.urlconnection.URLConnectionSender;

/**
 *
 * @author jtalbut
 */
@OpenAPIDefinition
public class Main extends Application {
  
  private static final Logger logger = LoggerFactory.getLogger(Main.class);
  
private static final String MAVEN_PROJECT_NAME = "SpudSoft Query Engine";
private static final String MAVEN_PROJECT_VERSION = "0.0.10-6-main";

private static final String NAME = "query-engine";
  
  private MeterRegistry meterRegistry;
  private Vertx vertx;
  private HttpServer httpServer;
  
  private DirCache dirCache;
  private PipelineDefnLoader defnLoader;
  private AuditorImpl auditor;
  
  private int port;
  
  /**
   * Main method.
   * @param args Command line arguments that should have the same form as properties with the query-engine prefix, no dashes are required.
   *   
   */
  public static void main(String[] args) {
    Main main = new Main();
    main.innerMain(args, System.out)
            .onSuccess(statusCode -> {
              if (statusCode > 0) {
                main.shutdown(statusCode);
              }
            })
            .onFailure(ex -> {
              logger.error("Failed: ", ex);
              main.shutdown(-1);
            });
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
    
  private ObjectMapper configureObjectMapper(ObjectMapper mapper) {
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    mapper.configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);
    mapper.setDefaultMergeable(Boolean.TRUE);
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    return mapper;
  }
  
  @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "False positive, the dirs at this stage cannot be specified by the user")
  protected Future<Integer> innerMain(String[] args, PrintStream stdout) {
    
    Params4J<Parameters> p4j = Params4J.<Parameters>factory()
            .withConstructor(() -> new Parameters())
            .withDirGatherer(new File(getBaseConfigDir()), FileType.Yaml)
            .withSecretsGatherer(new File(getBaseConfigDir() + "/conf.d").toPath(), 0, 0, 0, StandardCharsets.UTF_8)
            .withEnvironmentVariablesGatherer(NAME, false)
            .withSystemPropertiesGatherer(NAME)
            .withCommandLineArgumentsGatherer(args, null)
            .withMixIn(TracingOptions.class, TracingOptionsMixin.class)
            .create();

    
    
    for (String arg : args) {
      if ("-?".equals(arg) || "--help".equals(arg)) {
        StringBuilder usage = new StringBuilder();
        List<ConfigurationProperty> propDocs = p4j.getDocumentation(new Parameters(), "--", null, Arrays.asList(Pattern.compile(".*VertxOptions.*"), Pattern.compile(".*HttpServerOptions.*")));
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
                .append("    --helpenv").append(" ".repeat(maxNameLen + 1 - 9)).append("display this environment variable form of this help\n")
                ;
        
        for (ConfigurationProperty prop : propDocs) {
          prop.appendUsage(usage, maxNameLen);
        }
        stdout.println(usage.toString());
        return Future.succeededFuture(1);
      }
      if ("--helpenv".equals(arg)) {
        StringBuilder usage = new StringBuilder();
        List<ConfigurationProperty> propDocs = p4j.getDocumentation(new Parameters(), "--", null, Arrays.asList(Pattern.compile(".*VertxOptions.*"), Pattern.compile(".*HttpServerOptions.*")));
        int maxNameLen = propDocs.stream().map(p -> p.name.length()).max(Integer::compare).get();

        usage.append("This is a list of environment variables understood by the query-engine.\n")
                .append("Call with --help for moire details of configuration\n")
                ;

        for (ConfigurationProperty prop : propDocs) {
          prop.appendEnv(usage, maxNameLen, "--", NAME);
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

    configureObjectMapper(DatabindCodec.mapper());
    configureObjectMapper(DatabindCodec.prettyMapper());

    try {
      logger.info("Params: {}", DatabindCodec.mapper().writeValueAsString(params));    
    } catch (JsonProcessingException ex) {
      logger.error("Failed to convert params to json: ", ex);
    }

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
    
    HttpTracing zipkinTracing = buildZipkinTrace(params.getZipkin());
    if (zipkinTracing != null) {
      vertxOptions.setTracingOptions(new ZipkinTracingOptions(zipkinTracing));
    }
    vertx = Vertx.vertx(vertxOptions);
    meterRegistry = (PrometheusMeterRegistry) BackendRegistries.getDefaultNow();
    
    auditor = new AuditorImpl(vertx, meterRegistry, params.getAudit());
    try {
      auditor.prepare();
      logger.info("Audit database prepared");
    } catch (Throwable ex) {
      logger.error("Failed to prepare audit database: ", ex);
      return Future.succeededFuture(-2);
    }
    
    httpServer = vertx.createHttpServer(params.getHttpServerOptions());
    try {
      File baseConfigFile = new File(params.getBaseConfigPath());
      prepareBaseConfigPath(baseConfigFile);
      dirCache = DirCache.cache(baseConfigFile.toPath(), Duration.of(params.getFileStabilisationDelaySeconds(), ChronoUnit.SECONDS), Pattern.compile("\\..*"));
      defnLoader = new PipelineDefnLoader(meterRegistry, vertx, params.getPipelineCache(), dirCache);
    } catch (Throwable ex) {
      logger.error("Unable to config pipeline loader: ", ex);
      return Future.succeededFuture(2);
    }
    Router router = Router.router(vertx);
    
    if (!Strings.isNullOrEmpty(params.getCorsAllowedOriginRegex())) {
      CorsHandler corsHandler = CorsHandler.create()
              .addRelativeOrigin(params.getCorsAllowedOriginRegex());
      corsHandler.allowedMethods(
              ImmutableSet
                      .<HttpMethod>builder()
                      .add(HttpMethod.GET)
                      .add(HttpMethod.PUT)
                      .add(HttpMethod.DELETE)
                      .add(HttpMethod.POST)
                      .build()
      );
      router.route("/*").handler(corsHandler); 
    }
    
    RequestContextBuilder rcb = createRequestContextBuilder(params);
    
    List<Object> controllers = new ArrayList<>();
    controllers.add(new InfoHandler(rcb, defnLoader, outputAllErrorMessages()));
    controllers.add(new DocHandler(rcb, outputAllErrorMessages()));
    addExtraControllers(params, controllers);
    List<Object> providers = Arrays.asList(
        new JacksonJsonProvider(PipelineDefnLoader.JSON_OBJECT_MAPPER, JacksonJsonProvider.BASIC_ANNOTATIONS)
    );

    OpenAPIConfiguration openApiConfig = createOpenapiConfiguration(controllers);
    OpenApiHandler openApiHandler = new OpenApiHandler(this, openApiConfig, "/api");
    
    PipelineExecutor pipelineExecutor = new PipelineExecutorImpl(params.getSecrets());
    router.route(QueryRouter.PATH_ROOT + "/*").handler(new QueryRouter(vertx, auditor, rcb, defnLoader, pipelineExecutor, outputAllErrorMessages()));
    Router mgmtRouter = Router.router(vertx);
    
    mgmtRouter.get("/prometheus").handler(new PrometheusScrapingHandlerImpl()).setName("Prometheus");
    HeapDumpRoute.createAndDeploy(mgmtRouter);
    InFlightRoute.createAndDeploy(router, mgmtRouter);
    LogbackMgmtRoute.createAndDeploy(mgmtRouter);    
    ThreadDumpRoute.createAndDeploy(mgmtRouter);
    ManagementRoute.createAndDeploy(router, mgmtRouter);
    router.get("/api").handler(rc -> {
      rc.response().setStatusCode(301).putHeader("Location", "/openapi").end();
    });
    router.route("/api/*").handler(new JaxRsHandler(vertx, meterRegistry, "/api", controllers, providers));
    router.route("/ui/*").handler(UiRouter.create(vertx, "/ui", "/www", "/www/index.html"));
    router.getWithRegex("/openapi\\..*").blockingHandler(openApiHandler);
    router.get("/openapi").handler(openApiHandler.getUiHandler());
    router.route("/").handler(ctx -> {
      ctx.redirect("/ui");
    });

    return httpServer
            .requestHandler(router)
            .listen()            
            .compose(svr -> {
              port = svr.actualPort();
              if (params.getSampleDataLoads() != null && !params.getSampleDataLoads().isEmpty()) {
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
            ;
    
  }  
  
  private Future<Void> performSampleDataLoads(Iterator<DataSourceConfig> iter) {
    if (iter.hasNext()) {
      DataSourceConfig source = iter.next();
      String url = source.getUrl();
      SampleDataLoader loader;
      if (url.startsWith("mysql")) {
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

  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
  public static void prepareBaseConfigPath(File baseConfigFile) {
    if (!baseConfigFile.exists()) {
      logger.info("Creating base config dir at {}", baseConfigFile);
      baseConfigFile.mkdirs();
    }
    if (!baseConfigFile.isDirectory()) {
      logger.warn("Base config dir ({}) is not a directory", baseConfigFile);
    }
    String[] children = baseConfigFile.list();
    if (children != null && children.length == 0) {
      logger.info("Creating sample configs");
      extractSampleFile(baseConfigFile, "samples/demo/FeatureRichExample.yaml");
      extractSampleFile(baseConfigFile, "samples/demo/LookupValues.yaml");
      extractSampleFile(baseConfigFile, "samples/sub1/sub2/DynamicEndpointPipelineIT.yaml");
      extractSampleFile(baseConfigFile, "samples/sub1/sub2/JsonToPipelineIT.json");
      extractSampleFile(baseConfigFile, "samples/sub1/sub2/TemplatedJsonToPipelineIT.json.vm");
      extractSampleFile(baseConfigFile, "samples/sub1/sub2/TemplatedYamlToPipelineIT.yaml.vm");
      extractSampleFile(baseConfigFile, "samples/sub1/sub2/YamlToPipelineIT.yaml");
      extractSampleFile(baseConfigFile, "samples/sub1/sub2/permissions.jexl");
      extractSampleFile(baseConfigFile, "samples/sub1/permissions.jexl");
      extractSampleFile(baseConfigFile, "samples/permissions.jexl");
    } else {
      logger.info("Not creating sample configs because {} already contains {} files", baseConfigFile, children == null ? null : children.length);
    }
  }
  
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
  private static void extractSampleFile(File baseConfigDir, String path) {
    try {
      File destFile = new File(baseConfigDir, path.substring(8));
      File destParent = destFile.getParentFile();
      
      if (!destParent.exists()) {
        destParent.mkdirs();
      }
      
      try (InputStream is = Main.class.getResourceAsStream("/" + path)) {
        Files.copy(is, destFile.toPath());
      }
      
    } catch (Throwable ex) {
      logger.warn("Failed to copy sample {}: ", ex);
    }
  }
  

  protected RequestContextBuilder createRequestContextBuilder(Parameters params) {    
    OpenIdDiscoveryHandler discoverer = null;
    JwtValidatorVertx validator = null;

    JwtValidationConfig jwtConfig = params.getJwt();
    discoverer = new JWKSOpenIdDiscoveryHandlerImpl(WebClient.create(vertx)
            , IssuerAcceptabilityHandler.create(jwtConfig.getAcceptableIssuerRegexes()
                    , jwtConfig.getAcceptableIssuersFile()
                    , jwtConfig.getFilePollPeriodDuration()
            ), jwtConfig.getDefaultJwksCacheDuration().toSeconds());
    validator = JwtValidatorVertx.create((JWKSOpenIdDiscoveryHandlerImpl) discoverer);   
    
    RequestContextBuilder rcb = new RequestContextBuilder(WebClient.create(vertx), validator, discoverer, params.getOpenIdIntrospectionHeaderName(), jwtConfig.getRequiredAudience());
    return rcb;
  }
  
  private OpenAPIConfiguration createOpenapiConfiguration(List<Object> resources) {
    return new SwaggerConfiguration()
            .resourceClasses(Stream.concat(resources.stream(), Stream.of(this)).map(r -> r.getClass().getCanonicalName())
                    .collect(Collectors.toSet()))
            .prettyPrint(true)
            .openAPI31(Boolean.TRUE)
            .openAPI(
                    new OpenAPI()
                            .info(
                                    new Info()
                                            .title(MAVEN_PROJECT_NAME)
                                            .version(MAVEN_PROJECT_VERSION)
                            )
            );
  }

  static HttpTracing buildZipkinTrace(ZipkinConfig config) {

    Reporter<Span> spanReporter;

    if ((config == null) || Strings.isNullOrEmpty(config.getBaseUrl())) {
      return null;
    } else {
      if (!config.getBaseUrl().endsWith("/")) {
        config.setBaseUrl(config.getBaseUrl() + "/");
      }
      spanReporter = AsyncReporter.create(URLConnectionSender.create(config.getBaseUrl() + "api/v2/spans"));
    }
            
    Tracing tracing = Tracing.newBuilder()
            .localServiceName(config.getServiceName() == null ? "query-engine" : config.getServiceName())
            .spanReporter(spanReporter)
            .sampler(Sampler.ALWAYS_SAMPLE)
            .build();
    HttpTracing httpTracing = HttpTracing.newBuilder(tracing)
            .serverParser(new HttpServerParser())
            .build();
    return httpTracing;
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
  
  protected boolean outputAllErrorMessages() {
    return false;
  }
  
  protected void addExtraControllers(Parameters params, List<Object> controllers) {
  }
}
