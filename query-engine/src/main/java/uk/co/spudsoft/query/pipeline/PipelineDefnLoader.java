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
package uk.co.spudsoft.query.pipeline;

import com.fasterxml.jackson.annotation.JsonInclude;
import uk.co.spudsoft.query.trees.AsyncDirTreeMapper;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.Hashing;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.Json;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.dircache.DirCache;
import uk.co.spudsoft.dircache.DirCacheTree;
import uk.co.spudsoft.query.main.CacheConfig;
import uk.co.spudsoft.query.defn.Pipeline;
import uk.co.spudsoft.query.exec.conditions.ConditionInstance;
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.json.ObjectMapperConfiguration;
import uk.co.spudsoft.query.web.ServiceException;

/**
 * Class that provides a loaded pipeline and that manages the caches to do so efficiently.
 * <P>
 * This is largely based on using the {@link uk.co.spudsoft.dircache.DirCache} to manage the filesystem.
 * 
 * @author jtalbut
 */
@SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "The path should come from configuration, not end user")
public final class PipelineDefnLoader {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(PipelineDefnLoader.class);

  private static final String PERMISSIONS_FILENAME = "permissions.jexl";
  
  /**
   * Paths cannot container any characters that have special meaning to paths on Windows or Linux, other than a slash.
   * They also may not begin with a slash.
   * 
   */
  private static final Pattern GOOD_PATH = Pattern.compile("[^/\"|.<>:?*\\p{C}][^\"|.<>:?*\\p{C}]+");
  private static final List<String> EXTENSIONS = Arrays.asList(".json", ".yaml", ".yml", ".json.vm", ".yaml.vm", ".yml.vm");
  
  private final MeterRegistry meterRegistry;
  private final Vertx vertx;
  private final FileCache<Pipeline> pipelineCache;
  private final FileCache<Template> templateCache;
  private final FileCache<ConditionInstance> permissionsCache;
  private final DirCache dirCache;
  private final FileSystem fs;
  private final VelocityEngine velocity;
  
  private static final DeserializationProblemHandler PROBLEM_HANDLER = new PipelineParsingErrorHandler();
  /**
   * The ObjectMapper to reading/writing JSON files containing {@link Pipeline} definitions.
   */
  public static final ObjectMapper JSON_OBJECT_MAPPER = createObjectMapper(null);
  /**
   * The ObjectMapper to reading/writing YAML files containing {@link Pipeline} definitions.
   */
  public static final ObjectMapper YAML_OBJECT_MAPPER = createObjectMapper(new YAMLFactory());  

  private static ObjectMapper createObjectMapper(JsonFactory jf) {
    ObjectMapper mapper = new ObjectMapper(jf);
    ObjectMapperConfiguration.configureObjectMapper(mapper);
    mapper.setDefaultMergeable(Boolean.TRUE);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.addHandler(PROBLEM_HANDLER);
    mapper.registerModule(new JavaTimeModule());
    return mapper;
  }

  /**
   * Constructor.
   * @param meterRegistry The {@link MeterRegistry} for generating metrics.
   * @param vertx The Vert.x instance.
   * @param cacheConfig Configuration to use for the pipeline and template caches.
   * @param dirCache The {@link DirCache} of the filesystem.
   * @throws IOException if something goes wrong with Velocity initialization.
   */  
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "No real option for accessing a MeterRegistry")
  public PipelineDefnLoader(
          MeterRegistry meterRegistry
          , Vertx vertx
          , CacheConfig cacheConfig
          , DirCache dirCache
  ) throws IOException {
    this.meterRegistry = meterRegistry;
    this.vertx = vertx;
    this.dirCache = dirCache;
    
    CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder()
            .recordStats()
            ;

    if (cacheConfig.getMaxDuration() != null && cacheConfig.getMaxDuration().isZero()) {
      cacheBuilder.maximumSize(1);
    } else {
      if (cacheConfig.getMaxItems() > 0) {
        cacheBuilder.maximumSize(cacheConfig.getMaxItems());
      }
      if (cacheConfig.getMaxDuration() != null) {
        cacheBuilder.expireAfterWrite(cacheConfig.getMaxDuration());
      }
    }

    this.fs = vertx.fileSystem();
    
    this.pipelineCache = new FileCache<>(fs
            , meterRegistry
            , "pipeline-cache"
            , cacheConfig.getMaxItems()
            , cacheConfig.getMaxDuration()
            );
    this.templateCache = new FileCache<>(fs
            , meterRegistry
            , "template-cache"
            , cacheConfig.getMaxItems()
            , cacheConfig.getMaxDuration()
            );
    this.permissionsCache = new FileCache<>(fs
            , meterRegistry
            , "permissions-cache"
            , cacheConfig.getMaxItems()
            , cacheConfig.getMaxDuration()
            );
    
    this.velocity = new VelocityEngine();
    velocity.setProperty(VelocityEngine.RESOURCE_LOADERS, "string");
    velocity.addProperty("resource.loader.string.class", StringResourceLoader.class.getName());
    velocity.addProperty("resource.loader.string.repository.static", "false");
    velocity.init();    
    
    if (cacheConfig.getPurgePeriod() != null) {
      vertx.setPeriodic(cacheConfig.getPurgePeriod().toMillis(), l -> purgeCaches());
    }
  }
  
  /**
   * Create a flat {@link Set} of all files in a {@link DirCacheTree.Directory directory tree}.
   * @param dir The {@link DirCacheTree.Directory directory tree}.
   * @param files The {@link Set} of files being collected.
   */
  static void addFilesToSet(DirCacheTree.Directory dir, Set<DirCacheTree.File> files) {
    if (dir != null) {
      dir.getChildren().forEach(node -> {
        if (node instanceof DirCacheTree.File f) {
          files.add(f);
        } else {
          DirCacheTree.Directory d = (DirCacheTree.Directory) node;
          addFilesToSet(d, files);
        }
      });
    }
  }
  
  private void purgeCaches() {
    logger.trace("Purging caches of expired files");
    Set<DirCacheTree.File> files = new HashSet<>();
    addFilesToSet(dirCache.getRoot(), files);
    pipelineCache.purge(files);
    templateCache.purge(files);
    permissionsCache.purge(files);
  }
  
  /**
   * Validate a requested path.
   * <p>
   * A valid path must match the {@link #GOOD_PATH} regular expression and must not contain a double slash.
   * Paths are always normalized using NFC before any processing is carried out.
   * 
   * @param path the path to validate.
   * @return the normalized, valid, path.
   * @throws ServiceException if the requested path is not valid.
   */
  public static String validatePath(String path) throws ServiceException {
    if (path == null) {
      throw new ServiceException(400, "The requested path is not valid", new IllegalArgumentException("Null path"));
    }    
    String normPath = Normalizer.normalize(path, Normalizer.Form.NFC);
    if (!GOOD_PATH.matcher(normPath).matches()) {
      logger.warn("{} does not match {}", GOOD_PATH, normPath);
      throw new ServiceException(400, "The requested path is not valid", new IllegalArgumentException("Path does not match GOOD_PATH regex"));
    }
    if (normPath.contains("//")) {
      throw new ServiceException(400, "The requested path is not valid", new IllegalArgumentException("Path contains double slash"));
    }
    return normPath;
  }
  
  private Future<Void> checkPermissions(DirCacheTree.File permsFile, RequestContext req) {    
    return permissionsCache.get(permsFile, buffer -> new ConditionInstance(buffer.toString()))
            .compose(condition -> {
              try {
                if (condition.evaluate(req, null)) {
                  return Future.succeededFuture();
                } else {
                  String expression = condition.getSourceText().trim();
                  if (expression.contains("\n")) {
                    logger.warn("Access prevented by the expression from {} with {}", permsFile.getPath(), req);
                  } else {
                    logger.warn("Access prevented by the expression from {} ({}) with {}", permsFile.getPath(), expression, req);
                  }
                  return Future.failedFuture(new ServiceException(403, "Forbidden"));
                }
              } catch (Throwable ex) {
                return Future.failedFuture(ex);
              }
            });
  }
  
  private Future<DirCacheTree.Directory> navigateDirs(RequestContext req, DirCacheTree.Directory dir, String[] parts, int offset) {

    DirCacheTree.Node permsNode = dir.get(PERMISSIONS_FILENAME);

    DirCacheTree.Directory childDir = offset < parts.length - 2 ? dir.getDir(parts[offset + 1]) : null;
    if (permsNode != null) {
      logger.debug("Permissions file found at {}", permsNode.getPath());
      return checkPermissions((DirCacheTree.File) permsNode, req)
              .compose(v -> childDir == null ? Future.succeededFuture(dir) : navigateDirs(req, childDir, parts, offset + 1));
    } else {
      return childDir == null ? Future.succeededFuture(dir) : navigateDirs(req, childDir, parts, offset + 1);
    }
  }
  
  private String filePathToUrlPath(DirCacheTree.Node node) {
    String urlPath = dirCache.getRoot().getPath().relativize(node.getPath()).toString();
    if (File.separatorChar == '\\') {
        urlPath = urlPath.replaceAll("\\\\", "/");
    }
    return urlPath;
  }
  
  private Future<Boolean> dirValidator(RequestContext req, DirCacheTree.Directory dir) {
    DirCacheTree.Node permsNode = dir.get(PERMISSIONS_FILENAME);
    if (permsNode == null) {
      return Future.succeededFuture(true);
    } else {
      return checkPermissions((DirCacheTree.File) permsNode, req)
              .map(v -> Boolean.TRUE)
              .recover(ex -> {
                if (ex instanceof ServiceException se) {
                  if (se.getStatusCode() == 403) {
                    return Future.succeededFuture(Boolean.FALSE);
                  }
                }
                return Future.failedFuture(ex);
              });
    }
  }
  
  private Future<PipelineNodesTree.PipelineDir> mapDir(DirCacheTree.Directory dir, List<PipelineNodesTree.PipelineNode> list) {
    if (list.isEmpty()) {
      return Future.succeededFuture();
    } else {
      return Future.succeededFuture(new PipelineNodesTree.PipelineDir(filePathToUrlPath(dir), list));
    }
  }
  
  private Future<PipelineNodesTree.PipelineFile> loadPipeline(DirCacheTree.File file, RequestContext req) {
    if (PERMISSIONS_FILENAME.equals(file.getName())) {
      return Future.succeededFuture();
    } else {
      logger.debug("Loading {}", file);
      return readPipelineFromFile(file, req)
              .onSuccess(paf -> {
                if (logger.isDebugEnabled()) {
                  logger.debug("Loaded {} as {}", file, Json.encode(paf.pipeline));
                }
                try {
                  paf.pipeline.validate();
                } catch (Throwable ex) {
                  logger.warn("File {} invalid: {}", file, ex.getMessage());
                }
              })
              .recover(ex -> {
                logger.warn("Failed to parse file {}: ", file, ex);
                return Future.failedFuture(ex);
              })
              .map(pipelineAndFile -> new PipelineNodesTree.PipelineFile(
                      filePathToUrlPath(file)
                      , pipelineAndFile.pipeline.getTitle()
                      , pipelineAndFile.pipeline.getDescription()
                      , pipelineAndFile.pipeline.getArgumentGroups()
                      , pipelineAndFile.pipeline.getArguments()
                      , pipelineAndFile.pipeline.getFormats()
              ))
              ;
    }
  }
  
  /**
   * Get a tree of pipelines that are accessible to the current request context.
   * @param req The request context to use in assessing accessibility.
   * @return a Future that will be completed with a tree of pipelines that are accessible to the current request context.
   */
  public Future<PipelineNodesTree.PipelineDir> getAccessible(RequestContext req) {
    
    return AsyncDirTreeMapper.<PipelineNodesTree.PipelineNode, PipelineNodesTree.PipelineDir, PipelineNodesTree.PipelineFile>map(
            dirCache.getRoot()
            , dir -> dirValidator(req, dir)
            , (dir, list) -> mapDir(dir, list)
            , file -> {
              if (file.getSize() == 0) {
                logger.info("File {} is empty, skipping it", file);
                return Future.succeededFuture();                
              }
              return loadPipeline(file, req)
                      .recover(ex -> {
                        logger.warn("Failed to load pipeline {}:", file, ex);
                        return Future.succeededFuture();
                      });
            }
    );
  }
  
  private Future<DirCacheTree.File> findSource(RequestContext req, String path) {
    
    String parts[] = path.split("/");

    DirCacheTree.Directory root = dirCache.getRoot();
    
    return navigateDirs(req, root, parts, -1)
            .compose(dir -> {
              String leafName = parts[parts.length - 1];
              for (DirCacheTree.Node child : dir.getChildren()) {
                if (!(child instanceof DirCacheTree.File)) {
                  continue ;
                }
                for (String extn : EXTENSIONS) {
                  String dirEntry = child.getName();
                  if (dirEntry.length() == leafName.length() + extn.length() && dirEntry.startsWith(leafName) && dirEntry.endsWith(extn)) {
                    return Future.succeededFuture((DirCacheTree.File) child);
                  }
                }
              }    
              return Future.failedFuture(new ServiceException(404, "File not found"));
            });
  }
  
  /**
   * Read a {@link Pipeline} from a file containing JSON.
   * @param absolutePath the path to the file that is to be read.
   * @return A Future that will be completed with the {@link Pipeline} definition when the file has been written.
   */
  public Future<Pipeline> readJsonFile(String absolutePath) {
    return fs.readFile(absolutePath)
            .compose(buffer -> {
              try {
                return Future.succeededFuture(JSON_OBJECT_MAPPER.readValue(buffer.getBytes(), Pipeline.class));
              } catch (Throwable ex) {
                return Future.failedFuture(ex);
              }
            });
  }
  
  /**
   * Read a {@link Pipeline} from a file containing YAML.
   * @param absolutePath the path to the file that is to be read.
   * @return A Future that will be completed with the {@link Pipeline} definition when the file has been written.
   */
  public Future<Pipeline> readYamlFile(String absolutePath) {
    return fs.readFile(absolutePath)
            .compose(buffer -> {
              try {
                return Future.succeededFuture(YAML_OBJECT_MAPPER.readValue(buffer.getBytes(), Pipeline.class));
              } catch (Throwable ex) {
                return Future.failedFuture(ex);
              }
            });
  }
  
  /**
   * Write the provided {@link Pipeline} definition as JSON to a file.
   * @param absolutePath the path to the file that is to be written.
   * @param pipeline the {@link Pipeline} to convert to JSON.
   * @return A Future that will be completed when the file has been written.
   */
  public Future<Void> writeJsonFile(String absolutePath, Pipeline pipeline) {
    try {
      byte[] bytes = JSON_OBJECT_MAPPER.writeValueAsBytes(pipeline);
      return fs.writeFile(absolutePath, Buffer.buffer(bytes));
    } catch (Throwable ex) {
      return Future.failedFuture(ex);
    }
  }
  
  /**
   * Write the provided {@link Pipeline} definition as YAML to a file.
   * @param absolutePath the path to the file that is to be written.
   * @param pipeline the {@link Pipeline} to convert to YAML.
   * @return A Future that will be completed when the file has been written.
   */
  public Future<Void> writeYamlFile(String absolutePath, Pipeline pipeline) {
    try {
      byte[] bytes = YAML_OBJECT_MAPPER.writeValueAsBytes(pipeline);
      return fs.writeFile(absolutePath, Buffer.buffer(bytes));
    } catch (Throwable ex) {
      return Future.failedFuture(ex);
    }
  }
  
  private Future<Pipeline> readPipeline(DirCacheTree.File file, ObjectMapper mapper) {
    return pipelineCache.get(file, buffer -> {
            try {
              byte[] bytes = buffer.getBytes();
              Pipeline pipeline = mapper.readValue(bytes, Pipeline.class);
              pipeline.setSha256(Hashing.sha256().hashBytes(bytes).toString());
              return pipeline;
            } catch (IOException ex) {
              throw new UncheckedIOException("IOException thrown mapping pipeline: ", ex);
            }
    });
  }
    
  private static VelocityContext buildVelocityContext(RequestContext context) {
    VelocityContext vc = new VelocityContext();
    vc.put("request", context);
    if (context.getCookies() != null) {
      vc.put("cookies", context.getCookies().stream().collect(Collectors.toMap(c -> c.getName(), c -> c)));
    }
    vc.put("params", context.getParams());
    return vc;
  }
  
  private final AtomicLong velocityTemplateId = new AtomicLong();
  
  private Future<Pipeline> readTemplate(DirCacheTree.File file, ObjectMapper mapper, RequestContext context) {
    String templateId = Long.toString(velocityTemplateId.incrementAndGet());
    return templateCache.get(file, buffer -> {
      StringResourceRepository repo = (StringResourceRepository) velocity.getApplicationAttribute(StringResourceLoader.REPOSITORY_NAME_DEFAULT);      
      repo.putStringResource(templateId, buffer.toString());
      Template template = velocity.getTemplate(templateId);
      repo.removeStringResource(templateId);
      return template;
    }).compose(template -> {
      StringWriter sw = new StringWriter();
      template.merge(buildVelocityContext(context), sw);
      try {
        String config = sw.toString();
        logger.trace("Template evaluated as: {} using {}", config, context);
        Pipeline pipeline = mapper.readValue(config, Pipeline.class);
        pipeline.setSha256(Hashing.sha256().hashString(config, StandardCharsets.UTF_8).toString());
        return Future.succeededFuture(pipeline);
      } catch (Throwable ex) {
        return Future.failedFuture(ex);
      }
    });
  }  
  
  /**
   * Record containing a {@link uk.co.spudsoft.dircache.DirCacheTree.File} and a {@link uk.co.spudsoft.query.defn.Pipeline}.
   * @param file {@link uk.co.spudsoft.dircache.DirCacheTree.File} representing the file on disc.
   * @param pipeline {@link uk.co.spudsoft.query.defn.Pipeline} definition loaded from the file.
   */
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "It's job is to container mutable objects")
  public record PipelineAndFile(DirCacheTree.File file, Pipeline pipeline){};
  
  /**
   * Load a pipeline from a specific path.
   * <p>
   * The pipeline must be accessible to the current request context, and will be read from cache if it's already been loaded.
   * 
   * @param srcPath The path to the pipeline definition.
   * @param context The request context.
   * @param parseErrorHandler Handler for any exceptions that occur during the loading of the file.
   * @return A Future that will be completed with a {@link PipelineAndFile} when then pipeline has been read.
   * @throws IOException if an IO error occurs.
   * @throws ServiceException If the path is not valid.
   */
  public Future<PipelineAndFile> loadPipeline(String srcPath, RequestContext context, BiConsumer<DirCacheTree.File, Throwable> parseErrorHandler) throws IOException, ServiceException {
    String path = validatePath(srcPath);
    
    return findSource(context, path)
            .compose(file -> {
              try {
                return readPipelineFromFile(file, context)
                        .onSuccess(paf -> {
                          if (logger.isDebugEnabled()) {
                            logger.debug("Loaded {} as {}", srcPath, Json.encode(paf.pipeline));
                          }
                        })
                        .recover(ex -> {
                          logger.warn("Failed to parse file {}: ", srcPath, ex);
                          if (parseErrorHandler != null) {
                            parseErrorHandler.accept(file, ex);
                          }
                          return Future.failedFuture(ex);
                        });
              } catch (Throwable ex) {
                logger.warn("Failed to parse file {}: ", srcPath, ex);
                if (parseErrorHandler != null) {
                  parseErrorHandler.accept(file, ex);
                }
                return Future.failedFuture(ex);
              }
            });
  }

  private Future<PipelineAndFile> readPipelineFromFile(DirCacheTree.File file, RequestContext context) {
    logger.trace("Reading pipeline {}", file);
    if (file.getName().endsWith(".json")) {
      return readPipeline(file, JSON_OBJECT_MAPPER).map(pipeline -> new PipelineAndFile(file, pipeline));
    } else if (file.getName().endsWith(".yaml") || file.getName().endsWith(".yml")) {
      return readPipeline(file, YAML_OBJECT_MAPPER).map(pipeline -> new PipelineAndFile(file, pipeline));
    } else if (file.getName().endsWith(".json.vm")) {
      return readTemplate(file, JSON_OBJECT_MAPPER, context).map(pipeline -> new PipelineAndFile(file, pipeline));
    } else if (file.getName().endsWith(".yaml.vm") || file.getName().endsWith(".yml.vm")) {
      return readTemplate(file, YAML_OBJECT_MAPPER, context).map(pipeline -> new PipelineAndFile(file, pipeline));
    }
    return Future.failedFuture("Failed to find valid pipeline " + file.getPath());
  }
  
}
