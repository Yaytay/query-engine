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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import uk.co.spudsoft.query.main.defn.Argument;
import uk.co.spudsoft.query.main.defn.Pipeline;
import uk.co.spudsoft.query.main.defn.Processor;
import uk.co.spudsoft.query.main.defn.SourcePipeline;

/**
 *
 * @author jtalbut
 */
public class PipelineExecutorImpl implements PipelineExecutor {
  
  @Override
  public Future<Void> validatePipeline(Pipeline definition) {
    return Future.succeededFuture();
  }
    
  @Override
  public List<ProcessorInstance> createProcessors(Vertx vertx, Context context, SourcePipeline definition) {
    List<ProcessorInstance> result = new ArrayList<>();
    for (Processor processor : definition.getProcessors()) {
      result.add(processor.createInstance(vertx, context));
    }
    return result;
  }

  @Override
  public Map<String, ArgumentInstance> prepareArguments(Map<String, Argument> definitions, Map<String, String> values) {
    Map<String, ArgumentInstance> result = new HashMap<>();
    for (Entry<String, Argument> entry : definitions.entrySet()) {
      String value = values.containsKey(entry.getKey()) ? values.get(entry.getKey()) : entry.getValue().getDefaultValue();
      ArgumentInstance instance = new ArgumentInstance(entry.getValue(), value);
      result.put(entry.getKey(), instance);
    }
    return result;
  }  
  
  private Future<Void> initializeProcessors(PipelineInstance pipeline, Iterator<ProcessorInstance> iter) {
    if (!iter.hasNext()) {
      return Future.succeededFuture();
    } else {
      return iter.next()
              .initialize(this, pipeline)
              .compose(v -> initializeProcessors(pipeline, iter));
    }
  }
  
  private Future<Void> buildPipes(ReadStream<JsonObject> previousReadStream, Iterator<ProcessorInstance> iter, WriteStream<JsonObject> finalWriteStream) {
    if (iter.hasNext()) {
      ProcessorInstance processor = iter.next();
      previousReadStream.pipeTo(processor.getWriteStream());
      return buildPipes(processor.getReadStream(), iter, finalWriteStream);
    } else {
      return previousReadStream.pipeTo(finalWriteStream);
    }
  }
  
  @Override
  public Future<Void> initializePipeline(PipelineInstance pipeline) {                
    return pipeline.getSource().initialize(this, pipeline)
            .compose(v -> initializeProcessors(pipeline, pipeline.getProcessors().iterator()))
            .compose(v -> pipeline.getSink().initialize(this, pipeline))
            .compose(v -> buildPipes(pipeline.getSource().getReadStream(), pipeline.getProcessors().iterator(), pipeline.getSink().getWriteStream()))
            ;
  }
  
}
