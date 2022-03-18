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
import uk.co.spudsoft.query.main.exec.ProcessorInstance;

/**
 *
 * @author jtalbut
 */
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME, 
  include = JsonTypeInfo.As.PROPERTY, 
  property = "type")
@JsonSubTypes({ 
  @JsonSubTypes.Type(value = ProcessorLimit.class, name = "LIMIT") 
  , @JsonSubTypes.Type(value = ProcessorGroupConcat.class, name = "GROUP_CONCAT") 
})
public abstract class Processor {
  
  private final ProcessorType type;

  @JsonIgnore
  public abstract ProcessorInstance<? extends Processor> createInstance(Vertx vertx, Context context);
  
  public ProcessorType getType() {
    return type;
  }

  protected Processor(final ProcessorType type) {
    this.type = type;
  }
  
  
}
