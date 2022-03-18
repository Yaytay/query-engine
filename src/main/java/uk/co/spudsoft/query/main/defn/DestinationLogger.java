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
 * A Destination class for the DestinationLoggerInstance.
 * This Destination does nothing except log the data received.
 * @author jtalbut
 */
@JsonDeserialize(builder = DestinationLogger.Builder.class)
public class DestinationLogger extends Destination {
  
  @Override
  public DestinationInstance<? extends Destination> createInstance(Vertx vertx, Context context) {
    return new DestinationLoggerInstance();
  }

  /**
   * Builder class for DestinationLogger.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private DestinationType type = DestinationType.Logger;

    private Builder() {
    }

    /**
     * Set the type of the Destination.
     * @param value must be DestinationType.Logger.
     * @return this, so that the builder may be used fluently.
     */
    public Builder type(final DestinationType value) {
      if (value != DestinationType.Logger) {
        throw new IllegalArgumentException("Can only be DestinationType.Logger");
      }
      this.type = value;
      return this;
    }

    /**
     * Construct a new instance of the DestinationLogger class.
     * @return a new instance of the DestinationLogger class.
     */
    public DestinationLogger build() {
      return new DestinationLogger(type);
    }
  }

  /**
   * Construct a new instance of the DestinationLogger.Builder class.
   * @return a new instance of the DestinationLogger.Builder class.
   */
  public static Builder builder() {
    return new Builder();
  }

  private DestinationLogger(final DestinationType type) {
    super(type);
  }
    
}
