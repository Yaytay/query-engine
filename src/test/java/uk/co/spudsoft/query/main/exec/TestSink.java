/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jtalbut
 */
public class TestSink implements QuerySink {
  
  private static final Logger logger = LoggerFactory.getLogger(TestSink.class);

  private final LoggingWriteStream<JsonObject> loggingStream = new LoggingWriteStream<>();
  
  @Override
  public WriteStream<JsonObject> getWriteStream() {
    return loggingStream;
  }

}
