/*
 * Copyright (C) 2024 jtalbut
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
import io.vertx.core.Vertx;
import io.vertx.core.streams.ReadStream;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.defn.ProcessorExpression;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.conditions.JexlEvaluator;
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.exec.procs.query.FilteringStream;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;

/**
 * Use a JEXL expression to set a field, or to evaluate a predicate, on each row.
 * 
 * @author jtalbut
 */
public class ProcessorExpressionInstance implements ProcessorInstance {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ProcessorScriptInstance.class);
  
  private static final ZoneId UTC = ZoneId.of("UTC");
  
  private final String name;
  private final SourceNameTracker sourceNameTracker;
  private final ProcessorExpression definition;
  private ReadStream<DataRow> stream;
  
  private RequestContext requestContext;
  private final ImmutableMap<String, Object> arguments;
  
  private Types types;
  
  private JexlEvaluator predicate;
  private JexlEvaluator field;
  
    
  /**
   * Constructor.
   * @param vertx the Vert.x instance.
   * @param sourceNameTracker the name tracker used to record the name of this source at all entry points for logger purposes.
   * @param context the Vert.x context.
   * @param definition the definition of this processor.
   * @param name the name of this processor, used in tracking and logging.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Be aware that the point of sourceNameTracker is to modify the context")
  public ProcessorExpressionInstance(Vertx vertx, SourceNameTracker sourceNameTracker, Context context, ProcessorExpression definition, String name) {
    this.sourceNameTracker = sourceNameTracker;
    this.definition = definition;
    this.arguments = ImmutableCollectionTools.copy(requestContext == null ? null : requestContext.getArguments());
    this.name = name;
  }
  
  @Override
  public String getName() {
    return name;
  }

  private boolean runPredicate(DataRow data) {
    sourceNameTracker.addNameToContextLocalData();
    return predicate.evaluate(requestContext, data);
  }
  
  private DataRow runFieldSet(DataRow data) {
    sourceNameTracker.addNameToContextLocalData();
    Object result = field.evaluateAsObject(requestContext, data);
    Comparable<?> typedResult;
    try {
      typedResult = definition.getFieldType().cast(result);
      data.put(definition.getField(), definition.getFieldType(), typedResult);
    } catch (Throwable ex) {
      logger.warn("Expression evaluation resulted in {} ({}): ", result, result.getClass(), ex);
    }
    return data;
  }
  
  @Override
  public Future<ReadStreamWithTypes> initialize(PipelineExecutor executor, PipelineInstance pipeline, String parentSource, int processorIndex, ReadStreamWithTypes input) {
    this.requestContext = pipeline.getRequestContext();
    this.types = input.getTypes();
    this.stream = input.getStream();
    
    if (!Strings.isNullOrEmpty(definition.getField())) {
      DataType existingType = types.get(definition.getField());
      if (existingType == null) {
        types.putIfAbsent(definition.getField(), definition.getFieldType());
      } else {
        if (existingType != DataType.Null && existingType != definition.getFieldType()) {
          IllegalArgumentException ex = new IllegalArgumentException("Attempt to change type of field " + definition.getField() + " from " + existingType + " to " + definition.getFieldType());
          return Future.failedFuture(ex);
        }
      }
    }
 
    this.stream = input.getStream();
    if (!Strings.isNullOrEmpty(definition.getPredicate())) {
      predicate = new JexlEvaluator(definition.getPredicate());
      stream = new FilteringStream<>(stream, this::runPredicate);
    }
    if (!Strings.isNullOrEmpty(definition.getField())) {
      field = new JexlEvaluator(definition.getFieldValue());
      stream = new MappingStream<>(stream, this::runFieldSet);
    }
    
    return Future.succeededFuture(new ReadStreamWithTypes(stream, types));
  }

}
