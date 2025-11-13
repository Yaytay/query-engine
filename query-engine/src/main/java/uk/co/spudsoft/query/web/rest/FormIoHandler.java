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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.FilterFactory;
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.pipeline.PipelineDefnLoader;
import uk.co.spudsoft.query.pipeline.PipelineDefnLoader.PipelineAndFile;
import uk.co.spudsoft.query.web.formio.FormBuilder;
import static uk.co.spudsoft.query.web.rest.InfoHandler.reportError;

/**
 * JAX-RS class implementing the REST API for outputting <a href="https://form.io/">FormIO</a> definitions of forms for capturing pipeline arguments..
 * <p>
 * FormIO is used as the mechanism for representing arguments because it provides a simple way for front ends to display pipeline arguments 
 * without introducing overly complex dependencies.
 *
 * @author jtalbut
 */
@Path("/formio")
@Timed
public class FormIoHandler {
  
  private static final Logger logger = LoggerFactory.getLogger(FormIoHandler.class);

  private final PipelineDefnLoader loader;
  private final FilterFactory filterFactory;
  private final boolean outputAllErrorMessages;
  private final boolean requireSession;

  static class PipelineStreamer implements StreamingOutput {
    
    private final PipelineAndFile pipelineAndFile;
    private final FormBuilder builder;

    PipelineStreamer(RequestContext requestContext, PipelineAndFile pipelineAndFile, int columns, FilterFactory filterFactory) {
      this.pipelineAndFile = pipelineAndFile;
      this.builder = new FormBuilder(requestContext, columns, filterFactory);
    }

    @Override
    public void write(OutputStream output) throws IOException, WebApplicationException {
      try {
        builder.buildForm(pipelineAndFile, output);
      } catch (Throwable ex) {
        logger.error("Failed to build form: ", ex);
      }
      closeQuietly(output);
    }
    
    void closeQuietly(OutputStream stream) {
      try {
        stream.close();
      } catch (Throwable ex) {
        logger.debug("Ignoring exception closing output stream: ", ex);
      }
    }

  }
  
  /**
   * Constructor.
   * 
   * @param loader Loader for providing the details of pipelines.
   * @param filterFactory Factory object for creating {@link uk.co.spudsoft.query.exec.filters.Filter} instances, used her to document the available Filters.
   * @param outputAllErrorMessages In a production environment error messages should usually not leak information that may assist a bad actor, set this to true to return full details in error responses.
   * @param requireSession If true any requests that do not have a login session will fail.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The PipelineDefnLoader is mutable because it changes the filesystem")
  public FormIoHandler(PipelineDefnLoader loader, FilterFactory filterFactory, boolean outputAllErrorMessages, boolean requireSession) {
    this.loader = loader;
    this.filterFactory = filterFactory;
    this.outputAllErrorMessages = outputAllErrorMessages;
    this.requireSession = requireSession;
  }
  
  /**
   * Get the formio definition of a single pipeline.
   * 
   * @param response JAX-RS Asynchronous response, connected to the Vertx request by the RESTeasy JAX-RS implementation.
   * @param routingContext The Vert.x {@link RoutingContext}.
   * @param path The path to the pipeline that is to be loaded and have it's arguments converted to a form.
   * @param columns The number of columns that the form should occupy.
   */
  @GET
  @Path("/{path:.*}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Return a form.io definition for a given document")
  @ApiResponse(
          responseCode = "200"
          , description = "A form.io definition for a given document."
          , content = @Content(
                  mediaType = MediaType.APPLICATION_JSON
                  , schema = @Schema(
                  )
          )
  )
  public void getFormIO(
          @Context RoutingContext routingContext
          , @Suspended final AsyncResponse response
          , @Schema(
                  description = "The path to the quiery, as returned by a call to get /api/info/available"
            )
            @PathParam("path")
            String path
          , @Schema(
                  description = "The number of columns to use when displaying arguments"
                  , minimum = "1"
                  , maximum = "12"
                  , defaultValue = "1"
                  , requiredMode = Schema.RequiredMode.NOT_REQUIRED
            )
            @QueryParam("columns") 
            Integer columns
  ) {
    
    if (columns != null)  {
      if (columns < 1) {
        columns = 1;
      } else if (columns > 12) {
        columns = 12;
      }
    }
    int colCount = columns == null ? 1 : columns;
    
    RequestContext unauthedRequestContext = RequestContext.retrieveRequestContext(routingContext);
    try {
      RequestContext requestContext = HandlerAuthHelper.getRequestContext(routingContext, requireSession);

      loader.loadPipeline(path, requestContext, null)
              .compose(pipelineAndFile -> {
                try {
                  return Future.succeededFuture(new PipelineStreamer(requestContext, pipelineAndFile, colCount, filterFactory));
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
      reportError(unauthedRequestContext, logger, "Failed to get FormIO data: ", response, ex, outputAllErrorMessages);
    }    

  }
}
