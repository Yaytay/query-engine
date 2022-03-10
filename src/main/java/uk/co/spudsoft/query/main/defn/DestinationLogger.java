/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import uk.co.spudsoft.query.main.exec.DestinationInstance;
import uk.co.spudsoft.query.main.exec.dests.logger.DestinationLoggerInstance;

/**
 *
 * @author jtalbut
 */
@JsonDeserialize(builder = DestinationLogger.Builder.class)
public class DestinationLogger extends Destination {

  @Override
  public DestinationInstance<? extends Destination> createInstance(Vertx vertx, Context context) {
    return new DestinationLoggerInstance();
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private DestinationType type = DestinationType.Logger;

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
