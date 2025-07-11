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
package uk.co.spudsoft.query.exec.procs.sort;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.streams.WriteStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link io.vertx.core.streams.WriteStream} that writes to a file.
 * <P>
 * Each item passed in to the {@link #write(java.lang.Object)} method is converted into a byte array by the Serializer passed in to the constructor.
 * The size of this array, followed by the array itself, are then written to the output file.
 * <P>
 * This process does not use explicitly Java serialization, the {@link Serializer} passed in to the constructor must handle the 
 * full conversion from an object to a byte array.
 *
 * @author jtalbut
 * @param <T> The type of object that will be written to the file.
 */
public class SerializeWriteStream<T> implements WriteStream<T> {

  private static final Logger logger = LoggerFactory.getLogger(SerializeWriteStream.class);
  
  /**
   * Functional interface for converting an item into a byte array that can be written to disc.
   * @param <T> The type of item being serialized.
   */
  public interface Serializer<T> {
    /**
     * Convert the item to a byte array.
     * @param item the item to be converted.
     * @return a byte array suitable for storing on disc.
     * @throws IOException if the conversion cannot be carried out.
     */
    byte[] serialize(T item) throws IOException;
  }
      
  private static final int DEFAULT_BUFFER_SIZE = 1 << 20;
  
  private final AsyncFile file;
  private final Serializer<T> serializer;
  
  private int bufferSize = DEFAULT_BUFFER_SIZE;
  
  private Buffer writeBuffer;
  private int writePos;
  private Promise<Void> writePromise;

  /**
   * Constructor.
   * 
   * The AsyncFile should have been opened with one of the writable OpenOptions.
   * 
   * The serializer will always be called with a single T object and should convert that to a byte array (that will be read by the deserailizer 
   * passed in to a SerializeReadStream).
   * 
   * @param file The AsyncFile that is going to be read.
   * @param serializer Function to convert a single T object into a byte buffer.
   * 
   */
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public SerializeWriteStream(AsyncFile file, Serializer<T> serializer) {
    this.file = file;
    this.serializer = serializer;
  }

  /**
   * Constructor.
   * 
   * The AsyncFile should have been opened with one of the writable OpenOptions.
   * 
   * The serializer will always be called with a single T object and should convert that to a byte array (that will be read by the deserailizer 
   * passed in to a SerializeReadStream).
   * 
   * @param file The AsyncFile that is going to be read.
   * @param serializer Function to convert a single T object into a byte buffer.
   * @param bufferSize The amount of data (bytes) to buffer before writing to the file WriteStream.
   * 
   */
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public SerializeWriteStream(AsyncFile file, Serializer<T> serializer, int bufferSize) {
    this.file = file;
    this.serializer = serializer;
    this.bufferSize = bufferSize;
  }

  static byte[] byteArrayFromInt(int value) {
    return new byte[] {
              (byte) (value >> 24)
            , (byte) (value >> 16)
            , (byte) (value >> 8)
            , (byte) value
    };
  }

  @Override
  public SerializeWriteStream<T> exceptionHandler(Handler<Throwable> hndlr) {
    file.exceptionHandler(hndlr);
    return this;
  }
  
  private Future<Void> writeWriteBuffer(boolean recreate) {
    Buffer buffer = writeBuffer;
    int pos = writePos;
    Promise<Void> promise = writePromise;

    if (recreate) {
      writeBuffer = Buffer.buffer(bufferSize);
      writePos = 0;
      writePromise = Promise.promise();
    } else {
      writeBuffer = null;
      writePos = 0;
      writePromise = null;
    }

    file.write(buffer.slice(0, pos), promise);
    return promise.future();
  }

  @Override
  public Future<Void> write(T t) {
    byte[] serialized;
    try {
      serialized = serializer.serialize(t);
    } catch (Throwable ex) {
      return Future.failedFuture(ex);
    }
    if (writeBuffer == null) {
      writeBuffer = Buffer.buffer(bufferSize);
      writePos = 0;
      writePromise = Promise.promise();
    } 
    int spaceRequired = 4 + serialized.length;
    if (spaceRequired > bufferSize) {
      Buffer buff = Buffer.buffer(4 + spaceRequired);
      buff.appendBytes(byteArrayFromInt(serialized.length));
      buff.appendBytes(serialized);
      if (writePos > 0) {
        return writeWriteBuffer(true)
                .compose(v -> {
                  return file.write(buff);
                });
      } else {
        return file.write(buff);
      }
     } else if (writePos + spaceRequired > bufferSize) {
      // Need to wait for file writes
      Future<Void> result = writeWriteBuffer(true);
      writeBuffer.appendBytes(byteArrayFromInt(serialized.length));
      writeBuffer.appendBytes(serialized);
      writePos += spaceRequired;
      return result;
    } else {
      writeBuffer.appendBytes(byteArrayFromInt(serialized.length));
      writeBuffer.appendBytes(serialized);
      writePos += spaceRequired;
      // Don't need to wait for buffer writes
      return Future.succeededFuture();
    }
  }

  @Override
  public void write(T t, Handler<AsyncResult<Void>> hndlr) {
    Future<Void> f = write(t);
    f.onComplete(hndlr);
  }

  @Override
  public void end(Handler<AsyncResult<Void>> hndlr) {
    logger.trace("endHandler");
    if (writePos > 0) {
      writeWriteBuffer(false)
              .andThen(ar -> {
                if (ar.succeeded()) {
                  file.end(hndlr);
                } else {
                  hndlr.handle(ar);
                }
              });
    } else {
      file.end(hndlr);
    }
  }

  /**
   * Set the maximum size of the write queue to {@code maxSize}. You will still be able to write to the stream even
   * if there is more than {@code maxSize} items in the write queue. This is used as an indicator to provide flow control.
   * <p>
   * The value is defined by the implementation of the stream, in this case it is measured in bytes, not items.
   * </p>
   * 
   * @param maxSize  the max size of the write stream in bytes
   * @return a reference to this, so the API can be used fluently
   */
  @Override
  public SerializeWriteStream<T> setWriteQueueMaxSize(int maxSize) {
    file.setWriteQueueMaxSize(maxSize);
    return this;
  }

  @Override
  public boolean writeQueueFull() {
    return file.writeQueueFull();
  }

  @Override
  public SerializeWriteStream<T> drainHandler(Handler<Void> hndlr) {
    file.drainHandler(hndlr);
    return this;
  }
  
}
