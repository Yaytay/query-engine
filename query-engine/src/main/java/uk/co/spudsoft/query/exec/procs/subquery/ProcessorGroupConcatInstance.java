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
package uk.co.spudsoft.query.exec.procs.subquery;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.streams.ReadStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.defn.ProcessorGroupConcat;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.exec.Auditor;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.context.PipelineContext;

/**
 * {@link uk.co.spudsoft.query.exec.ProcessorInstance} that acts similarly to the MySQL <a href="https://dev.mysql.com/doc/refman/8.0/en/aggregate-functions.html#function_group-concat">GROUP_CONCAT</A> aggregate function.
 *
 * A sub query is run and merged with the primary query.
 * The join is always a merge join, so the primary query must be sorted by the {@link uk.co.spudsoft.query.defn.ProcessorGroupConcat#parentIdColumns} and the sub query
 * must be sorted by the {@link uk.co.spudsoft.query.defn.ProcessorGroupConcat#childIdColumns}.
 *
 *
 *
 * @author jtalbut
 */
 public class ProcessorGroupConcatInstance extends AbstractJoiningProcessor {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ProcessorGroupConcatInstance.class);

  private final ProcessorGroupConcat definition;
  private final Set<String> childIdColumns;
  private Types childTypes;

  /**
   * Constructor.
   * @param vertx the Vert.x instance.
   * @param meterRegistry MeterRegistry for production of metrics.
   * @param auditor The auditor that the source should use for recording details of the data accessed.
   * @param pipelineContext The context in which this {@link SourcePipeline} is being run.
   * @param definition the definition of this processor.
   * @param name the name of this processor, used in tracking and logging.
   */
  public ProcessorGroupConcatInstance(Vertx vertx, MeterRegistry meterRegistry, Auditor auditor, PipelineContext pipelineContext, ProcessorGroupConcat definition, String name) {
    super(logger, vertx, meterRegistry, auditor, pipelineContext, name, definition.getParentIdColumns(), definition.getChildIdColumns(), definition.isInnerJoin());
    this.definition = definition;
    this.childIdColumns = ImmutableSet.copyOf(definition.getChildIdColumns());
  }

  /**
   * This exists solely for unit testing.
   * @param childTypes The Types for the child stream, usually set during initialisation.
   */
  void setChildTypes(Types childTypes) {
    this.childTypes = childTypes;
  }

  @Override
  Future<ReadStream<DataRow>> initializeChild(PipelineExecutor executor, PipelineInstance pipeline, String parentSource, int processorIndex) {

    return initializeChildStream(executor, pipeline, "input", definition.getInput())
            .compose(rswt -> {
              if (Strings.isNullOrEmpty(definition.getChildValueColumn())) {
                childTypes = rswt.getTypes();
                childTypes.forEach(cd -> {
                  if (!childIdColumns.contains(cd.name())) {
                    types.putIfAbsent(cd.name(), DataType.String);
                  }
                });
              } else if (Strings.isNullOrEmpty(definition.getParentValueColumn())) {
                types.putIfAbsent(definition.getChildValueColumn(), DataType.String);
              } else {
                types.putIfAbsent(definition.getParentValueColumn(), DataType.String);
              }
              return Future.succeededFuture(rswt.getStream());
            });
  }

  @Override
  protected DataRow processChildren(DataRow parentRow, List<DataRow> childRows) {
    logger.trace("Got child rows: {}", childRows);

    /**
     * Three options:
     * 1. childValueColumn && parentValueColumn
     *    One field to bring over and rename.
     * 2. childValueColumn && ! parentValueColumn
     *    One field to bring over without renaming.
     * 3. ! childValueColumn
     *    Bring over all child fields that aren't in the ID without renaming
     * Evaluated in reverse order :)
     */
    if (Strings.isNullOrEmpty(definition.getChildValueColumn())) {
      childTypes.forEach(cd -> {
        if (!childIdColumns.contains(cd.name())) {
          String result = childRows.stream()
                  .map(r -> r.get(cd.name()))
                  .filter(o -> o != null)
                  .map(o -> o.toString())
                  .collect(Collectors.joining(definition.getDelimiter()));
          parentRow.put(cd.name(), result);
        }
      });
    } else if (Strings.isNullOrEmpty(definition.getParentValueColumn())) {
      String result = childRows.stream()
              .map(r -> r.get(definition.getChildValueColumn()))
              .filter(o -> o != null)
              .map(o -> o.toString())
              .collect(Collectors.joining(definition.getDelimiter()));
      parentRow.put(definition.getChildValueColumn(), result);
    } else {
      String result = childRows.stream()
              .map(r -> r.get(definition.getChildValueColumn()))
              .filter(o -> o != null)
              .map(o -> o.toString())
              .collect(Collectors.joining(definition.getDelimiter()));
      parentRow.put(definition.getParentValueColumn(), result);
    }
    return parentRow;
  }

}
