/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.dests.logger;

import io.vertx.core.Context;
import uk.co.spudsoft.query.main.defn.Destination;
import uk.co.spudsoft.query.main.exec.DestinationInstance;
import uk.co.spudsoft.query.main.exec.DestinationInstanceFactory;

/**
 *
 * @author jtalbut
 */
public class SinkLoggerFactory implements DestinationInstanceFactory {

  @Override
  public DestinationInstance create(Context context, Destination definition) {
    return new DestinationLogger();
  }
  
}
