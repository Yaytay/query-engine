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
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.streams.ReadStream;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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

  static class FieldDefn {
    public final Object id;
    public final String key;
    public final String name;
    public final DataType type;
    public final String column;

    FieldDefn(Object id, String key, String name, DataType type, String column) {
      this.id = id;
      this.key = key;
      this.name = name;
      this.type = type;
      this.column = column;
    }
  }
    
  private final List<String> fieldValueColumnNames;
  private List<FieldDefn> fields; // Protected for the benefit of unit tests only
  
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
    this(vertx, sourceNameTracker, context, definition, name, null);
  }

  /**
   * Constructor.
   * @param vertx the Vert.x instance.
   * @param sourceNameTracker the name tracker used to record the name of this source at all entry points for logger purposes.
   * @param context the Vert.x context.
   * @param definition the definition of this processor.
   * @param name the name of this processor, used in tracking and logging.
   * @param fields override the collection of fields for testing
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Be aware that the point of sourceNameTracker is to modify the context")
  ProcessorDynamicFieldInstance(Vertx vertx, SourceNameTracker sourceNameTracker, Context context, ProcessorDynamicField definition, String name, List<FieldDefn> fields) {
    super(logger, vertx, sourceNameTracker, context, name, definition.getParentIdColumns(), definition.getValuesParentIdColumns(), definition.isInnerJoin());
    this.definition = definition;    
    if (Strings.isNullOrEmpty(definition.getFieldValueColumnName())) {
      this.fieldValueColumnNames = Collections.emptyList();
    } else {
      this.fieldValueColumnNames = ImmutableList.copyOf(definition.getFieldValueColumnName().split(","));
    }
    this.fields = fields;
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
                      fieldDefnStreamCapture.getReadStream().getStream()
                      , row -> {
                        if (row.isEmpty()) {
                          return null;
                        } else {
                          Object id = row.get(definition.getFieldIdColumn());
                          if (id == null) {
                            logger.debug("Skipping field defn row ({}) with no id", row.toString());
                            return null;
                          }
                          String name = Objects.toString(row.get(definition.getFieldNameColumn()));
                          if (Strings.isNullOrEmpty(name)) {
                            logger.debug("Skipping field defn row ({}) with no name", row.toString());
                            return null;
                          }
                          String key = definition.isUseCaseInsensitiveFieldNames() ? name.toLowerCase(Locale.ROOT) : name;
                          DataType type = DataType.valueOf(Objects.toString(row.get(definition.getFieldTypeColumn())));
                          if (type == null) {
                            logger.debug("Skipping field defn row ({}) because type ({}) is not understood", row.toString(), row.get(definition.getFieldTypeColumn()));
                            return null;
                          }
                          Comparable<?> valueColumn = row.get(definition.getFieldColumnColumn());
                          String column = valueColumn == null ? null : Objects.toString(valueColumn);
                          return new FieldDefn(id, key, name, type, column);
                        }
                      });
            })
            .compose(collated -> {
              fields = collated;
              for (FieldDefn field : fields) {
                types.putIfAbsent(field.key, field.name, field.type);
              }
              if (logger.isTraceEnabled()) {
                logger.trace("Defined dynamic fields: {}", Json.encode(fields));
                logger.trace("Dynamic field types: {}", types);
              }
              return initializeChildStream(executor, pipeline, "fieldValues", definition.getFieldValues()).map(rswt -> rswt.getStream());
            });
  }
  
  @Override
  DataRow processChildren(DataRow parentRow, List<DataRow> childRows) {
    logger.trace("Got child rows: {}", childRows);
    if (parentRow == null) {
      logger.warn("No parentRow matching {}", childRows);
      return null;
    }
    for (FieldDefn fieldDefn : fields) {
      boolean added = false;
      parentRow.putTypeIfAbsent(fieldDefn.key, fieldDefn.name, fieldDefn.type);
      for (DataRow row : childRows) {
        if (fieldDefn.id != null && fieldDefn.id.equals(row.get(definition.getValuesFieldIdColumn()))) {
          if (Strings.isNullOrEmpty(fieldDefn.column)) {
            for (String valueFieldName : this.fieldValueColumnNames) {
              Comparable<?> value = row.get(valueFieldName);
              if (value != null) {
                value = castValue(value, fieldDefn);
                parentRow.put(fieldDefn.key, fieldDefn.name, fieldDefn.type, value);
                break;
              }
            }
          } else {
            Comparable<?> value = row.get(fieldDefn.column);
            if (value != null) {
              value = castValue(value, fieldDefn);
            }
            parentRow.put(fieldDefn.key, fieldDefn.name, fieldDefn.type, value);
          }
          added = true;
          break;
        }
      }
      if (!added) {
        // Explicitly store null if nothing has already been written
        // If something has been written then either it's already null or it has a value that we can't improve on
        if (!parentRow.containsKey(fieldDefn.key)) {
          parentRow.put(fieldDefn.key, fieldDefn.name, fieldDefn.type, null);
        }
      }
    }
    logger.trace("Resulting row: {}", parentRow);
    return parentRow;
  }

  static Comparable<?> castValue(Comparable<?> value, FieldDefn fieldDefn) {
    try {
      value = fieldDefn.type.cast(value);
    } catch (Throwable ex) {
      logger.warn("Failed to cast field {} with value {} ({}) to {}", fieldDefn.key, value, value.getClass(), fieldDefn.type);
    }
    return value;
  }
}
