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

/**
 *
 * @author jtalbut
 */
public class ReadStreamToList {
  
  public static <T> Future<List<T>> capture(ReadStream<T> input) {
    return captureByBatch(input, 0, 0);
  }
  
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
  
  public static <T> Future<List<T>> captureByBatch(ReadStream<T> input, int initialFetch, int subsequentFetch) {
    Promise<List<T>> promise = Promise.promise();
    List<T> collected = new ArrayList<>();
    
    input.endHandler(v -> {
      promise.complete(collected);
    }).exceptionHandler(ex -> {
      promise.fail(ex);
    }).handler(item -> {
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
