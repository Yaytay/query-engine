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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.main.defn.Endpoint;
import uk.co.spudsoft.query.main.defn.ProcessorLimit;
import uk.co.spudsoft.query.main.exec.ProcessorInstance;
import uk.co.spudsoft.query.main.exec.procs.PassthroughStream2;

/**
 *
 * @author jtalbut
 */
public class ProcessorLimitInstance implements ProcessorInstance<ProcessorLimit> {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ProcessorLimitInstance.class);
  
  private final ProcessorLimit definition;
  private final PassthroughStream2<JsonObject> stream;
  
  private int count;

  public ProcessorLimitInstance(Vertx vertx, Context context, ProcessorLimit definition) {
    this.definition = definition;
    this.stream = new PassthroughStream2<>(this::process, context);
  }  
  
  private Future<Void> process(JsonObject data, PassthroughStream2.AsyncProcessor<JsonObject> chain) {
    try {
      logger.info("process {}", data);
      //if (++count <= definition.getLimit()) {
        return chain.handle(data);
      //} else {
      //return Future.succeededFuture();
      //}
    } catch(Throwable ex) {
      logger.error("Failed to process {}: ", data, ex);
      return Future.failedFuture(ex);
    }
  }
  
  @Override
  public Future<Void> initialize(Map<String, Endpoint> endpoints) {
    return Future.succeededFuture();
  }

  @Override
  public ReadStream<JsonObject> getReadStream() {
    return stream.readStream();
  }

  @Override
  public WriteStream<JsonObject> getWriteStream() {
    return stream.writeStream();
  }  
  
}
