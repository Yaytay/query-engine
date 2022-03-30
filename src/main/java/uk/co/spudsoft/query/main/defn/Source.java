/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import uk.co.spudsoft.query.main.exec.SourceInstance;

/**
 *
 * @author jtalbut
 */
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME, 
  include = JsonTypeInfo.As.PROPERTY, 
  property = "type")
@JsonSubTypes({ 
  @Type(value = SourceSql.class, name = "SQL"), 
  @Type(value = SourceTest.class, name = "Test") 
})
public interface Source {
  
  @JsonIgnore
  SourceInstance createInstance(Vertx vertx, Context context);
  
  SourceType getType();
  
  default void validateType(SourceType required, SourceType actual) {
    if (required != actual) {
      throw new IllegalArgumentException("Source of type " + required + " configured with type " + actual);
    }
  }
  
}
