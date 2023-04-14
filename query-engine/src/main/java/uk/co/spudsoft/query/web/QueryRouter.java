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

import uk.co.spudsoft.query.pipeline.PipelineDefnLoader;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
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
import uk.co.spudsoft.query.exec.FormatInstance;


/**
 *
 * @author jtalbut
 */
public class QueryRouter implements Handler<RoutingContext> {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(QueryRouter.class);
  
  public static final String PATH_ROOT = "/query";
  public static final String ROOT_SOURCE_DEFAULT_NAME = "Source";
  
  private final Vertx vertx;
  private final Auditor auditor;
  private final RequestContextBuilder requestContextBuilder;
  private final PipelineDefnLoader loader;
  private final PipelineExecutor pipelineExecutor;
  private final boolean outputAllErrorMessages;

  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The PipelineDefnLoader is mutable because it changes the filesystem")
  public QueryRouter(Vertx vertx
          , Auditor auditor
          , RequestContextBuilder requestContextBuilder
          , PipelineDefnLoader loader
          , PipelineExecutor pipelineExecutor
          , boolean outputAllErrorMessages
  ) {
    this.vertx = vertx;
    this.auditor = auditor;
    this.requestContextBuilder = requestContextBuilder;
    this.loader = loader;
    this.pipelineExecutor = pipelineExecutor;
    this.outputAllErrorMessages = outputAllErrorMessages;
  }
  
  static int indexOfLastDotAfterLastSlash(String path) {
    int dotPos = path.lastIndexOf(".");
    int slashPos = path.lastIndexOf("/");
    if (dotPos > slashPos) {
      return dotPos;
    } else {
      return -1;
    }
  }
    
  @Override
  public void handle(RoutingContext routingContext) {
    
    HttpServerRequest request = routingContext.request();
    if (request.method() == HttpMethod.GET) {
      try {
        String path = request.path();
        if (path.length() < 1 + PATH_ROOT.length()) {
          logger.warn("Invalid request, path too short: ", request.path());
          throw new ServiceException(400, "Invalid path");
        } else {
          HttpServerResponse response = routingContext.response();
          response.setChunked(true);
          path = path.substring(PATH_ROOT.length() + 1);
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
                    return auditor.recordRequest(requestContext).map(requestContext);
                  })
                  .compose(requestContext -> {                    
                    try {
                      logger.trace("Request context: {}", requestContext);
                      Vertx.currentContext().putLocal("req", requestContext);
                      return loader.loadPipeline(query, requestContext, file -> auditor.recordFileDetails(requestContext, file));
                    } catch (Throwable ex) {
                      return Future.failedFuture(ex);
                    }
                  })
                  .onComplete(ar -> {
                    RequestContext requestContext = Vertx.currentContext().getLocal("req");
                    response.headersEndHandler(v -> {
                      requestContext.setHeadersSentTime(System.currentTimeMillis());
                    });
                    response.bodyEndHandler(v -> {
                      auditor.recordResponse(requestContext, response);
                    });
                  })
                  .compose(pipeline -> pipelineExecutor.validatePipeline(pipeline))
                  .compose(pipeline -> {
                    PipelineInstance instance;
                    try {
                      Format chosenFormat = pipelineExecutor.getFormat(pipeline.getFormats(), formatRequest);
                      response.headers().set("content-type", chosenFormat.getMediaType().toString());
                      FormatInstance formatInstance = chosenFormat.createInstance(vertx, Vertx.currentContext(), response);
                      SourceInstance sourceInstance = pipeline.getSource().createInstance(vertx, Vertx.currentContext(), pipelineExecutor, ROOT_SOURCE_DEFAULT_NAME);
                      instance = new PipelineInstance(
                              pipelineExecutor.prepareArguments(pipeline.getArguments(), routingContext.request().params())
                              , pipeline.getSourceEndpoints()
                              , pipelineExecutor.createPreProcessors(vertx, Vertx.currentContext(), pipeline)
                              , sourceInstance
                              , pipelineExecutor.createProcessors(vertx, sourceInstance, Vertx.currentContext(), pipeline)
                              , formatInstance
                      );
                    } catch (Throwable ex) {
                      return Future.failedFuture(ex);
                    } 
                    
                    return pipelineExecutor.initializePipeline(instance).map(instance);
                  })
                  .compose(instance -> {
                    logger.info("Pipeline initiated");
                    return instance.getFinalPromise().future();
                  })
                  .onComplete(ar -> {
                    if (ar.failed()) {
                      Throwable ex = ar.cause();
                      logger.warn("Request failed: ", ex);
                      RequestContext requestContext = Vertx.currentContext().getLocal("req");
                      auditor.recordException(requestContext, ex);
                      
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
                      
                      routingContext.response().setStatusCode(statusCode).end(message);
                    } else {
                      vertx.getOrCreateContext().removeLocal(SourceInstance.SOURCE_CONTEXT_KEY);
                      logger.info("Request completed");
                    }
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
    
}
