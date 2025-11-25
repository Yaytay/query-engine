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
import io.micrometer.core.instrument.MeterRegistry;
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
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.exec.Auditor;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.SourceInstance;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.fmts.FormatCaptureInstance;
import uk.co.spudsoft.query.exec.fmts.ReadStreamToList;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;

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
  private static final Logger slf4jlogger = LoggerFactory.getLogger(ProcessorDynamicFieldInstance.class);

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

  /**
   * List of fields discovered during initialization.
   * This is only protected for the benefit of tests, this class should be considered final for production use.
   */
  protected ImmutableList<FieldDefn> fields; // Protected for the benefit of unit tests only

  /**
   * Constructor.
   * @param vertx the Vert.x instance.
   * @param meterRegistry MeterRegistry for production of metrics.
   * @param auditor The auditor that the source should use for recording details of the data accessed.
   * @param pipelineContext The context in which this {@link SourcePipeline} is being run.
   * @param definition the definition of this processor.
   * @param name the name of this processor, used in tracking and logging.
   */
  public ProcessorDynamicFieldInstance(Vertx vertx, MeterRegistry meterRegistry, Auditor auditor, PipelineContext pipelineContext, ProcessorDynamicField definition, String name) {
    super(slf4jlogger, vertx, meterRegistry, auditor, pipelineContext, name, definition.getParentIdColumns(), definition.getValuesParentIdColumns(), definition.isInnerJoin());
    this.definition = definition;
    if (Strings.isNullOrEmpty(definition.getFieldValueColumnName())) {
      this.fieldValueColumnNames = Collections.emptyList();
    } else {
      this.fieldValueColumnNames = ImmutableList.copyOf(definition.getFieldValueColumnName().split(","));
    }
  }

  @Override
  Future<ReadStream<DataRow>> initializeChild(PipelineExecutor executor, PipelineInstance pipeline, String parentSource, int processorIndex) {

    String childName = getName() + ".fieldDefns";
    PipelineContext childContext = pipeline.getPipelineContext().child(childName);
    
    SourceInstance sourceInstance = definition.getFieldDefns().getSource().createInstance(vertx, meterRegistry, auditor, childContext, executor);
    FormatCaptureInstance fieldDefnStreamCapture = new FormatCaptureInstance();
    PipelineInstance childPipeline = new PipelineInstance(
            childContext
            , pipeline.getDefinition()
            , pipeline.getArgumentInstances()
            , pipeline.getSourceEndpoints()
            , null
            , sourceInstance
            , executor.createProcessors(vertx, childContext, definition.getFieldDefns(), null)
            , fieldDefnStreamCapture
    );

    return executor.initializePipeline(childContext, childPipeline)
            .compose(v -> {
              return ReadStreamToList.map(pipelineContext
                      , fieldDefnStreamCapture.getReadStream().getStream()
                      , row -> {
                        return rowToFieldDefn(definition, row);
                      });
            })
            .compose(collated -> {
              fields = ImmutableCollectionTools.copy(collated);
              for (FieldDefn field : fields) {
                types.putIfAbsent(field.key, field.name, field.type);
              }
              if (slf4jlogger.isTraceEnabled()) {
                logger.trace().log("Defined dynamic fields: {}", Json.encode(fields));
                logger.debug().log("Dynamic field types: {}", types);
              }
              return initializeChildStream(executor, pipeline, "fieldValues", definition.getFieldValues()).map(rswt -> rswt.getStream());
            });
  }

  FieldDefn rowToFieldDefn(ProcessorDynamicField definition, DataRow row) {
    if (row.isEmpty()) {
      return null;
    } else {
      Object id = row.get(definition.getFieldIdColumn());
      if (id == null) {
        logger.debug().log("Skipping field defn row ({}) with no id", row.toString());
        return null;
      }
      Object fieldNameObject = row.get(definition.getFieldNameColumn());
      if (fieldNameObject == null) {
        logger.debug().log("Skipping field defn row ({}) because field name column {} is null", row.toString(), definition.getFieldNameColumn());
        return null;
      }
      String fieldNameString = (fieldNameObject instanceof String s) ? s : fieldNameObject.toString();
      if (Strings.isNullOrEmpty(fieldNameString)) {
        logger.debug().log("Skipping field defn row ({}) with no name", row.toString());
        return null;
      }
      String key = definition.isUseCaseInsensitiveFieldNames() ? fieldNameString.toLowerCase(Locale.ROOT) : fieldNameString;
      Object fieldTypeObject = row.get(definition.getFieldTypeColumn());
      if (fieldTypeObject == null) {
        logger.debug().log("Skipping field defn row ({}) because field type column {} is null", row.toString(), definition.getFieldTypeColumn());
        return null;
      }
      String fieldTypeString = (fieldTypeObject instanceof String s) ? s : fieldTypeObject.toString();
      DataType type;
      try {
        type = DataType.valueOf(fieldTypeString);
      } catch (Throwable ex) {
        logger.debug().log("Skipping field defn row ({}) because type ({}) is not understood", row.toString(), fieldTypeString);
        return null;
      }
      Comparable<?> valueColumn = row.get(definition.getFieldColumnColumn());
      String column = valueColumn == null ? null : Objects.toString(valueColumn);
      return new FieldDefn(id, key, fieldNameString, type, column);
    }
  }

  @Override
  DataRow processChildren(DataRow parentRow, List<DataRow> childRows) {
    logger.trace().log("Got child rows: {}", childRows);
    if (parentRow == null) {
      logger.warn().log("No parentRow matching {}", childRows);
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
    logger.trace().log("Resulting row: {}", parentRow);
    return parentRow;
  }

  Comparable<?> castValue(Comparable<?> value, FieldDefn fieldDefn) {
    try {
      value = fieldDefn.type.cast(pipelineContext, value);
    } catch (Throwable ex) {
      logger.warn().log("Failed to cast field {} with value {} ({}) to {}", fieldDefn.key, value, value.getClass(), fieldDefn.type);
    }
    return value;
  }
}
