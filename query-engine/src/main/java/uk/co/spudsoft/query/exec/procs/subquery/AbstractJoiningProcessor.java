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
import java.util.List;
import org.slf4j.Logger;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.SourceInstance;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.fmts.FormatCaptureInstance;

/**
 * Abstract implementation for {@link ProcessorDynamicFieldInstance} and {@link ProcessorGroupConcatInstance}.
 * 
 * @author jtalbut
 */
public abstract class AbstractJoiningProcessor implements ProcessorInstance {

  private final Logger logger;
  /**
   * The Vertx instance.
   */
  protected final Vertx vertx;
  /**
   * Source name tracker used to identify source names in log messages.
   */
  protected final SourceNameTracker sourceNameTracker;
  /**
   * Vertx context used by this class.
   */
  protected final Context context;
  
  /**
   * The name to use in logs for this processor.
   */
  private final String name;
  
  private final List<String> parentIdColumns;
  private final List<String> childIdColumns;
  private final boolean innerJoin;
  
  private MergeStream<DataRow, DataRow, DataRow> stream;
  
  /**
   * The Types captured during {@link #initialize(uk.co.spudsoft.query.exec.PipelineExecutor, uk.co.spudsoft.query.exec.PipelineInstance, java.lang.String, int, uk.co.spudsoft.query.exec.ReadStreamWithTypes)}.
   */
  protected Types types;
  
  /**
   * Constructor.
   * @param logger  The logger that should be used (so that log messages identify the child class).
   * @param vertx The vertx instance.
   * @param sourceNameTracker Source name tracker used to identify source names in log messages
   * @param context Vertx context used by this class.
   * @param name The name to use in logs for this processor - not nullable.
   * @param parentIdColumns The columns from the parent dataset that identifies a row.
   * @param childIdColumns The columns from the child dataset that identifies a row.
   * @param innerJoin If true parent rows without child rows will be excluded.
   * 
   * It is safe to suppress the "this-escape" lint check as long as none of the streams are flowing until the constructor completes.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Be aware that the point of sourceNameTracker is to modify the context")  
  @SuppressWarnings("this-escape")
  public AbstractJoiningProcessor(Logger logger, Vertx vertx, SourceNameTracker sourceNameTracker, Context context, String name, List<String> parentIdColumns, List<String> childIdColumns, boolean innerJoin) {
    this.logger = logger;
    this.vertx = vertx;
    this.sourceNameTracker = sourceNameTracker;
    this.context = context;
    this.name = name;
    this.parentIdColumns = parentIdColumns;
    this.childIdColumns = childIdColumns;
    this.innerJoin = innerJoin;
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * Initialize a child stream.
   * <p>
   * Helper method for subclasses.
   * 
   * @param executor The executor to use for running the child stream.
   * @param pipeline The overall pipeline instance being run.
   * @param fieldName The name of the field in the parent processor, for tracking purposes.
   * @param sourcePipeline The child pipeline to initialize.
   * @return A Future that will be completed with a {@link ReadStreamWithTypes} when initialization has completed.
   */
  protected Future<ReadStreamWithTypes> initializeChildStream(PipelineExecutor executor, PipelineInstance pipeline, String fieldName, SourcePipeline sourcePipeline) {
    SourceInstance sourceInstance = sourcePipeline.getSource().createInstance(vertx, context, executor, this.getName() + "." + fieldName);
    FormatCaptureInstance sinkInstance = new FormatCaptureInstance();
    
    PipelineInstance childPipeline = new PipelineInstance(
            pipeline.getArgumentInstances()
            , pipeline.getSourceEndpoints()
            , null
            , sourceInstance
            , executor.createProcessors(vertx, sourceInstance, context, sourcePipeline, null, name)
            , sinkInstance
    );
    return executor.initializePipeline(childPipeline)            
            .map(v -> sinkInstance.getReadStream());
  }

  /**
   * Abstract method that specializations must implement to process one parent row and a collection of related child rows.
   * @param parentRow The single DataRow from the parent stream.
   * @param childRows A list of matching DataRows from the child stream.
   * @return The resultant row - typically this is the modified ParentRow.
   */
  abstract DataRow processChildren(DataRow parentRow, List<DataRow> childRows);

  /**
   * Compare two DataRows by the {@link #parentIdColumns} and {@link #childIdColumns}.
   * @param parentRow The parent row.
   * @param childRow The child row.
   * @return  a negative integer, zero, or a positive integer as this object
   *          is less than, equal to, or greater than the specified object.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  protected int compare(DataRow parentRow, DataRow childRow) {
    for (int i = 0; i < parentIdColumns.size(); ++i) {
      Comparable parentKeyItem = parentRow.get(parentIdColumns.get(i));
      Comparable childKeyItem = childRow.get(childIdColumns.get(i));
      
      if (childKeyItem != null) {
        if (parentKeyItem.getClass() != childKeyItem.getClass()) {
          // Items are not of compatible types, if possible, extend them to the larger
          DataType parentType = parentRow.getType(parentIdColumns.get(i));
          DataType childType = childRow.getType(childIdColumns.get(i));
          DataType target = parentType.commonType(childType);
          if (target != parentType) {
            try {
              parentKeyItem = target.cast(parentKeyItem);
            } catch (Exception ex) {
              logger.warn("parentKeyItem {}/{}:{} cannot be converted to {}", parentIdColumns.get(i), parentType, parentKeyItem, target);
            }
          }
          if (target != childType) {
            try {
              childKeyItem = target.cast(childKeyItem);
            } catch (Exception ex) {
              logger.warn("childKeyItem {}/{}:{} cannot be converted to {}", childIdColumns.get(i), childType, childKeyItem, target);
            }
          }
        }
      }
      
      int compareResult = parentKeyItem.compareTo(childKeyItem);
      if (compareResult != 0) {
        return compareResult;
      }
    }
    return 0;
  }

  /**
   * Abstract method that specializations must implement to initialize the child streams.
   * <p>
   * Typically this will result in one or more calls to {@link #initializeChildStream(uk.co.spudsoft.query.exec.PipelineExecutor, uk.co.spudsoft.query.exec.PipelineInstance, java.lang.String, int, uk.co.spudsoft.query.defn.SourcePipeline)}.
   * @param executor The executor to use for running the child stream.
   * @param pipeline The overall pipeline instance being run.
   * @param parentSource The name of the parent source, for tracking purposes.
   * @param processorIndex The index of this processor, for tracking purposes.
   * @return A Future that will be completed with a {@link io.vertx.core.streams.ReadStream}&lt;{@link uk.co.spudsoft.query.exec.DataRow}@gt; when initialization has completed.
   */
  abstract Future<ReadStream<DataRow>> initializeChild(PipelineExecutor executor, PipelineInstance pipeline, String parentSource, int processorIndex);
  
  @Override
  public Future<ReadStreamWithTypes> initialize(PipelineExecutor executor, PipelineInstance pipeline, String parentSource, int processorIndex, ReadStreamWithTypes input) {
    
    if (parentIdColumns.size() != childIdColumns.size()) {
      return Future.failedFuture(new IllegalArgumentException("Incompatible parent ID columns (" + parentIdColumns.size() + ") and child ID columns (" + childIdColumns.size() + ")"));
    }
    this.types = input.getTypes();
    return initializeChild(executor, pipeline, parentSource, processorIndex)
            .compose(childStream -> {
              this.stream = new MergeStream<>(
                      context 
                      , input.getStream()
                      , childStream
                      , this::processChildren
                      , this::compare
                      , innerJoin
                      , 100
                      , 50
                      , 200
                      , 100
              );
              return Future.succeededFuture(new ReadStreamWithTypes(stream, types));
            })
            ;            
  }
  
}
  
