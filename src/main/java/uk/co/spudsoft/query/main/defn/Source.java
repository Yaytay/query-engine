/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import uk.co.spudsoft.query.main.exec.SourceInstance;
import uk.co.spudsoft.query.main.exec.SourceInstanceFactory;

/**
 *
 * @author jtalbut
 */
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME, 
  include = JsonTypeInfo.As.PROPERTY, 
  property = "type")
@JsonSubTypes({ 
  @Type(value = SourceHttp.class, name = "HTTP"), 
  @Type(value = SourceSql.class, name = "SQL"), 
  @Type(value = SourceTest.class, name = "Test") 
})
public abstract class Source {
  
  private final SourceType type;
  private final String endpoint;
  private final String query;

  @JsonIgnore
  public abstract SourceInstanceFactory<? extends Source, ? extends SourceInstance> getFactory();
  
  public SourceType getType() {
    return type;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public String getQuery() {
    return query;
  }

  protected Source(final SourceType type, final String endpoint, final String query) {
    this.type = type;
    this.endpoint = endpoint;
    this.query = query;
  }
  
}
