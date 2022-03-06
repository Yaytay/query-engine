/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import io.vertx.core.Context;
import uk.co.spudsoft.query.main.defn.Destination;

/**
 *
 * @author jtalbut
 */
public interface DestinationInstanceFactory {
  
  DestinationInstance create(Context context, Destination definition);
  
}
