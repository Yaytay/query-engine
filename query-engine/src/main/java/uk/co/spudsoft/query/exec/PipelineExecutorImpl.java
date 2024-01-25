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
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.streams.WriteStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.Argument;
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
 *
 * @author jtalbut
 */
public class PipelineExecutorImpl implements PipelineExecutor {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(PipelineExecutorImpl.class);

  private final Map<String, ProtectedCredentials> secrets;
  private final Map<String, Object> sharedMap;

  public PipelineExecutorImpl(Map<String, ProtectedCredentials> secrets) {
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
  public List<ProcessorInstance> createProcessors(Vertx vertx, SourceNameTracker sourceNameTracker, Context context, SourcePipeline definition) {
    List<ProcessorInstance> result = new ArrayList<>();
    for (Processor processor : definition.getProcessors()) {
      Condition condition = processor.getCondition();
      if (condition == null) {
        result.add(processor.createInstance(vertx, sourceNameTracker, context));
      } else {
        ConditionInstance cond = new ConditionInstance(condition.getExpression());
        RequestContext requestContext = RequestContextHandler.getRequestContext(context);
        if (cond.evaluate(requestContext)) {
          result.add(processor.createInstance(vertx, sourceNameTracker, context));
        }
      }
    }
    return result;
  }

  @Override
  public List<PreProcessorInstance> createPreProcessors(Vertx vertx, Context context, Pipeline definition) {
    List<PreProcessorInstance> result = new ArrayList<>();
    for (DynamicEndpoint de : definition.getDynamicEndpoints()) {
      result.add(de.createInstance(vertx, context, de));
    }
    return result;
  }
  
  @Override
  public Map<String, ArgumentInstance> prepareArguments(List<Argument> definitions, MultiMap valuesMap) {
    Map<String, ArgumentInstance> result = new HashMap<>();
    if (valuesMap == null) {
      valuesMap = new HeadersMultiMap();
    }
    for (Argument arg : definitions) {
      if (arg.isIgnored()) {
        continue ;
      }
      List<String> argValues = valuesMap.getAll(arg.getName());
      ImmutableList<String> values = ImmutableList.of();
      if (argValues != null && !argValues.isEmpty()) {
        values = ImmutableList.copyOf(argValues);
      } else if (!arg.isOptional()) {
        throw new IllegalArgumentException("The argument \"" + arg.getName() + "\" is mandatory and was not provided.");
      } else if (!Strings.isNullOrEmpty(arg.getDefaultValue())) {
        values = ImmutableList.of(arg.getDefaultValue());
      }
      
      if (!Strings.isNullOrEmpty(arg.getPermittedValuesRegex())) {
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
      
      ArgumentInstance instance = new ArgumentInstance(arg.getName(), arg, values);
      result.put(arg.getName(), instance);
    }
    return result;
  }  
  
  private Future<Void> initializeProcessors(PipelineInstance pipeline, String parentSource, Iterator<ProcessorInstance> iter, int index) {
    if (!iter.hasNext()) {
      return Future.succeededFuture();
    } else {
      return iter.next()
              .initialize(this, pipeline, parentSource, index)
              .compose(v -> initializeProcessors(pipeline, parentSource, iter, index + 1));
    }
  }
  
  private void buildPipes(Promise<Void> finalPromise, DataRowStream<DataRow> previousReadStream, Iterator<ProcessorInstance> iter, WriteStream<DataRow> finalWriteStream) {
    if (iter.hasNext()) {
      ProcessorInstance processor = iter.next();
      previousReadStream.pipeTo(processor.getWriteStream());
      buildPipes(finalPromise, processor.getReadStream(), iter, finalWriteStream);
    } else {
      previousReadStream.pipeTo(finalWriteStream, finalPromise);
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
            .compose(v -> pipeline.getSource().initialize(this, pipeline))
            .onSuccess(v -> {
              logger.debug("Source {} initialized", pipeline.getSource().getName());
            })
            .compose(v -> initializeProcessors(pipeline, pipeline.getSource().getName(), pipeline.getProcessors().iterator(), 1))
            .compose(v -> pipeline.getSink().initialize(this, pipeline))
            .compose(v -> {
              try {
                buildPipes(pipeline.getFinalPromise(), pipeline.getSource().getReadStream(), pipeline.getProcessors().iterator(), pipeline.getSink().getWriteStream());
                return Future.succeededFuture();
              } catch (Throwable ex) {
                return Future.failedFuture(ex);
              }
            })
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
              , requested.getName()
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
   * @param formats
   * @param requested
   * @return 
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
