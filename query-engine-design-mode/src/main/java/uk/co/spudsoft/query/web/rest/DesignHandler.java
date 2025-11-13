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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.dircache.DirCache;
import uk.co.spudsoft.dircache.DirCacheTree;
import uk.co.spudsoft.query.defn.Pipeline;
import uk.co.spudsoft.query.exec.FormatRequest;
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.pipeline.PipelineDefnLoader;
import uk.co.spudsoft.query.web.ServiceException;
import static uk.co.spudsoft.query.web.rest.InfoHandler.reportError;

/**
 * The DesignHandler class is responsible for handling HTTP requests and managing
 * design-related operations such as retrieving files/directories, validating pipelines,
 * managing file uploads, handling file/folder renaming, deletions, and more.
 *
 * This class interacts with the file system and provides RESTful services, enabling
 * clients to retrieve or modify pipeline definitions, files, and directory structures.
 * It is built to function asynchronously, leveraging Vert.x for non-blocking operations.
 */
@Path("/design")
@Timed
public class DesignHandler {

  private static final Logger logger = LoggerFactory.getLogger(DesignHandler.class);

  /**
   * Pattern defining characters that are not allowed in file and directory names.
   * This includes quotes, pipes, angle brackets, colons, question marks, asterisks, backslashes, and control characters.
   */
  private static final Pattern BANNED_CHARS = Pattern.compile("\\\"|\\||<|>|\\:|\\?|\\*|\\\\|\\p{C}");

  /**
   * Pattern defining valid file names with supported extensions.
   * Supported extensions include JSON, YAML, Velocity templates, and JEXL files.
   */
  private static final Pattern GOOD_FILE = Pattern.compile("[^/\"|<>:?*\\p{C}]+\\.(json|yaml|yml|json.vm|yaml.vm|yml.vm|jexl)");

  /**
   * Pattern defining valid file paths.
   * Similar to {@link #GOOD_FILE} but with additional restrictions for path components.
   */
  private static final Pattern GOOD_FILE_PATH = Pattern.compile("[^\"|.<>:?*\\p{C}]+\\.(json|yaml|yml|json.vm|yaml.vm|yml.vm|jexl)");

  /**
   * Pattern defining valid folder names.
   * Folder names cannot contain path separators, quotes, angle brackets, colons, question marks, asterisks, control characters, or dots.
   */
  private static final Pattern GOOD_FOLDER = Pattern.compile("[^/\"|<>:?*\\p{C}.]+");

  /**
   * Pattern defining valid folder paths.
   * Similar to {@link #GOOD_FOLDER} but with additional restrictions for path components.
   */
  private static final Pattern GOOD_FOLDER_PATH = Pattern.compile("[^\"|.<>:?*\\p{C}]+");

  /**
   * List of supported file extensions that this handler can process.
   */
  private static final List<String> EXTENSIONS = Arrays.asList(".json", ".yaml", ".yml", ".json.vm", ".yaml.vm", ".yml.vm", ".jexl");

  /**
   * Media type identifier for directory resources.
   */
  private static final String MEDIA_TYPE_FOLDER = "inode/directory";

  /**
   * Media type identifier for JSON resources.
   */
  private static final String MEDIA_TYPE_JSON = MediaType.APPLICATION_JSON;

  /**
   * Media type identifier for YAML resources.
   */
  private static final String MEDIA_TYPE_YAML = "application/yaml";

  /**
   * Media type identifier for Velocity-templated YAML resources.
   */
  private static final String MEDIA_TYPE_VELOCITY_YAML = "application/yaml+velocity";

  /**
   * Media type identifier for Velocity-templated JSON resources.
   */
  private static final String MEDIA_TYPE_VELOCITY_JSON = "application/json+velocity";

  /**
   * Media type identifier for JEXL resources.
   */
  private static final String MEDIA_TYPE_JEXL = "application/jexl";

  /**
   * MediaType object for directory resources.
   */
  static final MediaType MEDIA_TYPE_FOLDER_TYPE = MediaType.valueOf(MEDIA_TYPE_FOLDER);

  /**
   * MediaType object for JSON resources.
   */
  static final MediaType MEDIA_TYPE_JSON_TYPE = MediaType.valueOf(MEDIA_TYPE_JSON);

  /**
   * MediaType object for YAML resources.
   */
  static final MediaType MEDIA_TYPE_YAML_TYPE = MediaType.valueOf(MEDIA_TYPE_YAML);

  /**
   * MediaType object for Velocity-templated YAML resources.
   */
  static final MediaType MEDIA_TYPE_VELOCITY_YAML_TYPE = MediaType.valueOf(MEDIA_TYPE_VELOCITY_YAML);

  /**
   * MediaType object for Velocity-templated JSON resources.
   */
  static final MediaType MEDIA_TYPE_VELOCITY_JSON_TYPE = MediaType.valueOf(MEDIA_TYPE_VELOCITY_JSON);

  /**
   * MediaType object for JEXL resources.
   */
  static final MediaType MEDIA_TYPE_JEXL_TYPE = MediaType.valueOf(MEDIA_TYPE_JEXL);

  /**
   * Google MediaType object for JSON resources.
   */
  private static final com.google.common.net.MediaType MEDIA_TYPE_JSON_GOOGLE_TYPE = com.google.common.net.MediaType.parse(MEDIA_TYPE_JSON);

  /**
   * Google MediaType object for YAML resources.
   */
  private static final com.google.common.net.MediaType MEDIA_TYPE_YAML_GOOGLE_TYPE = com.google.common.net.MediaType.parse(MEDIA_TYPE_YAML);

  /**
   * Pattern to split paths using the system's file separator.
   */
  private static final Pattern FILE_SEPARATOR_PATTERN = Pattern.compile(Pattern.quote(File.separator));

  /**
   * Byte array containing the YAML document delimiter.
   */
  private static final byte[] YAML_DOC_DELIMITER = "---\n".getBytes(StandardCharsets.UTF_8);

  /**
   * Map for converting file extensions to their corresponding MediaType.
   */
  private static final Map<String, MediaType> EXTN_TO_TYPE = ImmutableMap.<String, MediaType>builder()
    .put("json", MEDIA_TYPE_JSON_TYPE)
    .put("yaml", MEDIA_TYPE_YAML_TYPE)
    .put("yml", MEDIA_TYPE_YAML_TYPE)
    .put("json.vm", MEDIA_TYPE_VELOCITY_JSON_TYPE)
    .put("yaml.vm", MEDIA_TYPE_VELOCITY_YAML_TYPE)
    .put("yml.vm", MEDIA_TYPE_VELOCITY_YAML_TYPE)
    .put("jexl", MEDIA_TYPE_JEXL_TYPE)
    .build();

  /**
   * Map for converting MediaType strings to their corresponding file extensions.
   */
  private static final Map<String, String> TYPE_TO_EXTN = ImmutableMap.<String, String>builder()
    .put(MEDIA_TYPE_JSON, "json")
    .put(MEDIA_TYPE_YAML, "yaml")
    .put(MEDIA_TYPE_VELOCITY_JSON, "json.vm")
    .put(MEDIA_TYPE_VELOCITY_YAML, "yaml.vm")
    .put(MEDIA_TYPE_JEXL, "jexl")
    .build();

  private static final String PIPELINE_TYPES = MEDIA_TYPE_JSON + "," + MEDIA_TYPE_YAML;
  private static final String FILE_TYPES = MEDIA_TYPE_JSON + "," + MEDIA_TYPE_YAML + "," + MEDIA_TYPE_VELOCITY_YAML + "," + MEDIA_TYPE_VELOCITY_JSON + "," + MEDIA_TYPE_JEXL;
  private static final String ALL_TYPES = MEDIA_TYPE_FOLDER + "," + MEDIA_TYPE_JSON + "," + MEDIA_TYPE_YAML + "," + MEDIA_TYPE_VELOCITY_YAML + "," + MEDIA_TYPE_VELOCITY_JSON + "," + MEDIA_TYPE_JEXL;

  private final Vertx vertx;
  private final FileSystem fs;
  private final PipelineDefnLoader loader;
  private final DirCache dirCache;
  private final java.nio.file.Path root;


  /**
   * Constructs a new instance of the DesignHandler.
   *
   * @param vertx The Vert.x instance to be used for asynchronous operations.
   * @param loader The PipelineDefnLoader instance for managing pipeline definitions.
   * @param dirCache The DirCache instance for managing cached directory structures.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The PipelineDefnLoader is mutable because it changes the filesystem")
  public DesignHandler(Vertx vertx, PipelineDefnLoader loader, DirCache dirCache) {
    this.vertx = vertx;
    this.fs = vertx.fileSystem();
    this.loader = loader;
    this.dirCache = dirCache;
    this.root = dirCache.getRoot().getPath();
  }

  /**
   * Determines the file type based on the file's extension provided in the name.
   * If the file name corresponds to a supported file type, the associated MediaType is returned.
   * Throws an exception if the file type is not supported.
   *
   * @param name The name of the file, including its extension, from which the file type will be determined.
   *             If the file has a ".vm" extension, the underlying real extension is also evaluated.
   * @return The corresponding MediaType object for the file extension if it is recognized.
   * @throws IllegalArgumentException If the file type cannot be processed due to an unsupported or invalid extension.
   */
  static MediaType getFileTypeFromName(String name) {
    int idx = name.lastIndexOf(".");
    if (idx > 0) {
      String extension = name.substring(idx + 1);
      if ("vm".equals(extension)) {
        idx = name.lastIndexOf(".", idx - 1);
        extension = name.substring(idx + 1);
      }
      MediaType type = EXTN_TO_TYPE.get(extension);
      if (type != null) {
        return type;
      }
    }
    throw new IllegalArgumentException("Files of that type cannot be processed");
  }

  /**
   * Checks if the design mode is enabled and returns a boolean value as a JSON response.
   *
   * @param response The suspended AsyncResponse that will be used to asynchronously return the result.
   * @param routingContext The Vert.x {@link RoutingContext}.
   * @param request The HTTP request received from the client, used for context or metadata if needed.
   */
  @GET
  @Path("/enabled")
  @Produces(MEDIA_TYPE_JSON)
  @Operation(description = "Return a single 'true', in order to check whether design mode is enabled")
  @ApiResponse(
          responseCode = "200"
          , description = "Returns 'true'."
          , content = @Content(
                  mediaType = MEDIA_TYPE_JSON
                  , schema = @Schema(implementation = Boolean.class)
          )
  )
  public void getEnabled(
          @Suspended final AsyncResponse response
          , @Context RoutingContext routingContext
          , @Context HttpServerRequest request
  ) {
    RequestContext unauthedRequestContext = RequestContext.retrieveRequestContext(routingContext);
    
    try {
      String json = Json.encode(Boolean.TRUE);
      response.resume(Response.ok(json, MEDIA_TYPE_JSON).build());
    } catch (Throwable ex) {
      reportError(unauthedRequestContext, logger, "Failed to return true: ", response, ex, true);
    }
  }

  /**
   * Retrieves a list of all files and directories known and sends the result as a JSON response.
   *
   * @param response The asynchronous response used to return the result of the operation.
   * @param routingContext The Vert.x {@link RoutingContext}.
   * @param request The HTTP server request providing context or metadata for the operation.
   */
  @GET
  @Path("/all")
  @Produces(MEDIA_TYPE_JSON)
  @Operation(description = "Return a list of all files and directories known")
  @ApiResponse(
          responseCode = "200"
          , description = "The list of all and directories files."
          , content = @Content(
                  mediaType = MEDIA_TYPE_JSON
                  , schema = @Schema(implementation = DesignNodesTree.DesignDir.class)
          )
  )
  public void getAll(
          @Suspended final AsyncResponse response
          , @Context RoutingContext routingContext
          , @Context HttpServerRequest request
  ) {
    RequestContext unauthedRequestContext = RequestContext.retrieveRequestContext(routingContext);
    try {
      DesignNodesTree.DesignDir relativeDir = new DesignNodesTree.DesignDir(root, dirCache.getRoot(), "");
      String json = Json.encode(relativeDir);
      response.resume(Response.ok(json, MEDIA_TYPE_JSON).build());
    } catch (Throwable ex) {
      reportError(unauthedRequestContext, logger, "Failed to generate list of pipelines: ", response, ex, true);
    }
  }

  /**
   * Determines if a specific media type should be prioritized over another based on the provided "Accept" header.
   *
   * The method evaluates the list of media types parsed from the given "Accept" header.
   * If the preferred media type appears in the list, the method will return true unless the media type
   * that should not be prioritized appears before it.
   * If the "Accept" header is null or empty, it will default to false.
   *
   * @param accept The "Accept" header value provided as a string. It specifies the list of acceptable media types.
   * @param prefer The media type that should be preferred if present in the parsed list of acceptable types.
   * @param over The media type that, if present earlier in the list, should prevent preference for the other media type.
   * @return A boolean indicating whether the preferred media type should be prioritized based on the criteria.
   */
  static boolean prefers(String accept, com.google.common.net.MediaType prefer, com.google.common.net.MediaType over) {
    if (Strings.isNullOrEmpty(accept)) {
      return false;
    }
    List<com.google.common.net.MediaType> types = FormatRequest.parseAcceptHeader(accept);
    for (com.google.common.net.MediaType type : types) {
      if (over.is(type)) {
        return false;
      }
      if (prefer.is(type)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Handles the retrieval of a file at a given path and returns its contents. The file type is determined
   * based on its name and directory structure. Depending on the `accept` header and its content type, the
   * method may attempt to convert the file's content between JSON and YAML before returning the response.
   *
   * In case of any failure during the operation, an appropriate error is logged and the response
   * with a failure status is sent back to the client.
   *
   * @param response the asynchronous response object, which is used to send the response back to the client
   * @param routingContext The Vert.x {@link RoutingContext}.
   * @param request the HTTP server request containing context about the incoming request
   * @param path the relative path of the file to retrieve, resolved to an absolute path in the method
   * @param accept the `accept` header from the HTTP request, indicating the desired response content type
   */
  @GET
  @Path("/file/{path: .*}")
  @Produces(FILE_TYPES)
  @Operation(description = "Return the contents of file.")
  @ApiResponse(
          responseCode = "200"
          , description = "The source of a single pipeline."
          , content = @Content(
                  mediaType = MediaType.WILDCARD
          )
  )
  public void getFile(
          @Suspended final AsyncResponse response
          , @Context RoutingContext routingContext
          , @Context HttpServerRequest request
          , @PathParam("path") String path
          , @HeaderParam("accept") String accept
  ) {
    RequestContext unauthedRequestContext = RequestContext.retrieveRequestContext(routingContext);
    try {
      path = normalizePath("Get", "file", path);
      String fullPath = resolveToAbsolutePath(path);

      MediaType type = getFileTypeFromName(fullPath);

      fs.readFile(fullPath)
              .onSuccess(buffer -> {
                byte[] bytes = buffer.getBytes();
                if (MEDIA_TYPE_JSON_TYPE.equals(type) && prefers(accept, MEDIA_TYPE_YAML_GOOGLE_TYPE, MEDIA_TYPE_JSON_GOOGLE_TYPE)) {
                  try {
                    JsonNode node = PipelineDefnLoader.JSON_OBJECT_MAPPER.readTree(bytes);
                    byte[] yamlBytes = PipelineDefnLoader.YAML_OBJECT_MAPPER.writeValueAsBytes(node);
                    response.resume(Response.ok(yamlBytes, MEDIA_TYPE_YAML_TYPE).build());
                    return ;
                  } catch (Throwable ex) {
                    logger.warn("Attempted to convert file contents from json to yaml failed: ", ex);
                  }
                } else if (MEDIA_TYPE_YAML_TYPE.equals(type) && prefers(accept, MEDIA_TYPE_JSON_GOOGLE_TYPE, MEDIA_TYPE_YAML_GOOGLE_TYPE)) {
                  try {
                    JsonNode node = PipelineDefnLoader.YAML_OBJECT_MAPPER.readTree(bytes);
                    byte[] jsonBytes = PipelineDefnLoader.JSON_OBJECT_MAPPER.writeValueAsBytes(node);
                    response.resume(Response.ok(jsonBytes, MEDIA_TYPE_JSON_TYPE).build());
                    return ;
                  } catch (Throwable ex) {
                    logger.warn("Attempted to convert file contents from json to yaml failed: ", ex);
                  }
                }
                response.resume(Response.ok(buffer.getBytes(), type).build());
              })
              .onFailure(ex -> {
                reportError(unauthedRequestContext, logger, "Failed to get file: ", response, ex, true);
              });

    } catch (Throwable ex) {
      reportError(unauthedRequestContext, logger, "Failed to get file: ", response, ex, true);
    }
  }

  /**
   * Handles a file change event by refreshing the directory cache and sending an appropriate HTTP response.
   * This method triggers asynchronous operations to manage the directory changes and respond to the client.
   *
   * @param requestContext  The request context.
   * @param ar The result of the asynchronous operation indicating success or failure.
   * @param response The asynchronous response object used to send the HTTP response back to the client.
   * @param action The description of the action performed, used for logging and error messages.
   */
  private void handleFileChange(RequestContext requestContext, AsyncResult<Void> ar, AsyncResponse response, String action) {
    if (ar.succeeded()) {
      vertx.<Void>executeBlocking(() -> {
                  dirCache.refresh();
                  return null;
                })
              .onSuccess(buffer -> {
                DesignNodesTree.DesignDir relativeDir = new DesignNodesTree.DesignDir(root, dirCache.getRoot(), "");
                String json = Json.encode(relativeDir);
                response.resume(Response.ok(json, MEDIA_TYPE_JSON).build());
              })
              .onFailure(ex -> {
                reportError(requestContext, logger, "Failed to " + action + ": ", response, ex, true);
              });
    } else {
      reportError(requestContext, logger, "Failed to " + action + ": ", response, ar.cause(), true);
    }
  }

  /**
   * Determines the content type of the provided HTTP request by inspecting its headers.
   *
   * @param request the HTTP request from which the "Content-Type" header is extracted
   * @return the parsed MediaType representing the content type of the request
   * @throws ServiceException if the "Content-Type" header is missing or cannot be parsed
   */
  private MediaType getContentType(HttpServerRequest request) throws ServiceException {
    String contentTypeRaw = request.headers().get("Content-Type");
    if (Strings.isNullOrEmpty(contentTypeRaw)) {
      throw new ServiceException(400, "Content type not specified");
    }
    try {
      return MediaType.valueOf(contentTypeRaw);
    } catch (IllegalArgumentException ex) {
      throw new ServiceException(400, "Content type not parseable");
    }
  }

  /**
   * Validates a pipeline based on the data provided in the body of the request.
   * The pipeline can be sent in either JSON or YAML format. If validation is successful,
   * a response indicating the valid state is returned. Errors occur when there are format
   * issues, validation failures, or other processing exceptions.
   *
   * @param response Asynchronous response to send the validation result back to the client.
   * @param routingContext The Vert.x {@link RoutingContext}.
   * @param request The HTTP request object to determine context including content type.
   * @param body The binary body of the request containing the pipeline definition.
   */
  @POST
  @Path("/validate")
  @Produces("text/plain")
  @Operation(description = "Validate a pipeline.")
  @Consumes(PIPELINE_TYPES)
  @ApiResponse(
          responseCode = "200"
          , description = "Text response stating validation state."
          , content = @Content(
                  mediaType = "text/plain"
          )
  )
  public void validate(
          @Suspended final AsyncResponse response
          , @Context RoutingContext routingContext
          , @Context HttpServerRequest request
          , byte[] body
  ) {
    RequestContext unauthedRequestContext = RequestContext.retrieveRequestContext(routingContext);
    try {
      logger.debug("validate ({} bytes)", body.length);
      MediaType contentType = getContentType(request);

      Pipeline forValidation = null;
      if (MEDIA_TYPE_YAML_TYPE.isCompatible(contentType)) {
        try {
          forValidation = PipelineDefnLoader.YAML_OBJECT_MAPPER.readValue(body, Pipeline.class);
        } catch (Throwable ex) {
          logger.warn("The YAML body cannot be parsed as a Pipeline: {}", ex);
          reportError(unauthedRequestContext, logger, "The YAML body cannot be parsed as a Pipeline: " + ex.getMessage(), response, new ServiceException(400, "The YAML body cannot be parsed as a Pipeline: " + ex.getMessage()), true);
        }
      } else if (MEDIA_TYPE_JSON_TYPE.isCompatible(contentType)) {
        try {
          forValidation = PipelineDefnLoader.JSON_OBJECT_MAPPER.readValue(body, Pipeline.class);
        } catch (Throwable ex) {
          logger.warn("The JSON body cannot be parsed as a Pipeline: {}", ex);
          reportError(unauthedRequestContext, logger, "The JSON body cannot be parsed as a Pipeline: " + ex.getMessage(), response, new ServiceException(400, "The JSON body cannot be parsed as a Pipeline: " + ex.getMessage()), true);
        }
      }
      if (forValidation != null) {
        try {
          forValidation.validate();
        } catch (Throwable ex) {
          logger.warn("The Pipeline is not valid: {}", ex);
          reportError(unauthedRequestContext, logger, "The Pipeline is not valid: " + ex.getMessage(), response, new ServiceException(400, "The Pipeline is not valid: " + ex.getMessage()), true);
        }
      }
      logger.debug("The pipeline is valid");
      response.resume(Response.ok("The pipeline is valid", "text/plain").build());

    } catch (Throwable ex) {
      logger.warn("Failed to validate file: ", ex);
      reportError(unauthedRequestContext, logger, "Failed to validate file: ", response, ex, true);
    }
  }

  /**
   * Handles the creation of a file or folder based on the input parameters and request context.
   * This method determines whether to create a file or folder based on the content type of the request
   * and validates the provided path and file extensions accordingly. It supports file content processing
   * and validation when necessary.
   *
   * @param response The asynchronous response used to manage the request lifecycle.
   * @param routingContext The Vert.x {@link RoutingContext}.
   * @param request The HTTP server request containing details such as headers and content type.
   * @param path The path where the file or folder is to be created, relative to the base directory. This must
   *             not start with a '/' and should conform to valid file or folder naming conventions.
   * @param body A byte array representing the content of the file to be created. If creating a folder, this may be null.
   */
  @PUT
  @Path("/file/{path: .*}")
  @Produces(MEDIA_TYPE_JSON)
  @Operation(description = "Create a new file or folder.")
  @Consumes(ALL_TYPES)
  @ApiResponse(
          responseCode = "200"
          , description = "The list of all directories and files."
          , content = @Content(
                  mediaType = MEDIA_TYPE_JSON
                  , schema = @Schema(implementation = DesignNodesTree.DesignDir.class)
          )
  )
  public void putFile(
          @Suspended final AsyncResponse response
          , @Context RoutingContext routingContext
          , @Context HttpServerRequest request
          , @PathParam("path") String path
          , byte[] body
  ) {
    RequestContext unauthedRequestContext = RequestContext.retrieveRequestContext(routingContext);
    try {
      MediaType contentType = getContentType(request);

      if (path.startsWith("/")) {
        reportError(unauthedRequestContext, logger, "The path (" + path + ") must not start with a '/'", response, new ServiceException(400, "Illegal path name"), true);
        return ;
      }

      Future<Void> creationFuture;
      String action;
      if (MEDIA_TYPE_FOLDER_TYPE.isCompatible(contentType)) {
        path = normalizePath("Put", "folder", path);
        String fullPath = resolveToAbsolutePath(path);

        if (!GOOD_FOLDER_PATH.matcher(fullPath).matches()) {
          reportError(unauthedRequestContext, logger, "Folder path (" + fullPath + ") does not match " + GOOD_FOLDER_PATH, response, new ServiceException(400, "Illegal folder name"), true);
          return ;
        }

        logger.debug("Creating folder {}", fullPath);
        creationFuture = fs.mkdirs(fullPath);
        action = "create folder";
      } else {
        path = normalizePath("Put", "file", path);
        String fullPath = resolveToAbsolutePath(path);

        if (!GOOD_FILE_PATH.matcher(fullPath).matches()) {
          reportError(unauthedRequestContext, logger, "File path (" + fullPath + ") does not match " + GOOD_FILE_PATH, response, new ServiceException(400, "Illegal file name"), true);
          return ;
        }

        int dotPos = fullPath.indexOf(".");
        String extn = fullPath.substring(dotPos + 1);
        if (extn != null) {
          extn = extn.toLowerCase();
        }
        MediaType typeForExtn = EXTN_TO_TYPE.get(extn);
        if (typeForExtn == null) {
          reportError(unauthedRequestContext, logger, "Unrecognised extension (" + extn + ") on file", response, new ServiceException(400, "Illegal file name; unrecognised extension"), true);
          return ;
        }

        if (MEDIA_TYPE_JSON_TYPE.isCompatible(contentType) && (MEDIA_TYPE_YAML_TYPE.isCompatible(typeForExtn))) {
          // Have been sent JSON for a yaml file, convert it
          JsonNode node = PipelineDefnLoader.JSON_OBJECT_MAPPER.readTree(body);
          body = PipelineDefnLoader.YAML_OBJECT_MAPPER.writeValueAsBytes(node);
          if (body.length > YAML_DOC_DELIMITER.length && Arrays.equals(body, 0, YAML_DOC_DELIMITER.length, YAML_DOC_DELIMITER, 0, YAML_DOC_DELIMITER.length)) {
            body = Arrays.copyOfRange(body, YAML_DOC_DELIMITER.length, body.length);
          }
        } else if (!typeForExtn.isCompatible(contentType)) {
          reportError(unauthedRequestContext, logger, "Incorrect extension (" + extn + ") for file of type " + contentType, response, new ServiceException(400, "Illegal file name; extension does not match content-type"), true);
          return ;
        }

        logger.debug("Creating file {}", fullPath);
        creationFuture = fs.writeFile(fullPath, Buffer.buffer(body));
        action = "create file";

        byte[] finalBody = body;
        creationFuture = creationFuture.compose(v -> {
          Pipeline forValidation = null;
          if (MEDIA_TYPE_YAML_TYPE.isCompatible(typeForExtn)) {
            try {
              forValidation = PipelineDefnLoader.YAML_OBJECT_MAPPER.readValue(finalBody, Pipeline.class);
            } catch (Throwable ex) {
              return Future.failedFuture("The file has been saved, but could not be parsed as a Pipeline: " + ex.getMessage());
            }
          } else if (MEDIA_TYPE_JSON_TYPE.isCompatible(typeForExtn)) {
            try {
              forValidation = PipelineDefnLoader.JSON_OBJECT_MAPPER.readValue(finalBody, Pipeline.class);
            } catch (Throwable ex) {
              return Future.failedFuture("The file has been saved, but could not be parsed as a Pipeline: " + ex.getMessage());
            }
          }
          if (forValidation != null) {
            try {
              forValidation.validate();
            } catch (Throwable ex) {
              return Future.failedFuture("The file has been saved, but is not a valid Pipeline: " + ex.getMessage());
            }
          }
          return Future.succeededFuture();
        });
      }
      creationFuture
            .andThen(ar -> handleFileChange(unauthedRequestContext, ar, response,  action));

    } catch (Throwable ex) {
      reportError(unauthedRequestContext, logger, "Failed to put file: ", response, ex, true);
    }
  }


  /**
   * Deletes a specified file or folder.
   * This method handles the deletion operation and provides appropriate error handling,
   * including cases where the file or folder does not exist or when attempting to delete
   * a non-empty directory.
   *
   * @param response an {@link AsyncResponse} used to send the response asynchronously
   * @param routingContext The Vert.x {@link RoutingContext}.
   * @param request an {@link HttpServerRequest} representing the HTTP request context
   * @param path a {@link String} representing the relative or absolute path of the file or folder to delete
   */
  @DELETE
  @Path("/file/{path: .*}")
  @Produces(MEDIA_TYPE_JSON)
  @Operation(description = "Delete a file or folder.")
  @ApiResponse(
          responseCode = "200"
          , description = "The list of all directories and files."
          , content = @Content(
                  mediaType = MEDIA_TYPE_JSON
                  , schema = @Schema(implementation = DesignNodesTree.DesignDir.class)
          )
  )
  public void deleteFile(
          @Suspended final AsyncResponse response
          , @Context RoutingContext routingContext
          , @Context HttpServerRequest request
          , @PathParam("path") String path
  ) {
    RequestContext unauthedRequestContext = RequestContext.retrieveRequestContext(routingContext);
    try {
      path = normalizePath("Delete", "file", path);
      String fullPath = resolveToAbsolutePath(path);

      DirCacheTree.Node target = nodeFromPath(dirCache.getRoot(), path.split("/"), 0);

      if (target == null) {
        reportError(unauthedRequestContext, logger, "File not found: ", response, new FileNotFoundException("File not found"), true);
        return ;
      } else if (target instanceof DirCacheTree.Directory dir) {
        if (!dir.getChildren().isEmpty()) {
          reportError(unauthedRequestContext, logger, "Directory not empty: ", response, new IllegalArgumentException("Directory not empty"), true);
        }
      }
      fs.delete(fullPath)
              .andThen(ar -> handleFileChange(unauthedRequestContext, ar, response, "delete file"));

    } catch (Throwable ex) {
      reportError(unauthedRequestContext, logger, "Failed to delete file: ", response, ex, true);
    }
  }

  /**
   * Renames a file or folder at the specified path.
   *
   * @param response The AsyncResponse object used to send the response back to the client.
   * @param routingContext The Vert.x {@link RoutingContext}.
   * @param request The HttpServerRequest object containing information about the HTTP request.
   * @param path The relative path of the file or folder to be renamed.
   * @param newName The new name for the file or folder.
   */
  @POST
  @Path("/rename/{path: .*}")
  @Produces(MEDIA_TYPE_JSON)
  @Operation(description = "Rename a file or folder.")
  @ApiResponse(
          responseCode = "200"
          , description = "The list of all directories and files."
          , content = @Content(
                  mediaType = MEDIA_TYPE_JSON
                  , schema = @Schema(implementation = DesignNodesTree.DesignDir.class)
          )
  )
  public void renameFile(
          @Suspended final AsyncResponse response
          , @Context RoutingContext routingContext
          , @Context HttpServerRequest request
          , @PathParam("path") String path
          , @QueryParam("name") String newName
  ) {
    RequestContext unauthedRequestContext = RequestContext.retrieveRequestContext(routingContext);
    try {
      path = normalizePath("Rename", "file", path);
      String fullPath = resolveToAbsolutePath(path);
      newName = Normalizer.normalize(newName, Normalizer.Form.NFC);

      int lastSlash = fullPath.lastIndexOf(File.separator);
      String newFullPath = fullPath.substring(0, lastSlash) + File.separator + newName;

      DirCacheTree.Node source = nodeFromPath(dirCache.getRoot(), path.split("/"), 0);
      if (source == null) {
        reportError(unauthedRequestContext, logger, "File not found", response, new ServiceException(404, "File not found"), true);
        return ;
      }

      Future<?> renameFuture;
      if (source instanceof DirCacheTree.Directory) {
        if (!GOOD_FOLDER.matcher(newName).matches()) {
          reportError(unauthedRequestContext, logger, "Folder name does not match " + GOOD_FOLDER, response, new ServiceException(400, "Illegal folder name"), true);
          return ;
        }

        renameFuture = renameFolder(unauthedRequestContext, newFullPath, response, fullPath);

      } else if (source instanceof DirCacheTree.File) {
        if (!GOOD_FILE.matcher(newName).matches()) {
          reportError(unauthedRequestContext, logger, "Filename does not match " + GOOD_FILE, response, new ServiceException(400, "Illegal file name"), true);
          return ;
        }
        int dotPos = path.indexOf(".");
        if (dotPos > 0) {
          // Original file has an extension, so this should too.
          String originalExtension = path.substring(dotPos);
          int newDotPos = newName.indexOf(".");
          String newExtension = newName.substring(newDotPos);
          if (!newExtension.equals(originalExtension)) {
            reportError(unauthedRequestContext, logger, "New filename extension (" + newExtension + ") does not match original extension (" + originalExtension + ")", response, new ServiceException(400, "Illegal file name (extension has been changed)"), true);
            return ;
          }
        }

        renameFuture = renameFile(unauthedRequestContext, newFullPath, response, fullPath);

      } else {
        reportError(unauthedRequestContext, logger, "Attempt to rename unknown file type (" + source.getClass().getName() + ")", response, new ServiceException(500, "Unrecognized file type"), true);
        return ;
      }

      renameFuture
              .onSuccess(buffer -> {
                DesignNodesTree.DesignDir relativeDir = new DesignNodesTree.DesignDir(root, dirCache.getRoot(), "");
                String json = Json.encode(relativeDir);
                response.resume(Response.ok(json, MEDIA_TYPE_JSON).build());
              })
              .onFailure(ex -> {
                reportError(unauthedRequestContext, logger, "Failed to rename: ", response, ex, true);
              });

    } catch (Throwable ex) {
      reportError(unauthedRequestContext, logger, "Failed to get file: ", response, ex, true);
    }
  }

  /**
   * Renames a file from the specified source path to a new destination path.
   * If the destination file already exists, an error is reported.
   *
   * @param requestContext The request context.
   * @param newFullPath The full path of the destination where the file is to be renamed.
   * @param response The asynchronous response object used to report success or failure.
   * @param fullPath The full path of the source file to be renamed.
   * @return A Future representing the completion of the file renaming operation.
   */
  private Future<Void> renameFile(RequestContext requestContext, String newFullPath, final AsyncResponse response, String fullPath) {
    return fs.exists(newFullPath)
            .compose(exists -> {
              if (exists) {
                reportError(requestContext, logger, "Destination file (" + newFullPath + ") already exists", response, new ServiceException(400, "Destination file already exists"), true);
                return Future.succeededFuture();
              } else {
                return fs.move(fullPath, newFullPath)
                        .compose(v -> {
                          return vertx.<Void>executeBlocking(() -> {
                            dirCache.refresh();
                            return null;
                          });
                        });
              }
            });
  }

  /**
   * Renames an existing folder by moving it to a new specified path. If the destination folder already exists,
   * an error is reported without performing the rename operation.
   *
   * @param requestContext The request context.
   * @param newFullPath The new full path where the folder should be moved.
   * @param response The asynchronous response handler to communicate success or failure of the operation.
   * @param fullPath The current full path of the folder to be renamed.
   * @return A Future object representing the asynchronous result. The Future completes when the operation is successful
   *         or fails with an appropriate exception.
   */
  private Future<Void> renameFolder(RequestContext requestContext, String newFullPath, final AsyncResponse response, String fullPath) {
    return fs.exists(newFullPath)
            .compose(exists -> {
              if (exists) {
                reportError(requestContext, logger, "Destination folder (" + newFullPath + ") already exists", response, new ServiceException(400, "Destination folder already exists"), true);
                return Future.succeededFuture();
              } else {
                return vertx.<Void>executeBlocking(() -> {
                  dirCache.stop();
                  return null;
                }).compose(v -> {
                  return fs.move(fullPath, newFullPath);
                }).compose(v -> {
                  return vertx.<Void>executeBlocking(() -> {
                    dirCache.start();
                    return null;
                  });
                });
              }
            });
  }

  /**
   * Retrieves the source of a pipeline, either in JSON or YAML format, based on the provided path.
   * The pipeline is returned as a response in JSON format. In case of errors, an appropriate
   * error response is returned.
   *
   * @param response the asynchronous response to resume the HTTP request.
   * @param routingContext The Vert.x {@link RoutingContext}.
   * @param request the HTTP server request providing context for the operation.
   * @param path the path to the pipeline resource. The path is resolved to an absolute file path for retrieval of the pipeline source.
   */
  @GET
  @Path("/pipeline/{path: .*}")
  @Produces(MEDIA_TYPE_JSON)
  @Operation(description = "Return the source of a pipeline.")
  @ApiResponse(
          responseCode = "200"
          , description = "The source of a single pipeline."
          , content = @Content(
                  mediaType = MEDIA_TYPE_JSON
                  , schema = @Schema(implementation = Pipeline.class)
          )
  )
  public void getPipeline(
          @Suspended final AsyncResponse response
          , @Context RoutingContext routingContext
          , @Context HttpServerRequest request
          , @PathParam("path") String path
  ) {
    RequestContext unauthedRequestContext = RequestContext.retrieveRequestContext(routingContext);

    try {
      path = normalizePath("Get", "pipeline", path);
      String fullPath = resolveToAbsolutePath(path);

      if (fullPath.endsWith(".json")) {
        loader.readJsonFile(fullPath)
                .onSuccess(pipeline -> {
                  response.resume(Response.ok(pipeline, MEDIA_TYPE_JSON).build());
                })
                .onFailure(ex -> {
                  reportError(unauthedRequestContext, logger, "Failed to read json pipeline: ", response, ex, true);
                })
                ;
      } else if (fullPath.endsWith(".yaml")) {
        loader.readYamlFile(fullPath)
                .onSuccess(pipeline -> {
                  response.resume(Response.ok(pipeline, MEDIA_TYPE_JSON).build());
                })
                .onFailure(ex -> {
                  reportError(unauthedRequestContext, logger, "Failed to read yaml pipeline: ", response, ex, true);
                })
                ;
      } else {
        throw new ServiceException(400, "Only plain json or yaml pipelines may be requested");
      }
    } catch (Throwable ex) {
      reportError(unauthedRequestContext, logger, "Failed to get pipeline: ", response, ex, true);
    }
  }

  /**
   * Retrieves a node from the directory cache tree based on the specified path components.
   *
   * @param root The root directory to start searching from.
   * @param pathParts An array of strings representing the path components.
   * @param index The current index in the pathParts array being processed.
   * @return The node corresponding to the specified path if found, or null if the node does not exist or is not a directory.
   */
  private DirCacheTree.Node nodeFromPath(DirCacheTree.Directory root, String[] pathParts, int index) {
    DirCacheTree.Node node = root.get(pathParts[index]);
    if (index == pathParts.length - 1) {
      return node;
    } else if (node instanceof DirCacheTree.Directory dir) {
      return nodeFromPath(dir, pathParts, index + 1);
    } else {
      return null;
    }
  }

  /**
   * Normalizes a given path and performs validation checks to ensure the path adheres to specified constraints.
   * Logs the requested and normalized path details if normalization occurs.
   *
   * @param method the HTTP method or operation type associated with the request, for logging purposes
   * @param item the item or resource associated with the request, for logging purposes
   * @param path the input path to be normalized and validated
   * @return the normalized path as a string
   * @throws ServiceException if the path contains prohibited patterns, such as `..`, starts with `/`,
   *                          or includes characters that match the banned character pattern
   */
  String normalizePath(String method, String item, String path) throws ServiceException {
    String normPath = Normalizer.normalize(path, Normalizer.Form.NFC);
    if (!normPath.equals(path)) {
      logger.debug("{} {} requested: {}, normalized from {}", method, item, normPath, path);
    } else {
      logger.debug("{} {} requested: {}", method, item, path);
    }
    if (normPath.contains("..")) {
      throw new ServiceException(400, "Path may not contain ..");
    }
    if (normPath.startsWith("/")) {
      throw new ServiceException(400, "Path may not start with /");
    }
    if (BANNED_CHARS.matcher(normPath).find()) {
      throw new ServiceException(400, "Path may not contain banned characters (must not match /" + BANNED_CHARS +"/)");
    }
    return normPath;
  }

  /**
   * Resolves the given normalized file path to its absolute path by appending it
   * to the root directory from the directory cache.
   *
   * @param normalizedPath the relative, normalized path to be resolved
   * @return the absolute path as a string
   */
  String resolveToAbsolutePath(String normalizedPath) {
    java.nio.file.Path fullPath = dirCache.getRoot().getPath().resolve(normalizedPath);
    String result = fullPath.toString();
    return result;
  }
}
