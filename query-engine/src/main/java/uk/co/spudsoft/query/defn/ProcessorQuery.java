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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.procs.query.ProcessorQueryInstance;

/**
 * Processor that curtails the output after the configured number of rows.
 * @author jtalbut
 */
@JsonDeserialize(builder = ProcessorQuery.Builder.class)
@Schema(description = """
                      Processor that filters output rows.
                      """
)
public class ProcessorQuery implements Processor {
    
  private final ProcessorType type;
  private final Condition condition;
  private final String id;
  private final String expression;

  @Override
  public ProcessorQueryInstance createInstance(Vertx vertx, SourceNameTracker sourceNameTracker, Context context) {
    return new ProcessorQueryInstance(vertx, sourceNameTracker, context, this);
  }

  @Override
  public void validate() {
    validateType(ProcessorType.QUERY, type);
    try {
      ProcessorQueryInstance.RSQL_PARSER.parse(expression);
    } catch (Throwable ex) {
      throw new IllegalArgumentException("Unable to parse FIQL query expression: " + ex.getMessage(), ex);
    }
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
  public String getId() {
    return id;
  }

  @Schema(description = """
                        A valid FIQL expression that will be evaluated on each row.
                        """
  )
  public String getExpression() {
    return expression;
  }
  
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Builder class should result in all instances being immutable when object is built")
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private ProcessorType type = ProcessorType.QUERY;
    private Condition condition;
    private String id;
    private String expression;

    private Builder() {
    }
    
    public Builder type(final ProcessorType value) {
      this.type = value;
      return this;
    }
    
    /**
     * Set the condition on the Pipeline in the builder.
     * @param value the condition on the Endpoint.
     * @return this, so that the builder may be used fluently.
     */
    public Builder condition(final Condition value) {
      this.condition = value;
      return this;
    }

    public Builder id(final String value) {
      this.id = value;
      return this;
    }

    public Builder expression(final String value) {
      this.expression = value;
      return this;
    }

    public ProcessorQuery build() {
      return new ProcessorQuery(type, condition, id, expression);
    }
  }

  public static ProcessorQuery.Builder builder() {
    return new ProcessorQuery.Builder();
  }

  private ProcessorQuery(final ProcessorType type, final Condition condition, final String id, final String expression) {
    validateType(ProcessorType.QUERY, type);
    this.type = type;
    this.condition = condition;
    this.id = id;
    this.expression = expression;
  }
  
  
  
  
}
