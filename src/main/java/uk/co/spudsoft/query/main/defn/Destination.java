/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import uk.co.spudsoft.query.main.exec.DestinationInstanceFactory;

/**
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
  
  private final DestinationType type;

  @JsonIgnore
  public abstract DestinationInstanceFactory getFactory();
  
  public DestinationType getType() {
    return type;
  }

  protected Destination(DestinationType type) {
    this.type = type;
  }
  
}
