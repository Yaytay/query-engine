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
   * @param input The ReadStream supplying the items.
   * @return A Future that will be completed with a {@link List} of items when the ReadStream has ended.
   */
  public static <T> Future<List<T>> capture(ReadStream<T> input) {
    return captureByBatch(input, 0, 0);
  }
  
  /**
   * Map all values from a {@link ReadStream} via a mapping function and store the resulting items as a {@link List}.
   * @param <T> The type of item be read from the ReadStream.
   * @param <U> The type of item be written to the {@link List}.
   * @param input The ReadStream supplying the items.
   * @param mapper Function to convert input items to the objects that will be stored in the {@link List}.
   * @return A Future that will be completed with a {@link List} of items when the ReadStream has ended.
   */
  public static <T, U> Future<List<U>> map(ReadStream<T> input, Function<T, U> mapper) {
    Promise<List<U>> promise = Promise.promise();
    List<U> collected = new ArrayList<>();
    
    input.endHandler(v -> {
      promise.complete(collected);
    }).exceptionHandler(ex -> {
      promise.fail(ex);
    }).handler(item -> {
      collected.add(mapper.apply(item));
    });
    
    input.resume();

    return promise.future();
  }
  
  /**
   * Capture values from a {@link ReadStream} to a {@link List}, requesting items from the input stream in chunks to avoid filling memory.
   * @param <T> The type of item be read from the ReadStream.
   * @param input The ReadStream supplying the items.
   * @param initialFetch The number of items to request on the first call.
   * @param subsequentFetch The number of items to request in the handler if the number of items collected is a multiple of this number.
   * @return A Future that will be completed with a {@link List} of items when the ReadStream has ended.
   */
  public static <T> Future<List<T>> captureByBatch(ReadStream<T> input, int initialFetch, int subsequentFetch) {
    Promise<List<T>> promise = Promise.promise();
    List<T> collected = new ArrayList<>();
    
    input.endHandler(v -> {
      promise.complete(collected);
    }).exceptionHandler(ex -> {
      promise.fail(ex);
    }).handler(item -> {
      logger.debug("Got item: {}", item);
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
