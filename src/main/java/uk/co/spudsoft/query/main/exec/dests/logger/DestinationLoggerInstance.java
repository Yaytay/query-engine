/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.dests.logger;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;
import uk.co.spudsoft.query.main.exec.DestinationInstance;
import uk.co.spudsoft.query.main.exec.PipelineExecutor;
import uk.co.spudsoft.query.main.exec.PipelineInstance;

/**
 *
 * @author jtalbut
 */
public class DestinationLoggerInstance implements DestinationInstance {
 
  private final LoggingWriteStream<JsonObject> stream = new LoggingWriteStream<>();
  
  /**
   * Constructor.
   */
  public DestinationLoggerInstance() {
  }

  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline) {
    return Future.succeededFuture();
  }
  
  @Override
  public WriteStream<JsonObject> getWriteStream() {
    return stream;
  }
  
}
