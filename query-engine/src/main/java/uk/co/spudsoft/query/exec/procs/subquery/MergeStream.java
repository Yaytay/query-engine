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
package uk.co.spudsoft.query.exec.procs.subquery;

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * {@link io.vertx.core.streams.ReadStream} implementation that takes two other {@link io.vertx.core.streams.ReadStream} instances and performs a merge join on them.
 * <P>
 * Both the source stream must be sorted by the comparator.
 * <P>
 * A merger function must be provided to combine a collection of objects of type U (from the secondary stream) into a single object of type T from the primary stream.
 * 
 * 
 * @author jtalbut
 * @param <T> the type of object in the primary stream.
 * @param <U> the type of object in the secondary stream.
 * @param <V> the type of object in the output stream.
 */
public class MergeStream<T, U, V> implements ReadStream<V> {
  
  private static final Logger logger = LoggerFactory.getLogger(MergeStream.class);
  
  private static final int MAX_ROWS_TO_BUFFER = 100;
  
  private final Object lock = new Object();
  
  private Handler<V> handler;
  private Handler<Throwable> exceptionHandler;
  private Handler<Void> endHandler;
  
  public interface BiComparator<T, U> {
    int compare(T p, U s);
  }
  
  private final Context context;
  private final ReadStream<T> primaryStream;
  private final ReadStream<U> secondaryStream;
  private final BiFunction<T, Collection<U>, V> merger;
  private final BiComparator<T, U> comparator;
  private final boolean innerJoin;

  private final int primaryStreamBufferHighThreshold;
  private final int primaryStreamBufferLowThreshold;
  private final int secondaryStreamBufferHighThreshold;
  private final int secondaryStreamBufferLowThreshold;
  
  private boolean secondaryEnded = false;
  private boolean primaryEnded = false;
  private T currentPrimary;
  private List<U> currentSecondaryRows;
  private final Deque<T> primaryRows = new ArrayDeque<>(MAX_ROWS_TO_BUFFER);
  private final Deque<U> secondaryRows = new ArrayDeque<>(MAX_ROWS_TO_BUFFER);
  
  private final AtomicBoolean emitting = new AtomicBoolean();
  private long demand;
  
  /**
   * Constructor.
   * @param context Vertx {@link io.vertx.core.Context} to run in.
   * @param primaryStream The primary stream, at most one item will be output for each item in this stream.
   * @param secondaryStream The second stream, to be matched against objects in the primary stream.
   * @param merger Function to use to combine a single object from the primary stream with a  collection of objects from the secondary stream into a single output object.
   * @param comparator Function to compare objects from the secondary stream with objects from the primary stream.
   * @param innerJoin If set to true objects from the primary stream will only be included if there is at least one object in the secondary stream to be merged.
   * @param primaryStreamBufferHighThreshold The maximum number of objects from the primary stream to buffer before pausing the primary stream.
   * @param primaryStreamBufferLowThreshold The minimum number of objects in the primary stream buffer before resuming the primary stream.
   * @param secondaryStreamBufferHighThreshold The maximum number of objects from the secondary stream to buffer, in addition to those that match the current primary object, before pausing the secondary stream.
   * @param secondaryStreamBufferLowThreshold The minimum number of objects in the secondary stream buffer, in addition to those that match the current primary object,  before resuming the secondary stream.
   */
  public MergeStream(Context context
          , ReadStream<T> primaryStream
          , ReadStream<U> secondaryStream
          , BiFunction<T, Collection<U>, V> merger
          , BiComparator<T, U> comparator
          , boolean innerJoin
          , int primaryStreamBufferHighThreshold
          , int primaryStreamBufferLowThreshold
          , int secondaryStreamBufferHighThreshold
          , int secondaryStreamBufferLowThreshold
  ) {
    logger.debug("Constructor {} and {}", primaryStream, secondaryStream);
    this.context = context;
    this.primaryStream = primaryStream;
    this.secondaryStream = secondaryStream;
    this.merger = merger;
    this.comparator = comparator;
    this.innerJoin = innerJoin;
    this.primaryStreamBufferHighThreshold = primaryStreamBufferHighThreshold;
    this.primaryStreamBufferLowThreshold = primaryStreamBufferLowThreshold;
    this.secondaryStreamBufferHighThreshold = secondaryStreamBufferHighThreshold;
    this.secondaryStreamBufferLowThreshold = secondaryStreamBufferLowThreshold;
  }
  
  @Override
  public MergeStream<T, U, V> exceptionHandler(Handler<Throwable> handler) {
    this.exceptionHandler = handler;
    primaryStream.exceptionHandler(handler);
    secondaryStream.exceptionHandler(handler);
    return  this;
  }

  @Override
  public MergeStream<T, U, V> handler(Handler<V> handler) {
    this.handler = handler;
    if (handler == null) {
      primaryStream
              .exceptionHandler(null)
              .endHandler(null)
              .handler(null);
      secondaryStream
              .exceptionHandler(null)
              .endHandler(null)
              .handler(null);
    } else {
      primaryStream.endHandler(v -> {
        logger.debug("Ending primary stream");
        synchronized (lock) {
          primaryEnded = true;
        }
        doEmit();
      });
      secondaryStream.endHandler(v -> {
        logger.debug("Ending secondary stream");
        synchronized (lock) {
          secondaryEnded = true;
        }
        doEmit();
      });
      primaryStream.handler(this::handlePrimaryItem);
      secondaryStream.handler(this::handleSecondaryItem);
    }    
    return this;
  }
  
  private void handlePrimaryItem(T item) {
    logger.debug("Handling primary {}", item);
    synchronized (lock) {
      if (currentPrimary == null) {
        currentPrimary = item;
        currentSecondaryRows = new ArrayList<>();
      } else {
        primaryRows.add(item);
        if (primaryRows.size() > this.primaryStreamBufferHighThreshold) {
          logger.debug("Pausing primary stream at {}", primaryRows.size());
          primaryStream.pause();
        }
      }
    }
  }

  private void handleSecondaryItem(U item) {
    logger.debug("Handling secondary {}", item);
    synchronized (lock) {
      if (currentPrimary == null) {
        if (primaryEnded) {
          doEmit();
        } else {
          secondaryRows.add(item);
        }
      } else {
        if (0 == comparator.compare(currentPrimary, item)) {
          currentSecondaryRows.add(item);
        } else {
          secondaryRows.add(item);
          doEmit();
        }
      }
      if (secondaryRows.size() > this.secondaryStreamBufferHighThreshold) {
        logger.debug("Pausing secondary stream at {}", secondaryRows.size());
        secondaryStream.pause();
      }
    }
  }

  private void doEmit() {
    if (emitting.compareAndSet(false, true)) {
      context.runOnContext(this::emit);
    }
  }
  
  private void bringInSecondaries() {
    if (currentPrimary != null) {
      while (!secondaryRows.isEmpty()) {
        U curSec = secondaryRows.peek();
        int compare = this.comparator.compare(currentPrimary, curSec);
        if (compare > 0) {
          logger.debug("Skipping secondary row {} because it is before {}", curSec, currentPrimary);
          secondaryRows.pop();
        } else if (compare == 0) {
          currentSecondaryRows.add(secondaryRows.pop());
        } else {
          break;
        }
      }
    }
  }
  
  private void emit(Void v) {
    boolean moreToDo = true;
    while (moreToDo) {
      Handler<V> capturedHandler = null;
      Handler<Void> capturedEndHandler = null;
      T mergePrimary = null;
      List<U> mergeSecondary = null;

      boolean resumePrimary = false;
      boolean resumeSecondary = false;
      synchronized (lock) {
        logger.info("Current: {} and {}, got {} primary rows{} and {} secondary rows{}"
                , currentPrimary
                , currentSecondaryRows
                , primaryRows.size()
                , primaryEnded ? " (ended)" : ""
                , secondaryRows.size()
                , secondaryEnded ? " (ended)" : ""
        );
        if (demand  != Long.MAX_VALUE) {
          if (demand <= 0) {
            emitting.set(false);
            return ;
          }
        }
        if (primaryRows.isEmpty() && primaryEnded 
                && currentPrimary == null && (currentSecondaryRows == null || currentSecondaryRows.isEmpty())) {
          emitting.set(false);
          moreToDo = false;
          capturedEndHandler = endHandler;
          endHandler = null;
        } else {
          if (!secondaryRows.isEmpty() && (currentSecondaryRows == null || currentSecondaryRows.isEmpty())) {
            bringInSecondaries();
          }
          if (!secondaryRows.isEmpty() || secondaryEnded) {
            mergePrimary = currentPrimary;
            mergeSecondary = currentSecondaryRows;
            capturedHandler = handler;
            currentSecondaryRows = new ArrayList<>();
            if (!primaryRows.isEmpty()) {
              currentPrimary = primaryRows.pop();
              bringInSecondaries();
            } else if (primaryEnded) {
              emitting.set(false);
              moreToDo = false;
              capturedEndHandler = endHandler;
              endHandler = null;
            } else {
              currentPrimary = null;
              emitting.set(false);
              moreToDo = false;
            }
          } else if (!secondaryEnded) {
            emitting.set(false);
            moreToDo = false;
          }
          resumePrimary = !primaryEnded && primaryRows.size() < primaryStreamBufferLowThreshold;
          resumeSecondary = !secondaryEnded && secondaryRows.size() < secondaryStreamBufferLowThreshold;

          if (capturedHandler != null) {
            if (!innerJoin || !mergeSecondary.isEmpty()) {
              if (demand != Long.MAX_VALUE) {
                --demand;
              }
            }
          }
        }
      }
      
      if (capturedHandler != null) {
        if (!innerJoin || !mergeSecondary.isEmpty()) {
          V result = merger.apply(mergePrimary, mergeSecondary);
          logger.debug("Outputting {}", result);
          capturedHandler.handle(result);
        }
      }
      if (capturedEndHandler != null) {
        logger.debug("Ending");
        capturedEndHandler.handle(null);
        primaryStream.handler(null);
        secondaryStream.handler(null);
      } else {
        if (resumePrimary) {
          logger.debug("Resuming primary stream at {} rows", primaryRows.size());
          primaryStream.resume();
        }
        if (resumeSecondary) {
          logger.debug("Resuming secondary stream at {} rows", secondaryRows.size());
          secondaryStream.resume();
        }
      }
    }
  }
  
  @Override
  public MergeStream<T, U, V> pause() {
    primaryStream.pause();
    secondaryStream.pause();
    synchronized (lock) {
      demand = 0;
    }
    return this;
  }

  @Override
  public MergeStream<T, U, V> resume() {
    primaryStream.resume();
    secondaryStream.resume();
    synchronized (lock) {
      demand = Long.MAX_VALUE;
    }
    doEmit();
    return this;
  }

  @Override
  public MergeStream<T, U, V> fetch(long amount) {
    if (amount < 0L) {
      throw new IllegalArgumentException();
    }
    primaryStream.resume();
    secondaryStream.resume();
    synchronized (lock) {
      demand += amount;
      if (demand < 0L) {
        demand = Long.MAX_VALUE;
      }
    }
    doEmit();
    return this;
  }

  @Override
  public MergeStream<T, U, V> endHandler(Handler<Void> endHandler) {
    this.endHandler = endHandler;
    return this;
  }
  
}
