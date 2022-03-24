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
 * The Pipeline is the fundamental unit of processing in QueryEngine.
 * A single Pipeline takes data from a single {@link Source}, passes it through any number of {@link Processor}s and finally delivers it to a {@link Destination}.
 * The {@link Processor}s themselves may pull in data from other {@link Source}s.
 * @author jtalbut
 */
@JsonDeserialize(builder = Pipeline.Builder.class)
public class Pipeline extends SourcePipeline {
  
  private final ImmutableMap<String, Argument> arguments;
  private final ImmutableMap<String, Endpoint> sourceEndpoints;
  
  private final Destination destination;  

  @Override
  public void validate() {    
  }

  public Map<String, Argument> getArguments() {
    return arguments;
  }

  public Map<String, Endpoint> getSourceEndpoints() {
    return sourceEndpoints;
  }
  
  public Destination getDestination() {
    return destination;
  }

  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Builder class should result in all instances being immutable when object is built")
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends SourcePipeline.Builder<Pipeline.Builder> {

    protected Map<String, Argument> arguments;
    protected Map<String, Endpoint> sourceEndpoints;
    private Destination destination;

    private Builder() {
    }

    public Builder arguments(final Map<String, Argument> value) {
      this.arguments = value;
      return self();
    }

    public Builder sourceEndpoints(final Map<String, Endpoint> value) {
      this.sourceEndpoints = value;
      return self();
    }

    public Builder destination(final Destination value) {
      this.destination = value;
      return this;
    }

    @Override
    public Pipeline build() {
      return new Pipeline(arguments, sourceEndpoints, source, processors, destination);
    }
  }

  public static Pipeline.Builder builder() {
    return new Pipeline.Builder();
  }

  private Pipeline(Map<String, Argument> arguments, Map<String, Endpoint> sourceEndpoints, Source source, List<Processor> processors, Destination destination) {
    super(source, processors);
    this.arguments = arguments == null ? ImmutableMap.of() : ImmutableMap.copyOf(arguments);
    this.sourceEndpoints = sourceEndpoints == null ? ImmutableMap.of() : ImmutableMap.copyOf(sourceEndpoints);
    this.destination = destination;
  }  
  
}
