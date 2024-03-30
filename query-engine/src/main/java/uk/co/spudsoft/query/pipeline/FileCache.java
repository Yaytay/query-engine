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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.dircache.DirCacheTree;

/**
 *
 * @author njt
 * @param <T>
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
          , int maximumSize
          , int maxDurationMs
  ) {
    this.fs = fs;
    CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder()
            .recordStats();

    if (maxDurationMs <= 0) {
      cacheBuilder.maximumSize(1);
    } else {
      cacheBuilder.maximumSize(maximumSize)
              .expireAfterWrite(maxDurationMs, TimeUnit.MILLISECONDS);
    }

    this.cache = cacheBuilder.build();
    if (meterRegistry != null) {
      GuavaCacheMetrics.monitor(meterRegistry, cache, name);
    }
  }
  
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

  public long size() {
    return cache.size();
  }
}
