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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Promise;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.ST;
import uk.co.spudsoft.query.defn.Endpoint;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;

/**
 *
 * @author jtalbut
 */
public class PipelineInstance {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(PipelineInstance.class);
  
  private final ImmutableMap<String, ArgumentInstance> arguments;
  private final Map<String, Endpoint> sourceEndpoints;
  private final ImmutableList<PreProcessorInstance> preProcessors;
  private final SourceInstance source;
  private final ImmutableList<ProcessorInstance> processors;
  private final FormatInstance sink;
  private final Promise<Void> finalPromise;

  public PipelineInstance(
          final Map<String, ArgumentInstance> arguments
          , final Map<String, Endpoint> sourceEndpoints
          , final List<PreProcessorInstance> preProcessors
          , final SourceInstance source
          , final List<ProcessorInstance> processors
          , final FormatInstance sink          
  ) {
    this.arguments = ImmutableCollectionTools.copy(arguments);
    this.sourceEndpoints = sourceEndpoints == null ? new HashMap<>() : new HashMap<>(sourceEndpoints);
    this.preProcessors = ImmutableCollectionTools.copy(preProcessors);
    this.source = source;
    this.processors = ImmutableCollectionTools.copy(processors);
    this.sink = sink;
    this.finalPromise = Promise.promise();
  }
  
  public ImmutableMap<String, ArgumentInstance> getArguments() {
    return arguments;
  }
  
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "DynamicSourceEndpoints do modify this value")
  public Map<String, Endpoint> getSourceEndpoints() {
    return sourceEndpoints;
  }

  public ImmutableList<PreProcessorInstance> getPreProcessors() {
    return preProcessors;
  }

  public SourceInstance getSource() {
    return source;
  }

  public List<ProcessorInstance> getProcessors() {
    return processors;
  }

  public FormatInstance getSink() {
    return sink;
  }

  public Promise<Void> getFinalPromise() {
    return finalPromise;
  }
  
  public String renderTemplate(String template) {
    if (Strings.isNullOrEmpty(template)) {
      return template;
    }
    try {
      ST st = new ST(template);
      arguments.forEach((k, v) -> {
        if (v.getValues() != null && !v.getValues().isEmpty()) {
          st.add(k, v.getValues().get(0));            
        }
      });
      return st.render();
    } catch (Throwable ex) {
      logger.warn("Failed to render template {} with values {}: ", template, arguments.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().getValues())));
      return null;
    }
  }
}
