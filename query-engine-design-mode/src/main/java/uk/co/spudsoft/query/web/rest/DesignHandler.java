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
import jakarta.activation.FileTypeMap;
import jakarta.activation.MimetypesFileTypeMap;
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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
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
import uk.co.spudsoft.query.pipeline.PipelineDefnLoader;
import uk.co.spudsoft.query.web.ServiceException;
import static uk.co.spudsoft.query.web.rest.InfoHandler.reportError;

/**
 *
 * @author jtalbut
 */
@Path("/design")
@Timed
public class DesignHandler {
  
  private static final Logger logger = LoggerFactory.getLogger(DesignHandler.class);
  
  private static final Pattern GOOD_FILE = Pattern.compile("[^/\"|<>:?*\\p{C}]+\\.(json|yaml|yml|json.vm|yaml.vm|yml.vm|jexl)");
  private static final Pattern GOOD_FILE_PATH = Pattern.compile("[^/\"|.<>:?*\\p{C}][^\"|.<>:?*\\p{C}]+\\.(json|yaml|yml|json.vm|yaml.vm|yml.vm|jexl)");
  private static final Pattern GOOD_FOLDER = Pattern.compile("[^/\"|<>:?*\\p{C}.]+");
  private static final Pattern GOOD_FOLDER_PATH = Pattern.compile("[^/\"|.<>:?*\\p{C}][^\"|.<>:?*\\p{C}]+");
  private static final List<String> EXTENSIONS = Arrays.asList(".json", ".yaml", ".yml", ".json.vm", ".yaml.vm", ".yml.vm", ".jexl");
  
  private static final String MEDIA_TYPE_FOLDER = "inode/directory";
  private static final String MEDIA_TYPE_JSON = MediaType.APPLICATION_JSON;
  private static final String MEDIA_TYPE_YAML = "application/yaml";
  private static final String MEDIA_TYPE_VELOCITY_YAML = "application/yaml+velocity";
  private static final String MEDIA_TYPE_VELOCITY_JSON = "application/json+velocity";
  private static final String MEDIA_TYPE_JEXL = "application/jexl";

  private static final MediaType MEDIA_TYPE_FOLDER_TYPE = MediaType.valueOf(MEDIA_TYPE_FOLDER);
  private static final MediaType MEDIA_TYPE_JSON_TYPE = MediaType.valueOf(MEDIA_TYPE_JSON);
  private static final MediaType MEDIA_TYPE_YAML_TYPE = MediaType.valueOf(MEDIA_TYPE_YAML);
  private static final MediaType MEDIA_TYPE_VELOCITY_YAML_TYPE = MediaType.valueOf(MEDIA_TYPE_VELOCITY_YAML);
  private static final MediaType MEDIA_TYPE_VELOCITY_JSON_TYPE = MediaType.valueOf(MEDIA_TYPE_VELOCITY_JSON);
  private static final MediaType MEDIA_TYPE_JEXL_TYPE = MediaType.valueOf(MEDIA_TYPE_JEXL);
  
  private static final com.google.common.net.MediaType MEDIA_TYPE_JSON_GOOGLE_TYPE = com.google.common.net.MediaType.parse(MEDIA_TYPE_JSON);
  private static final com.google.common.net.MediaType MEDIA_TYPE_YAML_GOOGLE_TYPE = com.google.common.net.MediaType.parse(MEDIA_TYPE_YAML);
  
  private static final Pattern FILE_SEPARATOR_PATTERN = Pattern.compile(Pattern.quote(File.separator));
  
  private static final byte[] YAML_DOC_DELIMITER = "---\n".getBytes(StandardCharsets.UTF_8);
  
  private static final Map<String, MediaType> EXTN_TO_TYPE = ImmutableMap.<String, MediaType>builder()
          .put("json", MEDIA_TYPE_JSON_TYPE)
          .put("yaml", MEDIA_TYPE_YAML_TYPE)
          .put("yml", MEDIA_TYPE_YAML_TYPE)
          .put("json.jm", MEDIA_TYPE_VELOCITY_JSON_TYPE)
          .put("yaml.vm", MEDIA_TYPE_VELOCITY_YAML_TYPE)
          .put("yml.vm", MEDIA_TYPE_VELOCITY_YAML_TYPE)
          .put("jexl", MEDIA_TYPE_JEXL_TYPE)
          .build();
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
  private final FileTypeMap fileTypeMap;

  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The PipelineDefnLoader is mutable because it changes the filesystem")
  public DesignHandler(Vertx vertx, PipelineDefnLoader loader, DirCache dirCache) {
    this.vertx = vertx;
    this.fs = vertx.fileSystem();
    this.loader = loader;
    this.dirCache = dirCache;
    this.root = dirCache.getRoot().getPath();
    this.fileTypeMap = getMimetypesFileTypeMap();
  }

  private static FileTypeMap getMimetypesFileTypeMap() {
    try {
      return new MimetypesFileTypeMap();
    } catch (Throwable ex) {
      logger.error("Failed to load required mime.types map: ", ex);
      return MimetypesFileTypeMap.getDefaultFileTypeMap();
    }
  }
  
  @GET
  @Path("/enabled")
  @Produces(MEDIA_TYPE_JSON)
  @Operation(description = "Return a single 'true', in order to check whether deisng mode is enabled")
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
          , @Context HttpServerRequest request
  ) {
    try {
      String json = Json.encode(Boolean.TRUE);
      response.resume(Response.ok(json, MEDIA_TYPE_JSON).build());
    } catch (Throwable ex) {
      reportError("Failed to return true: ", response, ex, true);
    }
  }
  
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
          , @Context HttpServerRequest request
  ) {
    try {
      DesignNodesTree.DesignDir relativeDir = new DesignNodesTree.DesignDir(root, dirCache.getRoot(), "");
      String json = Json.encode(relativeDir);
      response.resume(Response.ok(json, MEDIA_TYPE_JSON).build());
    } catch (Throwable ex) {
      reportError("Failed to generate list of pipelines: ", response, ex, true);
    }
  }
  
  /**
   * Return true if the request prefers 'prefer' to 'over'.
   * 
   * More specifically, returns true if the media types listed in the accept header give a higher weight
   * to prefer than to over.
   * The 'over' media type is expected to be the native type of the resource.
   * If this function returns true the resource should be converted from 'over' to 'prefer'.
   * 
   * @param accept The accept header received on the request.
   * @param prefer The media type that may be preferred.
   * @param over The media type that may be not preferred.
   * @return true if the caller should convert from 'over' to 'prefer'.
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
          , @Context HttpServerRequest request
          , @PathParam("path") String path
          , @HeaderParam("accept") String accept
  ) {
    try {
      path = normalizePath("Get", "file", path);
      String fullPath = resolveToAbsolutePath(path);
      
      String type = fileTypeMap.getContentType(fullPath);
      
      fs.readFile(fullPath)
              .onSuccess(buffer -> {
                byte[] bytes = buffer.getBytes();
                if (MEDIA_TYPE_JSON.equals(type) && prefers(accept, MEDIA_TYPE_YAML_GOOGLE_TYPE, MEDIA_TYPE_JSON_GOOGLE_TYPE)) {
                  try {
                    JsonNode node = PipelineDefnLoader.JSON_OBJECT_MAPPER.readTree(bytes);
                    byte[] yamlBytes = PipelineDefnLoader.YAML_OBJECT_MAPPER.writeValueAsBytes(node);
                    response.resume(Response.ok(yamlBytes, MEDIA_TYPE_YAML_TYPE).build());
                    return ;
                  } catch (Throwable ex) {
                    logger.warn("Attempted to conver file contents from json to yaml failed: ", ex);
                  }
                } else if (MEDIA_TYPE_YAML.equals(type) && prefers(accept, MEDIA_TYPE_JSON_GOOGLE_TYPE, MEDIA_TYPE_YAML_GOOGLE_TYPE)) {
                  try {
                    JsonNode node = PipelineDefnLoader.YAML_OBJECT_MAPPER.readTree(bytes);
                    byte[] jsonBytes = PipelineDefnLoader.JSON_OBJECT_MAPPER.writeValueAsBytes(node);
                    response.resume(Response.ok(jsonBytes, MEDIA_TYPE_JSON_TYPE).build());
                    return ;
                  } catch (Throwable ex) {
                    logger.warn("Attempted to conver file contents from json to yaml failed: ", ex);
                  }
                } 
                response.resume(Response.ok(buffer.getBytes(), type).build());
              })
              .onFailure(ex -> {
                reportError("Failed to get file: ", response, ex, true);
              });
      
    } catch (Throwable ex) {
      reportError("Failed to get file: ", response, ex, true);
    }
  }

  private void handleFileChange(AsyncResult<Void> ar, AsyncResponse response, String action) {
    if (ar.succeeded()) {
      vertx.executeBlocking(promise -> {
                  try {
                    dirCache.refresh();
                  } catch (Throwable ex) {
                    logger.error("Calling DirCache.refresh failed (failure ignored): ", ex);
                  }
                  promise.complete();
                })
              .onSuccess(buffer -> {
                DesignNodesTree.DesignDir relativeDir = new DesignNodesTree.DesignDir(root, dirCache.getRoot(), "");
                String json = Json.encode(relativeDir);
                response.resume(Response.ok(json, MEDIA_TYPE_JSON).build());
              })
              .onFailure(ex -> {
                reportError("Failed to " + action + ": ", response, ex, true);
              });
    } else {
      reportError("Failed to " + action + ": ", response, ar.cause(), true);
    }
  }
  
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
          , @Context HttpServerRequest request
          , byte[] body
  ) {
    try {
      logger.debug("validate ({} bytes)", body.length);
      MediaType contentType = getContentType(request);
            
      Pipeline forValidation = null;
      if (MEDIA_TYPE_YAML_TYPE.isCompatible(contentType)) {
        try {
          forValidation = PipelineDefnLoader.YAML_OBJECT_MAPPER.readValue(body, Pipeline.class);
        } catch (Throwable ex) {
          logger.warn("The YAML body cannot be parsed as a Pipeline: {}", ex);
          reportError("The YAML body cannot be parsed as a Pipeline: " + ex.getMessage(), response, new ServiceException(400, "The YAML body cannot be parsed as a Pipeline: " + ex.getMessage()), true);
        }
      } else if (MEDIA_TYPE_JSON_TYPE.isCompatible(contentType)) {
        try {
          forValidation = PipelineDefnLoader.JSON_OBJECT_MAPPER.readValue(body, Pipeline.class);          
        } catch (Throwable ex) {
          logger.warn("The JSON body cannot be parsed as a Pipeline: {}", ex);
          reportError("The JSON body cannot be parsed as a Pipeline: " + ex.getMessage(), response, new ServiceException(400, "The JSON body cannot be parsed as a Pipeline: " + ex.getMessage()), true);
        }
      }
      if (forValidation != null) {
        try {
          forValidation.validate();
        } catch (Throwable ex) {
          logger.warn("The Pipeline is not valid: {}", ex);
          reportError("The Pipeline is not valid: " + ex.getMessage(), response, new ServiceException(400, "The Pipeline is not valid: " + ex.getMessage()), true);
        }
      }
      logger.debug("The pipeline is valid");
      response.resume(Response.ok("The pipeline is valid", "text/plain").build());
      
    } catch (Throwable ex) {
      logger.warn("Failed to validate file: ", ex);
      reportError("Failed to validate file: ", response, ex, true);
    }
  }
  
  @PUT
  @Path("/file/{path: .*}")
  @Produces(MEDIA_TYPE_JSON)
  @Operation(description = "Create a new file or folder.")
  @Consumes(ALL_TYPES)
  @ApiResponse(
          responseCode = "200"
          , description = "The list of all and directories files."
          , content = @Content(
                  mediaType = MEDIA_TYPE_JSON
                  , schema = @Schema(implementation = DesignNodesTree.DesignDir.class)
          )
  )
  public void putFile(
          @Suspended final AsyncResponse response       
          , @Context HttpServerRequest request
          , @PathParam("path") String path
          , byte[] body
  ) {
    try {
      MediaType contentType = getContentType(request);
            
      Future<Void> creationFuture;
      String action;
      if (MEDIA_TYPE_FOLDER_TYPE.isCompatible(contentType)) {
        path = normalizePath("Put", "folder", path);
        String fullPath = resolveToAbsolutePath(path);

        if (!GOOD_FOLDER_PATH.matcher(fullPath).matches()) {
          reportError("Folder name does not match " + GOOD_FOLDER, response, new ServiceException(400, "Illegal folder name"), true);
          return ;
        }
        
        logger.debug("Creating folder {}", fullPath);
        creationFuture = fs.mkdirs(fullPath);
        action = "create folder";
      } else {
        path = normalizePath("Put", "file", path);
        String fullPath = resolveToAbsolutePath(path);

        if (!GOOD_FILE_PATH.matcher(fullPath).matches()) {
          reportError("Filename does not match " + GOOD_FILE, response, new ServiceException(400, "Illegal file name"), true);
          return ;
        }
        
        
        int dotPos = fullPath.indexOf(".");
        String extn = fullPath.substring(dotPos + 1);
        if (extn != null) {
          extn = extn.toLowerCase();
        }
        MediaType typeForExtn = EXTN_TO_TYPE.get(extn);
        if (typeForExtn == null) {
          reportError("Unrecognised extension (" + extn + ") on file", response, new ServiceException(400, "Illegal file name; unrecognised extension"), true);
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
          reportError("Incorrect extension (" + extn + ") for file of type " + contentType, response, new ServiceException(400, "Illegal file name; extension does not match content-type"), true);
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
            .andThen(ar -> handleFileChange(ar, response,  action));
      
    } catch (Throwable ex) {
      reportError("Failed to put file: ", response, ex, true);
    }
  }
  
  @DELETE
  @Path("/file/{path: .*}")
  @Produces(MEDIA_TYPE_JSON)
  @Operation(description = "Delete a file or folder.")
  @ApiResponse(
          responseCode = "200"
          , description = "The list of all and directories files."
          , content = @Content(
                  mediaType = MEDIA_TYPE_JSON
                  , schema = @Schema(implementation = DesignNodesTree.DesignDir.class)
          )
  )
  public void deleteFile(
          @Suspended final AsyncResponse response       
          , @Context HttpServerRequest request
          , @PathParam("path") String path
  ) {
    try {
      path = normalizePath("Delete", "file", path);
      String fullPath = resolveToAbsolutePath(path);
      
      DirCacheTree.Node target = nodeFromPath(dirCache.getRoot(), path.split("/"), 0);

      if (target == null) {
        reportError("File not found: ", response, new FileNotFoundException("File not found"), true);
        return ;        
      } else if (target instanceof DirCacheTree.Directory dir) {
        if (!dir.getChildren().isEmpty()) {
          reportError("Directory not empty: ", response, new IllegalArgumentException("Directory not empty"), true);
        }
      }
      fs.delete(fullPath)
              .andThen(ar -> handleFileChange(ar, response, "delete file"));
      
    } catch (Throwable ex) {
      reportError("Failed to delete file: ", response, ex, true);
    }
  }

  @POST
  @Path("/rename/{path: .*}")
  @Produces(MEDIA_TYPE_JSON)
  @Operation(description = "Rename a file or folder.")
  @ApiResponse(
          responseCode = "200"
          , description = "The list of all and directories files."
          , content = @Content(
                  mediaType = MEDIA_TYPE_JSON
                  , schema = @Schema(implementation = DesignNodesTree.DesignDir.class)
          )
  )
  public void renameFile(
          @Suspended final AsyncResponse response       
          , @Context HttpServerRequest request
          , @PathParam("path") String path
          , @QueryParam("name") String newName
  ) {
    try {
      path = normalizePath("Rename", "file", path);
      String fullPath = resolveToAbsolutePath(path);
      newName = Normalizer.normalize(newName, Normalizer.Form.NFC);
      
      int lastSlash = fullPath.lastIndexOf(File.separator);
      String newFullPath = fullPath.substring(0, lastSlash) + File.separator + newName;
      
      DirCacheTree.Node source = nodeFromPath(dirCache.getRoot(), path.split("/"), 0);      
      if (source == null) {
        reportError("File not found", response, new ServiceException(404, "File not found"), true);
        return ;
      } 

      Future<?> renameFuture;
      if (source instanceof DirCacheTree.Directory) {
        if (!GOOD_FOLDER.matcher(newName).matches()) {
          reportError("Folder name does not match " + GOOD_FOLDER, response, new ServiceException(400, "Illegal folder name"), true);
          return ;
        }
        
        renameFuture = renameFolder(newFullPath, response, fullPath);
        
      } else if (source instanceof DirCacheTree.File) {
        if (!GOOD_FILE.matcher(newName).matches()) {
          reportError("Filename does not match " + GOOD_FILE, response, new ServiceException(400, "Illegal file name"), true);
          return ;
        }
        int dotPos = path.indexOf(".");
        if (dotPos > 0) {
          // Original file has an extension, so this should too.
          String originalExtension = path.substring(dotPos);
          int newDotPos = newName.indexOf(".");
          String newExtension = newName.substring(newDotPos);
          if (!newExtension.equals(originalExtension)) {
            reportError("New filename extension (" + newExtension + ") does not match original extension (" + originalExtension + ")", response, new ServiceException(400, "Illegal file name (extension has been changed)"), true);
            return ;
          }
        }
        
        renameFuture = renameFile(newFullPath, response, fullPath);
        
      } else {
        reportError("Attempt to rename unknown file type (" + source.getClass().getName() + ")", response, new ServiceException(500, "Unrecognized file type"), true);
        return ;
      }
      
      renameFuture
              .onSuccess(buffer -> {
                DesignNodesTree.DesignDir relativeDir = new DesignNodesTree.DesignDir(root, dirCache.getRoot(), "");
                String json = Json.encode(relativeDir);
                response.resume(Response.ok(json, MEDIA_TYPE_JSON).build());
              })
              .onFailure(ex -> {
                reportError("Failed to rename: ", response, ex, true);
              });
      
    } catch (Throwable ex) {
      reportError("Failed to get file: ", response, ex, true);
    }
  }

  private Future<?> renameFile(String newFullPath, final AsyncResponse response, String fullPath) {
    Future<?> renameFuture;
    renameFuture = fs.exists(newFullPath)
            .compose(exists -> {
              if (exists) {
                reportError("Destination file (" + newFullPath + ") already exists", response, new ServiceException(400, "Destination file already exists"), true);
                return Future.succeededFuture();
              } else {
                return fs.move(fullPath, newFullPath)
                        .compose(v -> {
                          return vertx.executeBlocking(p -> {
                            dirCache.refresh();
                            p.complete();
                          });
                        });
              }
            });
    return renameFuture;
  }

  private Future<?> renameFolder(String newFullPath, final AsyncResponse response, String fullPath) {
    Future<?> renameFuture;
    renameFuture = fs.exists(newFullPath)
            .compose(exists -> {
              if (exists) {
                reportError("Destination folder (" + newFullPath + ") already exists", response, new ServiceException(400, "Destination folder already exists"), true);
                return Future.succeededFuture();
              } else {
                return vertx.executeBlocking(p -> {
                  dirCache.stop();
                  p.complete();
                }).compose(v -> {
                  return fs.move(fullPath, newFullPath);
                }).compose(v -> {
                  return vertx.executeBlocking(p -> {
                    try {
                      dirCache.start();
                      p.complete();
                    } catch (IOException ex) {
                      logger.error("Failed to restart Dir-Cache: ", ex);
                    }
                  });
                });
              }
            });
    return renameFuture;
  }
  
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
          , @Context HttpServerRequest request
          , @PathParam("path") String path
  ) {
    
    try {
      path = normalizePath("Get", "pipeline", path);
      String fullPath = resolveToAbsolutePath(path);

      if (fullPath.endsWith(".json")) {
        loader.readJsonFile(fullPath)
                .onSuccess(pipeline -> {
                  response.resume(Response.ok(pipeline, MEDIA_TYPE_JSON).build());
                })
                .onFailure(ex -> {
                  reportError("Failed to read json pipeline: ", response, ex, true);
                })
                ;
      } else if (fullPath.endsWith(".yaml")) {
        loader.readYamlFile(fullPath)
                .onSuccess(pipeline -> {
                  response.resume(Response.ok(pipeline, MEDIA_TYPE_JSON).build());
                })
                .onFailure(ex -> {
                  reportError("Failed to read yaml pipeline: ", response, ex, true);
                })
                ;        
      } else {
        throw new ServiceException(400, "Only plain json or yaml pipelines may be requested");
      }      
    } catch (Throwable ex) {
      reportError("Failed to get pipeline: ", response, ex, true);
    }    
  }
    
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
    return normPath;
  }
  
  String resolveToAbsolutePath(String normalizedPath) {
    java.nio.file.Path fullPath = dirCache.getRoot().getPath().resolve(normalizedPath);
    String result = fullPath.toString();
    return result;
  }
}
