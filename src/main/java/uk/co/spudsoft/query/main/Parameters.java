/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main;

import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;

/**
 *
 * @author jtalbut
 */
public class Parameters {

  private VertxOptions vertxOptions;
  private HttpServerOptions httpServerOptions;
  private boolean exitOnRun;
  private String baseConfigPath;

  /**
   * Get the VertxOptions that will be used when creating the Vertx instance.
   * @return the VertxOptions that will be used when creating the Vertx instance.
   */
  public VertxOptions getVertxOptions() {
    return vertxOptions == null ? null : new VertxOptions(vertxOptions);
  }

  /**
   * Set the VertxOptions that will be used when creating the Vertx instance.
   * @param vertxOptions the VertxOptions that will be used when creating the Vertx instance.
   */
  public void setVertxOptions(VertxOptions vertxOptions) {
    this.vertxOptions = vertxOptions == null ? null : new VertxOptions(vertxOptions);
  }

  /**
   * Get the HttpServerOptions that will be used when creating the HTTP server.
   * The {@link io.vertx.core.http.HttpServerOptions#setMaxHeaderSize(int)} method should be particularly useful when running behind a proxy that passes large JSON headers.
   * @return the HttpServerOptions that will be used when creating the HTTP server.
   */
  public HttpServerOptions getHttpServerOptions() {
    return httpServerOptions == null ? null : new HttpServerOptions(httpServerOptions);
  }

  /**
   * Set the HttpServerOptions that will be used when creating the HTTP server.
   * @param httpServerOptions the HttpServerOptions that will be used when creating the HTTP server.
   */
  public void setHttpServerOptions(HttpServerOptions httpServerOptions) {
    this.httpServerOptions = httpServerOptions == null ? null : new HttpServerOptions(httpServerOptions);
  }  
  
  /**
   * Get the exitOnRun value.
   * When the exitOnRun value is set to true the process will end rather than waiting for requests.
   * This is expected to be useful for things such as JIT compilers or 
   * @return the exitOnRun value.
   */
  public boolean isExitOnRun() {
    return exitOnRun;
  }

  public void setExitOnRun(boolean exitOnRun) {
    this.exitOnRun = exitOnRun;
  }

  public String getBaseConfigPath() {
    return baseConfigPath;
  }

  public void setBaseConfigPath(String baseConfigPath) {
    this.baseConfigPath = baseConfigPath;
  }
  
  
  
}
