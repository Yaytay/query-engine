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
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.vertx.core.file.FileSystemException;
import io.vertx.core.http.HttpServerRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
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
import uk.co.spudsoft.query.exec.conditions.RequestContextBuilder;
import uk.co.spudsoft.query.main.ExceptionToString;
import uk.co.spudsoft.query.pipeline.PipelineDefnLoader;
import uk.co.spudsoft.query.web.ServiceException;

/**
 *
 * @author jtalbut
 */
@Path("/info")
@Timed
public class InfoHandler {
  
  private static final Logger logger = LoggerFactory.getLogger(InfoHandler.class);

  private final RequestContextBuilder requestContextBuilder;
  private final PipelineDefnLoader loader;
  private final boolean outputAllErrorMessages;

  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The PipelineDefnLoader is mutable because it changes the filesystem")
  public InfoHandler(RequestContextBuilder requestContextBuilder, PipelineDefnLoader loader, boolean outputAllErrorMessages) {
    this.requestContextBuilder = requestContextBuilder;
    this.loader = loader;
    this.outputAllErrorMessages = outputAllErrorMessages;
  }
  
  @GET
  @Path("/available")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Return a list of available pipelines")
  @ApiResponse(
          responseCode = "200"
          , description = "The list of available pipelines."
          , content = @Content(
                  mediaType = MediaType.APPLICATION_JSON
                  , array = @ArraySchema(
                          minItems = 0
                          , schema = @Schema(implementation = PipelineNodesTree.PipelineNode.class)
                  )
          )
  )
  public void getAvailable(
          @Suspended final AsyncResponse response
          , @Context HttpServerRequest request
  ) {
    requestContextBuilder.buildRequestContext(request)
            .compose(context -> {
              logger.trace("API Request: {}", context);
              return loader.getAccessible(context);
            })
            .onSuccess(ap -> {
              response.resume(Response.ok(ap, MediaType.APPLICATION_JSON).build());
            })
            .onFailure(ex -> {
              reportError("Failed to generate list of available pipelines: ", response, ex, outputAllErrorMessages);
            });

  }
  
  static void reportError(String log, AsyncResponse response, Throwable ex, boolean outputAllErrorMessages) {
    logger.error(log, ex);
    
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
        message = "File not found";
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
