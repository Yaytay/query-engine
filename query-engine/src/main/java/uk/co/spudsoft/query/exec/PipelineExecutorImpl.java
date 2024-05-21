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
import com.google.common.net.MediaType;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.Argument;
import uk.co.spudsoft.query.defn.ArgumentValue;
import uk.co.spudsoft.query.defn.Condition;
import uk.co.spudsoft.query.defn.DynamicEndpoint;
import uk.co.spudsoft.query.defn.Pipeline;
import uk.co.spudsoft.query.defn.Processor;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;
import uk.co.spudsoft.query.main.ProtectedCredentials;
import uk.co.spudsoft.query.defn.Format;
import uk.co.spudsoft.query.exec.conditions.ConditionInstance;
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.web.RequestContextHandler;

/**
 * Concrete implementation of PipelineExecutor interface.
 * @author jtalbut
 */
public class PipelineExecutorImpl implements PipelineExecutor {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(PipelineExecutorImpl.class);

  private final FilterFactory filterFactory;
  private final Map<String, ProtectedCredentials> secrets;
  private final Map<String, Object> sharedMap;

  /**
   * Constructor.
   * @param filterFactory The {@link FilterFactory} for creating {@link ProcessorInstance} objects from command line arguments.
   * @param secrets The preconfigured secrets that can be used by pipelines.
   */
  public PipelineExecutorImpl(FilterFactory filterFactory, Map<String, ProtectedCredentials> secrets) {
    this.filterFactory = filterFactory;
    this.secrets = ImmutableCollectionTools.copy(secrets);
    this.sharedMap = new HashMap<>();
  }

  @Override
  public ProtectedCredentials getSecret(String name) {
    return secrets.get(name);
  }

  @Override
  public Object get(String name) {
    return sharedMap.get(name);
  }

  @Override
  public void put(String name, Object value) {
    sharedMap.put(name, value);
  }
  
  @Override
  public Future<Pipeline> validatePipeline(Pipeline definition) {
    try {
      definition.validate();
    } catch (Throwable ex) {
      return Future.failedFuture(ex);
    }
    return Future.succeededFuture(definition);
  }
    
  @Override
  public List<ProcessorInstance> createProcessors(Vertx vertx, SourceNameTracker sourceNameTracker, Context context, SourcePipeline definition, MultiMap params) {
    List<ProcessorInstance> result = new ArrayList<>();
    for (Processor processor : definition.getProcessors()) {
      Condition condition = processor.getCondition();
      if (condition == null) {
        result.add(processor.createInstance(vertx, sourceNameTracker, context));
      } else {
        ConditionInstance cond = new ConditionInstance(condition.getExpression());
        RequestContext requestContext = RequestContextHandler.getRequestContext(context);
        if (cond.evaluate(requestContext, null)) {
          result.add(processor.createInstance(vertx, sourceNameTracker, context));
        }
      }
    }
    if (params != null) {
      for (Entry<String, String> entry : params.entries()) {
        ProcessorInstance processor = filterFactory.createFilter(vertx, sourceNameTracker, context, entry.getKey(), entry.getValue());
        if (processor != null) {
          result.add(processor);
        }
      }
    }
    
    return result;
  }

  @Override
  public List<PreProcessorInstance> createPreProcessors(Vertx vertx, Context context, Pipeline definition) {
    List<PreProcessorInstance> result = new ArrayList<>();
    for (DynamicEndpoint de : definition.getDynamicEndpoints()) {
      result.add(de.createInstance(vertx, context));
    }
    return result;
  }
  
  private boolean possibleValuesContains(Argument argument, String value) {
    for (ArgumentValue argValue : argument.getPossibleValues()) {
      if (argValue.getValue().equals(value)) {
        return true;
      }
    }
    return false;
  }
  
  @Override
  public Map<String, ArgumentInstance> prepareArguments(RequestContext requestContext, List<Argument> definitions, MultiMap valuesMap) {
    Map<String, ArgumentInstance> result = new HashMap<>();
    Map<String, Object> arguments = new HashMap<>();
    if (valuesMap == null) {
      valuesMap = new HeadersMultiMap();
    }
    for (Argument arg : definitions) {
      if (arg.isIgnored()) {
        continue ;
      }
      List<String> argValues = valuesMap.getAll(arg.getName());
      if (arg.isValidate()) {
        for (String argValue : argValues) {
          if (arg.getPossibleValues() != null && !arg.getPossibleValues().isEmpty()) {
            if (!possibleValuesContains(arg, argValue)) {
              logger.warn("Argument {} has been passed in as {}, the value {} is not in the list of possible values ({})"
                      , arg.getName(), argValues, argValue, arg.getPossibleValues());
              throw new IllegalArgumentException("The argument \"" + arg.getName() + "\" passed in as the value \"" + argValue + "\" is not permitted.");
            }
          }
        }
      }
      
      ImmutableList<String> values = ImmutableList.of();
      if (argValues != null && !argValues.isEmpty()) {
        values = ImmutableList.copyOf(argValues);
      } else if (!arg.isOptional()) {
        throw new IllegalArgumentException("The argument \"" + arg.getName() + "\" is mandatory and was not provided.");
      } else if (!Strings.isNullOrEmpty(arg.getDefaultValue())) {
        // Default values need to be JEXL expressions
        values = ImmutableList.of(arg.getDefaultValue());
      }
      
      if (arg.isValidate() && !Strings.isNullOrEmpty(arg.getPermittedValuesRegex())) {
        Pattern pattern;
        try {
          pattern = Pattern.compile(arg.getPermittedValuesRegex());
        } catch (Throwable ex) {
          throw new IllegalArgumentException("The argument \"" + arg.getName() + "\" does not have a valid permittedValuesRegex." + ex.getMessage());
        }
        for (String value : values) {
          if (!pattern.matcher(value).matches()) {
            throw new IllegalArgumentException("The argument \"" + arg.getName() + "\" has been passed a value that does not match its permittedValuesRegex.");
          }
        }
      }
      
      if (values.size() > 1 && !arg.isMultiValued()) {
        throw new IllegalArgumentException("The argument \"" + arg.getName() + "\" has been provided " + values.size() + " times but is not multivalued.");
      }
      
      if (values.size() == 1) {
        arguments.put(arg.getName(), values.get(0));
      } else {
        arguments.put(arg.getName(), values);
      }
      
      // Parsing of values occurs as part of construction on ArgumentInstance
      ArgumentInstance instance = new ArgumentInstance(arg, values);
      if (arg.isValidate()) {
        instance.validateMinMax();
      }
      result.put(arg.getName(), instance);
    }
    requestContext.setArguments(arguments);
    return result;
  }  
  
  private Future<ReadStreamWithTypes> initializeProcessors(PipelineInstance pipeline, String parentSource, Iterator<ProcessorInstance> iter, int index, ReadStreamWithTypes input) {
    logger.debug("initializeProcessors({}, {}, {}, {}, {})", pipeline, parentSource, iter, index, input);
    if (!iter.hasNext()) {
      return Future.succeededFuture(input);
    } else {
      ProcessorInstance processor = iter.next();
      return processor.initialize(this, pipeline, parentSource, index, input)
              .compose(streamWithTypes -> {
                return initializeProcessors(pipeline, parentSource, iter, index + 1, streamWithTypes);
              });
    }
  }
  
  private Future<Void> runPreProcessors(PipelineInstance pipeline, Iterator<PreProcessorInstance> iter) {
    if (iter.hasNext()) {
      PreProcessorInstance pp = iter.next();
      return pp.initialize(this, pipeline)
              .compose(v -> runPreProcessors(pipeline, iter));
    } else {
      return Future.succeededFuture();
    }
  }
  
  private Future<Void> runPreProcessors(PipelineInstance pipeline) {
    if (pipeline.getPreProcessors().isEmpty()) {
      return Future.succeededFuture();
    } else {
      logger.debug("Pipeline has at least one preprocessor");
      Iterator<PreProcessorInstance> iter = pipeline.getPreProcessors().iterator();
      return runPreProcessors(pipeline, iter);
    }
  }
  
  @Override
  public Future<Void> initializePipeline(PipelineInstance pipeline) {    
    return runPreProcessors(pipeline)
            .compose(v -> {
              return pipeline.getSource().initialize(this, pipeline);
            })
            .compose(sourceStreamWithTypes -> {
              logger.debug("Source {} initialized", pipeline.getSource().getName());
              return initializeProcessors(pipeline, pipeline.getSource().getName(), pipeline.getProcessors().iterator(), 1, sourceStreamWithTypes);
            })
            .compose(streamWithTypes -> {
              logger.debug("Processors ({}) initialized", pipeline.getProcessors().size());

              return pipeline.getSink().initialize(this, pipeline, streamWithTypes);
            })
            .andThen(ar -> pipeline.getFinalPromise().handle(ar))
            ;
  }
  
  @Override 
  public Format getFormat(List<Format> formats, FormatRequest requested) {
    if (requested == null) {
      return formats.get(0);
    }
    if (!Strings.isNullOrEmpty(requested.getName())) {
      for (Format format : formats) {
        if (requested.getName().equals(format.getName())) {
          return format;
        }
      }
      logger.info("The format {} was requested, but the supported format are: {}"
              , requested.getName()
              , formats.stream().map(d -> d.getName()).filter(f -> f != null).collect(Collectors.toList()));
      throw new IllegalArgumentException("The requested format is not supported for this request");
    }
    if (!Strings.isNullOrEmpty(requested.getExtension())) {
      for (Format format : formats) {
        if (requested.getExtension().equals(format.getExtension())) {
          return format;
        }
      }
      logger.info("The extension {} was requested, but the supported extensions are: {}"
              , requested.getExtension()
              , formats.stream().map(d -> d.getExtension()).filter(f -> f != null).collect(Collectors.toList()));
      throw new IllegalArgumentException("The requested extension is not supported for this request");
    }
    if (requested.getAccept() != null && !requested.getAccept().isEmpty()) {
      Format format = findBestAcceptableFormat(formats, requested);
      if (format != null) {
        return format;
      }
      logger.info("The media types {} were requested, but the supported media types are: {}"
              , requested.getAccept()
              , formats.stream().map(d -> d.getMediaType()).filter(f -> f != null).collect(Collectors.toList()));
      throw new IllegalArgumentException("The requested media type is not supported for this request");
    }
    return formats.get(0);
  }

  /**
   * Return the best Format from the list of Formats according to the List of Acceptable MediaType ranges from the FormatRequest.
   * The List of Acceptable MediaType ranges must already be sorted by preference:
   * <ul>
   * <li>Firstly by q value (with default q value being 1 (best)).
   * <li>Secondly by specificity, wildcards come after more specific types.
   * </ul>
   * @param formats the formats specified in the pipeline definition.
   * @param requested the FormatRequest built from the HTTP request.
   * @return the best format found in the formats.
   */
  protected Format findBestAcceptableFormat(List<Format> formats, FormatRequest requested) {
    for (MediaType rangeRequested : requested.getAccept()) {
      MediaType rangeRequestedWithoutParameters = rangeRequested.withoutParameters();
      for (Format format : formats) {
        if (format.getMediaType() != null) {
          if (format.getMediaType().is(rangeRequestedWithoutParameters)) {
            return format;
          }
        }
      }
    }
    return null;
  }

}
