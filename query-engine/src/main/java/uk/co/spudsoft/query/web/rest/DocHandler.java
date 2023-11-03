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
import io.vertx.core.Vertx;
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
import uk.co.spudsoft.query.web.MimeTypes;
import static uk.co.spudsoft.query.web.rest.InfoHandler.reportError;

/**
 *
 * @author jtalbut
 */
@Path("/docs")
@Timed
public class DocHandler {
  
  private static final Logger logger = LoggerFactory.getLogger(DocHandler.class.getName());
  
  private final boolean outputAllErrorMessages;
  private final boolean requireSession;
  
  private static final String BASE_DIR = "/docs/";
  static final DocNodesTree.DocDir DOCS = new DocNodesTree.DocDir(
          "/"
          , Arrays.asList(new DocNodesTree.DocFile("Introduction.html", "Introduction")
                  , new DocNodesTree.DocFile("Getting Started.html", "Getting Started")
                  , new DocNodesTree.DocFile("Configuration.html", "Configuration")
                  , new DocNodesTree.DocDir(
                          "Parameters"
                          , Arrays.asList(
                                  new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.Audit.html", "Audit")
                                  , new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.CacheConfig.html", "CacheConfig")
                                  , new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.defn.Condition.html", "Condition")
                                  , new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.Credentials.html", "Credentials")
                                  , new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.DataSourceConfig.html", "DataSourceConfig")
                                  , new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.JwtValidationConfig.html", "JwtValidationConfig")
                                  , new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.logging.LogbackOptions.html", "LogbackOptions")
                                  , new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.Parameters.html", "Parameters")
                                  , new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.ProtectedCredentials.html", "ProtectedCredentials")
                                  , new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.ZipkinConfig.html", "ZipkinConfig")
                          )
                  )
                  , new DocNodesTree.DocFile("Audit.html", "Audit")
                  , new DocNodesTree.DocFile("query-engine-compose.yml", "")
                  , new DocNodesTree.DocDir(
                          "Design Mode"
                          , Arrays.asList(
                                  new DocNodesTree.DocFile("Design Mode/Design Mode.html", "Design Mode")
                          )
                  )
                  , new DocNodesTree.DocDir(
                          "Samples"
                          , Arrays.asList(
                                  new DocNodesTree.DocFile("Samples/Samples Data.html", "Sample Data")
                                  , new DocNodesTree.DocFile("Samples/Test Database ERD.svg", "")
                          )
                  )
                  
          )
  );
  private final Set<String> knownDocs = extractKnownDocs(DOCS);
  
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

  public DocHandler(boolean outputAllErrorMessages, boolean requireSession) {
    this.outputAllErrorMessages = outputAllErrorMessages;
    this.requireSession = requireSession;
  }
  
  @GET
  @Path("/")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Return a tree of available  documentation")
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
    try {
      HandlerAuthHelper.getRequestContext(Vertx.currentContext(), requireSession);

      response.resume(Response.ok(DOCS, MediaType.APPLICATION_JSON).build());
    } catch (Throwable ex) {
      reportError(logger, "Failed to generate list of available documentation: ", response, ex, outputAllErrorMessages);
    }

  }
  
  @GET
  @Path("/{path:.*}")
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
    
    try {
      HandlerAuthHelper.getRequestContext(Vertx.currentContext(), requireSession);
      
      if (knownDocs.contains(path)) {
        String contents;
        try (InputStream strm = getClass().getResourceAsStream(BASE_DIR + path)) {
          contents = new String(strm.readAllBytes(), StandardCharsets.UTF_8);
        }
        
        response.resume(Response.ok(contents, MimeTypes.getMimeTypeForFilename(path)).build());
      } else {
        throw new FileNotFoundException(path);
      }
    } catch (Throwable ex) {
      reportError(logger, "Failed to get requested documentation: ", response, ex, outputAllErrorMessages);
    }

  }

  
}
