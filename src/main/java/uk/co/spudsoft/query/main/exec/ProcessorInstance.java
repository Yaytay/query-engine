/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import java.util.Map;
import uk.co.spudsoft.query.main.defn.Endpoint;
import uk.co.spudsoft.query.main.defn.Processor;

/**
 *
 * @author jtalbut
 * @param <T> The type of processor configuration used to create this ProcessorInstance.
 */
public interface ProcessorInstance<T extends Processor> {
  
  
  Future<Void> initialize(Map<String, Endpoint> endpoints);
  ReadStream<JsonObject> getReadStream();
  WriteStream<JsonObject> getWriteStream();
  
  
}
