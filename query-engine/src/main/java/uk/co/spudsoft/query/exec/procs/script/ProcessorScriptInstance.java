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
package uk.co.spudsoft.query.exec.procs.script;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.tsegismont.streamutils.impl.MappingStream;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.streams.ReadStream;
import java.time.ZoneId;
import java.util.List;
import java.util.function.BiFunction;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.Pipeline;
import uk.co.spudsoft.query.defn.ProcessorScript;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.exec.procs.query.FilteringStream;
import uk.co.spudsoft.query.web.RequestContextHandler;


/**
 * Process rows of the output using GraalVM scripting.
 * 
 * Any installed language may be used, though by default this is restricted to Javascript.
 * 
 * @author jtalbut
 */
public final class ProcessorScriptInstance implements ProcessorInstance {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ProcessorScriptInstance.class);
  
  private static final ZoneId UTC = ZoneId.of("UTC");
  
  private final SourceNameTracker sourceNameTracker;
  private final Context context;
  private final ProcessorScript definition;
  private ReadStream<DataRow> stream;
  
  private Engine engine;
  
  private Source predicateSource;
  private Source processSource;
  
  private final RequestContext requestContext;
  private final Pipeline pipeline;
  private final ImmutableMap<String, Object> arguments;
  
  private Types types;
  
  
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Be aware that the point of sourceNameTracker is to modify the context")
  public ProcessorScriptInstance(Vertx vertx, SourceNameTracker sourceNameTracker, Context context, ProcessorScript definition) {
    this.sourceNameTracker = sourceNameTracker;
    this.context = context;
    this.definition = definition;
    this.requestContext = RequestContextHandler.getRequestContext(context);
    this.pipeline = PipelineInstance.getPipelineDefinition(context);
    this.arguments = buildArgumentMap(requestContext);
  }
  
  @Override
  public String getId() {
    return definition.getId();
  }

  private static ImmutableMap<String, Object> buildArgumentMap(RequestContext requestContext) {
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object>builder();
    if (requestContext != null) {
      MultiMap args = requestContext.getArguments();
      if (args != null) {
        for (String key : args.names()) {
          List<String> values = args.getAll(key);
          if (values.size() == 1) {
            builder.put(key, values.get(0));
          } else {
            builder.put(key, values);
          }
        }      
      }
    }
    return builder.build();
  }

  private boolean runPredicate(DataRow data) {
    sourceNameTracker.addNameToContextLocalData(context);
    return runSource(engine, "predicate", definition.getLanguage(), predicateSource, data, (returnValue, row) -> {
      logger.debug("returnValue ({}): {}", nullableClass(returnValue), returnValue);
      logger.debug("row ({}): {}", nullableClass(row), row);
      return returnValue.asBoolean();
    });
  }
  
  static Class<?> nullableClass(Object o) {
    if (o == null) {
      return null;
    } else {
      return o.getClass();
    }
  }

  private DataRow runProcess(DataRow data) {
    sourceNameTracker.addNameToContextLocalData(context);
    return runSource(engine, "process", definition.getLanguage(), processSource, data, (returnValue, row) -> {
      logger.debug("returnValue ({}): {}", nullableClass(returnValue), returnValue);
      logger.debug("row ({}): {}", nullableClass(row), row);
      return row;
     });
  }
  
  // Compare the bindings with PipelineInstance#renderTemplate and ConditionInstance#evaluate
  <T> T runSource(Engine engine, String name, String language, Source source, DataRow data, BiFunction<Value, DataRow, T> postProcess) {
    try (org.graalvm.polyglot.Context graalContext = org.graalvm.polyglot.Context.newBuilder(language).engine(engine).build()) {
      Value bindings = graalContext.getBindings(language);
      bindings.putMember("request", requestContext);
      bindings.putMember("pipeline", pipeline);
      bindings.putMember("args", ProxyObject.fromMap(arguments));
      bindings.putMember("row", new ProxyDataRow(data)); // ProxyObject.fromMap(data.getMap()));
      Value outputValue = graalContext.eval(source);    
      T result = postProcess.apply(outputValue, data);
      logger.debug("Running {} {} gave {}", name, source.getCharacters(), result);
      return result;
    } catch (Throwable ex) {
      logger.warn("Failed to evaluate {}: ", name, ex);
      return null;
    }
  }
  
  static Comparable<?> mapToNativeObject(Value value) {
    if (value.isNull()) {
      return null;
    } else if (value.isNumber()) {
      if (value.fitsInLong()) {
        return value.asLong();
      } else {
        return value.asDouble();
      }
    } else if (value.isBoolean()) {
      return value.asBoolean();
    } else if (value.isString()) {
      return value.asString();
    } else if (value.isDate()) {
      if (value.isTime()) {
        if (value.isTimeZone()) {
          ZoneId zoneId = value.asTimeZone();
          if (zoneId.equals(UTC)) {
            return value.asInstant();
          } else {
            return value.asDate().atTime(value.asTime()).atZone(value.asTimeZone());
          }
        } else {
          return value.asDate().atTime(value.asTime());
        }
      } else {
        return value.asDate();
      }
    } else if (value.isTime()) {
      return value.asTime();
    } else if (value.isDuration()) {
      return value.asDuration();
//    } else if (value.isHostObject()) {
//      return value.asHostObject();
//    } else if (value.isNativePointer()) {
//      return value.asNativePointer();
    } else if (value.isException()) {
      value.throwException();
    } 
    logger.warn("Unknown value type: {}", value);
    return null;
  }

  @Override
  public Future<ReadStreamWithTypes> initialize(PipelineExecutor executor, PipelineInstance pipeline, String parentSource, int processorIndex, ReadStreamWithTypes input) {
    try {
      engine = Engine.newBuilder()
              .option("engine.WarnInterpreterOnly", "false")
              .build();
    } catch (Throwable ex) {
      engine = Engine.newBuilder()
              .option("engine.WarnInterpreterOnly", "false")
              .build();
    }
    
    this.types = input.getTypes();
    this.stream = input.getStream();
    if (!Strings.isNullOrEmpty(definition.getPredicate())) {
      predicateSource = Source.newBuilder(definition.getLanguage(), definition.getPredicate(), Integer.toString(hashCode()) + ":predicate").cached(true).buildLiteral();
      stream = new FilteringStream<>(stream, this::runPredicate);
    }
    if (!Strings.isNullOrEmpty(definition.getProcess())) {
      processSource = Source.newBuilder(definition.getLanguage(), definition.getProcess(), Integer.toString(hashCode()) + ":process").cached(true).buildLiteral();
      stream = new MappingStream<>(stream, this::runProcess);
    }
 
    this.stream.endHandler(v -> {
      if (engine != null) {
        engine.close();
      }
    });
    return Future.succeededFuture(new ReadStreamWithTypes(stream, types));
  }

}
