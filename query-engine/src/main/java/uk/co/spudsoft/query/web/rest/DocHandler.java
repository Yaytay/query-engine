/*
 * Copyright (C) 2023 jtalbut
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

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.conditions.RequestContextBuilder;
import static uk.co.spudsoft.query.web.rest.InfoHandler.reportError;

/**
 *
 * @author jtalbut
 */
@Path("/docs")
@Timed
public class DocHandler {
  
  private static final Logger logger = LoggerFactory.getLogger(DocHandler.class.getName());
  
  private final RequestContextBuilder requestContextBuilder;
  private final boolean outputAllErrorMessages;
  
  private static final String BASE_DIR = "/docs/";
  private final DocNodesTree.DocDir docs = new DocNodesTree.DocDir(
          "/"
          , Arrays.asList(new DocNodesTree.DocFile("Introduction.MD", "Introduction")
                  , new DocNodesTree.DocFile("Getting Started.MD", "Getting Started")
                  , new DocNodesTree.DocFile("query-engine-compose.yml", "")
                  , new DocNodesTree.DocDir(
                          "Design Mode"
                          , Arrays.asList(
                                  new DocNodesTree.DocFile("Design Mode/Design Mode.MD", "Design Mode")
                          )
                  )
                  
          )
  );
  private final Set<String> knownDocs = extractKnownDocs(docs);
  
  private static Set<String> extractKnownDocs(DocNodesTree.DocDir root) {
    Set<String> result = new HashSet<>();
    extractKnownDocs(result, root);
    return result;
  }

  private static void extractKnownDocs(Set<String> result, DocNodesTree.DocDir root) {
    for (DocNodesTree.DocNode node : root.getChildren()) {
      if (node instanceof DocNodesTree.DocFile doc) {
        result.add(doc.getPath());
      } else if (node instanceof DocNodesTree.DocDir dir) {
        extractKnownDocs(result, dir);
      }
    }
  }

  public DocHandler(RequestContextBuilder requestContextBuilder, boolean outputAllErrorMessages) {
    this.requestContextBuilder = requestContextBuilder;
    this.outputAllErrorMessages = outputAllErrorMessages;
  }
  
  @GET
  @Path("/")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Return a list of available  documents")
  @ApiResponse(
          responseCode = "200"
          , description = "The list of available documents."
          , content = @Content(
                  mediaType = MediaType.APPLICATION_JSON
                  , array = @ArraySchema(
                          minItems = 0
                          , schema = @Schema(implementation = DocNodesTree.DocNode.class)
                  )
          )
  )
  public void getAvailable(
          @Suspended final AsyncResponse response
          , @Context HttpServerRequest request
  ) {
    requestContextBuilder.buildRequestContext(request)
            .compose(context -> {
              logger.trace("Document Request: {}", context);
              return Future.succeededFuture(docs);
            })
            .onSuccess(ap -> {
              response.resume(Response.ok(ap, MediaType.APPLICATION_JSON).build());
            })
            .onFailure(ex -> {
              reportError("Failed to generate list of available documentation: ", response, ex, outputAllErrorMessages);
            });

  }
  
  @GET
  @Path("/{path:.*}")
  @Produces("text/markdown")
  @Operation(description = "Return some documentation")
  @ApiResponse(
          responseCode = "200"
          , description = "A documnent about Query Engine."
          , content = @Content(mediaType = "text/markdown")
  )
  public void getDoc(
          @Suspended final AsyncResponse response
          , @Context HttpServerRequest request
          , @PathParam("path") String path
  ) {
    requestContextBuilder.buildRequestContext(request)
            .compose(context -> {
              logger.trace("Document Request: {}", context);
              try {
                if (knownDocs.contains(path)) {
                  String contents;
                  try (InputStream strm = getClass().getResourceAsStream(BASE_DIR + path)) {
                    contents = new String(strm.readAllBytes(), StandardCharsets.UTF_8);
                  }
                  return Future.succeededFuture(contents);
                } else {
                  return Future.failedFuture(new FileNotFoundException(path));
                }
              } catch (Throwable ex) {
                return Future.failedFuture(ex);
              }
            })
            .onSuccess(contents -> {
              response.resume(Response.ok(contents, "text/markdown").build());
            })
            .onFailure(ex -> {
              reportError("Failed to get requested documentation: ", response, ex, outputAllErrorMessages);
            });

  }

  
}
