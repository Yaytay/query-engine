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
import io.github.tsegismont.streamutils.impl.MappingStream;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.streams.ReadStream;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.defn.ProcessorExpression;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.exec.Auditor;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes; 
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.conditions.JexlEvaluator;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.procs.AbstractProcessor;
import uk.co.spudsoft.query.exec.procs.query.FilteringStream;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;

/**
 * Use a JEXL expression to set a field, or to evaluate a predicate, on each row.
 *
 * @author jtalbut
 */
public class ProcessorExpressionInstance extends AbstractProcessor {

  @SuppressWarnings("constantname")
  private static final Logger slf4jlogger = LoggerFactory.getLogger(ProcessorScriptInstance.class);

  private static final ZoneId UTC = ZoneId.of("UTC");

  private final ProcessorExpression definition;
  private ReadStream<DataRow> stream;

  private final ImmutableMap<String, Object> arguments;

  private Types types;

  private JexlEvaluator predicate;
  private JexlEvaluator field;


  /**
   * Constructor.
   * @param vertx the Vert.x instance.
   * @param meterRegistry MeterRegistry for production of metrics.
   * @param auditor The auditor that the source should use for recording details of the data accessed.
   * @param pipelineContext The context in which this {@link SourcePipeline} is being run.
   * @param definition the definition of this processor.
   * @param name the name of this processor, used in tracking and logging.
   */
  public ProcessorExpressionInstance(Vertx vertx, MeterRegistry meterRegistry, Auditor auditor, PipelineContext pipelineContext, ProcessorExpression definition, String name) {
    super(slf4jlogger, vertx, meterRegistry, auditor, pipelineContext, name);
    this.definition = definition;
    this.arguments = ImmutableCollectionTools.copy(pipelineContext.getRequestContext() == null ? null : pipelineContext.getRequestContext().getArguments());
  }

  private boolean runPredicate(DataRow data) {
    return predicate.evaluate(pipelineContext.getRequestContext(), data);
  }

  private DataRow runFieldSet(DataRow data) {
    Object result = field.evaluateAsObject(pipelineContext.getRequestContext(), data);
    Comparable<?> typedResult;
    try {
      typedResult = definition.getFieldType().cast(pipelineContext, result);
      data.put(definition.getField(), definition.getFieldType(), typedResult);
    } catch (Throwable ex) {
      logger.warn().log("Expression evaluation resulted in {} ({}): ", result, result.getClass(), ex);
    }
    return data;
  }

  @Override
  public Future<ReadStreamWithTypes> initialize(PipelineExecutor executor, PipelineInstance pipeline, String parentSource, int processorIndex, ReadStreamWithTypes input) {
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
      stream = new FilteringStream<>(pipelineContext, stream, this::runPredicate);
    }
    if (!Strings.isNullOrEmpty(definition.getField())) {
      field = new JexlEvaluator(definition.getFieldValue());
      stream = new MappingStream<>(stream, this::runFieldSet);
    }

    return Future.succeededFuture(new ReadStreamWithTypes(stream, types));
  }

}
