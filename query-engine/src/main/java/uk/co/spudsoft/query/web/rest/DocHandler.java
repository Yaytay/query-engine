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

import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.web.MimeTypes;
import static uk.co.spudsoft.query.web.rest.InfoHandler.reportError;

/**
 * JAX-RS class implementing the REST API for outputting the (non-javadoc) documentation.
 * <p>
 * Some of this documentation is written as <a href="https://asciidoc.org/">AsciiDoc</a> files, others are AsciiDoc generated from
 * javadoc. All the AsciiDoc files are processed into HTML files by the build process.
 * <p>
 * The files and directories are all hardcoded in this file, partly to ensure security and partly to provide control over the
 * presentation of the files (order and naming).
 *
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
  static final DocNodesTree.DocDir BUILT_IN_DOCS = new DocNodesTree.DocDir(
          "/",
          Arrays.asList(new DocNodesTree.DocFile("Introduction.html", "Introduction"),
                  new DocNodesTree.DocFile("Getting Started.html", "Getting Started"),
                  new DocNodesTree.DocFile("Configuration.html", "Configuration"),
                  new DocNodesTree.DocDir(
                          "Parameters",
                          Arrays.asList(
                                  new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.Parameters.html", "Parameters"),
                                  new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.Audit.html", "Audit"),
                                  new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.AuthEndpoint.html", "TracingConfig"),
                                  new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.BasicAuthConfig.html", "BasicAuthConfig"),
                                  new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.BasicAuthGrantType.html", "BasicAuthGrantType"),
                                  new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.CacheConfig.html", "CacheConfig"),
                                  new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.ClientCredentials.html", "ClientCredentials"),
                                  new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.defn.Condition.html", "Condition"),
                                  new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.CookieConfig.html", "CacheConfig"),
                                  new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.Credentials.html", "Credentials"),
                                  new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.defn.DataType.html", "DataTypes"),
                                  new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.DataSourceConfig.html", "DataSourceConfig"),
                                  new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.Endpoint.html", "Endpoint"),
                                  new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.JwtValidationConfig.html", "JwtValidationConfig"),
                                  new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.logging.LogbackOptions.html", "LogbackOptions"),
                                  new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.Persistence.html", "ProtectedCredentials"),
                                  new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.ProcessorConfig.html", "ProcessorConfig"),
                                  new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.ProtectedCredentials.html", "ProtectedCredentials"),
                                  new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.SessionConfig.html", "TracingConfig"),
                                  new DocNodesTree.DocFile("Parameters/uk.co.spudsoft.query.main.TracingConfig.html", "TracingConfig")
                          )
                  ),
                  new DocNodesTree.DocFile("Authentication.html", "Authentication"),
                  new DocNodesTree.DocFile("Audit.html", "Audit"),
                  new DocNodesTree.DocFile("query-engine-compose.yml", ""),
                  new DocNodesTree.DocDir(
                          "Design Mode",
                          Arrays.asList(
                                  new DocNodesTree.DocFile("Design Mode/Design Mode.html", "Design Mode")
                          )
                  ),
                  new DocNodesTree.DocDir(
                          "Samples",
                          Arrays.asList(
                                  new DocNodesTree.DocFile("Samples/Sample Data.html", "Sample Data"),
                                  new DocNodesTree.DocFile("Samples/Test Database ERD.svg", "")
                          )
                  )
          )
  );
  private final Set<String> knownDocs;
  private final DocNodesTree.DocDir docsRoot;
  private final String altDocsRoot;

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

  static String baseFileName(java.nio.file.Path filename) {
    filename = filename.getFileName();
    if (filename == null) {
      return null;
    }
    String name = filename.toString();
    int idx = name.lastIndexOf(".");
    if (idx >= 0) {
      name = name.substring(0, idx);
    }
    return name;
  }

  @SuppressFBWarnings("PATH_TRAVERSAL_IN")
  static class DocGatherer extends SimpleFileVisitor<java.nio.file.Path> {

    private final java.nio.file.Path basePath;
    private final Stack<List<DocNodesTree.DocNode>> currentNodesStack = new Stack<>();
    private DocNodesTree.DocDir lastDirNode = null;

    DocGatherer(String basePath) {
       this.basePath = Paths.get(basePath).normalize().toAbsolutePath();
    }

    Stack<List<DocNodesTree.DocNode>> getCurrentNodesStack() {
      return currentNodesStack;
    }
    
    @Override
    public FileVisitResult visitFile(java.nio.file.Path file, BasicFileAttributes attrs) {
      logger.info("Visiting {}", file);
      if (!attrs.isSymbolicLink()) {
        currentNodesStack.getLast().add(new DocNodesTree.DocFile(basePath.relativize(file).toString(), baseFileName(file))
        );
      }

      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(java.nio.file.Path dir, BasicFileAttributes attrs) {
      logger.info("Entering {}", dir);
      if (!attrs.isSymbolicLink()) {
        currentNodesStack.push(new ArrayList<>());
        return FileVisitResult.CONTINUE;
      } else {
        return FileVisitResult.SKIP_SUBTREE;
      }
    }

    @Override
    public FileVisitResult postVisitDirectory(java.nio.file.Path dir, IOException exc) throws IOException {
      logger.info("Leaving {}", dir);
      if (exc != null) {
        throw exc;
      }
      List<DocNodesTree.DocNode> lastContents = currentNodesStack.removeLast();
      if (!lastContents.isEmpty()) {
        var docDir = new DocNodesTree.DocDir(basePath.relativize(dir).toString(), lastContents);
        if (currentNodesStack.isEmpty()) {
          lastDirNode = docDir;
        } else {
          currentNodesStack.getLast().add(docDir);
        }
      }

      return FileVisitResult.CONTINUE;
    }

  }

  @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "User has specified the path to use for alternative documentation")
  static DocNodesTree.DocDir buildAlternativeDocs(String basePath) {

    DocGatherer docGatherer = new DocGatherer(basePath);

    try {
      Files.walkFileTree(docGatherer.basePath, EnumSet.noneOf(FileVisitOption.class), 20, docGatherer);
    } catch (IOException ex) {
      logger.error("Error whilst reading alternative docs: ", ex);
    }
    return docGatherer.lastDirNode;

  }

  /**
   * Constructor.
   *
   * @param alternativeDocumentation path to a directory of alternative documentation to display instead of the built-in docs.
   * @param outputAllErrorMessages In a production environment error messages should usually not leak information that may assist
   * a bad actor, set this to true to return full details in error responses.
   * @param requireSession If true any requests that do not have a login session will fail.
   */
  public DocHandler(String alternativeDocumentation, boolean outputAllErrorMessages, boolean requireSession) {
    this.outputAllErrorMessages = outputAllErrorMessages;
    this.requireSession = requireSession;

    if (Strings.isNullOrEmpty(alternativeDocumentation)) {
      this.docsRoot = BUILT_IN_DOCS;
      this.knownDocs = extractKnownDocs(docsRoot);
      this.altDocsRoot = null;
    } else if ("/dev/null".equals(alternativeDocumentation)) {
      this.docsRoot = null;
      this.knownDocs = new HashSet<>();
      this.altDocsRoot = null;
    } else {
      this.docsRoot = buildAlternativeDocs(alternativeDocumentation);
      this.knownDocs = extractKnownDocs(docsRoot);
      this.altDocsRoot = alternativeDocumentation.endsWith(File.separator) ? alternativeDocumentation : alternativeDocumentation + File.separator;
    }
  }

  /**
   * Return a tree of available documentation.
   *
   * @param response JAX-RS Asynchronous response, connected to the Vert.x request by the RESTeasy JAX-RS implementation.
   * @param routingContext The Vert.x routing context.
   */
  @GET
  @Path("/")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Return a tree of available  documentation")
  @ApiResponse(
          responseCode = "200",
          description = "The list of available documents.",
          content = @Content(
                  mediaType = MediaType.APPLICATION_JSON,
                  array = @ArraySchema(
                          minItems = 0,
                          schema = @Schema(implementation = DocNodesTree.DocNode.class)
                  )
          )
  )
  public void getAvailable(
          @Context RoutingContext routingContext
          , @Suspended final AsyncResponse response
  ) {
    try {
      HandlerAuthHelper.getRequestContext(routingContext, requireSession);

      if (docsRoot == null) {
        logger.info("Request for documentation when disabled");
        response.resume(Response.status(Response.Status.NOT_FOUND).build());
      } else {
        response.resume(Response.ok(docsRoot, MediaType.APPLICATION_JSON).build());
      }
    } catch (Throwable ex) {
      reportError(logger, "Failed to generate list of available documentation: ", response, ex, outputAllErrorMessages);
    }

  }

  /**
   * Get a single file from the documentation.
   *
   * @param response JAX-RS Asynchronous response, connected to the Vertx request by the RESTeasy JAX-RS implementation.
   * @param path Path to the requested file.
   */
  @GET
  @Path("/{path:.*}")
  @Operation(description = "Return some documentation")
  @ApiResponse(
          responseCode = "200",
          description = "A document about Query Engine.",
          content = @Content(mediaType = "text/markdown")
  )
  @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "User has specified the path to use for alternative documentation")
  public void getDoc(
          @Suspended final AsyncResponse response,
          @Schema(
                  description = "The path to the document, as returned by a call to get /api/docs"
          )
          @PathParam("path") String path
  ) {

    try {
      if (knownDocs.contains(path)) {
        if (altDocsRoot != null) {
          File file = new File(altDocsRoot + path);

          response.resume(Response.ok(file, MimeTypes.getMimeTypeForFilename(path)).build());
        } else {
          String contents;
          try (InputStream strm = getClass().getResourceAsStream(BASE_DIR + path)) {
            contents = new String(strm.readAllBytes(), StandardCharsets.UTF_8);
          }

          response.resume(Response.ok(contents, MimeTypes.getMimeTypeForFilename(path)).build());
        }
      } else {
        throw new FileNotFoundException(path);
      }
    } catch (Throwable ex) {
      reportError(logger, "Failed to get requested documentation: ", response, ex, outputAllErrorMessages);
    }

  }

}
