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
package uk.co.spudsoft.query.exec;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.WriteStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WriteStream that sends all data to a file as well as to another WriteStream.
 * 
 * If anything goes wrong whilst writing to the file it will be silently dropped and the existing file will be deleted.
 * 
 * @author jtalbut
 */
public final class CachingWriteStream implements WriteStream<Buffer> {
  
  private static final Logger logger = LoggerFactory.getLogger(CachingWriteStream.class);
  
  private final FileSystem fileSystem;
  private final String cacheFile;
  private WriteStream<Buffer> cacheStream;
  private final WriteStream<Buffer> destStream;
  
  private final Object lock = new Object();
  private Handler<Void> drainHandler;
  private Handler<Throwable> exceptionHandler;

  /**
   * Constructor.
   * @param fileSystem The Vert.x FileSystem for working with files.
   * @param cacheFile The path to the file to cache the stream to.
   * @param cacheStream The stream to cache.
   * @param destStream The output stream.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "It does store external mutable objects.")
  public  CachingWriteStream(FileSystem fileSystem, String cacheFile, WriteStream<Buffer> cacheStream, WriteStream<Buffer> destStream) {
    this.fileSystem = fileSystem;
    this.cacheFile = cacheFile;
    this.cacheStream = cacheStream;
    this.destStream = destStream;

    cacheStream.drainHandler(this::internalDrainHandler);
    destStream.drainHandler(this::internalDrainHandler);
    cacheStream.exceptionHandler(ex -> {
      internalExceptionHandler(false, ex);
    });
    destStream.exceptionHandler(ex -> {
      internalExceptionHandler(true, ex);
    });
  }
  
  /**
   * Factory method for creating a fully initialized CachingWriteStream.
   * 
   * There is absolutely no obligation to call this method, it's just a helper.
   * 
   * @param vertx The Vertx instance to use to obtain the {@link io.vertx.core.file.FileSystem}
   * @param destStream The primary destination stream.
   * @param filename The file to write the secondary stream to.
   * @return A Future containing an initialized CachingWriteStream.
   */
  public static Future<CachingWriteStream> cacheStream(Vertx vertx, WriteStream<Buffer> destStream, String filename) {
    FileSystem fileSystem = vertx.fileSystem();
    return fileSystem.open(filename, new OpenOptions().setCreateNew(true).setWrite(true))
            .map(asyncFile -> {
              CachingWriteStream cachingStream = new CachingWriteStream(fileSystem, filename, asyncFile, destStream);
              return cachingStream;
    });
  }
    
  @Override
  public WriteStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
    synchronized (lock) {
      this.exceptionHandler = handler;
    }
    return this;
  }

  @Override
  public Future<Void> write(Buffer data) {
    logger.trace("Writing {}", data);
    return Future.all(
            cacheStream.write(data).recover(ex -> {
              logger.warn("Failed to write to cache stream: ", ex);
              internalExceptionHandler(false, ex);
              return Future.succeededFuture();
            })
            , destStream.write(data)
    ).mapEmpty();
  }

  @Override
  public void write(Buffer data, Handler<AsyncResult<Void>> handler) {
    this.write(data).andThen(handler);
  }

  @Override
  public void end(Handler<AsyncResult<Void>> handler) {
    cacheStream.end(ar -> {
      if (ar.failed()) {
        fileSystem.delete(cacheFile);
      }
    });
    destStream.end(handler);
  }

  @Override
  public WriteStream<Buffer> setWriteQueueMaxSize(int maxSize) {
    cacheStream.setWriteQueueMaxSize(maxSize);
    destStream.setWriteQueueMaxSize(maxSize);
    return this;
  }

  @Override
  public boolean writeQueueFull() {
    return cacheStream.writeQueueFull() || destStream.writeQueueFull();
  }

  private void internalDrainHandler(Void v) {
    if (!writeQueueFull()) {
      Handler<Void> handler;
      synchronized (lock) {
        handler = drainHandler;
      }
      if (handler != null) {
        handler.handle(null);
      }
    }
  }
  
  private void internalExceptionHandler(boolean destFailed, Throwable ex) {
    // Exception one of the streams - delete the cache file and stop caching
    // If the problem was with the destStream report it to the client
    WriteStream<Buffer> stream;
    Handler<Throwable> handler = null;
    synchronized (lock) {
      stream = cacheStream;
      cacheStream = new NullWriteStream<>();
      if (destFailed) {
        handler = exceptionHandler;
      }
    }
    stream.end().andThen(ar -> {
      fileSystem.delete(cacheFile);
    });
    if (handler != null) {
      handler.handle(ex);
    }
  }
  
  @Override
  public WriteStream<Buffer> drainHandler(Handler<Void> handler) {
    synchronized (lock) {
      this.drainHandler = handler;
    }
    internalDrainHandler(null);
    return this;
  }
  
}
