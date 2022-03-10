/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;
import uk.co.spudsoft.query.main.defn.Destination;

/**
 *
 * @author jtalbut
 * @param <T>
 */
public interface DestinationInstance<T extends Destination> {
  
  WriteStream<JsonObject> getWriteStream();
  
}
