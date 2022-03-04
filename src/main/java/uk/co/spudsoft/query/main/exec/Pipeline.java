/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import java.util.List;
import java.util.Map;
import uk.co.spudsoft.query.main.defn.Endpoint;

/**
 *
 * @author jtalbut
 */
public class Pipeline {
  
  private Map<String, Endpoint> sourceEndpoints;
  
  // private List<SourceGenerator> sourceGenerators;
  
  private QuerySource source;
  private List<PipelineProcessor> processors;
  private QuerySink sink;

  public Map<String, Endpoint> getSourceEndpoints() {
    return sourceEndpoints;
  }

  public QuerySource getSource() {
    return source;
  }

  public List<PipelineProcessor> getProcessors() {
    return processors;
  }

  public QuerySink getSink() {
    return sink;
  }

  public static class Builder {

    private Map<String,Endpoint> sourceEndpoints;
    private QuerySource source;
    private List<PipelineProcessor> processors;
    private QuerySink sink;

    private Builder() {
    }

    public Builder sourceEndpoints(final Map<String,Endpoint> value) {
      this.sourceEndpoints = value;
      return this;
    }

    public Builder source(final QuerySource value) {
      this.source = value;
      return this;
    }

    public Builder processors(final List<PipelineProcessor> value) {
      this.processors = value;
      return this;
    }

    public Builder sink(final QuerySink value) {
      this.sink = value;
      return this;
    }

    public Pipeline build() {
      return new uk.co.spudsoft.query.main.exec.Pipeline(sourceEndpoints, source, processors, sink);
    }
  }

  public static Pipeline.Builder builder() {
    return new Pipeline.Builder();
  }

  private Pipeline(final Map<String, Endpoint> sourceEndpoints, final QuerySource source, final List<PipelineProcessor> processors, final QuerySink sink) {
    this.sourceEndpoints = sourceEndpoints;
    this.source = source;
    this.processors = processors;
    this.sink = sink;
  }

}
