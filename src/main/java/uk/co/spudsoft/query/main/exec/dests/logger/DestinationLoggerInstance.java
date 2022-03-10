/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.dests.logger;

import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.main.defn.DestinationLogger;
import uk.co.spudsoft.query.main.exec.DestinationInstance;

/**
 *
 * @author jtalbut
 */
public class DestinationLoggerInstance implements DestinationInstance<DestinationLogger> {
 
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(DestinationLoggerInstance.class);

  public DestinationLoggerInstance() {
  }

  @Override
  public WriteStream<JsonObject> getWriteStream() {
    return new LoggingWriteStream<>();
  }
  
}
