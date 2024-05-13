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
package uk.co.spudsoft.query.pipeline;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.GuavaCacheMetrics;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.dircache.DirCacheTree;

/**
 * Cache of data associated with each file.
 * @author jtalbut
 * @param <T> The type of data associated with files.
 */
public class FileCache<T> {  
  
  private static final Logger logger = LoggerFactory.getLogger(FileCache.class);
  
  private final FileSystem fs;
  private final Cache<DirCacheTree.File, T> cache;
    
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "FileSystem object should be considered immutable (though, of course, the file system isn't).")
  public FileCache(
          FileSystem fs
          , MeterRegistry meterRegistry
          , String name
          , Integer maximumSize
          , Duration maxDuration
  ) {
    this.fs = fs;
    CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder()
            .recordStats();

    if (maxDuration != null && maxDuration.isZero()) {
      cacheBuilder.maximumSize(1);
    } else {
      if (maximumSize != null) {
        cacheBuilder.maximumSize(maximumSize);
      }
      if (maxDuration != null) {
        cacheBuilder.expireAfterWrite(maxDuration);
      }
    }

    this.cache = cacheBuilder.build();
    if (meterRegistry != null) {
      GuavaCacheMetrics.monitor(meterRegistry, cache, name);
    }
  }
  
  /**
   * Get an item from the cache, loading it dynamically if it is not in the cache.
   * @param file The file to use as the cache key.
   * @param mapper Function to convert the loaded file into an object of type T.
   * @return A Future that will be completed either when the  file is found in the cache or when the mapper returns a value.
   */
  public Future<T> get(DirCacheTree.File file, Function<Buffer, T> mapper) {
    T cached = cache.getIfPresent(file);
    Future<T> resultFuture;
    if (cached != null) {
      resultFuture = Future.succeededFuture(cached);
    } else {
      logger.trace("Loading file {}", file);
      resultFuture = fs.readFile(file.getPath().toString())
            .compose(buffer -> {
              try {
                T result = mapper.apply(buffer);
                cache.put(file, result);
                return Future.succeededFuture(result);
              } catch (Throwable ex) {
                return Future.failedFuture(ex);
              }
            });
    }
    return resultFuture;
  }
  
  /**
   * Purge invalid items from the cache.
   * @param validKeys Set of keys that are known to be valid.
   */
  public void purge(Set<DirCacheTree.File> validKeys) {
    cache.cleanUp();
    
    Map<DirCacheTree.File, T> map = cache.asMap();
    for (Iterator<DirCacheTree.File> iter = map.keySet().iterator(); iter.hasNext();) {
      DirCacheTree.File file = iter.next();
      if (!validKeys.contains(file)) {
        iter.remove();
      }
    }
  }

  /**
   * The number of items in the cache.
   * @return number of items in the cache.
   */
  public long size() {
    return cache.size();
  }
}
