/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.procs.limit;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import java.util.Map;
import uk.co.spudsoft.query.main.defn.Endpoint;
import uk.co.spudsoft.query.main.defn.ProcessorLimit;
import uk.co.spudsoft.query.main.exec.ProcessorInstance;
import uk.co.spudsoft.query.main.exec.procs.PassthroughStream;

/**
 *
 * @author jtalbut
 */
public class ProcessorLimitInstance implements ProcessorInstance<ProcessorLimit> {
  
  private final ProcessorLimit definition;
  private final PassthroughStream<JsonObject> stream;
  
  private int count;

  public ProcessorLimitInstance(Vertx vertx, Context context, ProcessorLimit definition) {
    this.definition = definition;
    this.stream = new PassthroughStream<>(this::process, context, 100);
  }  
  
  private Future<JsonObject> process(JsonObject data) {
    if (++count <= definition.getLimit()) {
      return Future.succeededFuture(data);
    } else {
      return Future.succeededFuture();
    }
  }
  
  @Override
  public Future<Void> initialize(Map<String, Endpoint> endpoints) {
    return Future.succeededFuture();
  }

  @Override
  public ReadStream<JsonObject> getReadStream() {
    return stream.getReadStream();
  }

  @Override
  public WriteStream<JsonObject> getWriteStream() {
    return stream;    
  }  
  
}
