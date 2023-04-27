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
import io.vertx.core.json.Json;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.core.tracing.TracingOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.micrometer.impl.PrometheusScrapingHandlerImpl;
import io.vertx.tracing.zipkin.ZipkinTracingOptions;
import jakarta.ws.rs.core.Application;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
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
import uk.co.spudsoft.jwtvalidatorvertx.JwtValidatorVertx;
import uk.co.spudsoft.jwtvalidatorvertx.OpenIdDiscoveryHandler;
import uk.co.spudsoft.jwtvalidatorvertx.impl.JWKSOpenIdDiscoveryHandlerImpl;
import uk.co.spudsoft.mgmt.HeapDumpRoute;
import uk.co.spudsoft.mgmt.InFlightRoute;
import uk.co.spudsoft.mgmt.LogbackMgmtRoute;
import uk.co.spudsoft.mgmt.ManagementRoute;
import uk.co.spudsoft.mgmt.ThreadDumpRoute;
import uk.co.spudsoft.params4j.FileType;
import uk.co.spudsoft.params4j.Params4J;
import uk.co.spudsoft.query.exec.AuditorImpl;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineExecutorImpl;
import uk.co.spudsoft.query.exec.conditions.RequestContextBuilder;
import uk.co.spudsoft.query.json.TracingOptionsMixin;
import uk.co.spudsoft.query.pipeline.PipelineDefnLoader;
import uk.co.spudsoft.query.web.QueryRouter;
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
private static final String MAVEN_PROJECT_VERSION = "0.0.3-3-main-SNAPSHOT";

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
    main.innerMain(args)
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
   * @return The status code that would have been returned if this was a real command line execution.
   * @throws ExecutionException If the main function throws an exception.
   * @throws InterruptedException If the thread is interrupted whilst waiting for the main function to complete.
   */
  public int testMain(String[] args) throws ExecutionException, InterruptedException {    
    CompletableFuture<Integer> future = new CompletableFuture<>();
    innerMain(args)
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
    mapper.setDefaultMergeable(Boolean.TRUE);
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    return mapper;
  }
  
  @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "False positive, the dirs at this stage cannot be specified by the user")
  protected Future<Integer> innerMain(String[] args) {   
    
    Params4J<Parameters> p4j = Params4J.<Parameters>factory()
            .withConstructor(() -> new Parameters())
            .withDirGatherer(new File(getBaseConfigDir()), FileType.Yaml)
            .withSecretsGatherer(new File(getBaseConfigDir() + "/conf.d").toPath(), 0, 0, 0, StandardCharsets.UTF_8)
            .withEnvironmentVariablesGatherer(NAME, false)
            .withSystemPropertiesGatherer(NAME)
            .withCommandLineArgumentsGatherer(args, null)
            .withMixIn(TracingOptions.class, TracingOptionsMixin.class)
            .create();
    
    Parameters params = p4j.gatherParameters();
    LoggingConfiguration.configureLogback((LoggerContext) LoggerFactory.getILoggerFactory(), params.getLogging());

    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
    java.util.logging.Logger.getLogger("").setLevel(Level.FINEST);

    configureObjectMapper(DatabindCodec.mapper());
    
    logger.debug("Params: {}", Json.encode(params));

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
    router.route("/ui/*").handler(StaticHandler.create("www"));
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
              logger.info("Started on port {}", port);
              if (params.isExitOnRun()) {
                return Future.succeededFuture(1);
              }
              return Future.succeededFuture(0);
            });
    
  }  

  protected void prepareBaseConfigPath(File baseConfigFile) {
  }

  protected RequestContextBuilder createRequestContextBuilder(Parameters params) {    
    OpenIdDiscoveryHandler discoverer = null;
    JwtValidatorVertx validator = null;

    List<String> acceptableIssuerRegexes = params.getAcceptableIssuerRegexes();
    if (acceptableIssuerRegexes != null && !acceptableIssuerRegexes.isEmpty()) {
      discoverer = new JWKSOpenIdDiscoveryHandlerImpl(WebClient.create(vertx), acceptableIssuerRegexes, params.getDefaultJwksCacheDurationSeconds());
      validator = JwtValidatorVertx.create((JWKSOpenIdDiscoveryHandlerImpl) discoverer);   
    }
    
    RequestContextBuilder rcb = new RequestContextBuilder(WebClient.create(vertx), validator, discoverer, params.getOpenIdIntrospectionHeaderName(), params.getAudience());
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
