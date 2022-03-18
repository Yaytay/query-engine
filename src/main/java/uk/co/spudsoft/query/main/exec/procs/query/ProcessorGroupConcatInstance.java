/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.procs.query;

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
import uk.co.spudsoft.query.main.defn.ProcessorGroupConcat;
import uk.co.spudsoft.query.main.exec.ProcessorInstance;
import uk.co.spudsoft.query.main.exec.procs.PassthroughStream;

/**
 *
 * @author jtalbut
 */
public class ProcessorGroupConcatInstance implements ProcessorInstance<ProcessorGroupConcat> {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ProcessorGroupConcatInstance.class);
  
  private final ProcessorGroupConcat definition;
  private final PassthroughStream<JsonObject> stream;

  public ProcessorGroupConcatInstance(Vertx vertx, Context context, ProcessorGroupConcat definition) {
    this.definition = definition;
    this.stream = new PassthroughStream<>(this::passthroughStreamProcessor, context);
  }
  
  private Future<Void> passthroughStreamProcessor(JsonObject data, PassthroughStream.AsyncProcessor<JsonObject> chain) {
    try {
      logger.info("process {}", data);
      return chain.handle(data);
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
