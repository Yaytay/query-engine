/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import java.util.Map;
import uk.co.spudsoft.query.main.defn.Endpoint;
import uk.co.spudsoft.query.main.defn.Source;

/**
 * A SourceInstance is simply a stream of JsonObjects (rows).
 * 
 * @author jtalbut
 */
public interface SourceInstance<T extends Source> {
  
  Future<Void> initialize(Map<String, Endpoint> endpoints);
  ReadStream<JsonObject> getReadStream();
  
}
