/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import io.vertx.core.Future;

/**
 *
 * @author jtalbut
 */
public interface Initializable {
  
  /**
   * Take whatever steps are necessary to start reading or writing the streams.
   * 
   * @param executor The executor that can be used by the class to initialize (and run) any sub pipelines.
   * @param pipeline Definition of the pipeline, primarily for access to arguments and sourceEndpoints.
   * @return a Future that will be completed when the initialization is complete.
   */
  Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline);
  
}
