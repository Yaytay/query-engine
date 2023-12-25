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

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
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
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.pipeline.PipelineDefnLoader;
import uk.co.spudsoft.query.pipeline.PipelineNodesTree.PipelineDir;
import uk.co.spudsoft.query.pipeline.PipelineNodesTree.PipelineFile;
import uk.co.spudsoft.query.pipeline.PipelineNodesTree.PipelineNode;
import uk.co.spudsoft.query.web.ServiceException;
import uk.co.spudsoft.query.web.formio.FormBuilder;
import static uk.co.spudsoft.query.web.rest.InfoHandler.reportError;

/**
 *
 * @author jtalbut
 */
@Path("/formio")
@Timed
public class FormIoHandler {
  
  private static final Logger logger = LoggerFactory.getLogger(FormIoHandler.class);

  private final PipelineDefnLoader loader;
  private final boolean outputAllErrorMessages;
  private final boolean requireSession;

  private class PipelineStreamer implements StreamingOutput {
    
    private final PipelineFile pipeline;
    private final FormBuilder builder;

    PipelineStreamer(PipelineFile pipeline, int columns) {
      this.pipeline = pipeline;
      this.builder = new FormBuilder(columns);
    }

    @Override
    public void write(OutputStream output) throws IOException, WebApplicationException {
      builder.buildForm(pipeline, output);
    }
    
  }
  
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The PipelineDefnLoader is mutable because it changes the filesystem")
  public FormIoHandler(PipelineDefnLoader loader, boolean outputAllErrorMessages, boolean requireSession) {
    this.loader = loader;
    this.outputAllErrorMessages = outputAllErrorMessages;
    this.requireSession = requireSession;
  }
  
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
          @Suspended final AsyncResponse response
          , @Context HttpServerRequest request
          , @Context ObjectMapper objectMapper
          , @PathParam("path") String path
          , @QueryParam("columns") Integer columns
  ) {
    
    if (columns != null)  {
      if (columns < 1) {
        columns = 1;
      } else if (columns > 12) {
        columns = 12;
      }
    }
    int colCount = columns == null ? 1 : columns;
    
    try {
      RequestContext requestContext = HandlerAuthHelper.getRequestContext(Vertx.currentContext(), requireSession);

      loader.getAccessible(requestContext)
              .compose(root -> {
                try {
                  PipelineFile file = findFile(root, path);
                  return Future.succeededFuture(new PipelineStreamer(file, colCount));
                } catch (Throwable ex) {
                  return Future.failedFuture(ex);
                }
              })
              .onSuccess(fd -> {
                response.resume(Response.ok(fd, MediaType.APPLICATION_JSON).build());
              })
              .onFailure(ex -> {
                reportError(logger, "Failed to generate list of available pipelines: ", response, ex, outputAllErrorMessages);
              });
    } catch (Throwable ex) {
      reportError(logger, "Failed to get FormIO data: ", response, ex, outputAllErrorMessages);
    }    

  }
  
  private PipelineFile findFile(PipelineDir root, String path) throws ServiceException {
    String parts[] = path.split("/");
    PipelineDir current = root;
    for (int i = 0; i < parts.length; ++i) {
      int idx = i;
      Optional<PipelineNode> node = current.getChildren().stream().filter(n -> n.getName().equals(parts[idx])).findFirst();
      if (node.isEmpty()) {
        logger.warn("Path part {} from {} not found in available pipelines", parts[i], path);
        throw new ServiceException(404, "Not found");
      } else if (i < parts.length - 1) {
        if (node.get() instanceof PipelineDir dir) {
          current = dir;
        } else {
          logger.warn("Path part {} from {} is not a dir", parts[i], path);
          throw new ServiceException(404, "Not found");          
        }
      } else {
        if (node.get() instanceof PipelineFile file) {
          return file;
        } else {
          logger.warn("Path part {} from {} is not a file", parts[i], path);
          throw new ServiceException(404, "Not found");          
        }
      }
    }
    logger.warn("No parts in path \"{}\"!", path);
    throw new ServiceException(404, "Not found");          
  }
  
}
