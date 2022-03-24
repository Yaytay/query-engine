/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import uk.co.spudsoft.query.main.defn.Endpoint;

/**
 *
 * @author jtalbut
 */
public class PipelineInstance {
  
  private final ImmutableMap<String, ArgumentInstance> arguments;
  private final ImmutableMap<String, Endpoint> sourceEndpoints;
  
  // private List<SourceGenerator> sourceGenerators;
  
  private final SourceInstance source;
  private final ImmutableList<ProcessorInstance> processors;
  private final DestinationInstance sink;

  public PipelineInstance(
          final Map<String, ArgumentInstance> arguments
          , final Map<String, Endpoint> sourceEndpoints
          , final SourceInstance source
          , final List<ProcessorInstance> processors
          , final DestinationInstance sink
  ) {
    this.arguments = arguments == null ? ImmutableMap.of() : ImmutableMap.copyOf(arguments);
    this.sourceEndpoints = sourceEndpoints == null ? ImmutableMap.of() : ImmutableMap.copyOf(sourceEndpoints);
    this.source = source;
    this.processors = processors == null ? ImmutableList.of() : ImmutableList.copyOf(processors);
    this.sink = sink;
  }
  
  public ImmutableMap<String, ArgumentInstance> getArguments() {
    return arguments;
  }
  
  public Map<String, Endpoint> getSourceEndpoints() {
    return sourceEndpoints;
  }

  public SourceInstance getSource() {
    return source;
  }

  public List<ProcessorInstance> getProcessors() {
    return processors;
  }

  public DestinationInstance getSink() {
    return sink;
  }

}
