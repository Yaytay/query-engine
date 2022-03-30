/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import uk.co.spudsoft.query.main.exec.ProcessorInstance;
import uk.co.spudsoft.query.main.exec.procs.script.ProcessorScriptInstance;

/**
 *
 * @author jtalbut
 */
@JsonDeserialize(builder = ProcessorLimit.Builder.class)
public class ProcessorScript implements Processor {
  
  private final ProcessorType type;
  private final String language;
  private final String predicate;
  private final String process;

  @Override
  public ProcessorInstance createInstance(Vertx vertx, Context context) {
    return new ProcessorScriptInstance(vertx, vertx.getOrCreateContext(), this);
  }
  
  @Override
  public ProcessorType getType() {
    return type;
  }

  public String getLanguage() {
    return language;
  }

  public String getPredicate() {
    return predicate;
  }

  public String getProcess() {
    return process;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private ProcessorType type = ProcessorType.SCRIPT;
    private String language;
    private String predicate;
    private String process;

    private Builder() {
    }

    public Builder type(final ProcessorType value) {
      this.type = value;
      return this;
    }

    public Builder language(final String value) {
      this.language = value;
      return this;
    }

    public Builder predicate(final String value) {
      this.predicate = value;
      return this;
    }

    public Builder process(final String value) {
      this.process = value;
      return this;
    }

    public ProcessorScript build() {
      return new uk.co.spudsoft.query.main.defn.ProcessorScript(type, language, predicate, process);
    }
  }

  public static ProcessorScript.Builder builder() {
    return new ProcessorScript.Builder();
  }

  private ProcessorScript(final ProcessorType type, final String language, final String predicate, final String process) {
    validateType(ProcessorType.SCRIPT, type);
    this.type = type;
    this.language = language;
    this.predicate = predicate;
    this.process = process;
  }
  
  
  
  
}
