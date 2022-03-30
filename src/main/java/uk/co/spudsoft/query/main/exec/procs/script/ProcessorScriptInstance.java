/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.procs.script;

import com.google.common.base.Strings;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import java.time.ZoneId;
import java.util.function.BiFunction;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.main.defn.ProcessorScript;
import uk.co.spudsoft.query.main.exec.PipelineExecutor;
import uk.co.spudsoft.query.main.exec.PipelineInstance;
import uk.co.spudsoft.query.main.exec.ProcessorInstance;
import uk.co.spudsoft.query.main.exec.procs.AsyncHandler;
import uk.co.spudsoft.query.main.exec.procs.PassthroughStream;


/**
 *
 * @author jtalbut
 */
public class ProcessorScriptInstance implements ProcessorInstance {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ProcessorScriptInstance.class);
  
  private static final ZoneId UTC = ZoneId.of("UTC");
  
  private final ProcessorScript definition;
  private final PassthroughStream<JsonObject> stream;
  
  private Source predicateSource;
  private Source processSource;
  
  public ProcessorScriptInstance(Vertx vertx, Context context, ProcessorScript definition) {
    this.definition = definition;
    this.stream = new PassthroughStream<>(this::passthroughStreamProcessor, context);
  }  

  private Future<Void> passthroughStreamProcessor(JsonObject data, AsyncHandler<JsonObject> chain) {
    try {
      logger.info("{}@{}: process {}", this.getClass().getSimpleName(), this.hashCode(), data);
      if (predicateSource == null || runPredicate(data)) {
        if (processSource != null) {
          runProcess(data);
        }
        return chain.handle(data);
      } else {
        return Future.succeededFuture();
      }
    } catch(Throwable ex) {
      logger.error("Failed to process {}: ", data, ex);
      return Future.failedFuture(ex);
    }
  }
  
  private boolean runPredicate(JsonObject data) {
    return runSource("predicate", definition.getLanguage(), predicateSource, data, (v, jo) -> {
      return v.asBoolean();
    });
  }

  private void runProcess(JsonObject data) {
    runSource("process", definition.getLanguage(), processSource, data, (v, jo) -> {
      jo.forEach(e -> {
        if (e.getValue() instanceof Value) {
          e.setValue(mapToNativeObject((Value) e.getValue()));
        }
      });
      return null;
    });
  }
  
  static <T> T runSource(String name, String language, Source source, JsonObject data, BiFunction<Value, JsonObject, T> postProcess) {
    try (org.graalvm.polyglot.Context context = org.graalvm.polyglot.Context.newBuilder(language).option("engine.WarnInterpreterOnly", "false").build()) {
      Value bindings = context.getBindings(language);
      bindings.putMember("data", ProxyObject.fromMap(data.getMap()));
      Value outputValue = context.eval(source);    
      T result = postProcess.apply(outputValue, data);
      logger.debug("Running {} {} gave {}", name, source.getCharacters(), result);
      return result;
    } catch(Throwable ex) {
      logger.warn("Failed to evaluate {}: ", name, ex);
      return null;
    }
  }
  
  static Object mapToNativeObject(Value value) {
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
    } else if (value.isTimeZone()) {
      return value.asTimeZone();
    } else if (value.isDuration()) {
      return value.asDuration();
    } else if (value.isHostObject()) {
      return value.asHostObject();
    } else if (value.isNativePointer()) {
      return value.asNativePointer();
    } else if (value.isException()) {
      return value.throwException();
    } 
    logger.warn("Unknown value type: {}", value);
    return null;
  }
  
  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline) {
    if (!Strings.isNullOrEmpty(definition.getPredicate())) {
      predicateSource = Source.create(definition.getLanguage(), definition.getPredicate());
    }
    if (!Strings.isNullOrEmpty(definition.getProcess())) {
      processSource = Source.create(definition.getLanguage(), definition.getProcess());
    }    
    return Future.succeededFuture();
  }

  @Override
  public ReadStream<JsonObject> getReadStream() {
    return stream.readStream();
  }

  @Override
  public WriteStream<JsonObject> getWriteStream() {
    return stream.writeStream();
  }  
    
}
