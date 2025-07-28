/*
 * Copyright (C) 2022 jtalbut
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.co.spudsoft.query.defn;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.procs.script.ProcessorScriptInstance;

/**
 * Processor that runs a custom script on each row of the output.
 * <P>
 * The difference between the ProcessorScript and the {@link ProcessorExpression} is that the evaluation of JEXL expressions is a lot faster that the evaluation of JavaScript,
 * but there are fewer things that can be done in a single JEXL expression.
 * In general, prefer {@link ProcessorExpression} unless it is unable to achieve what you want.
 * 
 * @author jtalbut
 */
@JsonDeserialize(builder = ProcessorScript.Builder.class)
@Schema(description = """
                      Run a custom script on each row of the output.
                      """
)
public class ProcessorScript implements Processor {
  
  private final ProcessorType type;
  private final Condition condition;
  private final String name;
  private final String language;
  private final String predicate;
  private final String process;

  @Override
  public ProcessorScriptInstance createInstance(Vertx vertx, SourceNameTracker sourceNameTracker, Context context, MeterRegistry meterRegistry, String name) {
    return new ProcessorScriptInstance(vertx, sourceNameTracker, context, this, name);
  }
  
  @Override
  public void validate() {
    validateType(ProcessorType.SCRIPT, type);
  }
  
  @Override
  public ProcessorType getType() {
    return type;
  }

  @Override
  public Condition getCondition() {
    return condition;
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * The language to use, as understood by GraalVM.
   * <P>
   * By default the only acceptable value is "js", but custom builds can use other lanaguages.
   * @return The language to use, as understood by GraalVM.
   */
  @Schema(description = """
                        The language to use, as understood by GraalVM.
                        <P>
                        By default the only acceptable value is "js", but custom builds can use other lanaguages.
                        """
          , maxLength = 100
  )
  public String getLanguage() {
    return language;
  }

  /**
   * A predicate script that is used to determine whether or not the row should be discarded.
   * <P>
   * The script should return a value that is either true or false, if the value is false the row will be discarded.
   * @return A predicate script that is used to determine whether or not the row should be discarded.
   */
  @Schema(description = """
                        A predicate script is used to determine whether or not the row should be discarded.
                        <P>
                        The script should return a value that is either true or false, if the value is false the row will be discarded.
                        """
          , maxLength = 1000000
  )
  public String getPredicate() {
    return predicate;
  }

  /**
   * A process script that can manipulate the row in any way it wants.
   * @return a process script that can manipulate the row in any way it wants.
   */
  @Schema(description = """
                        A process script can manipulate the row in any way it wants.
                        <P>
                        """
          , maxLength = 1000000
  )
  public String getProcess() {
    return process;
  }

  /**
   * Builder class for ProcessorScript.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private ProcessorType type = ProcessorType.SCRIPT;
    private Condition condition;
    private String name;
    private String language = "js";
    private String predicate;
    private String process;

    private Builder() {
    }

    /**
     * Set the {@link ProcessorScript#type} value in the builder.
     * @param value The value for the {@link ProcessorScript#type}, must be {@link ProcessorType#DYNAMIC_FIELD}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder type(final ProcessorType value) {
      this.type = value;
      return this;
    }

    /**
     * Set the {@link ProcessorScript#condition} value in the builder.
     * @param value The value for the {@link ProcessorScript#condition}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder condition(final Condition value) {
      this.condition = value;
      return this;
    }
    
    /**
     * Set the {@link ProcessorScript#name} value in the builder.
     * @param value The value for the {@link ProcessorScript#name}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder name(final String value) {
      this.name = value;
      return this;
    }

    /**
     * Set the {@link ProcessorScript#language} value in the builder.
     * @param value The value for the {@link ProcessorScript#language}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder language(final String value) {
      this.language = value;
      return this;
    }

    /**
     * Set the {@link ProcessorScript#predicate} value in the builder.
     * @param value The value for the {@link ProcessorScript#predicate}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder predicate(final String value) {
      this.predicate = value;
      return this;
    }

    /**
     * Set the {@link ProcessorScript#process} value in the builder.
     * @param value The value for the {@link ProcessorScript#process}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder process(final String value) {
      this.process = value;
      return this;
    }

    /**
     * Construct a new instance of the ProcessorScript class.
     * @return a new instance of the ProcessorScript class.
     */
    public ProcessorScript build() {
      ProcessorScript result = new ProcessorScript(type, condition, name, language, predicate, process);
      result.validateType(ProcessorType.SCRIPT, type);
      return result;
    }
  }

  /**
   * Construct a new instance of the ProcessorScript.Builder class.
   * @return a new instance of the ProcessorScript.Builder class.
   */
  public static ProcessorScript.Builder builder() {
    return new ProcessorScript.Builder();
  }

  private ProcessorScript(final ProcessorType type, final Condition condition, final String name, final String language, final String predicate, final String process) {
    this.type = type;
    this.condition = condition;
    this.name = name;
    this.language = language;
    this.predicate = predicate;
    this.process = process;
  }
  
  
  
  
}
