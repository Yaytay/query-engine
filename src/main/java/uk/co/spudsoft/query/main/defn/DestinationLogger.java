/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import uk.co.spudsoft.query.main.exec.dests.logger.SinkLoggerFactory;
import uk.co.spudsoft.query.main.exec.DestinationInstanceFactory;

/**
 *
 * @author jtalbut
 */
@JsonDeserialize(builder = DestinationLogger.Builder.class)
public class DestinationLogger extends Destination {

  private static final SinkLoggerFactory FACTORY = new SinkLoggerFactory();
  
  @Override
  public DestinationInstanceFactory getFactory() {
    return FACTORY;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private DestinationType type;

    private Builder() {
    }

    public Builder type(final DestinationType value) {
      this.type = value;
      return this;
    }

    public DestinationLogger build() {
      return new DestinationLogger(type);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private DestinationLogger(final DestinationType type) {
    super(type);
  }
    
}
