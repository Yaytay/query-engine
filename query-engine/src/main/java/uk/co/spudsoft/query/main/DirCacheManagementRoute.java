/*
 * Copyright (C) 2025 jtalbut
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
package uk.co.spudsoft.query.main;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.dircache.DirCache;
import uk.co.spudsoft.dircache.DirCacheTree.Directory;
import uk.co.spudsoft.dircache.DirCacheTree.File;
import uk.co.spudsoft.dircache.DirCacheTree.Node;
import uk.co.spudsoft.mgmt.ContentTypes;

/**
 * Management endpoint router for exposing the DirCache details.
 * As with all management endpoints, this should not be exposed to end users.
 * @author jtalbut
 */
public class DirCacheManagementRoute implements Handler<RoutingContext> {

  private static final Logger logger = LoggerFactory.getLogger(DirCacheManagementRoute.class);
  
  /**
   * The path at which the standardDeploy method will put the router.
   */
  public static final String PATH = "dircache";
  
  /**
   * The cache being monitored.
   */
  private final DirCache dirCache;
  
  /**
   * Constructor.
   * @param dirCache The cache being monitored.
   */
  public DirCacheManagementRoute(DirCache dirCache) {
    this.dirCache = dirCache;
  }

  /**
   * Deploy the route to the router passed in at the normal endpoint.
   * 
   * The router passed in should be a sub router that is inaccessible to normal users.
   * 
   * @param router The router that this handler will be attached to.
   */
  public void standardDeploy(Router router) {
    router.route(HttpMethod.GET, "/" + PATH)
            .handler(this::handle)
            .setName("Dir Cache")
            .produces(ContentTypes.TYPE_JSON)
            .produces(ContentTypes.TYPE_HTML)
            .produces(ContentTypes.TYPE_PLAIN)
            ;
  }
  
  /**
   * Factory method to do standard deployment on newly constructed route.
   * 
   * The router passed in should be a sub router that is inaccessible to normal users.
   * 
   * @param router The router that this handler will be attached to.
   * @param dirCache The cache being monitored.
   */
  public static void createAndDeploy(Router router, DirCache dirCache) {
    DirCacheManagementRoute route = new DirCacheManagementRoute(dirCache);
    route.standardDeploy(router);
  }
  
  @Override
  public void handle(RoutingContext rc) {
    
    HttpServerRequest request = rc.request();
    
    if (request.method() == HttpMethod.GET) {
      
      ContentTypes.adjustFromParams(rc);
      
      if (!rc.queryParam("refresh").isEmpty()) {
        logger.debug("Refreshing dir cache based on explicit request");
        dirCache.refresh();
        logger.debug("Dir cache refreshed at {}", dirCache.getLastWalkTime());
      }

      if (ContentTypes.TYPE_JSON.equals(rc.getAcceptableContentType())) {
        HttpServerResponse response = rc.response();
        response.setStatusCode(200);
        response.putHeader(HttpHeaderNames.CONTENT_TYPE, ContentTypes.TYPE_JSON);
        response.end(buildDirTreeJson());
      } else if (ContentTypes.TYPE_HTML.equals(rc.getAcceptableContentType())) {
        HttpServerResponse response = rc.response();
        response.setStatusCode(200);
        response.putHeader(HttpHeaderNames.CONTENT_TYPE, ContentTypes.TYPE_HTML);
        response.end(buildDirTreeHtml());
      } else {
        HttpServerResponse response = rc.response();
        response.setStatusCode(200);
        response.putHeader(HttpHeaderNames.CONTENT_TYPE, ContentTypes.TYPE_PLAIN);
        response.end(buildDirTreeText());
      }
    } else {
      rc.next();
    }
  }

  private String buildDirTreeHtml() {
    StringBuilder result = new StringBuilder();
    
    result.append("<html><head>");
    result.append("<title>Dir Cache Contents</title>");
    result.append("<style>td { padding-left: 10px; padding-right: 10px; }</style>");
    result.append("</head><body>");
    result.append("Data last read: ").append(dirCache.getLastWalkTime());
    
    result.append("<table>\n");
    result.append("<thead><td>Path</td><td>Size</td><td>Last Modified</td></thead>\n");

    Directory root = dirCache.getRoot();
    
    processDirToHtml(result, root, "/");

    result.append("</table><body></html>\n");
    
    return result.toString();
  }

  @SuppressFBWarnings(value = "POTENTIAL_XML_INJECTION", justification = "This is manually building HTML")
  private void processDirToHtml(StringBuilder result, Directory dir, String currentPath) {
    List<Node> nodes = dir.getChildren();
    for (Node node : nodes) {
      if (node instanceof File file) {
        result.append("<tr><td>")
                .append(currentPath)
                .append(node.getName())
                .append("</td><td>")
                .append(file.getSize())
                .append("</td><td>")
                .append(node.getModified())
                .append("</td></tr>\n");
      }
    }
    for (Node node : nodes) {
      if (node instanceof Directory directory) {
        processDirToHtml(result, directory, currentPath + node.getName() + "/");
      }
    }    
  }  
  
  private Buffer buildDirTreeJson() {
    
    JsonObject result = new JsonObject();
    result.put("LastWalkTime", dirCache.getLastWalkTime());
    JsonArray arr = new JsonArray();
    result.put("Files", arr);
    processDirToJson(arr, dirCache.getRoot(), "/");

    return result.toBuffer();
    
  }

  private void processDirToJson(JsonArray result, Directory dir, String currentPath) {
    
    List<Node> nodes = dir.getChildren();
    for (Node node : nodes) {
      if (node instanceof File file) {
        JsonObject newFile = new JsonObject();
        newFile.put("path", currentPath + node.getName());
        newFile.put("size", file.getSize());
        newFile.put("lastModified", file.getModified());
        result.add(newFile);
      }
    }
    for (Node node : nodes) {
      if (node instanceof Directory directory) {
        processDirToJson(result, directory, currentPath + node.getName() + "/");
      }
    }
    
  }
  
  private String buildDirTreeText() {
    
    LocalDateTime lastWalk = dirCache.getLastWalkTime();
    Directory root = dirCache.getRoot();
    
    StringBuilder result = new StringBuilder();
    
    result.append("Last walk: ").append(lastWalk).append("\n\n");
    
    processDirToText(result, root, "/");
    
    return result.toString();
  }
  
  private void processDirToText(StringBuilder result, Directory dir, String currentPath) {
    List<Node> nodes = dir.getChildren();
    for (Node node : nodes) {
      if (node instanceof File file) {
        result.append(currentPath)
                .append(node.getName())
                .append("\t")
                .append(file.getSize())
                .append("\t")
                .append(node.getModified())
                .append("\n");
      }
    }
    for (Node node : nodes) {
      if (node instanceof Directory directory) {
        processDirToText(result, directory, currentPath + node.getName() + "/");
      }
    }
  }
  
}
