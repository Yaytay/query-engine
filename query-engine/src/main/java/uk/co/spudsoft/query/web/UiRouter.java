/*
 * Copyright (C) 2023 njt
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
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.impl.HttpUtils;
import io.vertx.core.http.impl.MimeMapping;
import io.vertx.core.net.impl.URIDecoder;
import io.vertx.ext.web.RoutingContext;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author njt
 */
public class UiRouter implements Handler<RoutingContext> {
  
  private static final Logger logger = LoggerFactory.getLogger(UiRouter.class);

  private final Vertx vertx;
  private final String baseResourcePath;
  private final String defaultFilePath;
  private final Cache<String, byte[]> cache;
  
  private final Map<String, String> additionalMimeExtensions = ImmutableMap.<String, String>builder()
          .put("webmanifest", "application/json")
          .build();
  
  public static UiRouter create(Vertx vertx, String baseResourcePath, String defaultFilePath) {
    return new UiRouter(vertx, baseResourcePath, defaultFilePath);
  }

  private UiRouter(Vertx vertx, String baseResourcePath, String defaultFilePath) {
    this.vertx = vertx;
    this.baseResourcePath = baseResourcePath;
    this.defaultFilePath = defaultFilePath;
    this.cache = CacheBuilder.newBuilder()
            .expireAfterAccess(60, TimeUnit.MINUTES)
            .maximumSize(150)
            .recordStats()
            .build();
  }
  
  @Override
  public void handle(RoutingContext context) {
    HttpServerRequest request = context.request();

    if (request.method() != HttpMethod.GET && request.method() != HttpMethod.HEAD) {
      logger.debug("{} request for {} ignored", request.method(), request.path());
      context.next();
    } else {
      if (!request.isEnded()) {
        request.pause();
      }
      // decode URL path
      String uriDecodedPath = URIDecoder.decodeURIComponent(context.normalizedPath(), false);
      // if the normalized path is null it cannot be resolved
      if (uriDecodedPath == null) {
        logger.warn("Invalid path: " + request.path());
        context.next();
        return;
      }
      // will normalize and handle all paths as UNIX paths
      String path = baseResourcePath + HttpUtils.removeDots(uriDecodedPath.replace('\\', '/'));
      
      byte[] fileBody = cache.getIfPresent(path);
      if (fileBody != null) {
        sendFile(context, path, fileBody);
      } else {
        vertx.<byte[]>executeBlocking(promise -> loadFile(promise, path))
                .onFailure(ex -> {
                  if (ex instanceof FileNotFoundException) {
                    context.response()
                            .setStatusCode(404)
                            .end("Page not found");
                  } else {
                    context.response()
                            .setStatusCode(500)
                            .end("Internal server error");
                  }
                })
                .onSuccess(loadedFileBody -> {
                  sendFile(context, path, loadedFileBody);
                })
                ;
      }
    }
  }
  
  private String getFileExtension(String file) {
    int li = file.lastIndexOf('.');
    if (li != -1 && li != file.length() - 1) {
      return file.substring(li + 1);
    } else {
      return null;
    }
  }
  
  private void sendFile(RoutingContext context, String path, byte[] body) {

    HttpServerResponse response = context.response();
    String extension = getFileExtension(path);
    String contentType = extension == null ? "text/html" : MimeMapping.getMimeTypeForExtension(extension);
    if (contentType == null) {
      contentType = additionalMimeExtensions.get(extension);
    }
    
    if (contentType != null) {
      if (contentType.startsWith("text")) {
        response.putHeader(HttpHeaders.CONTENT_TYPE, contentType + ";charset=utf-8");
      } else {
        response.putHeader(HttpHeaders.CONTENT_TYPE, contentType);
      }
    }
    
    response
            .setStatusCode(200)
            .end(context.request().method() == HttpMethod.HEAD ? Buffer.buffer() : Buffer.buffer(body));
  }

  private byte[] getDefaultFileBody() throws IOException {
    byte[] defaultFileBody = cache.getIfPresent(defaultFilePath);
    if (defaultFileBody == null) {
      logger.debug("Loading default file {}", defaultFilePath);
      try (InputStream is = this.getClass().getResourceAsStream(defaultFilePath)) {
        if (is == null) {
          return new byte[0];
        }
        defaultFileBody = is.readAllBytes();
        cache.put(defaultFilePath, defaultFileBody);
      }
    }
    return defaultFileBody;
  }
  
  /**
   * Load a resource and stick it in the cache.
   * Blocking method
   * @param promise Promise to be completed when the method finishes.
   * @param path Path to the resource to be loaded.
   */
  private void loadFile(Promise<byte[]> promise, String path) {
    logger.debug("Loading file {}", path);
    try (InputStream is = this.getClass().getResourceAsStream(path)) {
      if (is == null) {
        promise.complete(getDefaultFileBody());
        return ;
      }
      byte[] body = is.readAllBytes();
      cache.put(path, body);
      promise.complete(body);
    } catch(Throwable ex) {
      promise.fail(ex);
    }
  }
  
}
