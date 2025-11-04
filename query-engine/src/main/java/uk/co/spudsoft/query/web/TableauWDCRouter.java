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
package uk.co.spudsoft.query.web;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_SECURITY_POLICY;
import static io.netty.handler.codec.http.HttpHeaderNames.X_FRAME_OPTIONS;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.Utils;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static uk.co.spudsoft.query.defn.RateLimitScopeType.path;
import static uk.co.spudsoft.query.web.SecurityHeadersRouter.appendSources;

/**
 * Vertx Web Router for handling requests for the tableau WDC file.
 * 
 * The sole file served is the resource /tableau-wdc.html and it is accessed as /tableau/wdc.
 *
 * @author jtalbut
 */
public class TableauWDCRouter implements Handler<RoutingContext> {
  
  private static final Logger logger = LoggerFactory.getLogger(TableauWDCRouter.class);

  private final Vertx vertx;

  private final String csp;
  private volatile Buffer cachedContents;
  
  /**
   * Factory method.
   * @param vertx The Vert.x instance.
   * @return a newly created UiRouter instance.
   */
  public static TableauWDCRouter create(Vertx vertx) {
    return new TableauWDCRouter(vertx);
  }

  private TableauWDCRouter(Vertx vertx) {
    this.vertx = vertx;
    
    StringBuilder builder = new StringBuilder();
    builder.append("default-src 'self'; img-src 'self'");
    
    List<String> cspStyleSrcs = Arrays.asList("'sha256-MAO2RY4M5wHVCq8FSbf+FTYVoFP+sRNR1q7kDopNEeI='");
    appendSources(builder, "style-src", "'unsafe-hashes'", cspStyleSrcs);
    appendSources(builder, "connect-src", null, null);

    List<String> cspScriptSrcs = Arrays.asList("'unsafe-inline'", "'unsafe-eval'", "https://ajax.googleapis.com/ajax/libs/jquery/", "https://connectors.tableau.com/libs/");
    appendSources(builder, "script-src", "'unsafe-eval'", cspScriptSrcs);
    this.csp = builder.toString();
  }
  
  @Override
  public void handle(RoutingContext context) {
    HttpServerRequest request = context.request();

    if (request.method() != HttpMethod.GET && request.method() != HttpMethod.HEAD) {
      logger.debug("{} request for {} ignored", request.method(), request.path());
      context.next();
    } else {
      logger.debug("UI request for {}", context.normalizedPath());
      
      if ("/tableau/wdc.html".equals(context.normalizedPath())
              || "/tableau-wdc.html".equals(context.normalizedPath())
              || "/tableau-wdc2.html".equals(context.normalizedPath())
              ) {
        Buffer fileBody = cachedContents;
        if (fileBody != null) {
          sendWdc(context, fileBody);
        } else {
          vertx.executeBlocking(() -> loadFile("/tableau-wdc.html"))
                  .onFailure(ex -> {
                    logger.warn("Unexpected failure in request for {}: ", path, ex);
                    context.response()
                            .setStatusCode(500)
                            .end("Internal server error");
                  })
                  .onSuccess(loadedFile -> {
                    sendWdc(context, loadedFile);
                  })
                  ;
        }        
      } else {
        logger.debug("{} request for {} ignored", request.method(), request.path());
        context.next();
      }
    }
  }
  
  private void sendWdc(RoutingContext context, Buffer loadedFile) {

    HttpServerResponse response = context.response();
    String contentType = "text/html;charset=utf-8";
    
    MultiMap headers = response.headers();
    
    headers.add(HttpHeaders.CONTENT_TYPE, contentType + ";charset=utf-8");
    headers.add(X_FRAME_OPTIONS, "SAMEORIGIN");
    headers.add(CONTENT_SECURITY_POLICY, csp);
    
    // date header is mandatory
    headers.set("date", Utils.formatRFC1123DateTime(System.currentTimeMillis()));
    
    response
            .setStatusCode(200)
            .end(context.request().method() == HttpMethod.HEAD ? Buffer.buffer() : loadedFile);
  }

  /**
   * Load a resource and stick it in the cache.
   * Blocking method
   * @param promise Promise to be completed when the method finishes.
   * @param path Path to the resource to be loaded.
   */
  private Buffer loadFile(String path) throws IOException {
    logger.debug("Loading file {}", path);
    InputStream is = this.getClass().getResourceAsStream(path);
    try (is) {
      byte[] body = is.readAllBytes();
      this.cachedContents = Buffer.buffer(body);
      return cachedContents;
    }
  }
  
}
