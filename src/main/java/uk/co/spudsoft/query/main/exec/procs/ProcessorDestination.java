/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.procs;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;
import uk.co.spudsoft.query.main.exec.DestinationInstance;
import uk.co.spudsoft.query.main.exec.PipelineExecutor;
import uk.co.spudsoft.query.main.exec.PipelineInstance;
import uk.co.spudsoft.query.main.exec.ProcessorInstance;

/**
 * A simple wrapper class so that a Processor may be used as a Destination in a child pipeline.
 * 
 * @author jtalbut
 */
public class ProcessorDestination implements DestinationInstance {

  private final ProcessorInstance processor;

  public ProcessorDestination(ProcessorInstance processor) {
    this.processor = processor;
  }
  
  /**
   * Do nothing, the Processor is assumed to have initialized itself in the call to {@link uk.co.spudsoft.query.main.exec.ProcessorInstance#initialize(uk.co.spudsoft.query.main.exec.PipelineExecutor, uk.co.spudsoft.query.main.exec.PipelineInstance) }.
   * @param executor
   * @param pipeline
   * @return 
   */
  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline) {
    return Future.succeededFuture();
  }

  @Override
  public WriteStream<JsonObject> getWriteStream() {
    return processor.getWriteStream();
  }
  
}
