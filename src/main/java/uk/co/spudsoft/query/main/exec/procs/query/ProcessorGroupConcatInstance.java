package uk.co.spudsoft.query.main.exec.procs.query;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.main.defn.Endpoint;
import uk.co.spudsoft.query.main.defn.ProcessorGroupConcat;
import uk.co.spudsoft.query.main.exec.ProcessorInstance;
import uk.co.spudsoft.query.main.exec.SourceInstance;
import uk.co.spudsoft.query.main.exec.procs.PassthroughStream;
import uk.co.spudsoft.query.main.exec.procs.PassthroughWriteStream;

/**
 *
 * @author jtalbut
 */
public class ProcessorGroupConcatInstance implements ProcessorInstance<ProcessorGroupConcat> {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ProcessorGroupConcatInstance.class);
  
  private final Vertx vertx;
  private final Context context;
  private final ProcessorGroupConcat definition;
  private final PassthroughStream<JsonObject> parentStream;
  private final PassthroughWriteStream<JsonObject> childStream;
  private final List<JsonObject> currentChildRows;
  
  private PassthroughStream.AsyncProcessor<JsonObject> currentParentChain;
  private boolean parentEnded = false;
  private boolean childEnded = false;
  private JsonObject currentParentRow;
  private JsonObject currentChildRow;
  private Promise<Void> currentParentPromise;
  private Promise<Void> currentChildPromise;

  public ProcessorGroupConcatInstance(Vertx vertx, Context context, ProcessorGroupConcat definition) {
    this.vertx = vertx;
    this.context = context;
    this.definition = definition;
    this.parentStream = new PassthroughStream<>(this::parentStreamProcess, context);
    parentStream.readStream().endHandler(v -> {
      synchronized(this) {
        parentEnded = true;
      }
      processCurrent();
    });
    this.childStream = new PassthroughWriteStream<>(this::childStreamProcess, context);
    childStream.endHandler(v -> {
      synchronized(this) {
        childEnded = true;
      }
      processCurrent();
    });
    this.currentChildRows = new ArrayList<>();
  }
  
  private Future<Void> parentStreamProcess(JsonObject data, PassthroughStream.AsyncProcessor<JsonObject> chain) {
    try {
      logger.info("process {}", data);
      Promise<Void> result = Promise.promise();
      synchronized(this) {
        if (currentParentRow != null) {
          logger.warn("WTF: {}, {}, {}", currentParentRow, currentParentPromise, currentParentChain);
        }
        currentParentPromise = result;
        currentParentRow = data;
        currentParentChain = chain;
      }
      processCurrent();
      return result.future();
    } catch(Throwable ex) {
      logger.error("Failed to process {}: ", data, ex);
      return Future.failedFuture(ex);
    }
  }
  
  private Future<Void> childStreamProcess(JsonObject data) {
    try {
      logger.info("child process {}", data);
      Promise<Void> result = Promise.promise();
      synchronized(this) {
        currentChildRow = data;
        currentChildPromise = result;
      }
      processCurrent();
      return result.future();
    } catch(Throwable ex) {
      logger.error("Failed to process {}: ", data, ex);
      return Future.failedFuture(ex);
    }
  }
  
  @SuppressWarnings("unchecked")
  private Comparable<Object> getId(JsonObject row, String idField) {
    if (row == null) {
      return null;
    }
    Object parentIdObject = row.getValue(idField);
    if (parentIdObject instanceof Comparable comparable) {
      return comparable;
    }
    return null;    
  }
  
  private void processCurrent() {
    Comparable<Object> parentId;
    Comparable<Object> childId;
    synchronized(this) {
      logger.debug("processCurrent({}, {}, {})", currentParentRow, currentChildRow, currentChildRows);
      if (currentParentRow == null) {
        if (currentChildRow != null) {
          if (!currentChildRows.isEmpty()) {
            if (getId(currentChildRows.get(0), definition.getIdColumn()).equals(getId(currentChildRow, definition.getIdColumn()))) {
              currentChildRows.add(currentChildRow);
              currentChildRow = null;
              Promise<Void> promise = currentChildPromise;
              currentChildPromise = null;
              promise.complete();
            }
          }
        }
        if (parentEnded) {
          logger.debug("Parent ended");
        } else {
          // Not received first parent row yet, just wait for it.
          logger.debug("Parent not ended");
          return ;
        }
      }
      parentId = getId(currentParentRow, definition.getParentId());
      if (currentChildRow == null) {
        if (childEnded) {
          logger.debug("Child ended");
          if (!currentChildRows.isEmpty() && currentParentRow != null) {
            processCurrentChildren(parentId);
            return ;
          }
        } else {
          // Not received latest child row yet
          logger.debug("Child not ended");
          return ;
        }
      }
      childId = getId(currentChildRow, definition.getIdColumn());
    }
    int compareResult = childId.compareTo(parentId);
    if (compareResult < 0) {
      // Parent row is ahead of this child
      synchronized(this) {
        currentChildRow = null;
        currentChildPromise.complete();
        currentChildPromise = null;
      }
    } else if (compareResult == 0) {
      // Parent row is for this child
      synchronized(this) {
        currentChildRows.add(currentChildRow);
        currentChildRow = null;
        currentChildPromise.complete();
        currentChildPromise = null;
      }
    } else {
      // Parent row is behind this child
      processCurrentChildren(parentId);
    }
  }

  private void processCurrentChildren(Comparable<Object> parentId) {
    PassthroughStream.AsyncProcessor<JsonObject> chain;
    JsonObject row;
    synchronized(this) {
      logger.debug("Got child rows: {}", currentChildRows);
      String result = currentChildRows.stream()
              .map(r -> r.getValue(definition.getChildColumnName()))
              .filter(o -> o != null)
              .map(o -> o.toString())
              .collect(Collectors.joining(definition.getDelimiter()));
      currentParentRow.put(definition.getColumnName(), result);
      chain = currentParentChain;
      row = currentParentRow;
    }
    if (chain != null) {
      chain.handle(row)
              .onComplete(v -> {
                Promise<Void> parentPromise;
                Promise<Void> childPromise;
                synchronized(this) {
                  logger.debug("Clearing data for {} now", parentId);
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
                  childPromise.complete();
                }
                if (parentPromise != null) {
                  parentPromise.complete();
                }
              });
    }
  }
  
  @Override
  public Future<Void> initialize(Map<String, Endpoint> endpoints) {
    SourceInstance<?> source = definition.getSource().createInstance(vertx, context);
    return source.initialize(endpoints)
            .compose(v -> {
              source.getReadStream().pipeTo(childStream.writeStream());
              return Future.succeededFuture();
            });
  }

  @Override
  public ReadStream<JsonObject> getReadStream() {
    return parentStream.readStream();
  }

  @Override
  public WriteStream<JsonObject> getWriteStream() {
    return parentStream.writeStream();
  }  
  
}
