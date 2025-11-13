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
package uk.co.spudsoft.query.web;

import com.google.common.base.Strings;
import uk.co.spudsoft.query.pipeline.PipelineDefnLoader;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.streams.WriteStream;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.Utils;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.Auditor;
import uk.co.spudsoft.query.exec.FormatRequest;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.main.Authenticator;
import uk.co.spudsoft.query.main.ExceptionToString;
import uk.co.spudsoft.query.defn.Format;
import uk.co.spudsoft.query.defn.Pipeline;
import uk.co.spudsoft.query.exec.ArgumentInstance;
import uk.co.spudsoft.query.exec.CachingWriteStream;
import uk.co.spudsoft.query.logging.Log;


/**
 * Vert.x {@link io.vertx.core.Handler}&lt;{@link io.vertx.ext.web.RoutingContext}&gt; for handling pipeline requests.
 * <p>
 * The primary entry point for the Query Engine.
 *
 * @author jtalbut
 */
public class QueryRouter implements Handler<RoutingContext> {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(QueryRouter.class);
  
  /**
   * The URL path prefix for this router.
   */
  public static final String PATH_ROOT = "/query";
  /**
   * The base name of any sources that do not have a specified name.
   */
  public static final String ROOT_SOURCE_DEFAULT_NAME = "Source";
  
  private final Vertx vertx;
  private final MeterRegistry meterRegistry;
  private final Auditor auditor;
  private final Authenticator authenticator;
  private final PipelineDefnLoader loader;
  private final PipelineExecutor pipelineExecutor;
  private final String outputCacheDir;
  private final int writeStreamBufferSize;
  private final boolean outputAllErrorMessages;
  
  private final PipelineRunningVerticle[] verticles;
  
  private AtomicInteger count = new AtomicInteger();

  /**
   * Constructor.
   * 
   * @param vertx Vertx instance.
   * @param meterRegistry MeterRegistry for production of metrics.
   * @param auditor Auditor interface for tracking requests.
   * @param authenticator The builder that does the actual work.
   * @param loader Pipeline loader.
   * @param pipelineExecutor Pipeline executor.
   * @param outputCacheDir Directory to store output in where output caching is enabled (see {@link uk.co.spudsoft.query.defn.Pipeline#cacheDuration}).
   * @param writeStreamBufferSize The number of bytes to buffer before each write to the output, each write involves a context switch so this should not be too small.
   * @param outputAllErrorMessages In a production environment error messages should usually not leak information that may assist a bad actor, set this to true to return full details in error responses.
   * @param instances The number of {@link PipelineRunningVerticle}s to create, typically this should be the same as VertxOptions.getEventLoopPoolSize
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The PipelineDefnLoader is mutable because it changes the filesystem")
  public QueryRouter(Vertx vertx
          , MeterRegistry meterRegistry
          , Auditor auditor
          , Authenticator authenticator
          , PipelineDefnLoader loader
          , PipelineExecutor pipelineExecutor
          , String outputCacheDir
          , int writeStreamBufferSize
          , boolean outputAllErrorMessages
          , int instances
  ) {
    this.vertx = vertx;
    this.meterRegistry = meterRegistry;
    this.auditor = auditor;
    this.authenticator = authenticator;
    this.loader = loader;
    this.pipelineExecutor = pipelineExecutor;
    this.outputCacheDir = outputCacheDir;
    this.writeStreamBufferSize = writeStreamBufferSize;
    this.outputAllErrorMessages = outputAllErrorMessages;
    
    
    verticles = new PipelineRunningVerticle[instances];
    
    
  }
  
  /**
   * Deploy the Verticles for the QueryRouter.
   * @return A Future that will be completed when all deployment attempts are complete.
   */
  public Future<Void> deploy() {
    List<Future<Void>> futures = new ArrayList<>(verticles.length);
    for (int i = 0; i < verticles.length; ++i) {
      verticles[i] = new PipelineRunningVerticle(vertx, meterRegistry, auditor, pipelineExecutor);
      PipelineRunningVerticle  verticle = verticles[i];
      futures.add(vertx.deployVerticle(verticle).compose(name -> {
        return Future.succeededFuture();
      }));
    }
    return Future.all(futures)
            .onSuccess(cf -> {
              String httpThread = Thread.currentThread().getName();
              logger.atInfo().log("Deploy PipelineRunningVerticles, http thread: {}, verticle threads: {}", httpThread, Arrays.stream(verticles).map(v -> v.getThreadName()).collect(Collectors.toList()));
            })
            .mapEmpty();
  }
  
  /**
   * Find the index of the last dot after the last slash in a string.
   * @param path The string being examined.
   * @return the index of the last dot after the last slash in a string.
   */
  static int indexOfLastDotAfterLastSlash(String path) {
    int dotPos = path.lastIndexOf(".");
    int slashPos = path.lastIndexOf("/");
    if (dotPos > slashPos) {
      return dotPos;
    } else {
      return -1;
    }
  }
  
  static String removeMatrixParams(String input) {
    // Find the first occurrence of ? or # to identify where the path ends
    int queryStart = input.indexOf('?');
    int fragmentStart = input.indexOf('#');

    // Determine where the path portion ends
    int pathEnd = input.length();
    if (queryStart != -1) {
      pathEnd = Math.min(pathEnd, queryStart);
    }
    if (fragmentStart != -1) {
      pathEnd = Math.min(pathEnd, fragmentStart);
    }

    // Split the input into path and remainder (query + fragment)
    String pathPortion = input.substring(0, pathEnd);
    String remainder = pathEnd < input.length() ? input.substring(pathEnd) : "";

    // Remove matrix parameters only from the path portion
    String cleanedPath = pathPortion.replaceAll(";[^/?#]*", "");

    // Reconstruct the full URL/path
    return cleanedPath + remainder;
  }
  
    
  @Override
  public void handle(RoutingContext routingContext) {
    
    HttpServerRequest request = routingContext.request();
    String pipelineTitle[] = new String[1];
    RequestContext requestContext = RequestContext.retrieveRequestContext(routingContext);
    Log.decorate(logger.atTrace(), requestContext).log("Retrieved RequestContext@{}", System.identityHashCode(requestContext));
    
    if (verticles[0] == null) {
      throw new IllegalStateException("QueryRouter#deploy not called");
    }
    
    if (request.method() == HttpMethod.GET) {
      authenticator.authenticate(routingContext, requestContext)
              .compose(req -> {
                return auditor.recordRequest(req);
              })
              .compose(v -> {
                String path = request.path();
                if (path.length() < 1 + PATH_ROOT.length()) {
                  Log.decorate(logger.atWarn(), requestContext).log("Invalid request, path too short: ", request.path());
                  return Future.failedFuture(new ServiceException(400, "Invalid path"));
                }
                HttpServerResponse response = routingContext.response();
                
                response.closeHandler(v2 -> {
                  Log.decorate(logger.atWarn(), requestContext).log("The connection has been closed.");
                });
                
                WriteStream<Buffer> responseStream = response;
                response.setChunked(true);
                path = path.substring(PATH_ROOT.length() + 1);

                if (path.contains(";")) {
                  path = removeMatrixParams(path);
                }

                String extension = null;
                int dotPos = indexOfLastDotAfterLastSlash(path);
                if (dotPos > 0) {
                  extension = path.substring(dotPos + 1);
                  path = path.substring(0, dotPos);
                }
                String query = path;
                FormatRequest formatRequest = FormatRequest.builder()
                        .name(request.getParam("_fmt"))
                        .extension(extension)
                        .accept(request.getHeader("Accept"))
                        .build();
                routingContext.addHeadersEndHandler(v1 -> {
                  requestContext.setHeadersSentTime(System.currentTimeMillis());
                });
                routingContext.addBodyEndHandler(v1 -> {
                  auditor.recordResponse(requestContext, response);
                });

                return loader.loadPipeline(query, requestContext, (file, ex) -> auditor.recordFileDetails(requestContext, file, null))
                        .compose(pipelineAndFile -> {
                          pipelineTitle[0] = pipelineAndFile.pipeline().getTitle();

                          return auditor.recordFileDetails(requestContext, pipelineAndFile.file(), pipelineAndFile.pipeline())
                                  .map(v2 -> pipelineAndFile.pipeline());
                        })
                        .compose(pipeline -> {
                          return pipelineExecutor.validatePipeline(pipeline);
                        })
                        .compose(pipeline -> {
                          return auditor.runRateLimitRules(requestContext, pipeline);
                        })
                        .compose(pipeline -> {
                          responseStream.exceptionHandler(ex -> {
                            Log.decorate(logger.atWarn(), requestContext).log("Exception in response stream: ", ex);
                          });
                          // Four options:
                          // 1. No caching involved
                          // 2. Valid cache file avavailable, If-Modified-Since is before cacheExpiry - return 304 with Last-Modified
                          // 3. Valid cache file avavailable
                          // 4. Generate cache file
                          if (pipeline.supportsCaching()) {
                            return runCachedPipeline(pipeline, formatRequest, response, responseStream, routingContext);
                          } else {
                            return runPipeline(pipeline, requestContext, formatRequest, response, responseStream, routingContext);
                          }
                        });
              })
              .onComplete(ar -> {
                if (ar.succeeded()) {
                  pipelineExecutor.progressNotification(requestContext, pipelineTitle[0], null, null, null, true, true, "Pipeline completed.");                    
                } else {
                  auditor.recordException(requestContext, ar.cause());
                  internalError(ar.cause(), routingContext, outputAllErrorMessages);
                  pipelineExecutor.progressNotification(requestContext, pipelineTitle[0], null, null, null, true, false, "Pipeline failed: ", ar.cause());
                }
                Log.decorate(logger.atInfo(), requestContext).log("Request completed");
              });
    } else {
      routingContext.next();
    }
  }

  private boolean notModifiedSince(RoutingContext routingContext, LocalDateTime cacheExpiry) {
    final String modifiedSince = routingContext.request().getHeader(HttpHeaders.IF_MODIFIED_SINCE);
    if (modifiedSince != null) {
      long lastModified = Utils.parseRFC1123DateTime(modifiedSince);
      if (lastModified < cacheExpiry.toInstant(ZoneOffset.UTC).toEpochMilli()) {
        return true;
      }
    }
    return false;
  }
  
  private Future<Void> runCachedPipeline(Pipeline pipeline, FormatRequest formatRequest, HttpServerResponse response, WriteStream<Buffer> responseStream, RoutingContext routingContext) {

    RequestContext requestContext = RequestContext.retrieveRequestContext(routingContext);
    
    return auditor.getCacheFile(requestContext, pipeline)
            .compose(cacheDetails -> {
              if (cacheDetails == null) {
                Log.decorate(logger.atDebug(), requestContext).log("Caching pipeline {} with {} no previous run found.", requestContext.getPath(), pipeline.getCacheDuration());
                return runPipelineToCache(pipeline, requestContext, formatRequest, response, responseStream, routingContext);
              } else {
                // Return from cache
                Log.decorate(logger.atDebug(), requestContext).log("Caching pipeline {} found file {} from run {}.", requestContext.getPath(), cacheDetails.cacheFile(), cacheDetails.auditId());
                
                if (notModifiedSince(routingContext, cacheDetails.expiry())) {
                  response.setStatusCode(304);
                  // bodyEndHandler not called, so must explicitly audit reponse
                  auditor.recordResponse(requestContext, response);
                  return response.end();
                } else {
                  Format chosenFormat = pipelineExecutor.getFormat(pipeline.getFormats(), formatRequest);
                  String filename = buildDesiredFilename(chosenFormat);
                  if (filename != null) {
                    response.headers().set("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                  }
                  return vertx.fileSystem().open(cacheDetails.cacheFile(), new OpenOptions().setRead(true).setCreate(false))
                          .transform(ar -> {
                            if (ar.succeeded()) {
                              auditor.recordCacheFileUsed(requestContext, cacheDetails.cacheFile());
                              routingContext.lastModified(cacheDetails.expiry().toInstant(ZoneOffset.UTC));
                              return ar.result().pipeTo(responseStream);
                            } else {
                              Log.decorate(logger.atWarn(), requestContext).log("Failed to open cache file {}: ", cacheDetails, ar.cause());
                              // Failed to open cache file, so regenerate
                              return auditor.deleteCacheFile(requestContext, cacheDetails.auditId())
                                      .transform(ar2 -> {
                                        if (ar2.failed()) {
                                          Log.decorate(logger.atError(), requestContext).log("Failed to delete cache for {}: {}", cacheDetails.auditId(), ar2.cause());
                                        }
                                        return runPipelineToCache(pipeline, requestContext, formatRequest, response, responseStream, routingContext);
                                      });
                            }
                          });
                }
              }
            });
  }

  private Future<Void> runPipelineToCache(Pipeline pipeline, RequestContext requestContext, FormatRequest formatRequest, HttpServerResponse response, WriteStream<Buffer> responseStream, RoutingContext routingContext) {
    // No cache file found, so run pipeline to generate one
    String cacheFile = outputCacheDir + requestContext.getRequestId().replace('/', '_').replace(':', '-');
    return auditor.recordCacheFile(requestContext, cacheFile, LocalDateTime.now(ZoneOffset.UTC).plus(pipeline.getCacheDuration()))
            .transform(ar -> {
              if (ar.succeeded()) {
                return CachingWriteStream.cacheStream(vertx, responseStream, cacheFile)
                        .transform(ar2 -> {
                          if (ar2.succeeded()) {
                            return runPipeline(pipeline, requestContext, formatRequest, response, ar2.result(), routingContext);
                          } else {
                            Log.decorate(logger.atError(), requestContext).log("Failed to open cache file ({}) for {}: {}", cacheFile, requestContext.getRequestId(), ar2.cause());
                            return auditor.deleteCacheFile(requestContext, requestContext.getRequestId())
                                    .transform(ar3 -> {
                                      if (ar3.failed()) {
                                        Log.decorate(logger.atError(), requestContext).log("Failed to delete cache for {}: {}", cacheFile, requestContext.getRequestId(), ar3.cause());
                                      }
                                      return runPipeline(pipeline, requestContext, formatRequest, response, responseStream, routingContext);
                                    });
                          }
                        });
              } else {
                Log.decorate(logger.atError(), requestContext).log("Failed to record cache file ({}) for {} in database: {}", cacheFile, requestContext.getRequestId(), ar.cause());
                return runPipeline(pipeline, requestContext, formatRequest, response, responseStream, routingContext);
              }
            });
  }
  
  private Future<Void> runPipeline(Pipeline pipeline, RequestContext requestContext, FormatRequest formatRequest, HttpServerResponse response, WriteStream<Buffer> rawResponseStream, RoutingContext routingContext) {
    try {
      Format chosenFormat = pipelineExecutor.getFormat(pipeline.getFormats(), formatRequest);
      response.headers().set("Content-Type", chosenFormat.getMediaType().toString());
      String filename = buildDesiredFilename(chosenFormat);
      if (filename != null) {
        response.headers().set("Content-Disposition", "attachment; filename=\"" + filename + "\"");
      }
      if (pipeline.supportsCaching()) {
        routingContext.lastModified(Instant.ofEpochMilli(requestContext.getStartTime()));
      }
      
      Context vertxContext = vertx.getOrCreateContext();
      
      WriteStream<Buffer> responseStream = new BufferingContextAwareWriteStream(rawResponseStream, vertxContext, writeStreamBufferSize);
      
      MultiMap queryStringParams = routingContext.request().params();
      Map<String, ArgumentInstance> arguments = pipelineExecutor.prepareArguments(requestContext, pipeline.getArguments(), queryStringParams);
      
      PipelineRunningTask task = new PipelineRunningTask(requestContext, pipeline, chosenFormat, queryStringParams, arguments, responseStream);

      PipelineRunningVerticle verticle = chooseVerticle(requestContext);
      return verticle.handleRequest(task);
      
    } catch (Throwable ex) {
      return Future.failedFuture(ex);
    }
  }

  private PipelineRunningVerticle chooseVerticle(RequestContext requestContext) {
    String httpThread = Thread.currentThread().getName();
    
    for (int i = 0; i < verticles.length; ++i) {
      int next = count.updateAndGet(current ->
              current == Integer.MAX_VALUE ? 0 : current + 1
      );
      PipelineRunningVerticle verticle = verticles[next % verticles.length];
      if (!httpThread.equals(verticle.getThreadName())) {
        return verticle;
      }
    }
    Log.decorate(logger.atWarn(), requestContext).log("Failed to choose any verticle, http thread: {}, verticle threads: {}", httpThread, Arrays.stream(verticles).map(v -> v.getThreadName()).collect(Collectors.toList()));
    // Fallback to ignoring the thread
    int next = count.updateAndGet(current ->
            current == Integer.MAX_VALUE ? 0 : current + 1
    );
    PipelineRunningVerticle verticle = verticles[next % verticles.length];
    return verticle;
  }

  static void internalError(Throwable ex, RoutingContext routingContext, boolean outputAllErrorMessages) {
    RequestContext requestContext = RequestContext.retrieveRequestContext(routingContext);
    Log.decorate(logger.atWarn(), requestContext).log("Request failed: ", ex);
    
    int statusCode = 500;
    String message = "Failed";
    
    if (ex instanceof ServiceException serviceException) {
      statusCode = serviceException.getStatusCode();
      message = serviceException.getMessage();
    } else if (ex instanceof IllegalArgumentException) {
      statusCode = 400;
      message = ex.getMessage();
    }
    
    if (outputAllErrorMessages) {
      message = ExceptionToString.convert(ex, "\n\t");
    }
    
    HttpServerResponse response = routingContext.response();
    response.putHeader("Content-Type", "text/plain");
    response.setStatusCode(statusCode)
            .end(message);
  }

  static String buildDesiredFilename(Format chosenFormat) {
    String fmtFilename = chosenFormat.getFilename();
    String fmtExtention = chosenFormat.getExtension();
    
    if (Strings.isNullOrEmpty(fmtFilename)) {
      return null;
    } else {
      if (!fmtFilename.contains(".") && !Strings.isNullOrEmpty(fmtExtention)) {
        fmtFilename = fmtFilename + "." + fmtExtention;
      }
      return fmtFilename;
    }
  }

}
