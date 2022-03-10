/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import uk.co.spudsoft.query.main.defn.Endpoint;

/**
 *
 * @author jtalbut
 */
public class PipelineInstance {
  
  private final ImmutableMap<String, Endpoint> sourceEndpoints;
  
  // private List<SourceGenerator> sourceGenerators;
  
  private final SourceInstance<?> source;
  private final ImmutableList<ProcessorInstance<?>> processors;
  private final DestinationInstance<?> sink;

  public Map<String, Endpoint> getSourceEndpoints() {
    return sourceEndpoints;
  }

  public SourceInstance<?> getSource() {
    return source;
  }

  public List<ProcessorInstance<?>> getProcessors() {
    return processors;
  }

  public DestinationInstance<?> getSink() {
    return sink;
  }

  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Builder class should result in all instances being immutable when object is built")
  public static class Builder {

    private Map<String, Endpoint> sourceEndpoints;
    private SourceInstance<?> source;
    private List<ProcessorInstance<?>> processors;
    private DestinationInstance<?> sink;

    private Builder() {
    }

    public Builder sourceEndpoints(final Map<String, Endpoint> value) {
      this.sourceEndpoints = value;
      return this;
    }

    public Builder source(final SourceInstance<?> value) {
      this.source = value;
      return this;
    }

    public Builder processors(final List<ProcessorInstance<?>> value) {
      this.processors = value;
      return this;
    }

    public Builder sink(final DestinationInstance<?> value) {
      this.sink = value;
      return this;
    }

    public PipelineInstance build() {
      return new uk.co.spudsoft.query.main.exec.PipelineInstance(sourceEndpoints, source, processors, sink);
    }
  }

  public static PipelineInstance.Builder builder() {
    return new PipelineInstance.Builder();
  }

  private PipelineInstance(final Map<String, Endpoint> sourceEndpoints
          , final SourceInstance<?> source
          , final List<ProcessorInstance<?>> processors
          , final DestinationInstance<?> sink
  ) {
    this.sourceEndpoints = sourceEndpoints == null ? ImmutableMap.of() : ImmutableMap.copyOf(sourceEndpoints);
    this.source = source;
    this.processors = processors == null ? ImmutableList.of() : ImmutableList.copyOf(processors);
    this.sink = sink;
  }

}
