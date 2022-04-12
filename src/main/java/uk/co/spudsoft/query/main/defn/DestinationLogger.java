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
public class DestinationLogger implements Destination {

  private final DestinationType type;
  
  @Override
  public DestinationInstance createInstance(Vertx vertx, Context context) {
    return new DestinationLoggerInstance();
  }

  @Override
  public DestinationType getType() {
    return type;
  }
  
  /**
   * Builder class for DestinationLogger.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private DestinationType type = DestinationType.LOGGER;

    private Builder() {
    }

    /**
     * Set the type of the Destination.
     * @param value must be DestinationType.LOGGER.
     * @return this, so that the builder may be used fluently.
     */
    public Builder type(final DestinationType value) {
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
    validateType(DestinationType.LOGGER, type);
    this.type = type;
  }
    
}