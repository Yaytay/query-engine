/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import uk.co.spudsoft.query.main.defn.Pipeline;
import uk.co.spudsoft.query.main.defn.Processor;

/**
 *
 * @author jtalbut
 */
public class PipelineExecutor {
  
  public Future<Void> validatePipeline(Pipeline definition) {
    return Future.succeededFuture();
  }
  
  private List<ProcessorInstance<?>> createProcessors(Vertx vertx, Context context, Pipeline definition) {
    List<ProcessorInstance<?>> result = new ArrayList<>();
    for (Processor processor : definition.getProcessors()) {
      result.add(processor.createInstance(vertx, context));
    }
    return result;
  }
  
  public PipelineInstance buildPipelineFromDefinition(Vertx vertx, Context context, Pipeline definition) {
    return PipelineInstance.builder()
            .sourceEndpoints(definition.getSourceEndpoints())
            .source(definition.getSource().createInstance(vertx, context))
            .processors(createProcessors(vertx, context, definition))
            .sink(definition.getDestination().createInstance(vertx, context))
            .build();
  }
  
  public Future<Void> initializeProcessor(PipelineInstance pipeline, Iterator<ProcessorInstance<?>> iter) {
    if (!iter.hasNext()) {
      return Future.succeededFuture();
    } else {
      return iter.next()
              .initialize(pipeline.getSourceEndpoints())
              .compose(v -> initializeProcessor(pipeline, iter));
    }
  }
  
  public Future<Void> buildPipes(ReadStream<JsonObject> previousReadStream, Iterator<ProcessorInstance<?>> iter, WriteStream<JsonObject> finalWriteStream) {
    if (iter.hasNext()) {
      ProcessorInstance<?> processor = iter.next();
      previousReadStream.pipeTo(processor.getWriteStream());
      return buildPipes(processor.getReadStream(), iter, finalWriteStream);
    } else {
      return previousReadStream.pipeTo(finalWriteStream);
    }
  }
  
  public Future<Void> executePipeline(PipelineInstance pipeline) {                
    return pipeline.getSource().initialize(pipeline.getSourceEndpoints())
            .compose(v -> initializeProcessor(pipeline, pipeline.getProcessors().iterator()))
            .compose(v -> buildPipes(pipeline.getSource().getReadStream(), pipeline.getProcessors().iterator(), pipeline.getSink().getWriteStream()))
            ;
  }
  
}
