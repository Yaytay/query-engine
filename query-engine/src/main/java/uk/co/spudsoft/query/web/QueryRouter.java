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
import io.vertx.core.Future;
import io.vertx.core.Handler;
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
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.Auditor;
import uk.co.spudsoft.query.exec.FormatRequest;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.SourceInstance;
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.exec.conditions.RequestContextBuilder;
import uk.co.spudsoft.query.main.ExceptionToString;
import uk.co.spudsoft.query.defn.Format;
import uk.co.spudsoft.query.defn.Pipeline;
import uk.co.spudsoft.query.exec.ArgumentInstance;
import uk.co.spudsoft.query.exec.CachingWriteStream;
import uk.co.spudsoft.query.exec.FormatInstance;
import uk.co.spudsoft.query.exec.ProgressNotificationHandler;
import uk.co.spudsoft.query.exec.notifications.LoggingNotificationHandler;


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
  private final Auditor auditor;
  private final RequestContextBuilder requestContextBuilder;
  private final PipelineDefnLoader loader;
  private final PipelineExecutor pipelineExecutor;
  private final String outputCacheDir;
  private final boolean outputAllErrorMessages;

  /**
   * Constructor.
   * 
   * @param vertx Vertx instance.
   * @param auditor Auditor interface for tracking requests.
   * @param requestContextBuilder The builder that does the actual work.
   * @param loader Pipeline loader.
   * @param pipelineExecutor Pipeline executor.
   * @param outputCacheDir Directory to store output in where output caching is enabled (see {@link uk.co.spudsoft.query.defn.Pipeline#cacheDuration}).
   * @param outputAllErrorMessages In a production environment error messages should usually not leak information that may assist a bad actor, set this to true to return full details in error responses.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The PipelineDefnLoader is mutable because it changes the filesystem")
  public QueryRouter(Vertx vertx
          , Auditor auditor
          , RequestContextBuilder requestContextBuilder
          , PipelineDefnLoader loader
          , PipelineExecutor pipelineExecutor
          , String outputCacheDir
          , boolean outputAllErrorMessages
  ) {
    this.vertx = vertx;
    this.auditor = auditor;
    this.requestContextBuilder = requestContextBuilder;
    this.loader = loader;
    this.pipelineExecutor = pipelineExecutor;
    this.outputCacheDir = outputCacheDir;
    this.outputAllErrorMessages = outputAllErrorMessages;    
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
    if (request.method() == HttpMethod.GET) {
      try {
        String path = request.path();
        if (path.length() < 1 + PATH_ROOT.length()) {
          logger.warn("Invalid request, path too short: ", request.path());
          throw new ServiceException(400, "Invalid path");
        } else {
          HttpServerResponse response = routingContext.response();
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
          requestContextBuilder.buildRequestContext(request)
                  .compose(requestContext -> {
                    response.headersEndHandler(v -> {
                      requestContext.setHeadersSentTime(System.currentTimeMillis());
                    });
                    response.bodyEndHandler(v -> {
                      auditor.recordResponse(requestContext, response);
                    });

                    if (!Strings.isNullOrEmpty(requestContext.getRunID())) {
                      ProgressNotificationHandler progressNotificationHandler = new LoggingNotificationHandler();
                      ProgressNotificationHandler.storeNotificationHandler(progressNotificationHandler);
                    }
                    
                    return auditor.recordRequest(requestContext).map(v -> requestContext);
                  })
                  .compose(requestContext -> {
                    try {
                      logger.trace("Request context: {}", requestContext);
                      Vertx.currentContext().putLocal("req", requestContext);
                      
                      return loader.loadPipeline(query, requestContext, (file, ex) -> auditor.recordFileDetails(requestContext, file, null));
                    } catch (Throwable ex) {
                      return Future.failedFuture(ex);
                    }
                  })
                  .compose(pipelineAndFile -> {
                    pipelineTitle[0] = pipelineAndFile.pipeline().getTitle();
                    RequestContext requestContext = RequestContextHandler.getRequestContext(Vertx.currentContext());
                    
                    return auditor.recordFileDetails(requestContext, pipelineAndFile.file(), pipelineAndFile.pipeline())
                            .map(v -> pipelineAndFile.pipeline());
                  })
                  .compose(pipeline -> {
                    return pipelineExecutor.validatePipeline(pipeline);
                  })
                  .compose(pipeline -> {
                    RequestContext requestContext = RequestContextHandler.getRequestContext(Vertx.currentContext());
                    return auditor.runRateLimitRules(requestContext, pipeline);
                  })
                  .compose(pipeline -> {
                    responseStream.exceptionHandler(ex -> {
                      logger.warn("Exception in response stream: ", ex);
                    });
                    // Four options:
                    // 1. No caching involved
                    // 2. Valid cache file avavailable, If-Modified-Since is before cacheExpiry - return 304 with Last-Modified
                    // 3. Valid cache file avavailable
                    // 4. Generate cache file
                    if (pipeline.supportsCaching()) {
                      return runCachedPipeline(pipeline, formatRequest, response, responseStream, routingContext);
                    } else {
                      return runPipeline(pipeline, formatRequest, response, responseStream, routingContext);
                    }
                  })
                  .onComplete(ar -> {
                    if (ar.succeeded()) {
                      pipelineExecutor.progressNotification(pipelineTitle[0], null, null, null, true, true, "Pipeline completed.");                    
                    } else {
                      RequestContext requestContext = RequestContextHandler.getRequestContext(Vertx.currentContext());
                      auditor.recordException(requestContext, ar.cause());
                      internalError(ar.cause(), routingContext, outputAllErrorMessages);
                      pipelineExecutor.progressNotification(pipelineTitle[0], null, null, null, true, false, "Pipeline failed: ", ar.cause());
                    }
                    vertx.getOrCreateContext().removeLocal(SourceInstance.SOURCE_CONTEXT_KEY);
                    logger.info("Request completed");
                  });
        }
      } catch (ServiceException ex) {
        logger.warn("ServiceException: ", ex);
        routingContext.response().setStatusCode(ex.getStatusCode()).send(ex.getMessage());
      } catch (Throwable ex) {
        logger.warn("Failed: ", ex);
        routingContext.response().setStatusCode(500).send("Failed");
      }
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

    RequestContext requestContext = RequestContextHandler.getRequestContext(Vertx.currentContext());
    
    return auditor.getCacheFile(requestContext, pipeline)
            .compose(cacheDetails -> {
              if (cacheDetails == null) {
                logger.debug("Caching pipeline {} with {} no previous run found.", requestContext.getPath(), pipeline.getCacheDuration());
                return runPipelineToCache(pipeline, requestContext, formatRequest, response, responseStream, routingContext);
              } else {
                // Return from cache
                logger.debug("Caching pipeline {} found file {} from run {}.", requestContext.getPath(), cacheDetails.cacheFile(), cacheDetails.auditId());
                
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
                              logger.warn("Failed to open cache file {}: ", cacheDetails, ar.cause());
                              // Failed to open cache file, so regenerate
                              return auditor.deleteCacheFile(cacheDetails.auditId())
                                      .transform(ar2 -> {
                                        if (ar2.failed()) {
                                          logger.error("Failed to delete cache for {}: {}", cacheDetails.auditId(), ar2.cause());
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
    String cacheFile = outputCacheDir + requestContext.getRequestId().replace('/', '_');
    return auditor.recordCacheFile(requestContext, cacheFile, LocalDateTime.now(ZoneOffset.UTC).plus(pipeline.getCacheDuration()))
            .transform(ar -> {
              if (ar.succeeded()) {
                return CachingWriteStream.cacheStream(vertx, responseStream, cacheFile)
                        .transform(ar2 -> {
                          if (ar2.succeeded()) {
                            return runPipeline(pipeline, formatRequest, response, ar2.result(), routingContext);
                          } else {
                            logger.error("Failed to open cache file ({}) for {}: {}", cacheFile, requestContext.getRequestId(), ar2.cause());
                            return auditor.deleteCacheFile(requestContext.getRequestId())
                                    .transform(ar3 -> {
                                      if (ar3.failed()) {
                                        logger.error("Failed to delete cache for {}: {}", cacheFile, requestContext.getRequestId(), ar3.cause());
                                      }
                                      return runPipeline(pipeline, formatRequest, response, responseStream, routingContext);
                                    });
                          }
                        });
              } else {
                logger.error("Failed to record cache file ({}) for {} in database: {}", cacheFile, requestContext.getRequestId(), ar.cause());
                return runPipeline(pipeline, formatRequest, response, responseStream, routingContext);
              }
            });
  }
  
  private Future<Void> runPipeline(Pipeline pipeline, FormatRequest formatRequest, HttpServerResponse response, WriteStream<Buffer> responseStream, RoutingContext routingContext) {
    PipelineInstance instance;
    try {
      RequestContext requestContext = RequestContextHandler.getRequestContext(Vertx.currentContext());
      
      Format chosenFormat = pipelineExecutor.getFormat(pipeline.getFormats(), formatRequest);
      response.headers().set("Content-Type", chosenFormat.getMediaType().toString());
      String filename = buildDesiredFilename(chosenFormat);
      if (filename != null) {
        response.headers().set("Content-Disposition", "attachment; filename=\"" + filename + "\"");
      }
      if (pipeline.supportsCaching()) {
        routingContext.lastModified(Instant.ofEpochMilli(requestContext.getStartTime()));
      }
      
      FormatInstance formatInstance = chosenFormat.createInstance(vertx, Vertx.currentContext(), responseStream);
      SourceInstance sourceInstance = pipeline.getSource().createInstance(vertx, Vertx.currentContext(), pipelineExecutor, ROOT_SOURCE_DEFAULT_NAME);
      
      Vertx.currentContext().putLocal("pipeline", pipeline);
      
      Map<String, ArgumentInstance> arguments = pipelineExecutor.prepareArguments(requestContext, pipeline.getArguments(), routingContext.request().params());
      instance = new PipelineInstance(
              arguments
              , pipeline.getSourceEndpointsMap()
              , pipelineExecutor.createPreProcessors(vertx, Vertx.currentContext(), pipeline)
              , sourceInstance
              , pipelineExecutor.createProcessors(vertx, sourceInstance, Vertx.currentContext(), pipeline, routingContext.request().params(), null)
              , formatInstance
      );
      logger.debug("Instance: {}", instance);
    } catch (Throwable ex) {
      return Future.failedFuture(ex);
    }
    
    return pipelineExecutor.initializePipeline(instance).map(v -> instance)
                  .compose(i -> {
                    logger.info("Pipeline initiated");
                    return instance.getFinalPromise().future();
                  });
  }

  static void internalError(Throwable ex, RoutingContext routingContext, boolean outputAllErrorMessages) {
    logger.warn("Request failed: ", ex);
    
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
    response.putHeader("Content-Type", "texxt/plain");
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
