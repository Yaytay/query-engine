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
public interface Destination {
  
  /**
   * Create a DestinationInstance.
   * Each implementation of a DestinationInstance should subclass this class and provide a concrete implementation of this method.
   * Note that the context passed in to this method may not be the same as that returned by vertx.getOrCreateContext.
   * @param vertx The Vertx instance that will be used for the data processing.
   * @param context The Vertx context that will be used for this destination.
   * @return a newly created DestinationInstance object that will be used for processing the pipeline destination.
   */
  @JsonIgnore
  DestinationInstance createInstance(Vertx vertx, Context context);
  
  /**
   * Get the type of Destination being configured.
   * @return the type of Destination being configured.
   */
  DestinationType getType();

  default void validateType(DestinationType required, DestinationType actual) {
    if (required != actual) {
      throw new IllegalArgumentException("Processor of type " + required + " configured with type " + actual);
    }
  }
  
}
