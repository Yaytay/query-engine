/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import java.util.List;
import java.util.Map;
import uk.co.spudsoft.query.main.defn.Argument;
import uk.co.spudsoft.query.main.defn.Pipeline;
import uk.co.spudsoft.query.main.defn.SourcePipeline;

/**
 *
 * @author jtalbut
 */
public interface PipelineExecutor {

  Map<String, ArgumentInstance> prepareArguments(Map<String, Argument> definitions, Map<String, String> values);
  
  Future<Void> validatePipeline(Pipeline definition);
  
  List<ProcessorInstance> createProcessors(Vertx vertx, Context context, SourcePipeline definition);
  
  Future<Void> initializePipeline(PipelineInstance pipeline);

}
