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
import com.google.common.net.MediaType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
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
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.defn.DynamicEndpoint;
import uk.co.spudsoft.query.defn.Pipeline;
import uk.co.spudsoft.query.defn.Processor;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;
import uk.co.spudsoft.query.main.ProtectedCredentials;
import uk.co.spudsoft.query.defn.Format;
import uk.co.spudsoft.query.exec.conditions.ConditionInstance;
import uk.co.spudsoft.query.exec.conditions.JexlEvaluator;
import uk.co.spudsoft.query.exec.context.PipelineContext;

/**
 * Concrete implementation of PipelineExecutor interface.
 * @author jtalbut
 */
public class PipelineExecutorImpl implements PipelineExecutor {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(PipelineExecutorImpl.class);

  private final MeterRegistry meterRegistry;
  private final FilterFactory filterFactory;
  private final Map<String, ProtectedCredentials> secrets;
  private final Map<String, Object> sharedMap;

  /**
   * Constructor.
   * @param meterRegistry MeterRegistry for production of metrics.
   * @param filterFactory The {@link FilterFactory} for creating {@link ProcessorInstance} objects from command line arguments.
   * @param secrets The preconfigured secrets that can be used by pipelines.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "MeterRegistry is designed to be modified")
  PipelineExecutorImpl(MeterRegistry meterRegistry, FilterFactory filterFactory, Map<String, ProtectedCredentials> secrets) {
    this.meterRegistry = meterRegistry;
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
  public List<ProcessorInstance> createProcessors(Vertx vertx, PipelineContext pipelineContext, SourcePipeline definition, MultiMap params) {
    List<ProcessorInstance> result = new ArrayList<>();

    for (int index = 0; index < definition.getProcessors().size(); ++index) {
      Processor processor =  definition.getProcessors().get(index);
      
      String processorName = pipelineContext.getPipe()
              + "."
              + (Strings.isNullOrEmpty(processor.getName()) ? ("processors[" + index + "]") : processor.getName())
              ;
      Condition condition = processor.getCondition();
      if (condition == null) {
        logger.debug("Added {} processor named {} with no condition", processor.getType(), processorName);
        result.add(processor.createInstance(vertx, pipelineContext, meterRegistry, processorName));
      } else {
        ConditionInstance cond = new ConditionInstance(condition.getExpression());
        if (cond.evaluate(pipelineContext, null)) {
          logger.debug("Added {} processor named {} because condition {} met", processor.getType(), processorName, cond);
          result.add(processor.createInstance(vertx, pipelineContext, meterRegistry, processorName));
        } else {
          logger.debug("Skipped {} processor {} because condition {} not met", processor.getType(), processor.getName(), cond);
        }
      }
    }
    if (params != null) {
      int index = 0;
      for (Entry<String, String> entry : params.entries()) {
        ++index;
        String filterName = pipelineContext.getPipe() + ".filter[" + index + "]";
        ProcessorInstance processor = filterFactory.createFilter(vertx, pipelineContext, meterRegistry, entry.getKey(), entry.getValue(), filterName);
        if (processor != null) {
          result.add(processor);
        }
      }
    }

    return result;
  }

  @Override
  public List<PreProcessorInstance> createPreProcessors(Vertx vertx, PipelineContext pipelineContext, Pipeline definition) {
    List<PreProcessorInstance> result = new ArrayList<>();
    int index = 0;
    for (DynamicEndpoint de : definition.getDynamicEndpoints()) {
      result.add(de.createInstance(vertx, pipelineContext, meterRegistry, index++));
    }
    return result;
  }

  static boolean possibleValuesContains(Argument argument, String value) {
    for (ArgumentValue argValue : argument.getPossibleValues()) {
      if (argValue.getValue().equals(value)) {
        return true;
      }
    }
    return false;
  }

  static void addCastItem(PipelineContext pipelineContext, String name, ImmutableList.Builder<Comparable<?>> builder, DataType type, Object item) throws IllegalArgumentException {
    try {
      builder.add(type.cast(pipelineContext, item));
    } catch (Throwable ex) {
      logger.warn("Unable to cast '{}' ({}) as {}: ", item, item.getClass(), type, ex);
      throw new IllegalArgumentException("The argument \"" + name + "\" was passed a value which cannot be converted to " + type.name() + ".");
    }
  }

  static ImmutableList<Comparable<?>> evaluateDefaultValues(PipelineContext pipelineContext, Argument arg, Pattern permittedValuesPattern) throws Throwable {
    JexlEvaluator evaluator = new JexlEvaluator(arg.getDefaultValueExpression());
    Object raw = evaluator.evaluateAsObject(pipelineContext.getRequestContext(), null);
    if (raw == null) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<Comparable<?>> builder = ImmutableList.<Comparable<?>>builder();
    if (raw instanceof Object[] rawArray) {
      for (Object item : rawArray) {
        if (arg.isValidate()) {
          if (item instanceof String stringItem) {
            validateArgumentValue(pipelineContext, arg, permittedValuesPattern, stringItem, true);
          }
        }
        if (item != null) {
          addCastItem(pipelineContext, arg.getName(), builder, arg.getType(), item);
        }
      }
    } else if (raw instanceof List<?> rawList) {
      for (Object item : rawList) {
        if (arg.isValidate()) {
          if (item instanceof String stringItem) {
            validateArgumentValue(pipelineContext, arg, permittedValuesPattern, stringItem, true);
          }
        }
        if (item != null) {
          addCastItem(pipelineContext, arg.getName(), builder, arg.getType(), item);
        }
      }
    } else {
      if (arg.isValidate()) {
        if (raw instanceof String stringItem) {
          validateArgumentValue(pipelineContext, arg, permittedValuesPattern, stringItem, true);
        }
      }
      addCastItem(pipelineContext, arg.getName(), builder, arg.getType(), raw);
    }
    return builder.build();
  }

  static ImmutableList<Comparable<?>> castAndValidatePassedValues(PipelineContext pipelineContext, Argument arg, Pattern permittedValuesPattern, List<String> values) throws Throwable {
    ImmutableList.Builder<Comparable<?>> builder = ImmutableList.<Comparable<?>>builder();
    for (Object item : values) {
      if (arg.isValidate()) {
        if (item instanceof String stringItem) {
          validateArgumentValue(pipelineContext, arg, permittedValuesPattern, stringItem, false);
        }
      }
      if (item != null) {
        addCastItem(pipelineContext, arg.getName(), builder, arg.getType(), item);
      }
    }
    return builder.build();
  }

  static void validateArgumentValue(PipelineContext pipelineContext, Argument arg, Pattern permittedValuesPattern, String value, boolean defaultValue) throws IllegalArgumentException {
    if (arg.getPossibleValues() != null && !arg.getPossibleValues().isEmpty()) {
      if (!possibleValuesContains(arg, value)) {
        if (defaultValue) {
          logger.warn("Argument {} generated the default value \"{}\", which is not in the list of possible values ({})", arg.getName(), value, arg.getPossibleValues());
          throw new IllegalArgumentException("The argument \"" + arg.getName() + "\" generated a default value which is not permitted, please contact the designer.");
        } else {
          logger.warn("Argument {} was passed the value \"{}\", which is not in the list of possible values ({})", arg.getName(), value, arg.getPossibleValues());
          throw new IllegalArgumentException("The argument \"" + arg.getName() + "\" was passed a value which is not permitted.");
        }
      }
    }
    if (permittedValuesPattern != null && !permittedValuesPattern.matcher(value).matches()) {
      if (defaultValue) {
        logger.warn("Argument {} generated the default value \"{}\", which does not match \"{}\"", arg.getName(), value, arg.getPermittedValuesRegex());
        throw new IllegalArgumentException("The argument \"" + arg.getName() + "\" generated a default value which is not permitted, please contact the designer.");
      } else {
        logger.warn("Argument {} was passed the value \"{}\", which does not match \"{}\"", arg.getName(), value, arg.getPermittedValuesRegex());
        throw new IllegalArgumentException("The argument \"" + arg.getName() + "\" was passed a value which is not permitted.");
      }
    }
  }

  @Override
  public Map<String, ArgumentInstance> prepareArguments(RequestContext requestContext, List<Argument> definitions, MultiMap valuesMap) throws Throwable {

    PipelineContext pipelineContext = new PipelineContext(null, requestContext);
    
    Map<String, ArgumentInstance> result = new HashMap<>();
    Map<String, Object> arguments = new HashMap<>();
    if (valuesMap == null) {
      valuesMap = HeadersMultiMap.httpHeaders();
    }
    for (Argument arg : definitions) {
      if (arg.isIgnored()) {
        continue ;
      }

      Pattern permittedValuesPattern = null;
      if (arg.isValidate() && !Strings.isNullOrEmpty(arg.getPermittedValuesRegex())) {
        try {
          permittedValuesPattern = Pattern.compile(arg.getPermittedValuesRegex());
        } catch (Throwable ex) {
          throw new IllegalArgumentException("The argument \"" + arg.getName() + "\" does not have a valid permittedValuesRegex." + ex.getMessage());
        }
      }

      List<String> argStringValues = valuesMap.getAll(arg.getName());
      ImmutableList<Comparable<?>> values = null;

      if (arg.isEmptyIsAbsent()) {
        argStringValues = argStringValues.stream()
                .filter(a -> !Strings.isNullOrEmpty(a))
                .collect(Collectors.toList());
        if (argStringValues.isEmpty() && !arg.isOptional()) {
          continue ;
        }
      }

      if (arg.isHidden() && argStringValues != null && !argStringValues.isEmpty()) {
        throw new IllegalArgumentException("The argument \"" + arg.getName() + "\" is not permitted.");
      }

      if (arg.getCondition() != null && !Strings.isNullOrEmpty(arg.getCondition().getExpression())) {
        ConditionInstance conditionInstance = arg.getCondition().createInstance();
        if (!conditionInstance.evaluate(requestContext, null, null)) {
          // Condition not met, either use default or skip this argument
          if (Strings.isNullOrEmpty(arg.getDefaultValueExpression())) {
            continue ;
          } else {
            values = evaluateDefaultValues(pipelineContext, arg, permittedValuesPattern);
          }
        }
      }

      if (values == null && argStringValues != null && !argStringValues.isEmpty()) {
        values = castAndValidatePassedValues(pipelineContext, arg, permittedValuesPattern, argStringValues);
      } else if (!arg.isOptional() && !arg.isHidden()) {
        throw new IllegalArgumentException("The argument \"" + arg.getName() + "\" is mandatory and was not provided.");
      } else if (!Strings.isNullOrEmpty(arg.getDefaultValueExpression())) {
        values = evaluateDefaultValues(pipelineContext, arg, permittedValuesPattern);
      } else {
        values = ImmutableList.of();
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
        instance.validateMinMax(pipelineContext);
      }
      result.put(arg.getName(), instance);
    }
    requestContext.setArguments(arguments);
    logger.debug("Prepared arguments: {}", arguments);
    return result;
  }

  @Override
  public void progressNotification(
          RequestContext requestContext
          , String pipelineTitle
          , String sourceName
          , String processorName
          , Long count
          , boolean completed
          , Boolean succeeded
          , String message
          , Object... arguments
  ) {
  }

  private void progressNotificationInternal(PipelineInstance pipeline
          , SourceInstance source
          , ProcessorInstance processor
          , Long count
          , boolean completed
          , Boolean succeeded
          , String message
          , Object... arguments
  ) {
  }

  private Future<ReadStreamWithTypes> initializeProcessors(PipelineInstance pipeline, String parentSource, Iterator<ProcessorInstance> iter, int index, ReadStreamWithTypes input) {
    logger.debug("initializeProcessors({}, {}, {}, {}, {})", pipeline, parentSource, iter, index, input);
    if (!iter.hasNext()) {
      progressNotificationInternal(pipeline, null, null, (long) index, false, null, "All processors initialized", null, null);
      logger.debug("Types after all processors initialized: {}", input.getTypes());
      return Future.succeededFuture(input);
    } else {
      ProcessorInstance processor = iter.next();
      progressNotificationInternal(pipeline, null, processor, (long) index, false, null, "Initializing {}", processor.getName());
      return processor.initialize(this, pipeline, parentSource, index, input)
              .compose(streamWithTypes -> {
                progressNotificationInternal(pipeline, null, processor, (long) index, false, null, "Initialized {}", processor.getName());
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
              logger.debug("Source initialized");
              return initializeProcessors(pipeline, pipeline.getPipelineContext().getPipe(), pipeline.getProcessors().iterator(), 1, sourceStreamWithTypes);
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
