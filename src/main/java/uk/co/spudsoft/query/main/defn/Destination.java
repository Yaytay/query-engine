/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import uk.co.spudsoft.query.main.exec.DestinationInstance;

/**
 * The configuration for the final WriteStream of a pipeline.
 * 
 * Typically the final WriteStream is the HttpResponse, but it can be something else entirely.
 * In the case of an Excel format request, for example, the WriteStream will write to the excel file that will then be returned separately.
 * 
 * @author jtalbut
 */
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME, 
  include = JsonTypeInfo.As.PROPERTY, 
  property = "type")
@JsonSubTypes({ 
  @JsonSubTypes.Type(value = DestinationLogger.class, name = "Logger"), 
})
public abstract class Destination {
  
  /**
   * The type of Destination being configured.
   * This will drive the class used to handle the output data.
   */
  private final DestinationType type;

  /**
   * Create a DestinationInstance.
   * Each implementation of a DestinationInstance should subclass this class and provide a concrete implementation of this method.
   * Note that the context passed in to this method may not be the same as that returned by vertx.getOrCreateContext.
   * @param vertx The Vertx instance that will be used for the data processing.
   * @param context The Vertx context that will be used for this destination.
   * @return a newly created DestinationInstance object that will be used for processing the pipeline destination.
   */
  @JsonIgnore
  public abstract DestinationInstance<? extends Destination> createInstance(Vertx vertx, Context context);
  
  /**
   * Get the type of Destination being configured.
   * @return the type of Destination being configured.
   */
  public DestinationType getType() {
    return type;
  }

  /**
   * Set the type of Destination being configured.
   * @param type the type of Destination being configured.
   */
  protected Destination(DestinationType type) {
    this.type = type;
  }
  
}
