/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import io.vertx.core.Future;

/**
 *
 * @author jtalbut
 */
public class PipelineExecutor {
  
  public Future<Void> executePipeline(Pipeline pipeline) {
            
    return pipeline.getSource().initialize(pipeline.getSourceEndpoints())
            .compose(v -> pipeline.getSource().getReadStream().pipeTo(pipeline.getSink().getWriteStream()));
  }
  
}
