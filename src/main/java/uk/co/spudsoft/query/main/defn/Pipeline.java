/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jtalbut
 */
@JsonDeserialize(builder = Pipeline.Builder.class)
public class Pipeline {
  
  private final ImmutableMap<String, Endpoint> sourceEndpoints;
  
  // private List<SourceGenerator> sourceGenerators;  
  private final Source source;
  // private final List<Processor> processors;
  private final Destination destination;

  public void validate() {    
  }
  
  public Map<String, Endpoint> getSourceEndpoints() {
    return sourceEndpoints;
  }

  public Source getSource() {
    return source;
  }

  public Destination getDestination() {
    return destination;
  }

  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Builder class should result in all instances being immutable when object is built")
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private Map<String, Endpoint> sourceEndpoints;
    private Source source;
    private List<Processor> processors;
    private Destination destination;

    private Builder() {
    }

    public Builder sourceEndpoints(final Map<String, Endpoint> value) {
      this.sourceEndpoints = value;
      return this;
    }

    public Builder source(final Source value) {
      this.source = value;
      return this;
    }

    public Builder processors(final List<Processor> value) {
      this.processors = value;
      return this;
    }

    public Builder destination(final Destination value) {
      this.destination = value;
      return this;
    }

    public Pipeline build() {
      return new Pipeline(sourceEndpoints, source, processors, destination);
    }
  }

  public static Pipeline.Builder builder() {
    return new Pipeline.Builder();
  }

  private Pipeline(final Map<String, Endpoint> sourceEndpoints, final Source source, final List<Processor> processors, final Destination destination) {
    this.sourceEndpoints = sourceEndpoints == null ? null : ImmutableMap.copyOf(sourceEndpoints);
    this.source = source;
    // this.processors = processors == null ? null : ImmutableList.copyOf(processors);
    this.destination = destination;
  }

  
  
}
