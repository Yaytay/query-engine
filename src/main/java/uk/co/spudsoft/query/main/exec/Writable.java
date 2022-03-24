/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;

/**
 *
 * @author jtalbut
 */
public interface Writable {
  
  /**
   * Return the write stream that the data will be passed to.
   * @return the write stream that the data will be passed to.
   */
  WriteStream<JsonObject> getWriteStream();
  
}
