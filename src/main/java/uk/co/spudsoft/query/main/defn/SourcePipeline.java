/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 *
 * @author jtalbut
 */
@JsonDeserialize(builder = SourcePipeline.Builder.class)
public class SourcePipeline {
  
  // private List<SourceGenerator> sourceGenerators;  
  private final Source source;
  private final ImmutableList<Processor> processors;

  public void validate() {    
  }

  public Source getSource() {
    return source;
  }

  public List<Processor> getProcessors() {
    return processors;
  }

  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Builder class should result in all instances being immutable when object is built")
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder<T extends Builder<T>> {

    protected Source source;
    protected List<Processor> processors;

    protected Builder() {
    }

    @SuppressWarnings("unchecked")
    final T self() {
        return (T) this;
    }
    
    public T source(final Source value) {
      this.source = value;
      return self();
    }

    public T processors(final List<Processor> value) {
      this.processors = value;
      return self();
    }

    public SourcePipeline build() {
      return new SourcePipeline(source, processors);
    }
  }

  public static SourcePipeline.Builder<?> builder() {
    return new SourcePipeline.Builder<>();
  }

  protected SourcePipeline(Source source, List<Processor> processors) {
    this.source = source;
    this.processors = processors == null ? ImmutableList.of() : ImmutableList.copyOf(processors);
  }    
}
