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
import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.procs.script.ProcessorScriptInstance;

/**
 * Processor that runs a custom script on each row of the output.
 * 
 * @author jtalbut
 */
@JsonDeserialize(builder = ProcessorLimit.Builder.class)
@Schema(description = """
                      Run a custom script on each row of the output.
                      """
)
public class ProcessorScript implements Processor {
  
  private final ProcessorType type;
  private final String language;
  private final String predicate;
  private final String process;

  @Override
  public ProcessorInstance createInstance(Vertx vertx, SourceNameTracker sourceNameTracker, Context context) {
    return new ProcessorScriptInstance(vertx, sourceNameTracker, context, this);
  }
  
  @Override
  public void validate() {
    validateType(ProcessorType.SCRIPT, type);
  }
  
  @Override
  public ProcessorType getType() {
    return type;
  }

  @Schema(description = """
                        The language to use, as understood by GraalVM.
                        <P>
                        By default the only acceptable value is "js", but custom builds can use other lanaguages.
                        """
  )
  public String getLanguage() {
    return language;
  }

  @Schema(description = """
                        A predicate script is used to determine whether or not the row should be discarded.
                        <P>
                        The script should return a value that is either true or false, if the value is false the row will be discarded.
                        """
  )
  public String getPredicate() {
    return predicate;
  }

  @Schema(description = """
                        A process script can manipulate the row in any way it wants.
                        <P>
                        """
  )
  public String getProcess() {
    return process;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private ProcessorType type = ProcessorType.SCRIPT;
    private String language = "js";
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
      return new uk.co.spudsoft.query.defn.ProcessorScript(type, language, predicate, process);
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
