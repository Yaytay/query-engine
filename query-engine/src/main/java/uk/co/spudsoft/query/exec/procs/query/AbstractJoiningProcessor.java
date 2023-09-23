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
package uk.co.spudsoft.query.exec.procs.query;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.streams.WriteStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.DataRowStream;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.SourceInstance;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.procs.AsyncHandler;
import uk.co.spudsoft.query.exec.procs.PassthroughStream;
import uk.co.spudsoft.query.exec.procs.ProcessorFormat;

/**
 *
 * @author jtalbut
 */
public abstract class AbstractJoiningProcessor implements ProcessorInstance {

  private final Logger logger;
  protected final Vertx vertx;
  protected final SourceNameTracker sourceNameTracker;
  protected final Context context;
  
  private final String parentIdColumn;
  private final String childIdColumn;
  private final boolean innerJoin;

  private AsyncHandler<DataRow> currentParentChain;
  private boolean childEnded = false;
  private boolean parentEnded = false;
  private DataRow currentParentRow;
  private DataRow currentChildRow;
  private Promise<Void> currentParentPromise;
  private Promise<Void> currentChildPromise;
  
  private final PassthroughStream parentStream;
  private final PassthroughStream childStream;
  
  private final List<DataRow> currentChildRows;
  
  private Comparable<Object> lastSeenParentId;
  
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Be aware that the point of sourceNameTracker is to modify the context")
  public AbstractJoiningProcessor(Logger logger, Vertx vertx, SourceNameTracker sourceNameTracker, Context context, String parentIdColumn, String childIdColumn, boolean innerJoin) {
    this.logger = logger;
    this.vertx = vertx;
    this.sourceNameTracker = sourceNameTracker;
    this.context = context;
    this.parentIdColumn = parentIdColumn;
    this.childIdColumn = childIdColumn;
    this.innerJoin = innerJoin;
    
    this.parentStream = new PassthroughStream(sourceNameTracker, this::parentStreamProcess, context);
    this.childStream = new PassthroughStream(sourceNameTracker, this::childStreamProcess, context);
    childStream.readStream().endHandler(v -> {
      logger.info("Child read stream ending");
      synchronized (this) {
        childEnded = true;
      }
      processCurrent();
    });
    childStream.writeStream().exceptionHandler(ex -> {
      sourceNameTracker.addNameToContextLocalData(context);
      logger.error("Processing failed: ", ex);
    });
    parentStream.endHandler(v -> {
      logger.info("Parent stream ending");
      synchronized (this) {
        parentEnded = true;
      }
      processCurrent();
    });
    this.currentChildRows = new ArrayList<>();    
  }
  
  protected Future<Void> initializeChildStream(PipelineExecutor executor, PipelineInstance pipeline, String parentSource, int processorIndex, SourcePipeline sourcePipeline) {
    SourceInstance sourceInstance = sourcePipeline.getSource().createInstance(vertx, context, executor, parentSource + "-" + processorIndex);
    PipelineInstance childPipeline = new PipelineInstance(
            pipeline.getArguments()
            , pipeline.getSourceEndpoints()
            , null
            , sourceInstance
            , executor.createProcessors(vertx, sourceInstance, context, sourcePipeline)
            , new ProcessorFormat(childStream.writeStream())
    );
    return executor.initializePipeline(childPipeline);
  }
  
  @SuppressWarnings({"unchecked", "rawtypes"})
  Comparable<Object> getId(DataRow row, String idField) {
    if (row == null) {
      return null;
    }
    Object parentIdObject = row.get(idField);
    if (parentIdObject instanceof Comparable comparable) {
      return comparable;
    }
    Map<String, Object> rowAsMap = row.getMap();
    if (rowAsMap != null && !rowAsMap.containsKey(idField)) {
      logger.warn("The row does not contain the ID field {}, is the configuration wrong?  The known fields are {}", idField, rowAsMap.keySet());
    }
    return null;    
  }
  
  protected Future<Void> parentStreamProcess(DataRow data, AsyncHandler<DataRow> chain) {
    try {
      sourceNameTracker.addNameToContextLocalData(context);
      logger.trace("parent process {}", data);
      Promise<Void> result = Promise.promise();
      synchronized (this) {
        currentParentPromise = result;
        currentParentRow = data;
        currentParentChain = chain;
      }
      processCurrent();
      return result.future();
    } catch (Throwable ex) {
      logger.error("Failed to process {}: ", data, ex);
      return Future.failedFuture(ex);
    }
  }

  protected Future<Void> childStreamProcess(DataRow data, AsyncHandler<DataRow> chain) {
    try {
      sourceNameTracker.addNameToContextLocalData(context);
      logger.trace("child process {}", data);
      Promise<Void> result = Promise.promise();
      synchronized (this) {
        currentChildRow = data;
        currentChildPromise = result;
      }
      processCurrent();
      return result.future();
    } catch (Throwable ex) {
      logger.error("Failed to process {}: ", data, ex);
      return Future.failedFuture(ex);
    }
  }
  
  protected void addChildMetadata(DataRow parentRow, DataRow childRow) {
  }
  
  private void processCurrent() {
    Comparable<Object> parentId;
    Comparable<Object> childId;
    sourceNameTracker.addNameToContextLocalData(context);
    synchronized (this) {
      logger.trace("processCurrent({}, {}, {})", currentParentRow, currentChildRow, currentChildRows);
      if (currentParentRow == null) {
        // We have a child row that hasn't been added to the list yet
        if (currentChildRow != null) {
          // If this child row is for a different parent row then we have to wait until we get a parent in
          if (currentChildRows.isEmpty() || getId(currentChildRows.get(0), childIdColumn).equals(getId(currentChildRow, childIdColumn))) {
            currentChildRows.add(currentChildRow);
            currentChildRow = null;
            Promise<Void> promise = currentChildPromise;
            currentChildPromise = null;
            promise.complete();
          } else if (parentEnded && (lastSeenParentId == null || getId(currentChildRow, childIdColumn).compareTo(lastSeenParentId) > 0)) {
            currentChildRow = null;
            currentChildPromise.complete();
            currentChildPromise = null;
          }
        }
        // Not received parent row yet, just wait for it.
        return ;
      }
      if (currentParentRow.isEmpty()) {
        if (currentChildRow != null) {
          DataRow row;
          AsyncHandler<DataRow> chain;
          synchronized (this) {
            addChildMetadata(currentParentRow, currentChildRow);
            chain = currentParentChain;
            row = currentParentRow;
          }
          chain(chain, row, null);
        } else {
          sourceNameTracker.addNameToContextLocalData(context);
          logger.trace("Received empty parent row, waiting for child row");
        }
        return ;
      } else {
        parentId = getId(currentParentRow, parentIdColumn);      
        lastSeenParentId = parentId;
        if (currentChildRow == null) {
          if (childEnded) {
            if (!currentChildRows.isEmpty()) {
              processCurrentChildrenAndChain(parentId);
              return ;
            }
          } else {
            // Not received latest child row yet
            return ;
          }
        }
      }
      childId = getId(currentChildRow, childIdColumn);
    }
    int compareResult = childId == null ? 1 : childId.compareTo(parentId);
    if (compareResult < 0) {
      // Parent row is ahead of this child - implies that some parent rows are missing so skip the child rows
      synchronized (this) {
        currentChildRow = null;
        currentChildPromise.complete();
        currentChildPromise = null;
      }
    } else if (compareResult == 0) {
      // Parent row is for this child
      synchronized (this) {
        currentChildRows.add(currentChildRow);
        currentChildRow = null;
        currentChildPromise.complete();
        currentChildPromise = null;
      }
    } else {
      // Parent row is behind this child
      processCurrentChildrenAndChain(parentId);
    }
  }

  protected void processCurrentChildrenAndChain(Comparable<Object> parentId) {
    AsyncHandler<DataRow> chain;
    DataRow row;
    synchronized (this) {
      processChildren(currentParentRow, currentChildRows);
      chain = currentParentChain;
      row = currentParentRow;
    }
    chain(chain, row, parentId);
  }

  private void chain(AsyncHandler<DataRow> chain, DataRow row, Comparable<Object> parentId) {
    if (chain != null) {
      chain.handle(row)
              .onComplete(v -> {
                sourceNameTracker.addNameToContextLocalData(context);
                Promise<Void> parentPromise;
                Promise<Void> childPromise;
                synchronized (this) {
                  if (parentId == null) {
                    logger.trace("Chained empty row");
                  } else {
                    logger.trace("Clearing data for {} now", parentId);
                  }
                  currentChildRows.clear();
                  parentPromise = currentParentPromise;
                  currentParentPromise = null;
                  currentParentRow = null;
                  childPromise = currentChildPromise;
                  currentChildPromise = null;
                  if (currentChildRow != null) {
                    currentChildRows.add(currentChildRow);
                  }
                  currentChildRow = null;
                }
                if (childPromise != null) {
                  childPromise.tryComplete();
                }
                if (parentPromise != null) {
                  parentPromise.complete();
                }
              });
    }
  }

  protected abstract void processChildren(DataRow parentRow, List<DataRow> childRows);
  
  @Override
  public WriteStream<DataRow> getWriteStream() {
    return parentStream.writeStream();
  }  

  @Override
  public DataRowStream<DataRow> getReadStream() {
    return parentStream.readStream();
  }
  
}
