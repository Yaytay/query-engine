/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;

/**
 * A QueryProcessor is simply a stream of JsonObjects (rows).
 * 
 * @author jtalbut
 */
public interface QueryProcessor extends ReadStream<JsonObject> {
  
}
