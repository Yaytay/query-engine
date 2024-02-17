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
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.streams.WriteStream;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jtalbut
 * @param <T> The type of object that will be written to the file.
 */
public class SerializeWriteStream<T> implements WriteStream<T> {

  private static final Logger logger = LoggerFactory.getLogger(SerializeWriteStream.class);
  
  private final AsyncFile file;
  private final Function<T, byte[]> serializer;

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
  public SerializeWriteStream(AsyncFile file, Function<T, byte[]> serializer) {
    this.file = file;
    this.serializer = serializer;
  }

  @Override
  public WriteStream<T> exceptionHandler(Handler<Throwable> hndlr) {
    file.exceptionHandler(hndlr);
    return this;
  }

  @Override
  public Future<Void> write(T t) {
    byte[] serialized;
    try {
      serialized = serializer.apply(t);
    } catch (Throwable ex) {
      return Future.failedFuture(ex);
    }
    logger.debug("Serialized size: {}", serialized.length);
    Buffer buff = Buffer.buffer(4 + serialized.length);
    buff.appendInt(serialized.length);
    buff.appendBytes(serialized);
    return file.write(buff);    
  }

  @Override
  public void write(T t, Handler<AsyncResult<Void>> hndlr) {
    Future<Void> f = write(t);
    hndlr.handle(f);
  }

  @Override
  public void end(Handler<AsyncResult<Void>> hndlr) {
    file.end(hndlr);
  }

  @Override
  public WriteStream<T> setWriteQueueMaxSize(int i) {
    file.setWriteQueueMaxSize(i);
    return this;
    
  }

  @Override
  public boolean writeQueueFull() {
    return file.writeQueueFull();
  }

  @Override
  public WriteStream<T> drainHandler(Handler<Void> hndlr) {
    file.drainHandler(hndlr);
    return this;
  }
  
}
