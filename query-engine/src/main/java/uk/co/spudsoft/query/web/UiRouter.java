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
package uk.co.spudsoft.query.web;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.cache.GuavaCacheMetrics;
import static io.netty.handler.codec.http.HttpHeaderNames.X_FRAME_OPTIONS;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.internal.net.RFC3986;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.Utils;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vertx Web Router for handling UI requests.
 * 
 * The files are all loaded from resources.
 * 
 * @author jtalbut
 */
public class UiRouter implements Handler<RoutingContext> {
  
  private static final Logger logger = LoggerFactory.getLogger(UiRouter.class);

  private final Vertx vertx;
  private final String stripPath;
  private final String baseResourcePath;
  private final String defaultFilePath;
  private final Cache<String, byte[]> cache;
  
  private final long bootTime = System.currentTimeMillis();

  private final Map<String, String> pathCspMap = ImmutableMap.<String, String>builder()
          .put("/index.html", "default-src 'self' 'sha256-GCoez1mDsbThY8diormWuepHO+IOb9/MxWt3wZH5+Fs=' 'sha256-uyCb6HK6D9ebM/jHz2e/N3b441L00QG/aknT2VzHqXU='")
          .build();
  
  /**
   * Factory method.
   * @param vertx The Vert.x instance.
   * @param meterRegistry MeterRegistry for production of metrics.
   * @param stripPath Any path that starts with this will have it stripped before further processing.
   * @param baseResourcePath The physical path to the UI resources.
   * @param defaultFilePath If the requested file does not exist as a resource this file is returned instead.
   * @return a newly created UiRouter instance.
   */
  public static UiRouter create(Vertx vertx, MeterRegistry meterRegistry, String stripPath, String baseResourcePath, String defaultFilePath) {
    return new UiRouter(vertx, meterRegistry, stripPath, baseResourcePath, defaultFilePath);
  }

  private record LoadedFile(String path, byte[] contents) {}
  
  private UiRouter(Vertx vertx, MeterRegistry meterRegistry, String stripPath, String baseResourcePath, String defaultFilePath) {
    this.vertx = vertx;
    this.stripPath = stripPath;
    this.baseResourcePath = baseResourcePath;
    this.defaultFilePath = defaultFilePath;
    this.cache = CacheBuilder.newBuilder()
            .expireAfterAccess(60, TimeUnit.MINUTES)
            .maximumSize(150)
            .recordStats()
            .build();
    if (meterRegistry != null) {
      GuavaCacheMetrics.monitor(meterRegistry, cache, "uifiles");
      meterRegistry.gauge("queryengine.cache.size"
              , Arrays.asList(
                      Tag.of("cachename", "uifiles")
              )
              , cache, c -> {
        synchronized (c) {
          return c.size();
        }
      });
    }
  }
  
  /**
   * Remove . and .. path components.
   * 
   * This must be called with a URL path, NOT a filesystem path, as the latter can lead to a escape from the resource location.
   * 
   * @param path The input path that may contain dots.
   * @return The path without any dots.
   */
  static String removeDots(String path) {
    Deque<String> segments = new ArrayDeque<>();
    for (String segment : path.split("/")) {
      switch (segment) {
        case "":
        case ".":
          break;
        case "..":
          if (!segments.isEmpty()) {
            segments.removeLast();
          }
          break;
        default:
          segments.add(segment);
      }
    }
    return "/" + String.join("/", segments);
  }

  @Override
  public void handle(RoutingContext context) {
    HttpServerRequest request = context.request();

    if (request.method() != HttpMethod.GET && request.method() != HttpMethod.HEAD) {
      logger.debug("{} request for {} ignored", request.method(), request.path());
      context.next();
    } else {
      // decode URL path
      String uriDecodedPath = RFC3986.decodeURIComponent(context.normalizedPath(), false);
      // if the normalized path is null it cannot be resolved
      if (uriDecodedPath == null) {
        logger.warn("Invalid path: " + request.path());
        context.next();
        return;
      }
      // will normalize and handle all paths as UNIX paths
      String normalizedPath = removeDots(uriDecodedPath.replace('\\', '/'));
      String strippedPath = normalizedPath.startsWith(stripPath) ? normalizedPath.substring(stripPath.length()) : normalizedPath;
      if (strippedPath.length() == 0) {
        strippedPath = "/index.html";
      }
      String path = baseResourcePath + strippedPath;
              
      logger.debug("UI request for {}", path);

      byte[] fileBody = cache.getIfPresent(path);
      if (fileBody != null) {
        sendFile(context, new LoadedFile(path, fileBody));
      } else {
        vertx.<LoadedFile>executeBlocking(() -> loadFile(path))
                .onFailure(ex -> {
                  logger.warn("Unexpected failure in request for {}: ", path, ex);
                  context.response()
                          .setStatusCode(500)
                          .end("Internal server error");
                })
                .onSuccess(loadedFile -> {
                  sendFile(context, loadedFile);
                })
                ;
      }
    }
  }
  
  /**
   * Return the file extension given a path or filename.
   * 
   * The extension is from the last dot to the end of the filename, as long as the dot is not the leading or trailing character
   * (in which case null is returned).
   * 
   * @param file The filename.
   * @return The extension.
   */
  static String getFileExtension(String file) {
    int li = file.lastIndexOf('.');
    if (li != -1 && li != file.length() - 1) {
      return file.substring(li + 1);
    } else {
      return null;
    }
  }
  
  private void sendFile(RoutingContext context, LoadedFile loadedFile) {

    HttpServerResponse response = context.response();
    String extension = getFileExtension(loadedFile.path);
    String contentType = extension == null ? "text/html" : MimeTypes.getMimeTypeForExtension(extension);
    logger.debug("File {} has extension {} and content type {}", loadedFile.path, extension, contentType);
    
    MultiMap headers = response.headers();
    
    if (contentType != null) {
      if (contentType.startsWith("text") && !contentType.contains("charset")) {
        headers.add(HttpHeaders.CONTENT_TYPE, contentType + ";charset=utf-8");
      } else {
        headers.add(HttpHeaders.CONTENT_TYPE, contentType);
      }
    }

    headers.add(X_FRAME_OPTIONS, "SAMEORIGIN");
    
    long maxAgeSeconds = 86400;
    if (loadedFile.path.endsWith("index.html")) {
      // Only cache index.html for 10 minutes
      maxAgeSeconds = 600;
    }
    Utils.addToMapIfAbsent(headers, HttpHeaders.CACHE_CONTROL, "public, immutable, max-age=" + maxAgeSeconds);
    Utils.addToMapIfAbsent(headers, HttpHeaders.LAST_MODIFIED, Utils.formatRFC1123DateTime(bootTime));
    // date header is mandatory
    headers.set("date", Utils.formatRFC1123DateTime(System.currentTimeMillis()));
    
    response
            .setStatusCode(200)
            .end(context.request().method() == HttpMethod.HEAD ? Buffer.buffer() : Buffer.buffer(loadedFile.contents));
  }

  private LoadedFile getDefaultFileBody() {
    byte[] defaultFileBody = cache.getIfPresent(defaultFilePath);
    if (defaultFileBody == null) {
      logger.debug("Loading default file {}", defaultFilePath);
      InputStream is = this.getClass().getResourceAsStream(defaultFilePath);
      try (is) {
        defaultFileBody = is.readAllBytes();
        cache.put(defaultFilePath, defaultFileBody);
      } catch (Throwable ex) {
        logger.warn("Failed to load default UI resource ({}): ", defaultFilePath, ex);
        return new LoadedFile(defaultFilePath, new byte[0]);
      }
    }
    return new LoadedFile(defaultFilePath, defaultFileBody);
  }
  
  /**
   * Load a resource and stick it in the cache.
   * Blocking method
   * @param promise Promise to be completed when the method finishes.
   * @param path Path to the resource to be loaded.
   */
  private LoadedFile loadFile(String path) throws IOException {
    logger.debug("Loading file {}", path);
    InputStream is = this.getClass().getResourceAsStream(path);
    if (is == null) {
      return getDefaultFileBody();
    }
    try (is) {
      byte[] body = is.readAllBytes();
      if (body.length == 0) {
        return getDefaultFileBody();
      } else {
        cache.put(path, body);
        return new LoadedFile(path, body);
      }
    } catch (Throwable ex) {
      logger.warn("Failed to load UI resource ({}): ", path, ex);
      throw ex;
    }
  }
  
}
