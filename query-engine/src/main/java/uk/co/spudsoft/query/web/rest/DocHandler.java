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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
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
 * JAX-RS class implementing the REST API for outputting the (non-javadoc) documentation.
 * <p>
 * Some of this documentation is written as <a href="https://asciidoc.org/">AsciiDoc</a> files, others
 * are AsciiDoc generated from javadoc.
 * All the AsciiDoc files are processed into HTML files by the build process.
 * <p>
 * The files and directories are all hardcoded in this file, partly to ensure security and partly to provide control over the presentation of the files (order and naming).
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
  
  /**
   * The tree of files that can be served.
   * <p>
   * This is could be private, but is used by tests.
   */
  static final DocNodesTree.DocDir DOCS = new DocNodesTree.DocDir(
          "/"
          , Arrays.asList(new DocNodesTree.DocFile("Introduction.html", "Introduction")
                  , new DocNodesTree.DocFile("Getting Started.html", "Getting Started")
                  , new DocNodesTree.DocFile("Configuration.html", "Configuration")
                  , new DocNodesTree.DocDir(
                          "Parameters"
                          , Arrays.asList(
                                  new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.Parameters.html", "Parameters")
                                  , new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.Audit.html", "Audit")
                                  , new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.AuthEndpoint.html", "TracingConfig")
                                  , new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.CacheConfig.html", "CacheConfig")
                                  , new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.defn.Condition.html", "Condition")
                                  , new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.CookieConfig.html", "CacheConfig")
                                  , new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.Credentials.html", "Credentials")
                                  , new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.defn.DataType.html", "DataTypes")
                                  , new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.DataSourceConfig.html", "DataSourceConfig")
                                  , new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.JwtValidationConfig.html", "JwtValidationConfig")
                                  , new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.logging.LogbackOptions.html", "LogbackOptions")
                                  , new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.Persistence.html", "ProtectedCredentials")
                                  , new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.ProcessorConfig.html", "ProcessorConfig")
                                  , new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.ProtectedCredentials.html", "ProtectedCredentials")
                                  , new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.SessionConfig.html", "TracingConfig")
                                  , new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.TracingConfig.html", "TracingConfig")
                          )
                  )
                  , new DocNodesTree.DocFile("Authentication.html", "Authentication")
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
                                  new DocNodesTree.DocFile("Samples/Sample Data.html", "Sample Data")
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


  /**
   * Constructor.
   * @param outputAllErrorMessages In a production environment error messages should usually not leak information that may assist a bad actor, set this to true to return full details in error responses.
   * @param requireSession If true any requests that do not have a login session will fail.
   */
  public DocHandler(boolean outputAllErrorMessages, boolean requireSession) {
    this.outputAllErrorMessages = outputAllErrorMessages;
    this.requireSession = requireSession;
  }
  
  /**
   * Return a tree of available  documentation.
   * @param response JAX-RS Asynchronous response, connected to the Vertx request by the RESTeasy JAX-RS implementation.
   */
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
  ) {
    try {
      HandlerAuthHelper.getRequestContext(Vertx.currentContext(), requireSession);

      response.resume(Response.ok(DOCS, MediaType.APPLICATION_JSON).build());
    } catch (Throwable ex) {
      reportError(logger, "Failed to generate list of available documentation: ", response, ex, outputAllErrorMessages);
    }

  }
  
  /**
   * Get a single file from the documentation.
   * @param response JAX-RS Asynchronous response, connected to the Vertx request by the RESTeasy JAX-RS implementation.
   * @param path Path to the requested file.
   */
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
          , @PathParam("path") String path
  ) {
    
    try {
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
