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
 * A DestinationInstance is created from a Destination (based on DestinationType).
 * 
 * @author jtalbut
 * @param <T> The type of destination configuration used to create this DestinationInstance.
 */
public interface DestinationInstance<T extends Destination> {
  
  /**
   * Return the write stream that the data will be passed to.
   * @return the write stream that the data will be passed to.
   */
  WriteStream<JsonObject> getWriteStream();
  
}
