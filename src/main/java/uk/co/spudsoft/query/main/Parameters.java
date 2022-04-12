/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main;

import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import uk.co.spudsoft.params4j.JavadocCapture;

/**
 *
 * @author jtalbut
 */
@JavadocCapture
public class Parameters {

  private VertxOptions vertxOptions = new VertxOptions();
  private HttpServerOptions httpServerOptions = new HttpServerOptions();
  private boolean exitOnRun = false;
  private String baseConfigPath = "/var/query-engine";
  private Audit audit = Audit.builder().build();

  /**
   * The VertxOptions that will be used when creating the Vertx instance.
   * @return the VertxOptions that will be used when creating the Vertx instance.
   */
  public VertxOptions getVertxOptions() {
    return vertxOptions == null ? null : new VertxOptions(vertxOptions);
  }

  /**
   * The VertxOptions that will be used when creating the Vertx instance.
   * @param vertxOptions the VertxOptions that will be used when creating the Vertx instance.
   */
  public void setVertxOptions(VertxOptions vertxOptions) {
    this.vertxOptions = vertxOptions == null ? null : new VertxOptions(vertxOptions);
  }

  /**
   * The HttpServerOptions that will be used when creating the HTTP server.
   * The {@link io.vertx.core.http.HttpServerOptions#setMaxHeaderSize(int)} method should be particularly useful when running behind a proxy that passes large JSON headers.
   * @return the HttpServerOptions that will be used when creating the HTTP server.
   */
  public HttpServerOptions getHttpServerOptions() {
    return httpServerOptions == null ? null : new HttpServerOptions(httpServerOptions);
  }

  /**
   * The HttpServerOptions that will be used when creating the HTTP server.
   * @param httpServerOptions the HttpServerOptions that will be used when creating the HTTP server.
   */
  public void setHttpServerOptions(HttpServerOptions httpServerOptions) {
    this.httpServerOptions = httpServerOptions == null ? null : new HttpServerOptions(httpServerOptions);
  }  
  
  /**
   * if true the process will end rather than waiting for requests
   * This is expected to be useful for things such as JIT compilers or CDS preparation.
   * @return the exitOnRun value.
   */
  public boolean isExitOnRun() {
    return exitOnRun;
  }

  /**
   * if true the process will end rather than waiting for requests
   * This is expected to be useful for things such as JIT compilers or CDS preparation.
   * @param exitOnRun the exitOnRun value.
   */
  public void setExitOnRun(boolean exitOnRun) {
    this.exitOnRun = exitOnRun;
  }

  /**
   * The path to the root of the configuration files.
   * @return the path to the root of the configuration files.
   */
  public String getBaseConfigPath() {
    return baseConfigPath;
  }

  /**
   * The path to the root of the configuration files.
   * @param baseConfigPath the path to the root of the configuration files.
   */
  public void setBaseConfigPath(String baseConfigPath) {
    this.baseConfigPath = baseConfigPath;
  }

  /**
   * Configuration of the audit of requests.
   * @return configuration of the audit of requests.
   */
  public Audit getAudit() {
    return audit;
  }

  /**
   * Set configuration of the audit of requests.
   * @param audit Configuration of the audit of requests.
   */
  public void setAudit(Audit audit) {
    this.audit = audit;
  }
  
}
