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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.streams.ReadStream;
import java.util.Collection;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.defn.ProcessorGroupConcat;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.SourceNameTracker;

/**
 * {@link uk.co.spudsoft.query.exec.ProcessorInstance} that acts similarly to the MySQL <a href="https://dev.mysql.com/doc/refman/8.0/en/aggregate-functions.html#function_group-concat">GROUP_CONCAT</A> aggregate function.
 * 
 * A sub query is run and merged with the primary query.
 * The join is always a merge join, so the primary query must be sorted by the {@link uk.co.spudsoft.query.defn.ProcessorGroupConcat#parentIdColumns} and the sub query 
 * must be sorted by the {@link uk.co.spudsoft.query.defn.ProcessorGroupConcat#childIdColumns}.
 * The values from the {@link uk.co.spudsoft.query.defn.ProcessorGroupConcat#childValueColumn} column are joined to form a single string, using
 * {@link uk.co.spudsoft.query.defn.ProcessorGroupConcat#delimiter} as a delimiter.
 * 
 * 
 * @author jtalbut
 */
 public class ProcessorGroupConcatInstance extends AbstractJoiningProcessor {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ProcessorGroupConcatInstance.class);
  
  private final ProcessorGroupConcat definition;

  /**
   * Constructor.
   * @param vertx the Vert.x instance.
   * @param sourceNameTracker the name tracker used to record the name of this source at all entry points for logger purposes.
   * @param context the Vert.x context.
   * @param definition the definition of this processor.
   * @param name the name of this processor, used in tracking and logging.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Be aware that the point of sourceNameTracker is to modify the context")
  public ProcessorGroupConcatInstance(Vertx vertx, SourceNameTracker sourceNameTracker, Context context, ProcessorGroupConcat definition, String name) {
    super(logger, vertx, sourceNameTracker, context, name, definition.getParentIdColumns(), definition.getChildIdColumns(), definition.isInnerJoin());
    this.definition = definition;
  }

  @Override
  Future<ReadStream<DataRow>> initializeChild(PipelineExecutor executor, PipelineInstance pipeline, String parentSource, int processorIndex) {
    types.putIfAbsent(definition.getParentValueColumn(), DataType.String);
    return initializeChildStream(executor, pipeline, "input", definition.getInput());
  }  

  @Override
  protected DataRow processChildren(DataRow parentRow, Collection<DataRow> childRows) {
    logger.debug("Got child rows: {}", childRows);
    String result = childRows.stream()
            .map(r -> r.get(definition.getChildValueColumn()))
            .filter(o -> o != null)
            .map(o -> o.toString())
            .collect(Collectors.joining(definition.getDelimiter()));
    parentRow.put(definition.getParentValueColumn(), result);
    return parentRow;
  }
 
}
