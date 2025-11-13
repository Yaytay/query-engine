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
package uk.co.spudsoft.query.exec.fmts;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.streams.ReadStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.logging.Log;

/**
 * Utility class for consuming an entire ReadStream and putting the contents into a List.
 * <p>
 * This class inherently breaks streaming, it is used for some "setup" queries as part of the pipeline but must not be used for the primary streams.
 *
 * @author jtalbut
 */
public class ReadStreamToList {
  
  private static final Logger logger = LoggerFactory.getLogger(ReadStreamToList.class);

  private ReadStreamToList() {
  }
  
  /**
   * Capture all values from a {@link ReadStream} as a {@link List}.
   * @param <T> The type of item be read from the ReadStream.
   * @param pipelineContext The context in which this {@link SourcePipeline} is being run.
   * @param input The ReadStream supplying the items.
   * @return A Future that will be completed with a {@link List} of items when the ReadStream has ended.
   */
  public static <T> Future<List<T>> capture(PipelineContext pipelineContext, ReadStream<T> input) {
    return captureByBatch(pipelineContext, input, 0, 0);
  }
  
  /**
   * Map all values from a {@link ReadStream} via a mapping function and store the resulting items as a {@link List}.
   * @param <T> The type of item be read from the ReadStream.
   * @param <U> The type of item be written to the {@link List}.
   * @param pipelineContext The context in which this {@link SourcePipeline} is being run.
   * @param input The ReadStream supplying the items.
   * @param mapper Function to convert input items to the objects that will be stored in the {@link List}.
   * @return A Future that will be completed with a {@link List} of items when the ReadStream has ended.
   */
  public static <T, U> Future<List<U>> map(PipelineContext pipelineContext, ReadStream<T> input, Function<T, U> mapper) {
    Promise<List<U>> promise = Promise.promise();
    List<U> collected = new ArrayList<>();
    
    Log log = new Log(logger, pipelineContext);
    
    input.endHandler(v -> {
      promise.tryComplete(collected);
    }).exceptionHandler(ex -> {
      log.warn().log("Exception capturing stream: {}", ex);
      promise.tryFail(ex);
    }).handler(item -> {
      try {
        U value = mapper.apply(item);
        collected.add(value);
      } catch (Throwable ex) {
        log.warn().log("Failed to map value ({}): ", item, ex);
      }
    });
    
    input.resume();

    return promise.future();
  }
  
  /**
   * Capture values from a {@link ReadStream} to a {@link List}, requesting items from the input stream in chunks to avoid filling memory.
   * @param <T> The type of item be read from the ReadStream.
   * @param pipelineContext The context in which this {@link SourcePipeline} is being run.
   * @param input The ReadStream supplying the items.
   * @param initialFetch The number of items to request on the first call.
   * @param subsequentFetch The number of items to request in the handler if the number of items collected is a multiple of this number.
   * @return A Future that will be completed with a {@link List} of items when the ReadStream has ended.
   */
  public static <T> Future<List<T>> captureByBatch(PipelineContext pipelineContext, ReadStream<T> input, int initialFetch, int subsequentFetch) {
    Promise<List<T>> promise = Promise.promise();
    List<T> collected = new ArrayList<>();
    
    Log log = new Log(logger, pipelineContext);
    
    input.endHandler(v -> {
      promise.tryComplete(collected);
    }).exceptionHandler(ex -> {
      promise.tryFail(ex);
    }).handler(item -> {
      log.debug().log("Got item: {}", item);
      collected.add(item);
      if (subsequentFetch > 0 && (collected.size() % subsequentFetch == 0)) {
        input.fetch(subsequentFetch);
      }
    });
    
    if (initialFetch > 0) {
      input.fetch(initialFetch);
    } else {
      input.resume();
    }
    
    return promise.future();
  }
  
}
