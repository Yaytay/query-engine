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
package uk.co.spudsoft.query.exec;

import uk.co.spudsoft.query.exec.context.RequestContext;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import uk.co.spudsoft.query.defn.Endpoint;
import uk.co.spudsoft.query.defn.Pipeline;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;

/**
 * An Instance of a Pipeline.
 * 
 * A pipeline starts as a definition ({@link uk.co.spudsoft.query.defn.Pipeline}) that must be converted into a PipelineInstance in order to be executed.
 * 
 * @author jtalbut
 */
public class PipelineInstance {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(PipelineInstance.class);
  
  private final PipelineContext pipelineContext;
  private final Pipeline definition;
  private final ImmutableMap<String, ArgumentInstance> argumentInstances;
  private final ImmutableMap<String, Object> arguments;
  private final Map<String, Endpoint> sourceEndpoints;
  private final ImmutableList<PreProcessorInstance> preProcessors;
  private final SourceInstance source;
  private final ImmutableList<ProcessorInstance> processors;
  private final FormatInstance sink;
  private final Promise<Void> finalPromise;

  /**
   * Constructor.
   * @param pipelineContext The context in which this {@link SourcePipeline} is being run.
   * @param definition The definition of the {@link Pipeline}.
   * @param argumentInstances the arguments passed in to the request, matched to argument in the {@link Pipeline} definition.
   * @param sourceEndpoints The set of {@link Endpoint} definitions from the {@link Pipeline} definition.
   * @param preProcessors The {@link PreProcessorInstance} objects instantiated from the {@link Pipeline} definition.
   * @param source The primary {@link SourceInstance} object instantiated from the {@link Pipeline} definition.
   * @param processors The {@link ProcessorInstance} objects instantiated from the {@link Pipeline} definition.
   * @param sink The {@link FormatInstance} object that will handle the output.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Definition must not be modified, RequestContext may be modified by permitted setters")
  public PipelineInstance(
          final PipelineContext pipelineContext          
          , final Pipeline definition
          , final Map<String, ArgumentInstance> argumentInstances
          , final Map<String, Endpoint> sourceEndpoints
          , final List<PreProcessorInstance> preProcessors
          , final SourceInstance source
          , final List<ProcessorInstance> processors
          , final FormatInstance sink
  ) {
    this.definition = definition;
    this.pipelineContext = pipelineContext;
    this.argumentInstances = ImmutableCollectionTools.copy(argumentInstances);
    this.arguments = buildArgumentMap(argumentInstances);
    this.sourceEndpoints = sourceEndpoints == null ? new HashMap<>() : new HashMap<>(sourceEndpoints);
    this.preProcessors = ImmutableCollectionTools.copy(preProcessors);
    this.source = source;
    this.processors = ImmutableCollectionTools.copy(processors);
    this.sink = sink;
    this.finalPromise = Promise.promise();
  }

  /**
   * Get the {@link RequestContext}.
   * @return the {@link RequestContext}.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "RequestContext may be modified by permitted setters")
  public PipelineContext getPipelineContext() {
    return pipelineContext;
  }

  /**
   * Get the pipeline definition.
   * @return the pipeline definition.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Definition should not be modified")
  public Pipeline getDefinition() {
    return definition;
  }
  
  /**
   * Build the {@link Map} of argument names to values (a simpler map than using {@link ArgumentInstance} objects.
   * <p>
   * This is for use by templates and scripts.
   * @param arguments The {@link Map} of arguments names to {@link ArgumentInstance} objects that contains the argument values.
   * @return an {@link ImmutableMap} of names to arguments values (potentially to multiple values).
   */
  public static ImmutableMap<String, Object> buildArgumentMap(Map<String, ArgumentInstance> arguments) {
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object>builder();
    if (arguments != null) {
      for (Entry<String, ArgumentInstance> entry : arguments.entrySet()) {
        String key = entry.getKey();
        ArgumentInstance value = entry.getValue();
        if (value.getValues().size() == 1) {
          builder.put(key, value.getValues().get(0));
        } else if (!value.getValues().isEmpty()) {
          builder.put(key, value.getValues());
        }
      }      
    }
    return builder.build();
  }

  /**
   * Get the {@link Map} of {@link ArgumentInstance} objects.
   * @return the {@link Map} of {@link ArgumentInstance} objects.
   */
  public ImmutableMap<String, ArgumentInstance> getArgumentInstances() {
    return argumentInstances;
  }
  
  /**
   * Get the {@link Map} of {@link Endpoint} objects.
   * @return the {@link Map} of {@link Endpoint} objects.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "DynamicSourceEndpoints do modify this value")
  public Map<String, Endpoint> getSourceEndpoints() {
    return sourceEndpoints;
  }

  /**
   * Get the {@link List} of {@link PreProcessorInstance} objects that will be run first.
   * @return the {@link List} of {@link PreProcessorInstance} objects.
   */
  public ImmutableList<PreProcessorInstance> getPreProcessors() {
    return preProcessors;
  }

  /**
   * Get the primary {@link SourceInstance}.
   * @return the primary {@link SourceInstance}.
   */
  public SourceInstance getSource() {
    return source;
  }

  /**
   * Get the {@link List} of {@link ProcessorInstance} objects.
   * @return the {@link List} of {@link ProcessorInstance} objects.
   */
  public List<ProcessorInstance> getProcessors() {
    return processors;
  }

  /**
   * Get the output {@link FormatInstance}.
   * @return the output {@link FormatInstance}.
   */
  public FormatInstance getSink() {
    return sink;
  }

  /**
   * Get the {@link Promise} that will be completed when the {@link PipelineInstance} has stream all the data.
   * @return the {@link Promise} that will be completed when the {@link PipelineInstance} has stream all the data.
   */
  public Promise<Void> getFinalPromise() {
    return finalPromise;
  }
  
  /**
   * Render a {@link ST StringTemplate}.
   * @param name the name of the template, used for error reporting.
   * @param template the {@link ST StringTemplate}.
   * @return the output from the {@link ST StringTemplate}.
   */
  // Compare the values added with the ProcessorScriptInstance#runSource and ConditionInstance#evaluate
  public String renderTemplate(String name, String template) throws IllegalStateException {
    if (Strings.isNullOrEmpty(template)) {
      return template;
    }
    StringTemplateListener errorListener = new StringTemplateListener();
    try {
      STGroup stgroup = new STGroup();
      stgroup.setListener(errorListener);
      ST st = new ST(stgroup, template);
      st.add("request", pipelineContext.getRequestContext());
      st.add("args", arguments);
      st.add("pipeline", definition);
      return st.render();
    } catch (Throwable ex) {
      logger.warn("Failed to render template {} with values {}: ", template, arguments, ex);
      logger.warn("Errors: ", Json.encode(errorListener.getErrors()));
      throw new IllegalStateException("Error(s) evaluating " + name + " template: " + Json.encode(errorListener.getErrors()), ex);
    }
  }
}
