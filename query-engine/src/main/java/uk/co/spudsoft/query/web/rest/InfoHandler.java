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
package uk.co.spudsoft.query.web.rest;

import uk.co.spudsoft.query.pipeline.PipelineNodesTree;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.vertx.core.Future;
import io.vertx.core.file.FileSystemException;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.logging.Log;
import uk.co.spudsoft.query.main.ExceptionToString;
import uk.co.spudsoft.query.pipeline.PipelineDefnLoader;
import uk.co.spudsoft.query.web.ServiceException;

/**
 * JAX-RS class implementing the REST API for outputting information about the available pipeline definitions.
 * <p>
 * The information presented comes from the {@link uk.co.spudsoft.query.pipeline.PipelineDefnLoader}.
 *
 * @author jtalbut
 */
@Path("/info")
@Timed
public class InfoHandler {
  
  private static final Logger logger = LoggerFactory.getLogger(InfoHandler.class);

  private final PipelineDefnLoader loader;
  private final boolean outputAllErrorMessages;
  private final boolean requireSession;

  /**
   * Constructor.
   * @param loader Pipeline loader.
   * @param outputAllErrorMessages In a production environment error messages should usually not leak information that may assist a bad actor, set this to true to return full details in error responses.
   * @param requireSession If true any requests that do not have a login session will fail.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The PipelineDefnLoader is mutable because it changes the filesystem")
  public InfoHandler(PipelineDefnLoader loader, boolean outputAllErrorMessages, boolean requireSession) {
    this.loader = loader;
    this.outputAllErrorMessages = outputAllErrorMessages;
    this.requireSession = requireSession;
  }
  
  /**
   * Get a list of available pipelines from the {@link uk.co.spudsoft.query.pipeline.PipelineDefnLoader}.
   * @param routingContext The Vert.x {@link RoutingContext}.
   * @param response JAX-RS Asynchronous response, connected to the Vertx request by the RESTeasy JAX-RS implementation.
   */
  @GET
  @Path("/available")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Return a list of available pipelines")
  @ApiResponse(
          responseCode = "200"
          , description = "The list of available pipelines."
          , content = @Content(
                  mediaType = MediaType.APPLICATION_JSON
                  , schema = @Schema(implementation = PipelineNodesTree.PipelineNode.class)
          )
  )
  public void getAvailable(
          @Context RoutingContext routingContext
          , @Suspended final AsyncResponse response
  ) {
    RequestContext unauthedRequestContext = RequestContext.retrieveRequestContext(routingContext);
    try {
      RequestContext requestContext = HandlerAuthHelper.getRequestContext(routingContext, requireSession);
      Log.decorate(logger.atDebug(), unauthedRequestContext).log("RequestId: {}", requestContext.getRequestId());

      loader.getAccessible(requestContext)
              .onSuccess(ap -> {
                Log.decorate(logger.atDebug(), unauthedRequestContext).log("Available: {}", ap);
                if (ap == null) {
                  response.resume(Response.status(Response.Status.NO_CONTENT).build());
                } else {
                  response.resume(Response.ok(ap, MediaType.APPLICATION_JSON).build());
                }
              })
              .onFailure(ex -> {
                reportError(unauthedRequestContext, logger, "Failed to generate list of available pipelines: ", response, ex, outputAllErrorMessages);
              });
    } catch (Throwable ex) {
      reportError(unauthedRequestContext, logger, "Failed to getAvailable pipelines: ", response, ex, outputAllErrorMessages);
    }    
  }
  
  
  
  /**
   * Get details of a pipeline that would be needed for a UI to run the pipeline without using formio.
   * 
   * @param response JAX-RS Asynchronous response, connected to the Vertx request by the RESTeasy JAX-RS implementation.
   * @param routingContext The Vert.x {@link RoutingContext}.
   * @param path The path to the pipeline that is to be loaded and have it's arguments converted to a form.
   */
  @GET
  @Path("/details/{path:.*}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Return details of a pipeline that would be needed for a UI to run the pipeline without using formio")
  @ApiResponse(
          responseCode = "200"
          , description = "Details of a pipeline as required by a custom UI."
          , content = @Content(
                  mediaType = MediaType.APPLICATION_JSON
                  , schema = @Schema(
                          implementation = PipelineDetails.class
                  )
          )
  )
  public void getDetails(
          @Context RoutingContext routingContext
          , @Schema(
                  description = "The path to the quiery, as returned by a call to get /api/info/available"
            )
            @PathParam("path")
            String path
          , @Suspended final AsyncResponse response
  ) {
    RequestContext unauthedRequestContext = RequestContext.retrieveRequestContext(routingContext);
    try {
      RequestContext requestContext = HandlerAuthHelper.getRequestContext(routingContext, requireSession);

      loader.loadPipeline(path, requestContext, null)
              .compose(pipelineAndFile -> {
                try {
                  return Future.succeededFuture(
                          new PipelineDetails(
                                  requestContext
                                  , pipelineAndFile.file().getName()
                                  , path
                                  , pipelineAndFile.pipeline().getTitle()
                                  , pipelineAndFile.pipeline().getDescription()
                                  , pipelineAndFile.pipeline().getArgumentGroups()
                                  , pipelineAndFile.pipeline().getArguments()
                                  , pipelineAndFile.pipeline().getFormats()
                          )
                  );
                } catch (Throwable ex) {
                  return Future.failedFuture(ex);
                }
              })
              .onSuccess(fd -> {
                response.resume(Response.ok(fd, MediaType.APPLICATION_JSON).build());
              })
              .onFailure(ex -> {
                reportError(unauthedRequestContext, logger, "Failed to generate list of available pipelines: ", response, ex, outputAllErrorMessages);
              });
    } catch (Throwable ex) {
      reportError(unauthedRequestContext, logger, "Failed to get pipeline data: ", response, ex, outputAllErrorMessages);
    }    

  }
    
  /**
   * Report an error to the JAX-RS {@link jakarta.ws.rs.container.AsyncResponse}.
   * 
   * @param logger The logger to use for logging the error, so that this method may be used by any of the JAX-RS handlers.
   * @param requestContext The context in which this request is being made.
   * @param log The message to write to the log.
   * @param response JAX-RS Asynchronous response, connected to the Vertx request by the RESTeasy JAX-RS implementation.
   * @param ex The exception to report.
   * @param outputAllErrorMessages In a production environment error messages should usually not leak information that may assist a bad actor, set this to true to return full details in error responses.
   */
  static void reportError(RequestContext requestContext, Logger logger, String log, AsyncResponse response, Throwable ex, boolean outputAllErrorMessages) {
    Log.decorate(logger.atError(), requestContext).log(log, ex);
    
    int statusCode = 500;
    String message = "Unknown error";

    if (ex instanceof ServiceException serviceException) {
      statusCode = serviceException.getStatusCode();
      message = serviceException.getMessage();
    } else if (ex instanceof FileNotFoundException) {
      statusCode = 404;
      message = ex.getMessage();
    } else if (ex instanceof IllegalArgumentException) {
      statusCode = 400;
      message = ex.getMessage();
    } else if (ex instanceof FileSystemException) {
      if (ex.getCause() instanceof NoSuchFileException) {
        statusCode = 404;
        message = "Not found";
      }
    }

    if (outputAllErrorMessages) {
      message = ExceptionToString.convert(ex, "\n\t");
    }
    
    response.resume(
            Response.status(statusCode)
                    .type(MediaType.TEXT_PLAIN)
                    .entity(message)
                    .build()
    );
  }

}
