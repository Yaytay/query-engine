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
import com.google.common.cache.CacheBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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
  public static final ObjectMapper JSON_OBJECT_MAPPER = createObjectMapper(null);
  public static final ObjectMapper YAML_OBJECT_MAPPER = createObjectMapper(new YAMLFactory());  

  private static ObjectMapper createObjectMapper(JsonFactory jf) {
    ObjectMapper mapper = new ObjectMapper(jf);
    ObjectMapperConfiguration.configureObjectMapper(mapper);
    mapper.setDefaultMergeable(Boolean.TRUE);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.addHandler(PROBLEM_HANDLER);
    return mapper;
  }
  
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

    if (cacheConfig.getMaxDurationMs() == 0) {
      cacheBuilder.maximumSize(1);
    } else {
      if (cacheConfig.getMaxItems() > 0) {
        cacheBuilder.maximumSize(cacheConfig.getMaxItems());
      }
      if (cacheConfig.getMaxDurationMs() > 0) {
        cacheBuilder.expireAfterWrite(cacheConfig.getMaxDurationMs(), TimeUnit.MILLISECONDS);
      }
    }

    this.fs = vertx.fileSystem();
    
    this.pipelineCache = new FileCache<>(fs
            , meterRegistry
            , "pipeline-cache"
            , cacheConfig.getMaxItems()
            , cacheConfig.getMaxDurationMs()
            );
    this.templateCache = new FileCache<>(fs
            , meterRegistry
            , "template-cache"
            , cacheConfig.getMaxItems()
            , cacheConfig.getMaxDurationMs()
            );
    this.permissionsCache = new FileCache<>(fs
            , meterRegistry
            , "permissions-cache"
            , cacheConfig.getMaxItems()
            , cacheConfig.getMaxDurationMs()
            );
    
    this.velocity = new VelocityEngine();
    velocity.setProperty(VelocityEngine.RESOURCE_LOADERS, "string");
    velocity.addProperty("resource.loader.string.class", StringResourceLoader.class.getName());
    velocity.addProperty("resource.loader.string.repository.static", "false");
    velocity.init();    
    
    if (cacheConfig.getPurgePeriodMs() > 0) {
      vertx.setPeriodic(cacheConfig.getPurgePeriodMs(), l -> purgeCaches());
    }
  }
  
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
                if (condition.evaluate(req)) {
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
      return readPipelineFromFile(file, req)
              .map(pipeline -> new PipelineNodesTree.PipelineFile(
                      filePathToUrlPath(file)
                      , pipeline.getTitle()
                      , pipeline.getDescription()
                      , pipeline.getArguments()
                      , pipeline.getFormats()
              ))
              ;
    }
  }
  
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
  
  public Future<Void> writeJsonFile(String absolutePath, Pipeline pipeline) {
    try {
      byte[] bytes = JSON_OBJECT_MAPPER.writeValueAsBytes(pipeline);
      return fs.writeFile(absolutePath, Buffer.buffer(bytes));
    } catch (Throwable ex) {
      return Future.failedFuture(ex);
    }
  }
  
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
              return mapper.readValue(buffer.getBytes(), Pipeline.class);
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
    vc.put("params", context.getArguments());
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
        return Future.succeededFuture(mapper.readValue(config, Pipeline.class));
      } catch (Throwable ex) {
        return Future.failedFuture(ex);
      }
    });
  }  
  
  public Future<Pipeline> loadPipeline(String srcPath, RequestContext context, Handler<DirCacheTree.File> fileNotifier) throws IOException, ServiceException {
    String path = validatePath(srcPath);
    
    return findSource(context, path)
            .compose(file -> {
              if (fileNotifier != null) {
                fileNotifier.handle(file);
              }
              return readPipelineFromFile(file, context);
            });
  }

  private Future<Pipeline> readPipelineFromFile(DirCacheTree.File file, RequestContext context) {
    logger.trace("Reading pipeline {}", file);
    if (file.getName().endsWith(".json")) {
      return readPipeline(file, JSON_OBJECT_MAPPER);
    } else if (file.getName().endsWith(".yaml") || file.getName().endsWith(".yml")) {
      return readPipeline(file, YAML_OBJECT_MAPPER);
    } else if (file.getName().endsWith(".json.vm")) {
      return readTemplate(file, JSON_OBJECT_MAPPER, context);
    } else if (file.getName().endsWith(".yaml.vm") || file.getName().endsWith(".yml.vm")) {
      return readTemplate(file, YAML_OBJECT_MAPPER, context);
    }
    return Future.failedFuture("Failed to find valid pipeline " + file.getPath());
  }
  
}
