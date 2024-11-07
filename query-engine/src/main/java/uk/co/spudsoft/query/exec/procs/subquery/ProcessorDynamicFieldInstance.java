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
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.defn.ProcessorDynamicField;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.SourceInstance;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.fmts.FormatCaptureInstance;
import uk.co.spudsoft.query.exec.fmts.ReadStreamToList;

/**
 * {@link uk.co.spudsoft.query.exec.ProcessorInstance} to generate fields dynamically from a query producing key/value pairs.
 * <P>
 * Configuration is via a {@link uk.co.spudsoft.query.defn.ProcessorDynamicField} that specifies three child pipelines.
 * Two of those pipelines are run to completion during initialisation, the third is run during the main flow.
 *
 * @author jtalbut
 */
public class ProcessorDynamicFieldInstance extends AbstractJoiningProcessor {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ProcessorDynamicFieldInstance.class);
  
  private final ProcessorDynamicField definition;

  private static class FieldDefn {
    public final Object id;
    public final String name;
    public final DataType type;
    public final String column;

    FieldDefn(Object id, String name, DataType type, String column) {
      this.id = id;
      this.name = name;
      this.type = type;
      this.column = column;
    }
  }
  
  private List<FieldDefn> fields;
  
  /**
   * Constructor.
   * @param vertx the Vert.x instance.
   * @param sourceNameTracker the name tracker used to record the name of this source at all entry points for logger purposes.
   * @param context the Vert.x context.
   * @param definition the definition of this processor.
   * @param name the name of this processor, used in tracking and logging.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Be aware that the point of sourceNameTracker is to modify the context")
  public ProcessorDynamicFieldInstance(Vertx vertx, SourceNameTracker sourceNameTracker, Context context, ProcessorDynamicField definition, String name) {
    super(logger, vertx, sourceNameTracker, context, name, definition.getParentIdColumns(), definition.getValuesParentIdColumns(), definition.isInnerJoin());
    this.definition = definition;    
  }

  @Override
  Future<ReadStream<DataRow>> initializeChild(PipelineExecutor executor, PipelineInstance pipeline, String parentSource, int processorIndex) {
    
    SourceInstance sourceInstance = definition.getFieldDefns().getSource().createInstance(vertx, context, executor, getName() + ".fieldDefns");
    FormatCaptureInstance fieldDefnStreamCapture = new FormatCaptureInstance();
    PipelineInstance childPipeline = new PipelineInstance(
            pipeline.getArgumentInstances()
            , pipeline.getSourceEndpoints()
            , null
            , sourceInstance
            , executor.createProcessors(vertx, sourceInstance, context, definition.getFieldDefns(), null, this.getName())
            , fieldDefnStreamCapture
    );
    
    return executor.initializePipeline(childPipeline)
            .compose(v -> {
              return ReadStreamToList.map(
                      fieldDefnStreamCapture.getReadStream()
                      , row -> {
                        if (row.isEmpty()) {
                          return null;
                        } else {
                          Object id = row.get(definition.getFieldIdColumn());
                          String name = Objects.toString(row.get(definition.getFieldNameColumn()));
                          DataType type = DataType.valueOf(Objects.toString(row.get(definition.getFieldTypeColumn())));
                          String column = Objects.toString(row.get(definition.getFieldColumnColumn()));              
                          return new FieldDefn(id, name, type, column);
                        }
                      });
            })
            .compose(collated -> {
              fields = collated;
              for (FieldDefn field : fields) {
                types.putIfAbsent(field.name, field.type);
              }
              return initializeChildStream(executor, pipeline, "fieldValues", definition.getFieldValues());
            });
  }
  
  @Override
  DataRow processChildren(DataRow parentRow, Collection<DataRow> childRows) {
    logger.debug("Got child rows: {}", childRows);
    if (parentRow == null) {
      logger.warn("No parentRow matching {}", childRows);
      return null;
    }
    for (FieldDefn fieldDefn : fields) {
      boolean added = false;
      parentRow.putTypeIfAbsent(fieldDefn.name, fieldDefn.type);
      for (DataRow row : childRows) {
        if (fieldDefn.id.equals(row.get(definition.getValuesFieldIdColumn()))) {
          parentRow.put(fieldDefn.name, row.get(fieldDefn.column));
          added = true;
          break;
        }
      }
      if (!added) {
        parentRow.put(fieldDefn.name, fieldDefn.type, null);
      }
    }
    logger.trace("Resulting row: {}", parentRow);
    return parentRow;
  }
}
