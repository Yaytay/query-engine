/*
 * Copyright (C) 2025 jtalbut
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

import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.streams.ReadStream;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.ProcessorMerge;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.Types;

/**
 * {@link uk.co.spudsoft.query.exec.ProcessorInstance} that acts similarly to the MySQL <a href="https://dev.mysql.com/doc/refman/8.0/en/aggregate-functions.html#function_group-concat">GROUP_CONCAT</A> aggregate function.
 * 
 * A sub query is run and merged with the primary query.
 * The join is always a merge join, so the primary query must be sorted by the {@link uk.co.spudsoft.query.defn.ProcessorMerge#parentIdColumns} and the sub query 
 * must be sorted by the {@link uk.co.spudsoft.query.defn.ProcessorMerge#childIdColumns}.
 * Every field from the child stream is added to the primary stream.
 * If there are multiple child rows matching a single parent row only the first will be processed.
 * 
 * 
 * @author jtalbut
 */
 public class ProcessorMergeInstance extends AbstractJoiningProcessor {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ProcessorMergeInstance.class);
  
  private final ProcessorMerge definition;
  private final Set<String> childIdColumns;
  private Types childTypes;
  
  /**
   * Constructor.
   * @param vertx the Vert.x instance.
   * @param sourceNameTracker the name tracker used to record the name of this source at all entry points for logger purposes.
   * @param context the Vert.x context.
   * @param definition the definition of this processor.
   * @param name the name of this processor, used in tracking and logging.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Be aware that the point of sourceNameTracker is to modify the context")
  public ProcessorMergeInstance(Vertx vertx, SourceNameTracker sourceNameTracker, Context context, ProcessorMerge definition, String name) {
    super(logger, vertx, sourceNameTracker, context, name, definition.getParentIdColumns(), definition.getChildIdColumns(), definition.isInnerJoin());
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
              childTypes = rswt.getTypes();
              childTypes.forEach(cd -> {
                if (!childIdColumns.contains(cd.name())) {
                  types.putIfAbsent(cd.name(), cd.type());
                }
              });
              return Future.succeededFuture(rswt.getStream());
            });
  }

  @Override
  protected DataRow processChildren(DataRow parentRow, List<DataRow> childRows) {
    logger.debug("Got child rows: {}", childRows);
    
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
    if (!childRows.isEmpty()) {
      DataRow childRow = childRows.get(0);
      childTypes.forEach(cd -> {
        if (!childIdColumns.contains(cd.name())) {
          parentRow.put(cd.name(), childRow.get(cd.name()));
        }
      });
    }
    return parentRow;
  }
 
}
