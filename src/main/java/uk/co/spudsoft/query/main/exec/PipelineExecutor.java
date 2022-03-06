/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import io.vertx.core.Context;
import io.vertx.core.Future;
import uk.co.spudsoft.query.main.defn.Pipeline;

/**
 *
 * @author jtalbut
 */
public class PipelineExecutor {
  
  public Future<Void> validatePipeline(Pipeline definition) {
    return Future.succeededFuture();
  }
  
  public PipelineInstance buildPipelineFromDefinition(Context context, Pipeline definition) {
    return PipelineInstance.builder()
            .sourceEndpoints(definition.getSourceEndpoints())
            .source(definition.getSource().getFactory().create(context, definition.getSource()))
            .sink(definition.getDestination().getFactory().create(context, definition.getDestination()))
            .build();
  }
  
  public Future<Void> executePipeline(PipelineInstance pipeline) {
            
    return pipeline.getSource().initialize(pipeline.getSourceEndpoints())
            .compose(v -> pipeline.getSource().getReadStream().pipeTo(pipeline.getSink().getWriteStream()));
  }
  
}
