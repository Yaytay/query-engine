/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;

/**
 *
 * @author jtalbut
 */
public interface PipelineProcessor {
  
  ReadStream<JsonObject> getReadStream();
  WriteStream<JsonObject> getWriteStream();
  
}
