/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;

/**
 *
 * @author jtalbut
 */
public interface Readable {
  
  /**
   * Return the read stream that will output the data.
   * @return the write stream that will output the data.
   */
  ReadStream<JsonObject> getReadStream();
  
}
